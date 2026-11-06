package com.rin23.app;

import android.util.AtomicFile;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.*;

final class KeySync {
 static final String URL="https://cf-pages-keys.pages.dev/public/skye.bin";
 static final String KEY="Nz9K1WvfZ5bEN2rjpzyURRDIB7zJ1ojb";
 private static final String HEADER="MarketUUID\tManifestUUID\tTypePack\tKey";
 static int validate(byte[] bytes)throws Exception{
  return KeyDataPolicy.validate(bytes);
 }
 static byte[] decode(byte[] raw)throws Exception{
  if(raw.length<=16||(raw.length-16)%16!=0)throw new IOException("invalid ciphertext");
  Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");
  cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8),"AES"),new IvParameterSpec(Arrays.copyOfRange(raw,0,16)));
  byte[] data=cipher.doFinal(raw,16,raw.length-16);return data;
 }
 static int cachedCount(File cache)throws Exception{
  try(InputStream in=new AtomicFile(cache).openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1){if(out.size()+n>KeyDataPolicy.MAX_BYTES)throw new IOException("cache too large");out.write(b,0,n);}return validate(out.toByteArray());
  }
 }
 static String sync(File folder)throws Exception{
  File cache=new File(folder,"keys.tsv");
  // Native engine still refreshes its keys. Do not make warm launches download twice.
  try {int count=cachedCount(cache);return "ใช้ฐานคีย์ในเครื่อง "+count+" รายการ";}catch(Exception ignored){}
  try{
   HttpURLConnection c=(HttpURLConnection)new URL(URL).openConnection();
   c.setInstanceFollowRedirects(false);c.setConnectTimeout(8000);c.setReadTimeout(10000);c.setRequestProperty("User-Agent",AppVersion.USER_AGENT);
   byte[] raw;
   try{if(c.getResponseCode()!=200)throw new IOException("HTTP response");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
    byte[] b=new byte[32768];int n;long deadline=System.nanoTime()+20000000000L;
    while((n=in.read(b))!=-1){if(out.size()+n>20000000||System.nanoTime()>deadline)throw new IOException("size or time limit");out.write(b,0,n);}raw=out.toByteArray();
   }}finally{c.disconnect();}
   byte[] data=decode(raw);int count=validate(data);
   AtomicFile atomic=new AtomicFile(cache);FileOutputStream output=null;
   try{output=atomic.startWrite();output.write(data);atomic.finishWrite(output);}catch(Exception e){if(output!=null)atomic.failWrite(output);throw e;}
   return "ฐานคีย์ล่าสุด "+count+" รายการ";
  }catch(Exception e){
   try(FileInputStream in=new AtomicFile(cache).openRead()){
    ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1){if(out.size()+n>20000000)throw new IOException("cache too large");out.write(b,0,n);}validate(out.toByteArray());
    return "อัปเดตไม่ได้ • ใช้ฐานคีย์สำรอง";
   }catch(Exception ignored){throw new IOException("อัปเดตฐานข้อมูลไม่ได้ กรุณาตรวจอินเทอร์เน็ตแล้วลองใหม่");}
  }
 }
}
