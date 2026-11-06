package com.rin23.app;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.GradientDrawable;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

/** Generated PNG assets, tinted at runtime for consistent active and inactive states. */
final class AppIcons {
 private static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 private static Drawable drawable(Context c,int resource,int color,int size){Drawable d=c.getDrawable(resource).mutate();d.setTint(color);d.setBounds(0,0,dp(c,size),dp(c,size));return d;}
 static void apply(TextView view,int resource,int color,int size){view.setCompoundDrawables(drawable(view.getContext(),resource,color,size),null,null,null);view.setCompoundDrawablePadding(view instanceof android.widget.Button&&view.length()==0?0:dp(view.getContext(),8));}
 static void applyTop(TextView view,int resource,int color,int size){view.setCompoundDrawables(null,drawable(view.getContext(),resource,color,size),null,null);view.setCompoundDrawablePadding(dp(view.getContext(),5));}
 static ImageView view(Context context,int resource,int color,int size){ImageView v=new ImageView(context);v.setImageDrawable(drawable(context,resource,color,size));v.setScaleType(ImageView.ScaleType.FIT_CENTER);v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return v;}
 static void ripple(View view,GradientDrawable background){view.setBackground(new RippleDrawable(ColorStateList.valueOf(ThemeActivity.light(view.getContext())?0x16000000:0x22ffffff),background,null));}
}
