package mai.icyrain;

import mai.icyrain.bluetooth.BluetoothService;
import mai.icyrain.bluetooth.ConnectionState;
import mai.icyrain.bluetooth.HandlerMessage;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current session.
 */
public class IcyRain extends Activity {
  // Key names received from the BluetoothService Handler

  public static final String SMIRF_MAC = "00:06:66:45:02:5A";
  public static final String DEVICE_NAME = "device_name";
  public static final String TOAST = "toast";

  private static final boolean D = true;
  // Intent request codes
  private static final int REQUEST_CONNECT_DEVICE = 1;

  private static final int REQUEST_ENABLE_BT = 2;
  // Debugging
  private static final String TAG = "IcyRain";

  // Local Bluetooth adapter
  private BluetoothAdapter mBluetoothAdapter = null;
  // Member object for the bluetooth services
  private BluetoothService mBluetoothService = null;
  // Name of the connected device
  private String mConnectedDeviceName = null;
  // Array adapter for the conversation thread
  private ArrayAdapter<String> mConversationArrayAdapter;

  private ListView mConversationView;
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
          mTitle.setText(R.string.title_connected_to);
          mTitle.append(mConnectedDeviceName);
          mConversationArrayAdapter.clear();
          break;
        case CONNECTING:
          mTitle.setText(R.string.title_connecting);
          break;
        case LISTEN:
        case IDLE:
          mTitle.setText(R.string.title_not_connected);
          break;
        }
        break;
      case HandlerMessage.WRITE:
        final byte[] writeBuf = (byte[]) msg.obj;
        // construct a string from the buffer
        final String writeMessage = new String(writeBuf);
        mConversationArrayAdapter.add("Me:  " + writeMessage);
        break;
      case HandlerMessage.READ:
        final byte[] readBuf = (byte[]) msg.obj;
        // construct a string from the valid bytes in the buffer
        final String readMessage = new String(readBuf, 0, msg.arg1);
        mConversationArrayAdapter.add(mConnectedDeviceName + ": opCode " + msg.arg2 + ", msg "
          + readMessage);
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
  private EditText mOutEditText;
  // String buffer for outgoing messages
  private StringBuffer mOutStringBuffer;
  private Button mSendButton;

  // Layout Views
  private TextView mTitle;

  // The action listener for the EditText widget, to listen for the return key
  private final TextView.OnEditorActionListener mWriteListener =
    new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        // If the action is a key-up event on the return key, send the message
        if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
          final String message = view.getText().toString();
          sendMessage(message);
        }
        if (D) {
          Log.i(TAG, "END onEditorAction");
        }
        return true;
      }
    };

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
    if (D) {
      Log.e(TAG, "+++ ON CREATE +++");
    }

    // Set up the window layout
    requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
    setContentView(R.layout.main);
    getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

    // Set up the custom title
    mTitle = (TextView) findViewById(R.id.title_left_text);
    mTitle.setText(R.string.app_name);
    mTitle = (TextView) findViewById(R.id.title_right_text);

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
  public boolean onCreateOptionsMenu(Menu menu) {
    final MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.option_menu, menu);
    return true;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    // Stop the Bluetooth services
    if (mBluetoothService != null) {
      mBluetoothService.stop();
    }
    if (D) {
      Log.e(TAG, "--- ON DESTROY ---");
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.scan:
      // Launch the DeviceListActivity to see devices and do scan
      final Intent serverIntent = new Intent(this, DeviceListActivity.class);
      startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
      return true;
    case R.id.discoverable:
      // Ensure this device is discoverable by others
      ensureDiscoverable();
      return true;
    }
    return false;
  }

  @Override
  public synchronized void onPause() {
    super.onPause();
    if (D) {
      Log.e(TAG, "- ON PAUSE -");
    }
  }

  @Override
  public synchronized void onResume() {
    super.onResume();
    if (D) {
      Log.e(TAG, "+ ON RESUME +");
    }

    // Performing this check in onResume() covers the case in which BT was
    // not enabled during onStart(), so we were paused to enable it...
    // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
    if (mBluetoothService != null) {
      if (mBluetoothService.getState() == ConnectionState.IDLE) {
        // get the bluesmirf directly and try to connect
        mBluetoothService.connect(mBluetoothAdapter.getRemoteDevice(SMIRF_MAC));
      }
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    if (D) {
      Log.e(TAG, "++ ON START ++");
    }

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

  @Override
  public void onStop() {
    super.onStop();
    if (D) {
      Log.e(TAG, "-- ON STOP --");
    }
  }

  private void ensureDiscoverable() {
    if (D) {
      Log.d(TAG, "ensure discoverable");
    }
    if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
      final Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
      discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
      startActivity(discoverableIntent);
    }
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
      mBluetoothService.write(send, 10);

      // Reset out string buffer to zero and clear the edit text field
      mOutStringBuffer.setLength(0);
      mOutEditText.setText(mOutStringBuffer);
    }
  }

  private void setupBluetooth() {
    Log.d(TAG, "setupBluetooth()");

    // Initialize the array adapter for the conversation thread
    mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
    mConversationView = (ListView) findViewById(R.id.in);
    mConversationView.setAdapter(mConversationArrayAdapter);

    // Initialize the compose field with a listener for the return key
    mOutEditText = (EditText) findViewById(R.id.edit_text_out);
    mOutEditText.setOnEditorActionListener(mWriteListener);

    // Initialize the send button with a listener that for click events
    mSendButton = (Button) findViewById(R.id.button_send);
    mSendButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        // Send a message using content of the edit text widget
        final TextView view = (TextView) findViewById(R.id.edit_text_out);
        final String message = view.getText().toString();
        sendMessage(message);
      }
    });

    // Initialize the BluetoothService to perform bluetooth connections
    mBluetoothService = BluetoothService.getInstance();
    mBluetoothService.setDefaultHandler(mHandler);

    // Initialize the buffer for outgoing messages
    mOutStringBuffer = new StringBuffer("");
  }
}
