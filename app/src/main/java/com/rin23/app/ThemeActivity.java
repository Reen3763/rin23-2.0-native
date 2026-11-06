package com.rin23.app;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
/** Theme changes rebuild views only; the foreground engine keeps running. */
public abstract class ThemeActivity extends Activity {
 protected ThemePalette palette;protected int BG,PANEL,RED,TEXT,MUTED,ON_ACCENT,BORDER,SOFT;
 static boolean light(android.content.Context c){return c.getSharedPreferences("appearance",0).getBoolean("light",false);}
 @Override public void onCreate(Bundle state){palette=new ThemePalette(light(this));
 setTheme(palette.light?R.style.AppThemeLight:R.style.AppTheme);
 BG=palette.background;PANEL=palette.surface;RED=palette.accent;TEXT=palette.text;MUTED=palette.muted;ON_ACCENT=palette.onAccent;BORDER=palette.border;SOFT=palette.soft;
 super.onCreate(state);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
 int flags=getWindow().getDecorView().getSystemUiVisibility();int lightFlags=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
 getWindow().getDecorView().setSystemUiVisibility(palette.light?flags|lightFlags:flags&~lightFlags);
 }
 @Override protected void onResume(){super.onResume();if(palette.light!=light(this))recreate();}
 protected void setLightTheme(boolean light){if(light==palette.light)return;getSharedPreferences("appearance",0).edit().putBoolean("light",light).apply();recreate();}
}
