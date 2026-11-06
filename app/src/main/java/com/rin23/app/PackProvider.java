package com.rin23.app;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.FileNotFoundException;
import java.util.*;

/** Read-only bridge: keeps a Minecraft filename on the URI without copying large packs. */
public final class PackProvider extends ContentProvider {
 static final String AUTHORITY="com.rin23.app.packs";
 static String mime(String name){String n=name.toLowerCase(Locale.ROOT);for(String ext:new String[]{"mcpack","mcaddon","mcworld","mctemplate"})if(n.endsWith("."+ext))return "application/"+ext;throw new IllegalArgumentException("ชนิดไฟล์นี้นำเข้า Minecraft ไม่ได้");}
 static synchronized Uri share(Context context,String source,String name){
  mime(name);Uri original=Uri.parse(source);validateSource(original);
  if(name.indexOf('/')>=0||name.indexOf('\\')>=0||name.indexOf('\0')>=0)throw new IllegalArgumentException("ชื่อไฟล์ไม่ถูกต้อง");
  SharedPreferences prefs=context.getSharedPreferences("pack-import",0);
  SharedPreferences.Editor edit=prefs.edit();long now=System.currentTimeMillis();
  for(String old:prefs.getAll().keySet())if(SharePolicy.token(old)&&!SharePolicy.active(prefs.getLong(old+".issued",0),now)){
   edit.remove(old).remove(old+".name").remove(old+".issued");
   context.revokeUriPermission(new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(old).build(),Intent.FLAG_GRANT_READ_URI_PERMISSION);
  }
  String token=UUID.randomUUID().toString();
  if(!edit.putString(token,source).putString(token+".name",name).putLong(token+".issued",now).commit())throw new IllegalStateException("บันทึกข้อมูลนำเข้าไม่ได้");
  return new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(token).appendPath(name).build();
 }
 private static void validateSource(Uri u){List<String> p=u.getPathSegments();if(!"content".equals(u.getScheme())||!"media".equals(u.getAuthority())||p.size()!=3||!"downloads".equals(p.get(1))||!("external".equals(p.get(0))||"external_primary".equals(p.get(0)))||!p.get(2).matches("[1-9][0-9]*"))throw new IllegalArgumentException("ไฟล์ไม่ได้อยู่ในรายการดาวน์โหลด");}
 private Uri original(Uri u){
  List<String> p=u.getPathSegments();if(!"content".equals(u.getScheme())||!AUTHORITY.equals(u.getAuthority())||u.getQuery()!=null||u.getFragment()!=null||p.size()!=2||!SharePolicy.token(p.get(0)))throw new IllegalArgumentException("Invalid import URI");
  SharedPreferences prefs=getContext().getSharedPreferences("pack-import",0);String src=prefs.getString(p.get(0),null),name=prefs.getString(p.get(0)+".name",null);
  if(src==null||!p.get(1).equals(name)||!SharePolicy.active(prefs.getLong(p.get(0)+".issued",0),System.currentTimeMillis()))throw new IllegalArgumentException("Unknown or expired pack");Uri original=Uri.parse(src);validateSource(original);return original;
 }
 @Override public boolean onCreate(){return true;}
 @Override public String getType(Uri u){original(u);return mime(u.getLastPathSegment());}
 @Override public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("Read only");try{return getContext().getContentResolver().openFileDescriptor(original(u),"r");}catch(IllegalArgumentException e){throw new FileNotFoundException("Unknown pack");}}
 @Override public Cursor query(Uri u,String[] projection,String selection,String[] args,String sort){
  Uri src=original(u);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
  Long size=null;try(Cursor c=getContext().getContentResolver().query(src,new String[]{OpenableColumns.SIZE},null,null,null)){if(c!=null&&c.moveToFirst()&&!c.isNull(0))size=c.getLong(0);}
  MatrixCursor result=new MatrixCursor(columns);Object[] row=new Object[columns.length];for(int i=0;i<columns.length;i++){if(OpenableColumns.DISPLAY_NAME.equals(columns[i]))row[i]=u.getLastPathSegment();else if(OpenableColumns.SIZE.equals(columns[i]))row[i]=size;}result.addRow(row);return result;
 }
 @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException("Read only");}
 @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException("Read only");}
 @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException("Read only");}
}
