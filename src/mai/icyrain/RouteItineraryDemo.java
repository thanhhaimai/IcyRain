package mai.icyrain;

import mai.icyrain.bluetooth.BluetoothService;
import mai.icyrain.bluetooth.ConnectionState;
import mai.icyrain.bluetooth.HandlerMessage;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.mapquest.android.maps.GeoPoint;
import com.mapquest.android.maps.MapView;
import com.mapquest.android.maps.MyLocationOverlay;
import com.mapquest.android.maps.RouteManager;
import com.mapquest.android.maps.RouteResponse;
import com.mapquest.android.maps.ServiceResponse.Info;

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
  protected RouteLocationListener locationListener;

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

    final EditText end = (EditText) findViewById(R.id.endTextView);
    final RouteItineraryView itinerary = (RouteItineraryView) findViewById(R.id.itinerary);

    final RouteManager routeManager = new RouteManager(this);
    routeManager.setMapView(this.mapView);
    routeManager.setBestFitRoute(true);

    // we want walking directions
    JSONObject options = new JSONObject();
    try {
      options.put("routeType", "pedestrian");
    } catch (JSONException e) {
      e.printStackTrace();
    }

    // pass in options
    routeManager.setOptions(options.toString());

    routeManager.setDebug(true);

    // callbacks for the routemanager
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

        // listen for location changes
        locationListener = new RouteLocationListener(routeResponse);
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 2, locationListener);

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

    // create an onclick listener for the instructional text
    createRouteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        hideSoftKeyboard(view);
        createRouteButton.setEnabled(false);
        Log.d("pullLocation", "hello there");

        myLocationOverlay.enableMyLocation();

        myLocationOverlay.runOnFirstFix(new Runnable() {

          @Override
          public void run() {
            final GeoPoint currentLocation = myLocationOverlay.getMyLocation();

            mapView.getController().animateTo(currentLocation);
            mapView.getController().setZoom(14);
            mapView.getOverlays().add(myLocationOverlay);
            myLocationOverlay.setFollowing(true);

            final double latitude = currentLocation.getLatitude();
            final double longitude = currentLocation.getLongitude();

            Log.d("pullLocation", "HELLO");
            final String startAt = "{latLng:{lat:" + latitude + ",lng:" + longitude + "}}";
            final String endAt = getText(end);
            Log.d("pullLocation", "startAt is " + startAt);
            Log.d("pullLocation", "endAt is " + endAt);

            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                routeManager.createRoute(startAt, endAt);
              }
            });
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
