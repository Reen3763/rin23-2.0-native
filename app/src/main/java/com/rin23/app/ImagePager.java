package com.rin23.app;
import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.*;

final class ImagePager extends HorizontalScrollView {
 private final LinearLayout strip;private final TextView dots;private final List<ImageView> pages=new ArrayList<>();private float startX,startY;private boolean horizontal=false,flung=false;private int downPage;
 ImagePager(Context c,TextView indicator){super(c);dots=indicator;setHorizontalScrollBarEnabled(false);setFillViewport(true);setOverScrollMode(OVER_SCROLL_NEVER);strip=new LinearLayout(c);strip.setClipChildren(true);setClipChildren(true);addView(strip,new LayoutParams(-2,-1));setOnScrollChangeListener((v,x,y,oldX,oldY)->showDots());}
 void pages(List<ImageView> images) {
  if (pages.equals(images)) return;
  int width = getWidth();
  int selected = width > 0 ? Math.round((float) getScrollX() / width) : 0;
  ImageView current = selected < pages.size() ? pages.get(selected) : null;
  strip.removeAllViews();
  pages.clear();
  pages.addAll(images);
  for (ImageView image : pages) {
   if (image.getParent() != null) ((ViewGroup) image.getParent()).removeView(image);
   strip.addView(image, new LinearLayout.LayoutParams(width, LayoutParams.MATCH_PARENT));
  }
  int retained = pages.indexOf(current);
  snapAfterLayout(retained >= 0 ? retained : selected);
 }

 private int pendingPage = -1;

 private void snapAfterLayout(int page) {
  pendingPage = Math.max(0, Math.min(page, pages.size() - 1));
  requestLayout();
 }

 @Override protected void onMeasure(int widthSpec, int heightSpec) {
  int width = MeasureSpec.getSize(widthSpec);
  for (ImageView image : pages) image.getLayoutParams().width = width;
  super.onMeasure(widthSpec, heightSpec);
 }

 @Override protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
  super.onSizeChanged(w, h, oldWidth, oldHeight);
  if (oldWidth > 0 && w != oldWidth && pendingPage < 0) {
   pendingPage = Math.round((float) getScrollX() / oldWidth);
  }
 }

 @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
  super.onLayout(changed, l, t, r, b);
  if (pendingPage >= 0) {
   scrollTo(pendingPage * getWidth(), 0);
   pendingPage = -1;
  }
  showDots();
 }
 private void showDots(){int n=pages.size(),selected=getWidth()>0?Math.round((float)getScrollX()/getWidth()):0;StringBuilder s=new StringBuilder();for(int i=0;i<n;i++)s.append(i==selected?"●  ":"○  ");String labels=s.toString().trim();if(!labels.contentEquals(dots.getText()))dots.setText(labels);dots.setContentDescription("รูป "+Math.min(n,selected+1)+" จาก "+n);}
 @Override public boolean onTouchEvent(MotionEvent e){switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:startX=e.getX();startY=e.getY();horizontal=false;flung=false;downPage=getWidth()>0?Math.round((float)getScrollX()/getWidth()):0;break;case MotionEvent.ACTION_MOVE:if(Math.abs(e.getX()-startX)>Math.abs(e.getY()-startY)+8){horizontal=true;getParent().requestDisallowInterceptTouchEvent(true);}break;}
  boolean result=super.onTouchEvent(e);if(e.getActionMasked()==MotionEvent.ACTION_UP){int page=getWidth()>0?Math.round((float)getScrollX()/getWidth()):0;if(!flung)post(()->smoothScrollTo(page*getWidth(),0));getParent().requestDisallowInterceptTouchEvent(false);}else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)getParent().requestDisallowInterceptTouchEvent(false);return result;
 }
 @Override public void fling(int velocity){int width=getWidth();if(width<=0)return;flung=true;int current=downPage;int target=Math.max(0,Math.min(pages.size()-1,current+(velocity>0?1:-1)));smoothScrollTo(target*width,0);}
}
