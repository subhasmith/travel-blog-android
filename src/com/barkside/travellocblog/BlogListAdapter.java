package com.barkside.travellocblog;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class BlogListAdapter extends BaseAdapter
{

   // For logging and debugging purposes
   private static final String TAG = "BlogListAdapter";

   /** Remember our context so we can use it when constructing views. */
   private Context mContext;

   private List<BlogListData> mItems = new ArrayList<BlogListData>();

   public BlogListAdapter(Context context)
   {
      mContext = context;
   }

   public void addItem(BlogListData it)
   {
      mItems.add(it);
   }

   public void setListItems(List<BlogListData> lit)
   {
      mItems = lit;
   }

   /** @return The number of items in the */
   public int getCount()
   {
      return mItems.size();
   }

   public Object getItem(int position)
   {
      return mItems.get(position);
   }

   public boolean areAllItemsSelectable()
   {
      return false;
   }

   /** Use the array index as a unique id. */
   public long getItemId(int position)
   {
      return position;
   }

   public boolean isSelectable(int position)
   {
      try
      {
         return mItems.get(position).isSelectable();
      }
      catch (IndexOutOfBoundsException aioobe)
      {
         return this.isSelectable(position);
      }
   }

   /** Bind our Views */
   public View getView(int position, View convertView, ViewGroup parent)
   {
      /** Inflate our Context */
      LayoutInflater inflater = (LayoutInflater) mContext
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      /** Fill our Views */
      if (convertView == null)
      {
         convertView = inflater.inflate(R.layout.blog_list, parent, false);
      }
      
      BlogListData item = mItems.get(position);
      if (item == null){
         Log.w(TAG, "No item found - probably load KML file error?");
         return null;
      }

      /*
       * For both name and description, we want to display as much as will fit on one line.
       * If the text is actually multiple line, we combine the lines into a single line.
       * And we use TextView to truncate it as needed, and show ellipses (...) at the point
       * of truncation.
       * Ran into a ellipsize bug in TextView (putting ... in middle and still showing words
       * after that and not truncating), so now replace all cr lf chars from
       * name and description. Now works better. The ... char is shown at end of TextView line.
       */
      String text = item.getNameText().trim().replaceAll("\\r|\\n", " ");
      ((TextView) convertView.findViewById(R.id.titleText)).setText(text);
      
      text = item.getDetailText().trim().replaceAll("\\r|\\n", " ");
      ((TextView) convertView.findViewById(R.id.dexcriptionText)).setText(text);
      return convertView;
   }
}
