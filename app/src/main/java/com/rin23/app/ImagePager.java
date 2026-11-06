package com.rin23.app;
import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.*;

final class ImagePager extends HorizontalScrollView {
 private final LinearLayout strip;private final TextView dots;private final List<ImageView> pages=new ArrayList<>();private float startX,startY;private boolean horizontal=false,flung=false;private int downPage;
 ImagePager(Context c,TextView indicator){super(c);dots=indicator;setHorizontalScrollBarEnabled(false);setFillViewport(true);setOverScrollMode(OVER_SCROLL_NEVER);strip=new LinearLayout(c);addView(strip,new LayoutParams(-2,-1));setOnScrollChangeListener((v,x,y,oldX,oldY)->showDots());}
 void pages(List<ImageView> images){int selected=getWidth()>0?Math.round((float)getScrollX()/getWidth()):0;strip.removeAllViews();pages.clear();pages.addAll(images);for(ImageView image:pages){if(image.getParent()!=null)((ViewGroup)image.getParent()).removeView(image);strip.addView(image,new LinearLayout.LayoutParams(Math.max(1,getWidth()),-1));}final int index=Math.min(selected,Math.max(0,pages.size()-1));post(()->{resize();scrollTo(index*getWidth(),0);showDots();});}
 private void resize(){for(ImageView image:pages){ViewGroup.LayoutParams p=image.getLayoutParams();p.width=getWidth();image.setLayoutParams(p);}}
 @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);int page=ow>0?Math.round((float)getScrollX()/ow):0;resize();post(()->{scrollTo(page*w,0);showDots();});}
 private void showDots(){int n=pages.size(),selected=getWidth()>0?Math.round((float)getScrollX()/getWidth()):0;StringBuilder s=new StringBuilder();for(int i=0;i<n;i++)s.append(i==selected?"●  ":"○  ");dots.setText(s.toString().trim());dots.setContentDescription("รูป "+Math.min(n,selected+1)+" จาก "+n);}
 @Override public boolean onTouchEvent(MotionEvent e){switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:startX=e.getX();startY=e.getY();horizontal=false;flung=false;downPage=getWidth()>0?Math.round((float)getScrollX()/getWidth()):0;break;case MotionEvent.ACTION_MOVE:if(Math.abs(e.getX()-startX)>Math.abs(e.getY()-startY)+8){horizontal=true;getParent().requestDisallowInterceptTouchEvent(true);}break;}
  boolean result=super.onTouchEvent(e);if(e.getActionMasked()==MotionEvent.ACTION_UP){int page=getWidth()>0?Math.round((float)getScrollX()/getWidth()):0;if(!flung)post(()->smoothScrollTo(page*getWidth(),0));getParent().requestDisallowInterceptTouchEvent(false);}else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)getParent().requestDisallowInterceptTouchEvent(false);return result;
 }
 @Override public void fling(int velocity){int width=getWidth();if(width<=0)return;flung=true;int current=downPage;int target=Math.max(0,Math.min(pages.size()-1,current+(velocity>0?1:-1)));smoothScrollTo(target*width,0);}
}
