/****************************************************************//**
* \file 	GCMIntentService.java
* \author 	Yuka Hasegawa
* \version 	1.0
* \date 	11.2012
*
* \brief 	Service which catches GCM from server
* \details 	This service handles PUSH notification from server and decide if it's needed to warn user
********************************************************************/



/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appspot.drivenotification.app;

import static com.appspot.drivenotification.app.CommonUtilities.*;

import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ViewDebug.FlagToString;
import android.widget.Toast;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import com.appspot.drivenotification.app.R;
import com.google.android.maps.GeoPoint;

/**
 * IntentService responsible for handling GCM messages.
 */

/**************************************
 * \brief  Service which catches GCM from server
 * 
 * *************************************/
public class GCMIntentService extends GCMBaseIntentService {

    @SuppressWarnings("hiding")
    private static final String TAG = "GCMIntentService";
    
    private LocationManager locationManager;
    private LocationListener locationListener;

    private Location lastKnownLocation;
    //private Time lastTime;
    
    private Handler mHandler;
    public GCMIntentService() {
        super(SENDER_ID);
    }
   
    /***
     * \brief Register Android device to server.
     */
    @Override
    protected void onRegistered(Context context, String registrationId) {
        Log.i(TAG, "Device registered: regId = " + registrationId);
        displayMessage(context, getString(R.string.gcm_registered));
        ServerUtilities.register(context, registrationId);//ここで実際にサーバに登録をPOST
    }

    
    /***
     * \brief Unregister Android device.
     */
    @Override
    protected void onUnregistered(Context context, String registrationId) {
        Log.i(TAG, "Device unregistered");
        displayMessage(context, getString(R.string.gcm_unregistered));
        if (GCMRegistrar.isRegisteredOnServer(context)) {
            ServerUtilities.unregister(context, registrationId);
        } else {
            // This callback results from the call to unregister made on
            // ServerUtilities when the registration to the server failed.
            Log.i(TAG, "Ignoring unregister callback");
        }
    }

    /***
     * \brief It is called when a message is received.
     */
    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.i(TAG, "Received message");
        Bundle extras = intent.getExtras();

        String message = "";
        if(extras.getString("message")!=null){
        	message+=extras.getString("message");
        }
        
        String latstr = extras.getString(LATITUDE);
        String lonstr = extras.getString(LONGITUDE);
        //Location sensorLocation = new Location(lonstr)
        Log.d("location","onMessage");

        //GeoPoint sensorGeo = new GeoPoint( (int)(Double.parseDouble(latstr)*1E6) , (int)(Double.parseDouble(lonstr)*1E6) );
        Location sensorLocation = new Location(LocationManager.GPS_PROVIDER);
        sensorLocation.setLatitude(Double.parseDouble(latstr));
        sensorLocation.setLongitude(Double.parseDouble(lonstr));

