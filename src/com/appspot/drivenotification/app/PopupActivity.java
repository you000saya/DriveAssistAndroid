/****************************************************************//**
* \file 	PopupActivity.java
* \author 	Yuka Hasegawa
* \version 	1.0
* \date 	11.2012
*
* \brief 	Popup Activity to warn user
* \details 	This is used when the application is not top on the device
********************************************************************/


package com.appspot.drivenotification.app;

import static com.appspot.drivenotification.app.CommonUtilities.*;

import com.appspot.drivenotification.app.R;
import com.google.android.maps.MapActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PopupActivity extends Activity {
	
    private final int FLAG_DISMISS_KEYGUARD = 
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            ;
    
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.popup);
	
		final String message = getIntent().getStringExtra(WARNING_MESSAGE);
		//public String isAppActive = getIntent().getStringExtra(IS_APP_ACTIVE);
		final double longitude = getIntent().getDoubleExtra(LONGITUDE,-1);
		final double latitude = getIntent().getDoubleExtra(LATITUDE,-1);
		
		TextView tvv = (TextView)findViewById(R.id.textview_popup_value);
		tvv.setText("Warning!\n"+"The temperature is\n "+message+" ℃");
		
		Button button_open_app = (Button)findViewById(R.id.button_open_app);
        button_open_app.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				//今見えているビューを閉じる
				//wm.removeView(view);
				getWindow().clearFlags(FLAG_DISMISS_KEYGUARD);
				
				//アプリを開く
				Intent intent = new Intent(getApplicationContext(),GoogleMapActivity.class);
				intent.putExtra(LONGITUDE, longitude);
				intent.putExtra(LATITUDE, latitude);
				intent.putExtra(WARNING_MESSAGE, message);
				startActivity(intent);
				
				finish();
			}
		});
        
        Button button_cancel = (Button)findViewById(R.id.button_cancel);
        button_cancel.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				//今見えているビューを消すだけ
				//wm.removeView(view);
				getWindow().clearFlags(FLAG_DISMISS_KEYGUARD);
				finish();
			}
        	
        });
		
	}
	
	@Override
    protected void onResume(){
        super.onResume();
        getWindow().addFlags(FLAG_DISMISS_KEYGUARD);
    }
	
	@Override
    protected void onStop(){
        super.onStop();
        getWindow().clearFlags(FLAG_DISMISS_KEYGUARD);
    }
	
}
