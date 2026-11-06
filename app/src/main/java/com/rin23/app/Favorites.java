package com.rin23.app;
import android.content.Context;
import android.content.SharedPreferences;
/** Shared store for list and detail screens in the same app process. */
final class Favorites {
 private static FavoriteStore instance;
 static synchronized FavoriteStore get(Context context)throws Exception{
  if(instance==null){final SharedPreferences prefs=context.getApplicationContext().getSharedPreferences("rin23_favorites",0);instance=new FavoriteStore(new FavoriteStore.Storage(){public String load(){return prefs.getString("items","[]");}public boolean save(String json){return prefs.edit().putString("items",json).commit();}});}return instance;
 }
}
