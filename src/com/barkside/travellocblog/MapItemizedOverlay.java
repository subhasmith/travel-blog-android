package com.barkside.travellocblog;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class MapItemizedOverlay extends ItemizedOverlay
{
   private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
   Context mContext;
   
   public MapItemizedOverlay(Drawable defaultMarker, Context context)
   {
      super(boundCenterBottom(defaultMarker));
      mContext = context;
   }

   public void addOverlay(OverlayItem overlay)
   {
      mOverlays.add(overlay);
      populate();
   }
   
   /* onTap used to show the tapped location name and description */
   
   @Override
   protected boolean onTap(int index) {
      OverlayItem item = mOverlays.get(index);
      AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
      dialog.setTitle(item.getTitle());
      dialog.setMessage(item.getSnippet());
      dialog.show();
      return true;
      }

   /* This draws the lines between points on the map */
   public void draw(Canvas canvas, MapView mapv, boolean shadow){ 
      super.draw(canvas, mapv, shadow); 

      Paint mPaint = new Paint(); 
      mPaint.setDither(true); 
      mPaint.setColor(Color.BLUE); 
      mPaint.setStyle(Paint.Style.FILL_AND_STROKE); 
      mPaint.setStrokeJoin(Paint.Join.ROUND); 
      mPaint.setStrokeCap(Paint.Cap.ROUND); 
      mPaint.setStrokeWidth(4); 

      for(int i = 1; i < mOverlays.size(); i++)
      {
         OverlayItem item = mOverlays.get(i);
         OverlayItem item2 = mOverlays.get(i-1);
         
         GeoPoint gP1 = item.getPoint();
         GeoPoint gP2 = item2.getPoint();
   
         Point p1 = new Point(); 
         Point p2 = new Point(); 
   
         Path path = new Path(); 
   
         mapv.getProjection().toPixels(gP1, p1); 
         mapv.getProjection().toPixels(gP2, p2); 
   
         path.moveTo(p2.x, p2.y); 
         path.lineTo(p1.x,p1.y); 
   
         canvas.drawPath(path, mPaint); 
      }
  } 

   @Override
   protected OverlayItem createItem(int i)
   {
      return mOverlays.get(i);
   }

   @Override
   public int size()
   {
      return mOverlays.size();
   }

}
