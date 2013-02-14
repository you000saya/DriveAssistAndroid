/****************************************************************//**
* \file 	GeocodeManager.java
* \author 	Yuka Hasegawa
* \version 	1.0
* \date 	11.2012
*
* \brief 	Address convert class
* \details 	Convert location(latitude,longitude) to address
********************************************************************/
package com.appspot.drivenotification.app;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

public class GeocodeManager {

	    /***
	     * \brief Convert location to address
	     * @param latitude 
	     * @param longitude
	     * @param context
	     * @return String address
	     * @throws IOException
	     */
		public static String point2address(double latitude, double longitude, Context context)
			throws IOException
		{
			String address_string = new String();

			Geocoder coder = new Geocoder(context, Locale.ENGLISH);
			List<Address> list_address = coder.getFromLocation(latitude, longitude, 1);

			if (!list_address.isEmpty()){

				// if decode succeed, get first result
				Address address = list_address.get(0);
				StringBuffer sb = new StringBuffer();

				// connect address into one string
				String s;
				for (int i = 0; (s = address.getAddressLine(i)) != null; i++){
					sb.append( s+" "/* + "\n" */);
				}

				address_string = sb.toString();
			}

			return address_string;
		}
}