        startLocationService(context,sensorLocation,message);
        

//        displayMessage(context, message,latstr,lonstr);	//アプリ上で通知
//        // notifies user
//        generateNotificationWithLocation(context, message,latstr,lonstr);	//ステータスバーに表示
//        
//        ActivityManager activityManager = ((ActivityManager) getSystemService(ACTIVITY_SERVICE));
//        List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);
//        
//        if(taskInfo!=null && taskInfo.get(0).topActivity.getClassName()!=GoogleMapActivity.class.getName()){
//        	generatePopup(context,message,latstr,lonstr);
//        }
//        getActivity();
//        generatePopup(context, isAppActive, message);
//        新しいActivityを作って表示
//        Intent intent_new = new Intent(context,PopupActivity.class);
//        intent_new.putExtra(WARNING_MESSAGE, message);
//        intent_new.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent_new);
    }
    
    
    @Override
    protected void onDeletedMessages(Context context, int total) {
        Log.i(TAG, "Received deleted messages notification");
        String message = getString(R.string.gcm_deleted, total);
        displayMessage(context, message);
        // notifies user
        generateNotification(context, message);
    }

    @Override
    public void onError(Context context, String errorId) {
        Log.i(TAG, "Received error: " + errorId);
        displayMessage(context, getString(R.string.gcm_error, errorId));
    }

    @Override
    protected boolean onRecoverableError(Context context, String errorId) {
        // log message
        Log.i(TAG, "Received recoverable error: " + errorId);
        displayMessage(context, getString(R.string.gcm_recoverable_error,
                errorId));
        return super.onRecoverableError(context, errorId);
    }

    /**
     * \breif Issues a notification to inform the user that server has sent a message with sensor location.
     */
    //make notification on upper side status bar 
    private static void generateNotificationWithLocation(Context context, String message,Location loc) {
        int icon = R.drawable.ic_stat_gcm;
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, message, when);
        String title = context.getString(R.string.app_name);
        Intent notificationIntent = new Intent(context, GoogleMapActivity.class);
        notificationIntent.putExtra(LATITUDE, loc.getLatitude());
        notificationIntent.putExtra(LONGITUDE, loc.getLongitude());
        notificationIntent.putExtra(WARNING_MESSAGE, message);
        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, title, message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, notification);
    }
    
    /**
     * \breif Issues a notification to inform the user that server has sent a message.
     */
    private static void generateNotification(Context context, String message) {
        int icon = R.drawable.ic_stat_gcm;
        long when = System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, message, when);
        String title = context.getString(R.string.app_name);
        Intent notificationIntent = new Intent(context, GoogleMapActivity.class);

        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, title, message, intent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, notification);
    }
    
    /***
     * \brief Try to get current location.
     * @param context application's context.
     * @param sensorLocation sensor location sent from server.
     * @param message message about sensor value.
     */
    private void startLocationService(final Context context,final Location sensorLocation,final String message){
    	stopLocationService();
    	
//    	Log.d("location", "location service started");

    	locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    	
    	if(locationManager == null){
    		return;
    	}
    	Criteria criteria = new Criteria();    
    	String provider = locationManager.getBestProvider(criteria, true);
    	
    	Location tmpLocation = locationManager.getLastKnownLocation(provider);
    	if(!tmpLocation.equals(lastKnownLocation)){
    		lastKnownLocation=tmpLocation;
    		setLocation(context,lastKnownLocation,sensorLocation,message);
    	}
    	else{
    	
	    	locationListener = new LocationListener(){
	    		@Override
	    		public void onLocationChanged(final Location location){
	    
	    			setLocation(context,lastKnownLocation,sensorLocation,message);
	    			
	    			locationManager.removeUpdates(this);
	    		}
	
				@Override
				public void onProviderDisabled(String arg0) {
					// TODO Auto-generated method stub
					
				}
	
				@Override
				public void onProviderEnabled(String provider) {
					// TODO Auto-generated method stub
					
				}
	
				@Override
				public void onStatusChanged(String provider, int status,
						Bundle extras) {
					// TODO Auto-generated method stub
					generateNotification(context, String.valueOf(status));
					
				}
	    	};
	    	
	    	locationManager.requestLocationUpdates(provider, 0,2, locationListener);
    	}
    	
    }
    
    /****
     *  \brief Kill LocationManager and LocationListener.
     */
    private void stopLocationService(){
    	
    	if(locationManager !=null){
    		if(locationListener !=null){
    			locationManager.removeUpdates(locationListener);
    			locationListener = null;
    		}
    		locationManager = null;
    	}
    }
    
    
    /***
     * 
     * @param context application's context.
     * @param currentLocation current location got by Android.
     * @param sensorLocation sensor location got by PUSH notification.
     * @param message message message about sensor value.
     */
    private void setLocation( Context context,Location currentLocation, Location sensorLocation,String message){
    	stopLocationService();
    	
    	//if sensor is in 10km distance
    	if(currentLocation.distanceTo(sensorLocation)<10*1E3){
    		
    		//check whether GoogleMapActivity is active or not
			ActivityManager activityManager = ((ActivityManager) getSystemService(ACTIVITY_SERVICE));
			List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);
	       
			if(taskInfo!=null && !taskInfo.get(0).topActivity.getClassName().equals(GoogleMapActivity.class.getName())){
				//only in the case that the activity is active, make a popup 
	        	generatePopup(context,message,sensorLocation);
	        	Log.d("pin", taskInfo.get(0).topActivity.getClassName()+":"+GoogleMapActivity.class.getName());
	        }
			CommonUtilities.displayMessage(context, message,sensorLocation );
    		generateNotificationWithLocation(context, message,sensorLocation);	//ステータスバーに表示
    		//generatePopup(context, message, sensorLocation);
    		//generateNotification(context, "here is setLocation"+currentLocation.toString());
    	}
    	//もし位置が近かったら通知する
    	//Toast.makeText(context, "current="+currentGeo.getLatitudeE6()+" "+currentGeo.getLongitudeE6(), Toast.LENGTH_LONG).show();
    	
    	//String message = getString(R.string.gcm_message);
//        displayMessage(context, message,latstr,lonstr);	//アプリ上で通知
//        // notifies user
//        generateNotificationWithLocation(context, message,latstr,lonstr);	//ステータスバーに表示
//        
//        ActivityManager activityManager = ((ActivityManager) getSystemService(ACTIVITY_SERVICE));
//        List<ActivityManager.RunningTaskInfo> taskInfo = activityManager.getRunningTasks(1);
//        
//        if(taskInfo!=null && taskInfo.get(0).topActivity.getClassName()!=GoogleMapActivity.class.getName()){
//        	generatePopup(context,message,latstr,lonstr);
//        }
    }
    

}
