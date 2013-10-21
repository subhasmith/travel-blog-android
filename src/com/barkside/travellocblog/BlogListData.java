package com.barkside.travellocblog;


/* Simple class to store the details in the main blog listView */
public class BlogListData implements Comparable<BlogListData>{
    
	private String mNameText = "";
	private String mDetailText = "";
	private boolean mSelectable = true;
	private long mTimeStamp = 0;

	public BlogListData(String nameText,
			String detailText) {

		mNameText = nameText;
		mDetailText = detailText;
		mSelectable = false;
		mTimeStamp = 0;
	}
	
	public boolean isSelectable() {
		return mSelectable;
	}
	
	public void setSelectable(boolean selectable) {
		mSelectable = selectable;
	}
	
	public String getNameText() {
		return mNameText;
	}
	
	public String getDetailText() {
		return mDetailText;
	}
  public long getTimeStamp() {
      return mTimeStamp;
   }
	/** Make FriendListData comparable by its name */
	@Override
	public int compareTo(BlogListData other) {
		if(this.mNameText != null)
			return this.mNameText.compareTo(other.getNameText()); 
		else 
			throw new IllegalArgumentException();
	}
}
