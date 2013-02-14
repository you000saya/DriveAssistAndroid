/****************************************************************//**
* \file 	CommonUtilities.java
* \author 	Yuka Hasegawa
* \version 	1.0
* \date 	11.2012
*
* \brief 	Common utilities class
* \details 	Common Utilities are gathered here
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

import static com.appspot.drivenotification.app.CommonUtilities.LATITUDE;
import static com.appspot.drivenotification.app.CommonUtilities.LONGITUDE;
import static com.appspot.drivenotification.app.CommonUtilities.WARNING_MESSAGE;

import com.google.android.maps.GeoPoint;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

/**
 * Helper class providing methods and constants common to other classes in the
 * app.
 */
public final class CommonUtilities {

    /**
     * Base URL of the Demo Server (such as http://my_host:8080/gcm-demo)
     */
    static final String SERVER_URL = "http://drivenotification.appspot.com";

    /**
     * Google API project id registered to use GCM.
     */
    static final String SENDER_ID = "447754804480";

    /**
     * Tag used on log messages.
     */
    static final String TAG = "GCMDemo";

    /**
     * Intent used to display a message in the screen.
     */
    static final String DISPLAY_MESSAGE_ACTION =
            "com.google.android.gcm.demo.app.DISPLAY_MESSAGE";

    /**
     * Intent's extra that contains the message to be displayed.
     */
    static final String EXTRA_MESSAGE = "message";
    static final String LATITUDE = "latitude";
    static final String LONGITUDE = "longitude";
    
  
    static final String WARNING_MESSAGE="warning";
    static final String IS_APP_ACTIVE="isAppActive";
    
    /**
     * Notifies UI to display a message.
     * <p>
     * This method is defined in the common helper because it's used both by
     * the UI and the background service.
     *
     * @param context application's context.
     * @param message message to be displayed.
     * @param loc location that road side sensor has.
     */
    static void displayMessage(Context context, String message,Location loc) {
        Intent intent = new Intent(DISPLAY_MESSAGE_ACTION);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(LATITUDE, loc.getLatitude());
        intent.putExtra(LONGITUDE, loc.getLongitude());
        context.sendBroadcast(intent);
        
    }
    
    //broad cast message only
    /***
     *\brief Display message only
     * @param context application's context.
     * @param message message to be displayed.
     */
    static void displayMessage(Context context,String message){
    	Intent intent = new Intent(DISPLAY_MESSAGE_ACTION);
    	intent.putExtra(EXTRA_MESSAGE, message);
    	context.sendBroadcast(intent);
    }
    
    
    /**
     * \brief Make Popup to warn user.
     * @param context application's context.
     * @param message message to be displayed.
     * @param loc location that road side sensor has.
     */
    static void generatePopup(Context context,String message, Location loc){
    	Intent intent_new = new Intent(context,PopupActivity.class);
        intent_new.putExtra(WARNING_MESSAGE, message);
        intent_new.putExtra(LATITUDE, loc.getLatitude());
        intent_new.putExtra(LONGITUDE, loc.getLongitude());
        intent_new.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent_new);
    	
    }
}
