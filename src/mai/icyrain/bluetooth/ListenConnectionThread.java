package mai.icyrain.bluetooth;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * This thread runs while listening for incoming connections. It behaves
 * like a server-side client. It runs until a connection is accepted
 * (or until cancelled).
 * 
 * @author thanhhaipmai@gmail.com (Thanh Hai Mai)
 */
class ListenConnectionThread extends Thread {
  private final BluetoothAdapter mAdapter;

  private final BluetoothServerSocket mServerSocket;
  private final String TAG;

  public ListenConnectionThread(BluetoothAdapter adapter, UUID myUUID, String broadCastName) {
    this(adapter, myUUID, broadCastName, "BTListenConnectionThread");
  }

  public ListenConnectionThread(BluetoothAdapter adapter, UUID myUUID, String broadCastName,
    String tag) {
    TAG = tag;
    mAdapter = adapter;
    BluetoothServerSocket tmp = null;

    // Create a new listening server socket
    try {
      tmp = mAdapter.listenUsingRfcommWithServiceRecord(broadCastName, myUUID);
    } catch (final IOException e) {
      Log.e(TAG, "listen() failed", e);
    }
    mServerSocket = tmp;
  }

  public void cancel() {
    Log.d(TAG, "cancel " + this);
    try {
      mServerSocket.close();
    } catch (final IOException e) {
      Log.e(TAG, "close() of server failed", e);
    }
  }

  @Override
  public void run() {
    Log.d(TAG, "BEGIN mAcceptThread" + this);
    setName("AcceptThread");
    BluetoothSocket socket = null;

    final ConnectionState state = BluetoothService.getInstance().getState();

    // Listen to the server socket if we're not connected
    while (state != ConnectionState.CONNECTED) {
      try {
        // This is a blocking call and will only return on a
        // successful connection or an exception
        socket = mServerSocket.accept();
      } catch (final IOException e) {
        Log.e(TAG, "accept() failed", e);
        break;
      }

      // If a connection was accepted
      if (socket != null) {
        switch (state) {
        case LISTEN:
        case CONNECTING:
          // Situation normal. Start the connected thread.
          BluetoothService.getInstance().cancelRequestConnectionThread();
          BluetoothService.getInstance().startConnectedThread(socket, socket.getRemoteDevice());
          break;
        case IDLE:
        case CONNECTED:
          // Either not ready or already connected. Terminate new socket.
          try {
            socket.close();
          } catch (final IOException e) {
            Log.e(TAG, "Could not close 7unwanted socket", e);
          }
          break;
        }
      }
    }
    Log.i(TAG, "END mAcceptThread");
  }
}
