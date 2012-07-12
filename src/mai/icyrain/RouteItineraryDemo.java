package mai.icyrain;

import java.util.List;

import mai.icyrain.bluetooth.BluetoothService;
import mai.icyrain.bluetooth.ConnectionState;
import mai.icyrain.bluetooth.HandlerMessage;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.location.Address;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.mapquest.android.Geocoder;
import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.MyLocationOverlay;
import com.mapquest.android.maps.RouteManager;
import com.mapquest.android.maps.RouteResponse;
import com.mapquest.android.maps.ServiceResponse.Info;

/**
 * This demo is to give you and idea on how to implement customized route
 * itinerary
 */
public class RouteItineraryDemo extends SimpleMap {

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
  private final Handler mHandler = new Handler() {
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
        new String(readBuf, 0, msg.arg1);
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

  protected MyLocationOverlay myLocationOverlay;

  protected EditText start;
  // README: set this to true to test on a real phone. Emu doesn't support
  // bluetooth.
  protected Boolean useBluetooth = true;

  @Override
  protected int getLayoutId() {
    return R.layout.custom_route_itinerary_demo;
  }

  @Override
  protected void init() {
    super.init();

    // find the objects we need to interact with
    mapView = (MapView) findViewById(R.id.map);
    myLocationOverlay = new MyLocationOverlay(this, mapView);
    final RelativeLayout mapLayout = (RelativeLayout) findViewById(R.id.mapLayout);
    final RelativeLayout itineraryLayout = (RelativeLayout) findViewById(R.id.itineraryLayout);
    final Button createRouteButton = (Button) findViewById(R.id.createRouteButton);
    final Button showItineraryButton = (Button) findViewById(R.id.showItineraryButton);
    final Button showMapButton = (Button) findViewById(R.id.showMapButton);
    final Button clearButton = (Button) findViewById(R.id.clearButton);
    findViewById(R.id.setTime);

    // PULL : current location
    // start = (EditText) findViewById(R.id.startTextView);
    final EditText end = (EditText) findViewById(R.id.endTextView);
    final RouteItineraryView itinerary = (RouteItineraryView) findViewById(R.id.itinerary);

    final RouteManager routeManager = new RouteManager(this);
    routeManager.setMapView(this.mapView);
    routeManager.setDebug(true);
    routeManager.setRouteCallback(new RouteManager.RouteCallback() {
      @Override
      public void onError(RouteResponse routeResponse) {
        final Info info = routeResponse.info;
        final int statusCode = info.statusCode;

        final StringBuilder message = new StringBuilder();
        message.append("Unable to create route.\n").append("Error: ").append(statusCode)
          .append("\n").append("Message: ").append(info.messages);
        Toast.makeText(getApplicationContext(), message.toString(), Toast.LENGTH_LONG).show();
        createRouteButton.setEnabled(true);
      }

      @Override
      public void onSuccess(RouteResponse routeResponse) {
        clearButton.setVisibility(View.VISIBLE);
        if (showItineraryButton.getVisibility() == View.GONE
          && showMapButton.getVisibility() == View.GONE) {
          showItineraryButton.setVisibility(View.VISIBLE);
        }
        itinerary.setRouteResponse(routeResponse);
        createRouteButton.setEnabled(true);
      }

    });

    // attach the show itinerary listener
    showItineraryButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mapLayout.setVisibility(View.GONE);
        itineraryLayout.setVisibility(View.VISIBLE);
        showItineraryButton.setVisibility(View.GONE);
        showMapButton.setVisibility(View.VISIBLE);
      }
    });

    // attach the show map listener
    showMapButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mapLayout.setVisibility(View.VISIBLE);
        itineraryLayout.setVisibility(View.GONE);
        showMapButton.setVisibility(View.GONE);
        showItineraryButton.setVisibility(View.VISIBLE);
      }
    });

    // attach the clear route listener
    clearButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        routeManager.clearRoute();
        clearButton.setVisibility(View.GONE);
        showItineraryButton.setVisibility(View.GONE);
        showMapButton.setVisibility(View.GONE);
        mapLayout.setVisibility(View.VISIBLE);
        itineraryLayout.setVisibility(View.GONE);
      }
    });

    /*
     * // attach the set time listener setTime.setOnClickListener(new
     * View.OnClickListener() {
     * @Override public void onClick(View v) { DialogFragment newFragment =
     * new TimePickerFragment(); newFragment.show(this., "timePicker"); }
     * });
     */

    // create an onclick listener for the instructional text
    createRouteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        hideSoftKeyboard(view);
        createRouteButton.setEnabled(false);
        Log.d("location", "hello there");

        myLocationOverlay.enableMyLocation();

        myLocationOverlay.runOnFirstFix(new Runnable() {

          @Override
          public void run() {
            final GeoPoint currentLocation = myLocationOverlay.getMyLocation();

            mapView.getController().animateTo(currentLocation);
            mapView.getController().setZoom(14);
            mapView.getOverlays().add(myLocationOverlay);
            myLocationOverlay.setFollowing(true);

            final Geocoder geocoder = new Geocoder(getBaseContext());
            final double latitude = currentLocation.getLatitude();
            final double longitude = currentLocation.getLongitude();
            List<Address> locations = null;
            Address location = null;

            try {
              locations = geocoder.getFromLocation(latitude, longitude, 1);
              location = locations.get(0);
            } catch (final Exception e) {
              Log.d("location", e.toString());
            }

            String startAt = "Berkeley"; // Default location

            if (location != null) {
              final String propLoc =
                location.getAddressLine(0) + ", " + location.getAddressLine(1) + ", "
                  + location.getPostalCode();

              startAt = propLoc;
            }
            Log.d("location", "HELLO");
            Log.d("location", startAt);
            final String endAt = getText(end);

            routeManager.createRoute(startAt, endAt);
          }
        });
      }
    });

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
}
