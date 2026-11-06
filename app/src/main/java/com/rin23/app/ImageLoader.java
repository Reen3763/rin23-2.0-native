package com.rin23.app;
import android.content.Context;
import android.graphics.*;
import android.os.*;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

final class ImageLoader {
 interface Loaded {void ready(int width,int height);}
 private static final class Picture {final Bitmap bitmap;final int width,height;Picture(Bitmap b,int w,int h){bitmap=b;width=w;height=h;}}
 private final LruCache<String,Picture> memory=new LruCache<String,Picture>(12*1024*1024){protected int sizeOf(String k,Picture p){return p.bitmap.getByteCount();}};private final int maxEdge;private final java.util.concurrent.ConcurrentHashMap<String,Object> locks=new java.util.concurrent.ConcurrentHashMap<>();
 private final ExecutorService pool=new ThreadPoolExecutor(3,3,0L,TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>());private final Handler main=new Handler(Looper.getMainLooper());private final File folder;
 ImageLoader(Context c){this(c,560);}ImageLoader(Context c,int edge){maxEdge=edge;folder=new File(c.getCacheDir(),"covers");folder.mkdirs();}
 void load(String url,ImageView view){load(url,view,null);}
 void load(String url,ImageView view,Loaded callback){view.setTag(url);view.animate().cancel();view.clearAnimation();view.setAlpha(1f);view.setImageAlpha(255);view.setImageTintList(null);view.clearColorFilter();view.setScaleType(ImageView.ScaleType.CENTER_CROP);if(!CatalogClient.safeImageUrl(url))return;view.setImageResource(R.drawable.rin_loading);android.view.animation.RotateAnimation spin=new android.view.animation.RotateAnimation(-3,3,1,.5f,1,.5f);spin.setDuration(700);spin.setRepeatMode(2);spin.setRepeatCount(-1);view.startAnimation(spin);Picture b=memory.get(url);if(b!=null){view.clearAnimation();view.setImageAlpha(255);view.setImageBitmap(b.bitmap);if(callback!=null)callback.ready(b.width,b.height);return;}
  pool.execute(()->{synchronized(locks.computeIfAbsent(url,k->new Object())){try{File file=new File(folder,hex(MessageDigest.getInstance("SHA-256").digest(url.getBytes("UTF-8"))));byte[] bytes;
   if(file.isFile()){bytes=read(new FileInputStream(file));file.setLastModified(System.currentTimeMillis());}else{
    HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setUseCaches(false);c.setRequestProperty("User-Agent",AppVersion.USER_AGENT);
    try{if(c.getResponseCode()!=200||!c.getURL().getProtocol().equals("https"))return;bytes=read(c.getInputStream());}finally{c.disconnect();}
    File tmp=File.createTempFile(".cover-",".tmp",folder);try{try(FileOutputStream out=new FileOutputStream(tmp)){out.write(bytes);}if(!tmp.renameTo(file))tmp.delete();}finally{if(tmp.exists())tmp.delete();}trim();
   }
   BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);if(opts.outWidth<=0||opts.outHeight<=0)return;
   final int originalWidth=opts.outWidth,originalHeight=opts.outHeight;opts.inSampleSize=1;if(callback!=null&&GalleryRules.panorama(originalWidth,originalHeight)){while(originalWidth/opts.inSampleSize>8192||originalHeight/opts.inSampleSize>2048||(long)(originalWidth/opts.inSampleSize)*(originalHeight/opts.inSampleSize)>6000000)opts.inSampleSize*=2;}else while(opts.outWidth/opts.inSampleSize>maxEdge||opts.outHeight/opts.inSampleSize>maxEdge)opts.inSampleSize*=2;opts.inJustDecodeBounds=false;
   Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);if(bitmap==null)return;Picture picture=new Picture(bitmap,originalWidth,originalHeight);memory.put(url,picture);main.post(()->{if(url.equals(view.getTag())){view.animate().cancel();view.clearAnimation();view.setAlpha(1f);view.setImageAlpha(255);view.setImageTintList(null);view.clearColorFilter();view.setImageBitmap(bitmap);if(callback!=null)callback.ready(picture.width,picture.height);}});
  }catch(Exception|OutOfMemoryError ignored){} }});
 }
 private byte[] read(InputStream source)throws Exception{try(InputStream in=source;ByteArrayOutputStream out=new ByteArrayOutputStream()){long deadline=System.nanoTime()+30000000000L;byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1){if(System.nanoTime()>deadline||out.size()+n>5*1024*1024)throw new IOException("image too large");out.write(b,0,n);}return out.toByteArray();}}
 private synchronized void trim(){File[] files=folder.listFiles();if(files==null)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified).reversed());long total=0;for(int i=0;i<files.length;i++){total+=files[i].length();if(i>=500||total>128L*1024*1024)files[i].delete();}}

 private String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}
 void close(){pool.shutdownNow();memory.evictAll();}
}
