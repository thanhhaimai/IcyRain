package mai.icyrain.bluetooth;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * This thread runs while attempting to make an outgoing connection
 * with a device. It runs straight through; the connection either
 * succeeds or fails.
 * 
 * @author thanhhaipmai@gmail.com (Thanh Hai Mai)
 */
class RequestConnectionThread extends Thread {
  private final BluetoothAdapter mAdapter;

  private final BluetoothDevice mDevice;
  private final BluetoothSocket mSocket;
  private final String TAG;

  public RequestConnectionThread(BluetoothDevice device, BluetoothAdapter adapter, UUID myUUID) {
    this(device, adapter, myUUID, "BTRequestConnectionThread");
  }

  public RequestConnectionThread(BluetoothDevice device, BluetoothAdapter adapter, UUID myUUID,
    String tag) {
    TAG = tag;
    mDevice = device;
    mAdapter = adapter;
    BluetoothSocket tmp = null;

    // Get a BluetoothSocket for a connection with the
    // given BluetoothDevice
    try {
      tmp = device.createRfcommSocketToServiceRecord(myUUID);
    } catch (final IOException e) {
      Log.e(TAG, "create() failed", e);
    }
    mSocket = tmp;
  }

  public void cancel() {
    try {
      mSocket.close();
    } catch (final IOException e) {
      Log.e(TAG, "close() of connect socket failed", e);
    }
  }

  @Override
  public void run() {
    Log.i(TAG, "BEGIN mConnectThread");
    setName("ConnectThread");

    // Always cancel discovery because it will slow down a connection
    mAdapter.cancelDiscovery();

    // Make a connection to the BluetoothSocket
    try {
      // This is a blocking call and will only return on a
      // successful connection or an exception
      mSocket.connect();
    } catch (final IOException e) {
      BluetoothService.getInstance().connectionFailed();
      // Close the socket
      try {
        mSocket.close();
      } catch (final IOException e2) {
        Log.e(TAG, "unable to close() socket during connection failure", e2);
      }
      return;
    }

    // Start the connected thread
    BluetoothService.getInstance().startConnectedThread(mSocket, mDevice);
  }
}
