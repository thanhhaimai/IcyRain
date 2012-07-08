package mai.icyrain.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

/**
 * This thread runs during a connection with a remote device.
 * It handles all incoming and outgoing transmissions.
 * 
 * @author thanhhaipmai@gmail.com (Thanh Hai Mai)
 */
class ConnectedThread extends Thread {
  private final Handler mHandler;
  private final InputStream mInStream;
  private final OutputStream mOutStream;
  private final BluetoothSocket mSocket;

  private final String TAG;

  public ConnectedThread(BluetoothSocket socket, Handler handler) {
    this(socket, handler, "BTConnectedThread");
  }

  public ConnectedThread(BluetoothSocket socket, Handler handler, String tag) {
    TAG = tag;
    Log.d(TAG, "create ConnectedThread");

    mSocket = socket;
    mHandler = handler;
    InputStream tmpIn = null;
    OutputStream tmpOut = null;

    // Get the BluetoothSocket input and output streams
    try {
      tmpIn = socket.getInputStream();
      tmpOut = socket.getOutputStream();
    } catch (final IOException e) {
      Log.e(TAG, "temp sockets not created", e);
    }

    mInStream = tmpIn;
    mOutStream = tmpOut;
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
    Log.i(TAG, "BEGIN mConnectedThread");
    final byte[] data = new byte[128];
    int size = 0;
    int index = 0;
    int opCode = 0;

    // Keep listening to the InputStream while connected
    while (true) {
      try {
        if (size == 0) {
          // We haven't read anything yet, so lets start looking for a header
          if (mInStream.available() >= 3) {
            // We have enough data for a header and size, lets check if the header is valid.
            if (mInStream.read() != 0x2F) {
              continue;
            }
            if (mInStream.read() != 0x75) {
              continue;
            }

            // The header is valid, now we need to get the opCode and the payload size.
            opCode = (byte) mInStream.read();
            size = (byte) mInStream.read();
            // Reset the index
            index = 0;
          }
        } else {
          // We got the header and size, lets get the data
          if (mInStream.available() > 0) {
            index += mInStream.read(data, index, size - index);
          }

          if (index != size) {
            // We haven't got all of the data yet, lets loop again.
            continue;
          }

          // We got the data, but we need to check if it passes the checksum
          byte checkSum = (byte) mInStream.read();
          for (int i = 0; i < size; i++) {
            checkSum ^= data[i];
          }
          if (checkSum == size) {
            // The data is what we look for, send the obtained bytes to the UI Activity
            mHandler.obtainMessage(HandlerMessage.READ, size, opCode, data.clone()).sendToTarget();
            size = 0;
          } else {
            // Too bad, but we need to discard everything and start again.
            final String error = "Checksum error, expect " + size + " but get " + checkSum;
            mHandler.obtainMessage(HandlerMessage.READ, error.length(), -1, error.getBytes())
              .sendToTarget();
            size = 0;
          }
        }

      } catch (final IOException e) {
        Log.e(TAG, "disconnected", e);
        BluetoothService.getInstance().connectionLost();
        break;
      }
    }
  }

  /**
   * Write to the connected OutStream.
   * 
   * @param data The bytes to write
   */
  public void write(byte[] data, int opCode) {
    try {
      // buffer.length must be less than 128 bytes
      final byte size = (byte) data.length;
      byte checkSum = size;
      final byte[] header = new byte[] { 0x2F, 0x75 };

      // sizeof(header) = 2, sizeof(size) = 1, sizeof(serviceCode) = 1, sizeof(checkSum) = 1
      final ByteBuffer buff = ByteBuffer.allocate(data.length + 2 + 1 + 1 + 1);

      // Write header, then size, then data, then checkSum
      buff.put(header);
      buff.put((byte) opCode);
      buff.put(size);
      for (final byte element : data) {
        checkSum ^= element;
        buff.put(element);
      }
      buff.put(checkSum);
      mOutStream.write(buff.array());

      // Share the sent message back to the UI Activity
      mHandler.obtainMessage(HandlerMessage.WRITE, -1, -1, data).sendToTarget();
    } catch (final IOException e) {
      Log.e(TAG, "Exception during write", e);
    }
  }
}
