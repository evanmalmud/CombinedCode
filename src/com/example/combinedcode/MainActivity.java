package com.example.combinedcode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.ShareActionProvider;
import android.widget.Toast;

public class MainActivity extends Activity implements OnMenuItemClickListener {

	private ShareActionProvider mShareActionProvider;
	private GraphicalView mChartView;
//BLUETOOTH INITIALIZATIONS
	// Debugging
    private static final String TAG = "MAINACTIVITY";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    //private ListView mConversationView;
    //private EditText mOutEditText;
    //private Button mSendButton;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(D) Log.e(TAG, "+++ ON CREATE +++");
		setContentView(R.layout.main);
		
		// Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
     // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
		if (mChartView == null) {
			RelativeLayout layout = (RelativeLayout) findViewById(R.id.chart);
			mChartView = ChartFactory.getLineChartView(this, getMyData(),
					getMyRenderer());
			layout.addView(mChartView);
		} else {
			mChartView.repaint(); // use this whenever data has changed and you
									// want to redraw
		}
	}
	@Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) 
            	setupChat();
        }
    }
	

	@Override
	public synchronized void onResume() {
		super.onResume();
		if(D) Log.e(TAG, "+ ON RESUME +");
		// Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
        
        
		if (mChartView != null) {
			mChartView.repaint();
		}
	}

	// Action Bar displays options when Menu item in Action bar is clicked
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);

	    // Locate MenuItem with ShareActionProvider
	    MenuItem item = menu.findItem(R.id.menu_item_share);

	    // Fetch and store ShareActionProvider
	    mShareActionProvider = (ShareActionProvider) item.getActionProvider();

	    // Return true to display menu
	    return true;
	}
	
	private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
       // mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
       // mConversationView = (ListView) findViewById(R.id.in);
       // mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        //mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        //mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        //mSendButton = (Button) findViewById(R.id.button_send);
       // mSendButton.setOnClickListener(new OnClickListener() {
          //  public void onClick(View v) {
                // Send a message using content of the edit text widget
                //TextView view = (TextView) findViewById(R.id.edit_text_out);
                //String message = view.getText().toString();
                //sendMessage(message);
           // }
       // });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }
	
	@Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    
    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    @SuppressWarnings("unused")
	private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    /*private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };*/

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                    mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    setStatus(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device);
    }
    
	
	public void doShare(View viewopts) {
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
//		shareIntent.putExtra(Intent.EXTRA_STREAM, uriToImage);
		shareIntent.setType("image/jpeg");
		startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
	}
	
	// Call to update the share intent
	@SuppressWarnings("unused")
	private void setShareIntent(Intent shareIntent) {
	    if (mShareActionProvider != null) {
	        mShareActionProvider.setShareIntent(shareIntent);
	    }
	}
	
	public void sendCollectSignal(View toast) {
		Toast.makeText(this, "Selected Collect Data", Toast.LENGTH_SHORT)
				.show();
	}

	public void openArchive(View newActivity) {
		Toast.makeText(this, "Selected Load Data", Toast.LENGTH_SHORT).show();
		Intent archiveData = new Intent(this, DisplayArchive.class);
		startActivity(archiveData);
	}

	// Plotting pop-up menu
	public void plotMenu(View view) {
		PopupMenu popup = new PopupMenu(this, view);
		popup.setOnMenuItemClickListener(this);
		popup.inflate(R.menu.plotting_menu);
		popup.show();
	}

	// Saving pop-up menu
	public void saveMenu(View display) {
		PopupMenu popup2 = new PopupMenu(this, display);
//		popup2.setOnMenuItemClickListener(this);
		popup2.inflate(R.menu.saving_menu);
		popup2.show();
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
	    switch (item.getItemId()) {
	        case R.id.text_file:
	        	if (item.isChecked()) {
	        		//item.setChecked(false);
	        		Toast.makeText(this, "Was true, set False", Toast.LENGTH_SHORT).show(); }
	            else {
	            	item.setChecked(true);
	            	Toast.makeText(this, "Checked", Toast.LENGTH_SHORT).show(); }
	            return true;
	        case R.id.binary_file:
	            if (item.isChecked()) {
	        		item.setChecked(false);
	        		Toast.makeText(this, "Was true, set False", Toast.LENGTH_SHORT).show(); }
	            else {
	            	item.setChecked(true);
	            	Toast.makeText(this, "Checked", Toast.LENGTH_SHORT).show(); }
	            return true;
	        case R.id.compressed_file:
	            if (item.isChecked()) {
	        		item.setChecked(false);
	        		Toast.makeText(this, "Was true, set False", Toast.LENGTH_SHORT).show(); }
	            else {
	            	item.setChecked(true);
	            	Toast.makeText(this, "Checked", Toast.LENGTH_SHORT).show(); }
	            return true;
	        case R.id.matlab_file:
	            if (item.isChecked()) {
	        		item.setChecked(false);
	        		Toast.makeText(this, "Was true, set False", Toast.LENGTH_SHORT).show(); }
	            else {
	            	item.setChecked(true);
	            	Toast.makeText(this, "Checked", Toast.LENGTH_SHORT).show(); }
	            return true;
	        case R.id.device_search:
	            // Launch the DeviceListActivity to see devices and do scan
	            serverIntent = new Intent(this, DeviceListActivity.class);
	            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	            return true;
	        case R.id.discoverable:
	            // Ensure this device is discoverable by others
	            ensureDiscoverable();
	            return true;
	    }
	            return false;
	    }
	 

	// For testing button & popup menu purposes only!
	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.raw_plot:
				Toast.makeText(this, "Selected Raw Plot", Toast.LENGTH_SHORT)
						.show();
				break;
			case R.id.range_plot:
				Toast.makeText(this, "Selected Range Plot", Toast.LENGTH_SHORT)
						.show();
				break;
			case R.id.doppler_plot:
				Toast.makeText(this, "Selected Doppler Plot", Toast.LENGTH_SHORT)
						.show();
				break;
			case R.id.fft_plot:
				Toast.makeText(this, "Selected FFT Plot", Toast.LENGTH_SHORT)
						.show();
				break;
			case R.id.sar_plot:
				Toast.makeText(this, "Selected SAR Plot", Toast.LENGTH_SHORT)
						.show();
				break;
			/*case R.id.text_file:
				// if (item.isChecked()) item.setChecked(false);
				// else item.setChecked(true);
				// return true;
				Toast.makeText(this, "Selected Text File", Toast.LENGTH_SHORT).show();
				break;
			case R.id.binary_file:
				Toast.makeText(this, "Selected Binary File", Toast.LENGTH_SHORT)
						.show();
				break;
			case R.id.compressed_file:
				Toast.makeText(this, "Selected Compressed File", Toast.LENGTH_SHORT)
						.show();
				break;
			case R.id.matlab_file:
				Toast.makeText(this, "Selected Matlab File", Toast.LENGTH_SHORT)
						.show();
				break;*/
		}
		return false;
	}

	public XYMultipleSeriesDataset getMyData() {
		XYMultipleSeriesDataset myData = new XYMultipleSeriesDataset();

		XYSeries dataSeries = new XYSeries("FMCW Radar- Simulated Data");

		// Find the directory for the SD Card using the API
		File sdcard = Environment.getExternalStorageDirectory();

		// Get the text file
		File file = new File(sdcard, "simuData.txt");

		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			int x = 0;

			while ((line = br.readLine()) != null & (x != 4000)) {
				x = x + 1;
				int y = Integer.parseInt(line);
				dataSeries.add(x, y);
			}
			br.close();
		} catch (IOException e) {
			Log.e("MainActivity", "IOError"); // You'll need to add proper error
												// handling here
		}
		myData.addSeries(dataSeries);

		return myData;
	}

	public XYMultipleSeriesRenderer getMyRenderer() {
		XYSeriesRenderer r = new XYSeriesRenderer();
		r.setColor(Color.BLUE);
		r.setLineWidth(2);

		r.setPointStyle(PointStyle.SQUARE); // CIRCLE, DIAMOND , POINT,
											// TRIANGLE, X
		r.setFillPoints(true); // not for point or x
								// don't know how to set point size or point
								// color

		// r.setFillBelowLine(true); // shows area of curves
		// r.setFillBelowLineColor(Color.TRANSPARENT); //set color other than
		// Default

		XYMultipleSeriesRenderer myRenderer = new XYMultipleSeriesRenderer();
		myRenderer.addSeriesRenderer(r);
		myRenderer.setPanEnabled(true, true);
		myRenderer.setZoomEnabled(true, false);
		myRenderer.setZoomButtonsVisible(true);

		String title = "FMCW Radar Data Plot";
		myRenderer.setChartTitle(title);
		int textSize = 24;
		myRenderer.setChartTitleTextSize(textSize);

		myRenderer.setZoomRate(10);

		myRenderer.setAxesColor(Color.BLACK);
		myRenderer.getXLabelsAlign();
		myRenderer.setXLabelsColor(Color.BLACK);
		myRenderer.setYLabelsColor(0, Color.BLACK);
		myRenderer.setShowAxes(true);
		myRenderer.setLabelsColor(Color.BLACK);

		myRenderer.setXTitle("Samples");
		myRenderer.setYTitle("Frequency");

		// background color of the PLOT ONLY
		myRenderer.setApplyBackgroundColor(true);
		myRenderer.setBackgroundColor(Color.LTGRAY); // Color.TRANSPARENT would
														// show the background
														// of the app
														// (MainActivity)

		myRenderer.setMarginsColor(Color.WHITE); // sets the background area of
													// the object itself
													// does not change the plots
													// background

		myRenderer.setGridColor(Color.DKGRAY);
		myRenderer.setXLabels(20);
		myRenderer.setYLabels(9);
		myRenderer.setShowGrid(true);
		return myRenderer;
	}
}
