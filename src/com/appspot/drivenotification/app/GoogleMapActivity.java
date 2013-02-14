/****************************************************************//**
* \file 	GoogleMapActivity.java
* \author 	Yuka Hasegawa
* \version 	1.0
* \date 	11.2012
*
* \brief 	Main Activity
* \details 	This is main activity class
********************************************************************/


package com.appspot.drivenotification.app;
import static com.appspot.drivenotification.app.CommonUtilities.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.appspot.drivenotification.app.BluetoothChatService;
import com.appspot.drivenotification.app.R;
import com.google.android.gcm.GCMRegistrar;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import com.readystatesoftware.maps.*;
import com.readystatesoftware.mapviewballoons.*;
import com.readystatesoftware.*;

//import android-mapviewballoons;

/**************************************
 * \brief  Main Activity Class
 * 
 * *************************************/
public class GoogleMapActivity extends MapActivity{
	
	// Debugging
    private static final String TAG = "BluetoothChat";
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
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private TextView mTitle;
//    private ListView mConversationView;
//    private EditText mOutEditText;
//    private Button mSendButton;
//    private Button mATZButton;
    private TextView mLocation;
    private TextView mSpeedValue;
    private TextView mRPMValue;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
//    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

	 private MyLocationOverlay overlay = null;
	 private MyBalloonOverlay markersOverlay;
	 AsyncTask<Void, Void, Void> mRegisterTask;
	 //private MyBalloonOverlay balloonoverlay
     private MapView mMap=null;
     
     //request code for ODB
     private static final String CODE_ATZ="ATZ";
     private static final String CODE_RPM="010C";
     private static final String CODE_SPEED="010D";
     private static final String CODE_MAF="0110";
     
     private Timer timerCar=null;
     
