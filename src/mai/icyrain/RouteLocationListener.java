/**
 */
package mai.icyrain;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import mai.icyrain.bluetooth.BluetoothService;
import mai.icyrain.bluetooth.ConnectionState;
import mai.icyrain.bluetooth.HandlerMessage;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.RouteResponse;
import com.mapquest.android.maps.RouteResponse.Route.Leg;
import com.mapquest.android.maps.RouteResponse.Route.Leg.Maneuver;

/**
 * @author achalddave@live.com (Achal Dave)
 */
public class RouteLocationListener extends Activity implements LocationListener {

  private static final boolean D = true;
  public static final String DEVICE_NAME = "device_name";

  // Intent request codes
  private static final int REQUEST_CONNECT_DEVICE = 1;
  private static final int REQUEST_ENABLE_BT = 2;
  public static final String SMIRF_MAC = "00:06:66:45:02:5A";

  // Debugging
  private static final String TAG = "IcyRain";
  public static final String TOAST = "toast";
  protected MapView mapView;

  // Local Bluetooth adapter
  protected BluetoothAdapter mBluetoothAdapter = null;

  // Member object for the bluetooth services
  protected BluetoothService mBluetoothService = null;

  // Name of the connected device
  protected String mConnectedDeviceName = null;

  // The Handler that gets information back from the BluetoothService
  private final GestureHandler mHandler = new GestureHandler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
      case HandlerMessage.STATE_CHANGE:
        if (D) {
          Log.i(TAG, "HandlerMessage.STATE_CHANGE: " + msg.arg1);
        }
        final ConnectionState state = ConnectionState.values()[msg.arg1];
        switch (state) {
        case CONNECTED:
          break;
        case CONNECTING:
          break;
        case LISTEN:
          break;
        case IDLE:
          break;
        }
        break;
      case HandlerMessage.WRITE:
        final byte[] writeBuf = (byte[]) msg.obj;
        new String(writeBuf);
        break;
      case HandlerMessage.READ:
        final byte[] readBuf = (byte[]) msg.obj;
        ByteBuffer buffer = ByteBuffer.wrap(readBuf);
        float p = buffer.getFloat();
        float r = buffer.getFloat();
        float y = buffer.getFloat();
        UpdateData(p, r, y);
        break;
      case HandlerMessage.DEVICE_NAME:
        // save the connected device's name
        mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
        Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName,
          Toast.LENGTH_SHORT).show();
        break;
      case HandlerMessage.TOAST:
        Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
          .show();
        break;
      }
    }
  };

  private Maneuver currManeuver;
  private int currManeuverIndex;
  private List<Maneuver> maneuvers;

  public RouteLocationListener(RouteResponse routeResponse) {
    if (!mBluetoothAdapter.isEnabled()) {
      final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
      // Otherwise, setup the bluetooth session
    } else {
      if (mBluetoothService == null) {
        setupBluetooth();
      }
    }
    List<Leg> legs = routeResponse.route.legs;
    if (legs != null) {
      for (Leg leg : legs) {
        List<Maneuver> maneuvers = leg.maneuvers;
        this.maneuvers = maneuvers;
        if (maneuvers != null) {
          int i = 1;
          for (Maneuver maneuver : maneuvers) {
            // Log.d("pullLocation", "maneuver " + i + " is " +
            // maneuver.startPoint.toString());
            if (!maneuver.directionName.equals("")) {
              Log.d("pullLocation",
                "turn " + maneuver.directionName + " at " + maneuver.startPoint.getLatitude()
                  + ", " + maneuver.startPoint.getLongitude());
            }
          }
          currManeuverIndex = 0;
          currManeuver = maneuvers.get(currManeuverIndex);
        }
      }
    } else {
      Log.d("pullLocation", "legs is null");
    }
  }

  @Override
  public void onLocationChanged(Location arg0) {
    double currLat = arg0.getLatitude();
    double currLng = arg0.getLongitude();
    Log.d("pullLocation", "My location is now " + currLat + ", " + currLng);

    double maneuverLat = currManeuver.startPoint.getLatitude();
    double maneuverLng = currManeuver.startPoint.getLongitude();
    float[] resultArray = new float[1];

    Log.d("pullLocation", "Maneuver location is " + currManeuver.startPoint.toString());
    Location.distanceBetween(currLat, currLng, maneuverLat, maneuverLng, resultArray);
    float distance = resultArray[0];
    Log.d("pullLocation", "Distance between is " + distance);
    if (distance < 80) {
      // TODO HAI: IMPLEMENT SENDING ARDUINO NOTIFICATIONS
      // it's time...
      int maneuverTurn = currManeuver.turnType;
      int direction = turnToDirectionInt(maneuverTurn);

      sendMessage("1011");
      // vibrate the arduino
      switch (direction) {
      case 0: // forward
        // send arduino a "go forward" vibrate

        break;
      case 1: // right
        // send arduino a "go right" vibrate
        break;
      case 2: // down
        // send arduino a "go backward" vibrate
        break;
      case 3: // left
        // send arduino a "go left" vibrate
        break;
      default:
        break;
      }
    }
  }

  @Override
  public void onProviderDisabled(String arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onProviderEnabled(String arg0) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
    // TODO Auto-generated method stub

  }

  // returns int between 0 and 3
  // forward: 0, right: 1, back: 2, left: 3
  private int turnToDirectionInt(int turnType) {
    /*
     * straight = 0
     * slight right = 1
     * right = 2
     * sharp right = 3
     * reverse = 4
     * sharp left = 5
     * left = 6
     * slight left = 7
     * right u-turn = 8
     * left u-turn =9
     * right merge =10
     * left merge = 11
     * right on ramp = 12
     * left on ramp = 13
     * right off ramp = 14
     * left off ramp = 15
     * right fork = 16
     * left fork = 17
     * straight fork = 18
     */
    int[] rightTurns = { 1, 2, 3, 8, 10, 12, 14, 16 };
    int[] leftTurns = { 5, 6, 7, 9, 11, 13, 15, 17 };
    int[] forward = { 0, 18 };
    int[] backward = { 4 };
    if (Arrays.asList(forward).contains(turnType)) {
      return 0;
    } else if (Arrays.asList(rightTurns).contains(turnType)) {
      return 1;
    } else if (Arrays.asList(backward).contains(turnType)) {
      return 2;
    } else if (Arrays.asList(leftTurns).contains(turnType)) {
      return 3;
    } else {
      return -1;
    }

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (D) {
      Log.d(TAG, "onActivityResult " + resultCode);
    }
    switch (requestCode) {
    case REQUEST_CONNECT_DEVICE:
      // When DeviceListActivity returns with a device to connect
      if (resultCode == Activity.RESULT_OK) {
        // Get the device MAC address
        final String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mBluetoothService.connect(device);
      }
      break;
    case REQUEST_ENABLE_BT:
      // When the request to enable Bluetooth returns
      if (resultCode == Activity.RESULT_OK) {
        // Bluetooth is now enabled, so set up it up
        setupBluetooth();
      } else {
        // User did not enable Bluetooth or an error occured
        Log.d(TAG, "BT not enabled");
        Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
        finish();
      }
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Get local Bluetooth adapter
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // If the adapter is null, then Bluetooth is not supported
    if (mBluetoothAdapter == null) {
      Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
      finish();
      return;
    }
  }

  @Override
  protected void onStart() {
    super.onStart();

    // If BT is not on, request that it be enabled.
    // setupBluetooth() will then be called during onActivityResult
    if (!mBluetoothAdapter.isEnabled()) {
      final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
      // Otherwise, setup the bluetooth session
    } else {
      if (mBluetoothService == null) {
        setupBluetooth();
      }
    }
  }

  private void setupBluetooth() {
    Log.d(TAG, "setupBluetooth()");

    // Initialize the BluetoothService to perform bluetooth connections
    mBluetoothService = BluetoothService.getInstance();
    mBluetoothService.setDefaultHandler(mHandler);
  }

  /**
   * Sends a message.
   * 
   * @param message A string of text to send.
   */
  private void sendMessage(String message) {
    // Check that we're actually connected before trying anything
    if (mBluetoothService.getState() != ConnectionState.CONNECTED) {
      Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
      return;
    }

    // Check that there's actually something to send
    if (message.length() > 0) {
      // Get the message bytes and tell the BluetoothService to write
      final byte[] send = message.getBytes();
      mBluetoothService.write(send, MessageOpCode.ECHO);
    }
  }

}
