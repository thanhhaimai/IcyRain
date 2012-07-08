package mai.icyrain.bluetooth;

import java.util.UUID;

import mai.icyrain.IcyRain;
import mai.icyrain.MessageOpCode;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * The main Bluetooth communication service. Provides a simple interface for communicating with
 * other bluetooth devices through its singleton instance.
 * 
 * @author thanhhaipmai@gmail.com (Thanh Hai Mai)
 */
public class BluetoothService {
  // Singleton instance.
  public static BluetoothService mInstance;

  // Unique UUID for this application.
  // For android - android connection, you can use any unique identifier.
  // For android - bluesmirf connection, you must use the following.
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  // Name for the SDP record when creating server socket
  private static final String NAME = "IcyRain";

  // For debug print.
  private static final String TAG = "BluetoothService";

  /**
   * @return the singleton of BluetoothService.
   */
  public synchronized static BluetoothService getInstance() {
    if (mInstance == null) {
      mInstance = new BluetoothService();
    }
    return mInstance;
  }

  // Member fields
  private final BluetoothAdapter mAdapter;

  private Handler mHandler = null;
  private ListenConnectionThread mListenConnectionThread;
  private RequestConnectionThread mRequestConnectionThread;
  private ConnectedThread mConnectedThread;

  private ConnectionState mState;

  private BluetoothService() {
    mAdapter = BluetoothAdapter.getDefaultAdapter();
    mState = ConnectionState.IDLE;
  }

  /**
   * Start the ConnectThread to initiate a connection to a remote device.
   * 
   * @param device The BluetoothDevice to connect
   */
  public synchronized void connect(BluetoothDevice device) {
    Log.d(TAG, "connect to: " + device);

    // Cancel any thread attempting to make a connection
    if (mState == ConnectionState.CONNECTING) {
      cancelRequestConnectionThread();
    }

    // Cancel any thread currently running a connection
    cancelConnectedThread();

    // Start the thread to connect with the given device
    mRequestConnectionThread = new RequestConnectionThread(device, mAdapter, MY_UUID);
    mRequestConnectionThread.start();
    setState(ConnectionState.CONNECTING);
  }

  /**
   * Set the default handler to return data to the UI.
   */
  public synchronized void setDefaultHandler(Handler handler) {
    mHandler = handler;
  }

  /**
   * Start the bluetooth service. Specifically start ListenConnectionThread to begin a session in
   * listening (server) mode. Called by the Activity onResume().
   */
  public synchronized void startListenForConnection() {
    Log.d(TAG, "start");

    // Cancel any thread attempting to make a connection
    cancelRequestConnectionThread();
    // Cancel any thread currently running a connection
    cancelConnectedThread();

    // Start the thread to listen on a BluetoothServerSocket
    if (mListenConnectionThread == null) {
      mListenConnectionThread = new ListenConnectionThread(mAdapter, MY_UUID, NAME);
      mListenConnectionThread.start();
    }
    setState(ConnectionState.LISTEN);
  }

  /**
   * Stop all threads
   */
  public synchronized void stop() {
    Log.d(TAG, "stop");
    cancelRequestConnectionThread();
    cancelConnectedThread();
    cancelListenConnectionThread();
    setState(ConnectionState.IDLE);
  }

  /**
   * Write to the ConnectedThread in an unsynchronized manner
   * 
   * @param data The bytes to write
   * @see ConnectedThread#write(byte[])
   */
  public void write(byte[] data, MessageOpCode opCode) {
    // Create temporary object
    ConnectedThread r;
    // Synchronize a copy of the ConnectedThread
    synchronized (this) {
      if (mState != ConnectionState.CONNECTED) {
        return;
      }
      r = mConnectedThread;
    }
    // Perform the write unsynchronized
    r.write(data, opCode.ordinal());
  }

  /**
   * Return the current connection state.
   */
  public synchronized ConnectionState getState() {
    return mState;
  }

  /**
   * Set the current state of the bluetooth connection.
   * 
   * @param state An integer defining the current connection state
   */
  synchronized void setState(ConnectionState state) {
    Log.d(TAG, "setState() " + mState + " -> " + state);
    mState = state;

    // Give the new state to the Handler so the UI Activity can update
    mHandler.obtainMessage(HandlerMessage.STATE_CHANGE, state.ordinal(), -1).sendToTarget();
  }

  synchronized void cancelConnectedThread() {
    if (mConnectedThread != null) {
      mConnectedThread.cancel();
      mConnectedThread = null;
    }
  }

  synchronized void cancelListenConnectionThread() {
    if (mListenConnectionThread != null) {
      mListenConnectionThread.cancel();
      mListenConnectionThread = null;
    }
  }

  synchronized void cancelRequestConnectionThread() {
    if (mRequestConnectionThread != null) {
      mRequestConnectionThread.cancel();
      mRequestConnectionThread = null;
    }
  }

  /**
   * Start the ConnectedThread to begin managing a Bluetooth connection.
   * 
   * @param socket The BluetoothSocket on which the connection was made
   * @param device The BluetoothDevice that has been connected
   */
  synchronized void startConnectedThread(BluetoothSocket socket, BluetoothDevice device) {
    Log.d(TAG, "connected");

    // Cancel any thread currently running a connection
    cancelConnectedThread();

    // Cancel the accept thread because we only want to connect to one device
    cancelListenConnectionThread();

    // Kill the thread that completed the connection
    mRequestConnectionThread = null;

    // Start the thread to manage the connection and perform transmissions
    mConnectedThread = new ConnectedThread(socket, mHandler);
    mConnectedThread.start();

    // Send the name of the connected device back to the UI Activity
    final Message msg = mHandler.obtainMessage(HandlerMessage.DEVICE_NAME);
    final Bundle bundle = new Bundle();
    bundle.putString(IcyRain.DEVICE_NAME, device.getName());
    msg.setData(bundle);
    mHandler.sendMessage(msg);

    setState(ConnectionState.CONNECTED);
  }

  /**
   * Indicate that the connection attempt failed and notify the UI Activity.
   */
  void connectionFailed() {
    setState(mListenConnectionThread != null ? ConnectionState.LISTEN : ConnectionState.IDLE);

    // Send a failure message back to the Activity
    final Message msg = mHandler.obtainMessage(HandlerMessage.TOAST);
    final Bundle bundle = new Bundle();
    bundle.putString(IcyRain.TOAST, "Unable to connect device, current state is " + mState.name());
    msg.setData(bundle);
    mHandler.sendMessage(msg);
  }

  /**
   * Indicate that the connection was lost and notify the UI Activity.
   */
  void connectionLost() {
    setState(mListenConnectionThread != null ? ConnectionState.LISTEN : ConnectionState.IDLE);

    // Send a failure message back to the Activity
    final Message msg = mHandler.obtainMessage(HandlerMessage.TOAST);
    final Bundle bundle = new Bundle();
    bundle
      .putString(IcyRain.TOAST, "Device connection was lost, current state is " + mState.name());
    msg.setData(bundle);
    mHandler.sendMessage(msg);
  }
}
