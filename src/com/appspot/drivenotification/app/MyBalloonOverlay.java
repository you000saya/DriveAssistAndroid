/****************************************************************//**
* \file 	MyBalloonOverlay.java
* \author 	Yuka Hasegawa
* \version 	1.0
* \date 	11.2012
*
* \brief 	Map overlay
* \details 	Display ballons on main map
********************************************************************/

package com.appspot.drivenotification.app;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay;

public class MyBalloonOverlay extends BalloonItemizedOverlay<OverlayItem> {

	private static final int NOT_GEOPOINT = -1;
	private ArrayList<OverlayItem> mItems = new ArrayList<OverlayItem>();
    private Context mCtx;
    
    /****
     * \brief Constructor.
     * @param defaultMarker picture used to show place on map.
     * @param mapView main MapView.
     */
	public MyBalloonOverlay(Drawable defaultMarker, MapView mapView) {
		super(boundCenterBottom(defaultMarker), mapView);//ここでピンの影の位置を指定
		// TODO Auto-generated constructor stub
		mCtx = mapView.getContext();
	}

	/***
	 * 
	 */
	@Override
	protected OverlayItem createItem(int i) {
		// TODO Auto-generated method stub
		return mItems.get(i);
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return mItems.size();
	}
	
	/***
	 * 
	 * @param oi OverlayItem to be added to map
	 */
	public void addItem(OverlayItem oi){
		if (getIndexGeoPoint(oi.getPoint()) == NOT_GEOPOINT) {//すでに同じ位置にピンが刺さっていないかの確認
			mItems.add(oi);
			populate();
		}
	}
	
	/***
	 * 
	 */
	@Override
	protected boolean onBalloonTap(int index, OverlayItem item){
		 Toast.makeText(mCtx,
	                item.getTitle() + " " + item.getSnippet() + "\n" +
	                item.getPoint().getLatitudeE6()/1000000.0 + item.getPoint().getLongitudeE6()/1000000.0,
	                Toast.LENGTH_LONG).show();
        return true;
	}
	
	/**
    * 新しい位置を追加する。但し、同じ位置がリストに存在したら追加しない。
    * @param point 位置
    * @param markerText マーカーに付随する文字列
    * @param snippet 断片文字列
    */
//   public void addNewItem(GeoPoint point, String markerText, String snippet) {
//      if (getIndexGeoPoint(point) == NOT_GEOPOINT) {
//         mItems.add(new OverlayItem(point, markerText, snippet));
//         populate();
//      }
//   }

	/***
	 * \brief If the return value is -1, there isn't that point yet.
	 * @param newPoint point to be checked index.
	 * @return int index.
	 */
   private int getIndexGeoPoint(GeoPoint newPoint) {
      int result = NOT_GEOPOINT;
      int size = mItems.size();
      for (int i = 0; i < size; i++) {
         OverlayItem item = mItems.get(i);
         GeoPoint point = item.getPoint();
         if (point.equals(newPoint)) {
            result = i;
            break;
         }
      }
      return result;
   }

}