     private final int INTERVAL_MSEC = 2000; 
     
     
     /***********************************
      * \brief This method is called first
      * 
      *  \param	savedInstanceState [in]
	  *  
      ***********************************:*/
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /***************************
         * \brief Create map view and initialization
         *******************************/
        
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.map);
       
        // Set up the window layout
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
               
        // keep backlight bright
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        mMap = (MapView) findViewById(R.id.map_view1);
        mMap.setBuiltInZoomControls(true);
        mMap.setClickable(true);	//to scroll map
        
        // enable zoom
        mMap.getController().setZoom(15);

        // textview to display address
        mLocation = (TextView) findViewById(R.id.textViewLocation);
        // handler to rewrite address
        final Handler ui_handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String str_address = msg.getData().get("str_address").toString();
                mLocation.setText( str_address );
            }
        };

        // define overlay on map
        overlay = new MyLocationOverlay(getApplicationContext(), mMap);
        overlay.onProviderEnabled(LocationManager.GPS_PROVIDER); //Get location from GPS
        overlay.enableMyLocation();
             
        mSpeedValue = (TextView)findViewById(R.id.textViewSpeed);
        mRPMValue = (TextView)findViewById(R.id.textViewRPM);

        // observe changing current location
        overlay.runOnFirstFix(new Runnable() {
            @Override
            public void run() {
                // move to new current location
                GeoPoint g = overlay.getMyLocation();
                //overlay.
                mMap.getController().animateTo(g);
                mMap.getController().setCenter(g);

                String str_address = null;
                try{
                    // get address
                    str_address = GeocodeManager.point2address(
                        (double)(g.getLatitudeE6() / 1000000),
                        (double)(g.getLongitudeE6() / 1000000),
                        getApplicationContext()
                    );
                }
                catch(IOException e)
                {
                    str_address = "address decoding failed...";
                }
                
                // message has address and let handler to change UI
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("str_address", str_address);
                message.setData(bundle);
                ui_handler.sendMessage(message);

                Log.d("GPS", "location update is detected");
            }
        });

        mMap.getOverlays().add(overlay);
        mMap.invalidate();
        
        /**
         *  \brief Prepare for PUSH notification
         */
        
        checkNotNull(SERVER_URL, "SERVER_URL");
        checkNotNull(SENDER_ID, "SENDER_ID");
        // Make sure the device has the proper dependencies.
        GCMRegistrar.checkDevice(getApplicationContext());
        // Make sure the manifest was properly set - comment out this line
        // while developing the app, then uncomment it when it's ready.
        GCMRegistrar.checkManifest(getApplicationContext());
               
        registerReceiver(mHandleMessageReceiver,new IntentFilter(DISPLAY_MESSAGE_ACTION));
        final String regId = GCMRegistrar.getRegistrationId(getApplicationContext());

        if (regId.equals("")) {
            // Automatically registers application on startup.
        	try{
        		GCMRegistrar.register(this, SENDER_ID);
            	Toast.makeText(getApplicationContext(), "registered", Toast.LENGTH_LONG).show();
        	}catch(IllegalStateException e){
        		Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        	}
        } else {
            // Device is already registered on GCM, check server.
            if (GCMRegistrar.isRegisteredOnServer(this)) {
                // Skips registration.
               // mDisplay.append(getString(R.string.already_registered) + "\n");
            	Toast.makeText(getApplicationContext(), R.string.already_registered, Toast.LENGTH_LONG).show();
                
            } else {
                // Try to register again, but not in the UI thread.
                // It's also necessary to cancel the thread onDestroy(),
                // hence the use of AsyncTask instead of a raw thread.
                final Context context = this;
                mRegisterTask = new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        boolean registered =
                                ServerUtilities.register(context, regId);
                        // At this point all attempts to register with the app
                        // server failed, so we need to unregister the device
                        // from GCM - the app will try to register again when
                        // it is restarted. Note that GCM will send an
                        // unregistered callback upon completion, but
                        // GCMIntentService.onUnregistered() will ignore it.
                        if (!registered) {
                            GCMRegistrar.unregister(context);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        mRegisterTask = null;
                    }

                };
                mRegisterTask.execute(null, null, null);
            }
        }
        
        /**
         *  \brief In the case this Activity started by notification or popup, display current sensor value
         */
        //if location information is attached to Intent, display that info
        String message = getIntent().getStringExtra(WARNING_MESSAGE);
        double longitude=getIntent().getDoubleExtra(LONGITUDE,-1);
        double latitude=getIntent().getDoubleExtra(LATITUDE,-1);
          
        //broadcast to this class reciever
        if(message!=null && longitude!=-1 && latitude!=-1){
        	
        	Location location = new Location(LocationManager.GPS_PROVIDER);
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            
            //broadcast to call CommonUtilities
        	displayMessage(getApplicationContext(), message,location);
        }
        /**
         *  \brief Prepare to connect to ODB via Bluetooth
         */
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        if(mBluetoothAdapter == null){
        	//if bluetooth adapter is null, the device doesn't have Bluetooth equipment
        	Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }
    }
	
	/**
	 * \brief Starting activity
	 * 
	 */
	@Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        /**
        * \brief If BT is not on, request that it be enabled.setupChat() will then be called during onActivityResult
         * */
         
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }
	
	/***
	 *  \brief Set up Bluetooth chat to read ODB values
	 */
	private void setupChat() {
		//文字列の表示に用いるTextViewを用意し、BluetoothChatServiceを開始するためのメソッド
        Log.d(TAG, "setupChat()");       
        // Initialize the array adapter for the conversation thread
//        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
//        mConversationView = (ListView) findViewById(R.id.listViewMessage); //チャットで使われる、文字列を表示するリスト
//        mConversationView.setAdapter(mConversationArrayAdapter);//文字列にアダプタをセット
//
//        // Initialize the compose field with a listener for the return key
//        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
//        mOutEditText.setOnEditorActionListener(mWriteListener);//自分で入力する文字列のためのリスナ
//
//        // Initialize the send button with a listener that for click events
//        mSendButton = (Button) findViewById(R.id.button_send);
//        mSendButton.setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//                // Send a message using content of the edit text widget
//                TextView view = (TextView) findViewById(R.id.edit_text_out);
//                String message = view.getText().toString();
//                sendMessage(message);
//            }
//        });
//        
//        mATZButton = (Button) findViewById(R.id.button_atz);
//        mATZButton.setOnClickListener(new OnClickListener(){
//        	public void onClick(View v){
//      		
//        		sendMessage(CODE_ATZ);
//        		timerCar = new Timer();
//                timerCar.schedule(new TimerTask(){      		
//        			@Override
//        			public void run() {
//        				// TODO Auto-generated method stub
//        				//車載センサにコードを送信
//        				//ここでmHandlerにpostしないと、別スレッドからUIが操作されたとして落ちる…
//        				mHandler.post(new Runnable(){
//                			public void  run(){
//                				sendMessage(CODE_SPEED);//とりあえず、10秒ごとにスピードを取得するためのコードを送信
//                			}
//                		});
//        			}
//        			
//                }, 10000,10000);
//        	}
//        });        
        
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

    }
	
	/***
	 *  \brief Call back Bluetooth activation result
	 *  
	 *  \param	requestCode [in]
	 * 	\param	resultCode [in]
	 * 	\param	intent [in]
	 */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
