package com.barkside.travellocblog;

import java.util.List;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class TripMapView extends MapActivity
{
   LinearLayout linearLayout;
   MapView mapView;
   List<Overlay> mapOverlays;
   Drawable drawable;
   MapItemizedOverlay itemizedOverlay;
   
   @Override
   protected void onCreate(Bundle arg0)
   {
      super.onCreate(arg0);
      setContentView(R.layout.map_trip);
      
      mapView = (MapView) findViewById(R.id.mapview);
      mapView.setBuiltInZoomControls(true);
      mapOverlays = mapView.getOverlays();
      
      Intent intent = getIntent();
      Bundle extras = intent.getExtras();
      /* Get the filename from an extra */
      String filename = extras.getString("TRIP");
      BlogData blogData = new BlogData();
      blogData.openBlog(filename);
       
      /* use a simple blue pointer */
      drawable = this.getResources().getDrawable(R.drawable.blue_dot);
      
      /* MapItemizedOverlay is a class that draws lines between points, and 
       * has the onTap() onClick function for each point to bring up a pop-up dialog 
       */
      itemizedOverlay = new MapItemizedOverlay(drawable, this);     
      
      int nwLat = -90 * 1000000;
      int nwLng = 180 * 1000000;
      int seLat = 90 * 1000000;
      int seLng = -180 * 1000000;
      
      /* we are trying to centre the entire trip on the map with some padding -
       * I got some of this from somewhere on the web but I can't remember where...
       */
      for(int i = 0; i < blogData.getMaxBlogElements(); i++)
      {
         BlogElement blog = blogData.getBlogElement(i);
         if((blog.name == null)||(blog.location == null)||
            (blog.location.length()==0)||(blog.name.length()==0))
         {
            continue;
         }
         String[] temp;
         float lat, lon;
         try
         {        
            temp = blog.location.split(",");
            if (temp.length < 2)
            {
               continue;
            }
            lon = Float.parseFloat(temp[0]);
            lat = Float.parseFloat(temp[1]);
         }
         catch(Exception e)
         {
            continue;
         }
         lon *= 1000000.0;
         lat *= 1000000.0;

         GeoPoint point = new GeoPoint(Math.round(lat),Math.round(lon));
         OverlayItem overlayItem = new OverlayItem(point, blog.name, blog.description);
         itemizedOverlay.addOverlay(overlayItem);
         nwLat = Math.max(nwLat, point.getLatitudeE6()); 
         nwLng = Math.min(nwLng, point.getLongitudeE6());
         seLat = Math.min(seLat, point.getLatitudeE6());
         seLng = Math.max(seLng, point.getLongitudeE6());
      }
      MapController mapController = mapView.getController();
      GeoPoint center = new GeoPoint((nwLat + seLat) / 2, (nwLng + seLng) / 2);
      // add padding in each direction
      int spanLatDelta = (int) (Math.abs(nwLat - seLat) * 1.1);
      int spanLngDelta = (int) (Math.abs(seLng - nwLng) * 1.1);
   
      // fit map to points
      mapController.setCenter(center);
      mapController.zoomToSpan(spanLatDelta, spanLngDelta);

      mapOverlays.add(itemizedOverlay);
   }

   @Override
   protected boolean isRouteDisplayed()
   {    
      return false;
      }
}
