package com.barkside.travellocblog;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class BlogListAdapter extends BaseAdapter
{

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

      ((TextView) convertView.findViewById(R.id.titleText)).setText(mItems.get(
            position).getNameText());
      ((TextView) convertView.findViewById(R.id.dexcriptionText))
            .setText(mItems.get(position).getDetailText());
      return convertView;
   }

}