//                finish();
            }
        }
    }

    
    /***
     * 
     * @param data
     * @param secure
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BLuetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);       
    }
	
    /***
     * \brief timer to get values from ODB frequently
     */
    private void setTimer(){
    	sendMessage(CODE_ATZ);
    	
		timerCar = new Timer();
        timerCar.schedule(new TimerTask(){      
        	
        	int check=0;
        	
			@Override
			public void run() {
				// TODO Auto-generated method stub
				//send codes to ODB
				//I should post to mHandler(UI controll)
				if(check==0){
					mHandler.post(new Runnable(){
	        			public void  run(){
	        				sendMessage(CODE_SPEED);
	        			}
	        		});
					check=1;
				}
				else if(check==1){
					mHandler.post(new Runnable(){
						public void run(){
							sendMessage(CODE_RPM);
						}
					});
					check=2;
				}
				else{
					mHandler.post(new Runnable(){
						public void run(){
							sendMessage(CODE_ATZ);
						}
					}); 
					check=0;
				}
			}
			
        }, 5000,INTERVAL_MSEC);
    }
    
	 /*****************************************************
	  * \brief The Handler that gets information back from the BluetoothChatService
	  *********************************************************/
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);	//display device name on upper title bar
//                    mConversationArrayAdapter.clear();

                    setTimer();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
