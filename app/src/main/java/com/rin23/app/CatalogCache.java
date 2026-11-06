package com.rin23.app;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
final class CatalogCache {
 private final File folder;
 CatalogCache(File root){folder=new File(root,"catalog-cache");}
 private File file(String key)throws Exception{byte[] hash=MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255));return new File(folder,s+".json");}
 synchronized String get(String key)throws Exception{File f=file(key);long age=System.currentTimeMillis()-f.lastModified();if(!f.isFile()||age<0||age>7L*24*3600000||f.length()>12*1024*1024)throw new IOException("no cached catalog");return new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8);}
 synchronized void put(String key,String value)throws Exception{byte[] data=value.getBytes(StandardCharsets.UTF_8);if(data.length>12*1024*1024)return;folder.mkdirs();File f=file(key),tmp=File.createTempFile(".catalog-",".tmp",folder);try{Files.write(tmp.toPath(),data);Files.move(tmp.toPath(),f.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}finally{tmp.delete();}File[] files=folder.listFiles();if(files!=null){Arrays.sort(files,Comparator.comparingLong(File::lastModified).reversed());long bytes=0;for(int i=0;i<files.length;i++){bytes+=files[i].length();if(i>=40||bytes>32L*1024*1024)files[i].delete();}}}
}
