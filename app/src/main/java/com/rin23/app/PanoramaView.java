package com.rin23.app;
import android.content.Context;
import android.view.*;
import android.widget.HorizontalScrollView;
/** Pan a single enlarged panorama; a vertical gesture still scrolls the detail page. */
final class PanoramaView extends HorizontalScrollView {
 private float x,y;
 PanoramaView(Context c){super(c);setHorizontalScrollBarEnabled(true);setFillViewport(false);setOverScrollMode(OVER_SCROLL_NEVER);}
 @Override public boolean onInterceptTouchEvent(MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN){x=e.getX();y=e.getY();}else if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&Math.abs(e.getX()-x)>Math.abs(e.getY()-y)+8){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);}return super.onInterceptTouchEvent(e);}
 @Override public boolean onTouchEvent(MotionEvent e){boolean handled=super.onTouchEvent(e);if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}return handled;}
}
