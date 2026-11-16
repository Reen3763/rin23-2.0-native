package com.rin23.app;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

final class NewReleaseBadge extends TextView {
 NewReleaseBadge(Context context) {
  super(context);
  setText("ใหม่");
  setTextSize(12);
  setTypeface(Typeface.DEFAULT, Typeface.BOLD);
  setTextColor(0xff281908);
  setGravity(Gravity.CENTER);
  setPadding(dp(11), dp(4), dp(11), dp(4));

  GradientDrawable background = new GradientDrawable();
  background.setColor(0xffffd27b);
  background.setCornerRadius(dp(20));
  setBackground(background);

  FrameLayout.LayoutParams position = new FrameLayout.LayoutParams(
      FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
      Gravity.TOP | Gravity.START);
  position.topMargin = dp(8);
  position.setMarginStart(dp(8));
  setLayoutParams(position);
  setVisibility(GONE);
 }

 void bind(Protocol.Item item) {
  setVisibility(CatalogClient.isNew(item, System.currentTimeMillis()) ? VISIBLE : GONE);
 }

 private int dp(int value) {
  return Math.round(value * getResources().getDisplayMetrics().density);
 }
}