//                mConversationArrayAdapter.add("Me:  " + writeMessage);	//自分が書き込んだメッセージの場合
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage="";
				try {
					readMessage = new String(readBuf, 0, msg.arg1,"UTF-16");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//もらったバッファの最初から指定文字分よんで、Stringに変換している
                //ODBから受け取ったメッセージをパース
                String value_str = parseMessage(readMessage);
                Toast.makeText(getApplicationContext(), readMessage, Toast.LENGTH_LONG).show();
//                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage/*value*/);
                if(value_str!=""){
                	//mSpeedValue=(TextView)map.findViewById(R.id.textViewSpeed);
                	//mSpeedValue.setText("Speed:"+value);
                	if(value_str.startsWith("Speed")){
                		mSpeedValue.setText(value_str);
                	}
                	if(value_str.startsWith("RPM")){
                		mRPMValue.setText(value_str);
                	}
                	
                }
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
    
  /**
   * \brief Parse data from ODB response to normal string
   * @param s
   * @return String parsed string
   */
    private String parseMessage(String s){
    	//とりあえずスピードのみを扱うものとして考える
    	String value_str = "";
    	int index = s.indexOf('>');
    	//Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();

    	if(index!=-1/*&&s.length()>40*/){
    		s=s.replace(' ','_');
  /**
   * \brief Change the way to parse up to response
   */
    		if(s.contains("41_0D")){//Speed		
    			String hex_str=(String)s.substring(index-5, index-3);
    			
    			int value = Integer.parseInt(hex_str,16);
    			value_str="Speed:"+value;
    			
    		}
    		if(s.contains("41_0C")){//RPM
    			String hex_str1=(String)s.substring(index-8,index-6);
    			int value1 = Integer.parseInt(hex_str1,16);
    			
    			String hex_str2=(String)s.substring(index-5,index-3);
    			int value2 = Integer.parseInt(hex_str2,16);
    			
    			float fvalue = (float) (((value1*256.0)+value2)/4.0);
    			value_str="RPM:"+fvalue;
    		}
    	}
    	return value_str;
    }
    
	 // The action listener for the EditText widget, to listen for the return key
//    private TextView.OnEditorActionListener mWriteListener =
//        new TextView.OnEditorActionListener() {
//        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
//            // If the action is a key-up event on the return key, send the message
//            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
//                String message = view.getText().toString();
//                sendMessage(message);
//            }
//            if(D) Log.i(TAG, "END onEditorAction");
//            return true;
//        }
//    };
//    

    
    /***
     *  \brief Start Bluetooth comunication
     */
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
    }
    
    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
        	//接続されたことが通知された
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
        	
        	//add "\r" to match ODB code form
        	message+="\r";
            byte[] send = message.getBytes();	//convert to byte to send on Bluetooth
            mChatService.write(send);	//give the date which is converted to byte to BluetoothChatService.class write method

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
//            mOutEditText.setText(mOutStringBuffer);
        }
    }
	
    /***
     * \brief Stop Bluetooth Communication and unregister MessageHandler
     */
	 @Override
    protected void onDestroy() {
		 GCMRegistrar.onDestroy(this);
	     super.onDestroy();
        if (mRegisterTask != null) {
            mRegisterTask.cancel(true);
        }
        unregisterReceiver(mHandleMessageReceiver);
        
    }
	 
	 /***
	  * 
	  * @param reference
	  * @param name
	  * 
	  * \brief If reference is null, throw NullPointException
	  */
	private void checkNotNull(Object reference, String name) {
        if (reference == null) {
            throw new NullPointerException(
                    getString(R.string.error_config, name));
        }
    }
	
	/***
	 * \brief MapActivity override method
	 */
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	/***
	 * \brief Catch broadcast message. Messages are broadcasted to display something
	 */
	public final BroadcastReceiver mHandleMessageReceiver =
            new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	
        	//display on map
            String temp = intent.getExtras().getString(EXTRA_MESSAGE);
           
	        double latitude = intent.getDoubleExtra(CommonUtilities.LATITUDE,-1);
	        double longitude = intent.getDoubleExtra(CommonUtilities.LONGITUDE,-1);
	        
	        //Toast.makeText(getApplicationContext(), "pin lat="+latitude+" lon="+longitude, Toast.LENGTH_LONG).show();
	        if(latitude!=-1 && longitude!=-1){
	        	GeoPoint geo = new GeoPoint((int)(latitude * 1E6),(int)(longitude * 1E6));
	        
	        	String locationstr=" latitude="+latitude+" longitude="+longitude;
	        
	        	Drawable drawable_pin = GoogleMapActivity.this.getResources().getDrawable(R.drawable.pin);
	        	markersOverlay = new MyBalloonOverlay(drawable_pin, mMap);

	        	OverlayItem oi = new OverlayItem(geo, "Temperature "+temp+" ℃", locationstr);
	        	markersOverlay.addItem(oi);
	        	if(!mMap.getOverlays().contains(markersOverlay)){
	        		mMap.getOverlays().add(markersOverlay);
	        	}

	        	mMap.invalidate();
	        }
	        intent.getExtras().clear();
        }
    };

    
    /***
     * \brief Make this Adnroid device discoverable
     */
    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    
    
    /***
     * \brief Prepare option menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }
    
    /***
     *  \brief This method is called when option menu item selscted
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.secure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        case R.id.insecure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }

    
    /**
     *  \brief Stop timer used to get OBD values
     */
    @Override
    public void onStop(){
    	super.onStop();
    	if(timerCar!=null){
    		timerCar.cancel();
    		timerCar=null;
    	}
    }
}
