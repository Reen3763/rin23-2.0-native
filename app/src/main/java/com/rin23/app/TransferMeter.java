package com.rin23.app;
import java.io.*;
import java.net.*;
import java.util.*;
final class TransferMeter {
 private final Map<String,Long> seen=new HashMap<>();
 void reset(){seen.clear();}
 long sample(File root){scan(root,0);long n=0;for(long size:seen.values())n+=size;return n;}
 private void scan(File root,int depth){if(depth>4)return;File[] files=root.listFiles();if(files==null)return;for(File file:files){if(file.isDirectory())scan(file,depth+1);else if(file.getName().equals("downloaded_dlc.zip")){String key=file.getAbsolutePath();seen.put(key,Math.max(seen.containsKey(key)?seen.get(key):0,file.length()));}}}
 static long size(List<String> urls){if(urls.isEmpty())return -1;long total=0;for(String url:urls){HttpURLConnection c=null;try{if(!RemotePolicy.https(url))return -1;c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(7000);c.setReadTimeout(7000);c.setRequestMethod("HEAD");c.setRequestProperty("User-Agent","libhttpclient/1.0.0.0");long n=c.getContentLengthLong();if(c.getResponseCode()!=200||n<=0)return -1;total=Math.addExact(total,n);}catch(Exception e){return -1;}finally{if(c!=null)c.disconnect();}}return total;}
 static int percent(long done,long total){if(total<=0)return -1;return (int)Math.max(0,Math.min(100,Math.floor(100.0*done/total)));}
 static String bytes(long n){if(n<0)return "ไม่ทราบขนาด";if(n<1024)return n+" B";double value=n;String[] units={"B","KB","MB","GB","TB"};int u=0;while(value>=1024&&u<units.length-1){value/=1024;u++;}return String.format(Locale.ROOT,"%.1f %s",value,units[u]);}
}
