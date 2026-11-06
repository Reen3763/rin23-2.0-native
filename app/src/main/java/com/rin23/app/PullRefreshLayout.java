package com.rin23.app;
import android.content.Context;
import android.view.*;
import android.widget.*;

/** Intercepts only a downward vertical gesture while the catalog is at its top. */
final class PullRefreshLayout extends FrameLayout {
 interface Refresh {boolean canRefresh();void refresh();}
 private ListView target;private Refresh action;private final TextView indicator;private float downX,downY;private boolean pulling,eligible;private final float threshold,slop;
 PullRefreshLayout(Context c){super(c);threshold=72*c.getResources().getDisplayMetrics().density;slop=ViewConfiguration.get(c).getScaledTouchSlop();indicator=new TextView(c);indicator.setTextColor(new ThemePalette(ThemeActivity.light(c)).accent);indicator.setTextSize(12);indicator.setGravity(Gravity.CENTER);indicator.setVisibility(GONE);addView(indicator,new FrameLayout.LayoutParams(-1,(int)(56*c.getResources().getDisplayMetrics().density),Gravity.TOP));}
 void attach(ListView view,Refresh callback){target=view;action=callback;addView(view,0,new FrameLayout.LayoutParams(-1,-1));}
 private boolean top(){return target!=null&&!target.canScrollVertically(-1);}
 public boolean onInterceptTouchEvent(android.view.MotionEvent e){switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:downX=e.getX();downY=e.getY();pulling=false;eligible=top()&&action!=null&&action.canRefresh();break;case MotionEvent.ACTION_MOVE:float dy=e.getY()-downY,dx=Math.abs(e.getX()-downX);if(eligible&&top()&&dy>slop&&dy>dx*1.4f){pulling=true;return true;}break;case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:reset();break;}return false;}
 public boolean onTouchEvent(MotionEvent e){if(!pulling)return super.onTouchEvent(e);float delta=Math.max(0,e.getY()-downY);if(e.getActionMasked()==MotionEvent.ACTION_MOVE){float travel=Math.min(threshold*1.5f,delta*0.5f);target.setTranslationY(travel);indicator.setVisibility(VISIBLE);indicator.setText(delta>=threshold?"ปล่อยเพื่อรีเฟรช":"ดึงลงเพื่อรีเฟรช");return true;}if(e.getActionMasked()==MotionEvent.ACTION_UP){boolean refresh=delta>=threshold&&top()&&action.canRefresh();reset();if(refresh)action.refresh();return true;}if(e.getActionMasked()==MotionEvent.ACTION_CANCEL){reset();return true;}return true;}
 private void reset(){pulling=false;eligible=false;if(target!=null)target.setTranslationY(0);indicator.setVisibility(GONE);}
}
