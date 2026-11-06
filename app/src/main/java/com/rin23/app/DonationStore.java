package com.rin23.app;
import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import java.util.*;

/** Never persists a voucher link/code or selected QR image. */
final class DonationStore extends SQLiteOpenHelper {
 static final class Entry {String hash,receipt,state,message;long amount,time;}
 DonationStore(Context c){super(c,"donations.db",null,1);SQLiteDatabase db=getWritableDatabase();db.execSQL("UPDATE donations SET state='not_sent',message='การตรวจสอบถูกหยุด ยังไม่ได้ส่งคำขอรับซอง' WHERE state='checking'");db.execSQL("UPDATE donations SET state='unknown',message='แอปถูกปิดขณะส่ง กรุณาตรวจผล ห้ามส่งซ้ำอัตโนมัติ' WHERE state='submitted'");}
 public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE donations(hash TEXT PRIMARY KEY,receipt TEXT NOT NULL,state TEXT NOT NULL,message TEXT NOT NULL,amount INTEGER NOT NULL,time INTEGER NOT NULL)");}
 public void onUpgrade(SQLiteDatabase db,int old,int next){throw new IllegalStateException("Unsupported donation database version");}
 synchronized Entry begin(String hash){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{Entry old=find(hash);if(old!=null&&!old.state.equals("not_sent"))return null;ContentValues v=new ContentValues();v.put("hash",hash);v.put("receipt",UUID.randomUUID().toString());v.put("state","checking");v.put("message","กำลังตรวจสอบผู้ให้บริการ");v.put("amount",0);v.put("time",System.currentTimeMillis());db.insertWithOnConflict("donations",null,v,SQLiteDatabase.CONFLICT_REPLACE);db.setTransactionSuccessful();return find(hash);}finally{db.endTransaction();}}
 synchronized void submitted(String hash){ContentValues v=new ContentValues();v.put("state","submitted");v.put("message","ส่งคำขอแล้ว กำลังรอยืนยัน");if(getWritableDatabase().update("donations",v,"hash=? AND state='checking'",new String[]{hash})!=1)throw new IllegalStateException("บันทึกจุดตรวจสอบไม่สำเร็จ จึงยังไม่ส่งคำขอ");}
 synchronized void finish(String hash,String state,long amount,String message){ContentValues v=new ContentValues();v.put("state",state);v.put("amount",amount);v.put("message",message);if(getWritableDatabase().update("donations",v,"hash=?",new String[]{hash})!=1)throw new IllegalStateException("บันทึกผลไม่ได้");}
 synchronized Entry find(String hash){try(Cursor c=getReadableDatabase().query("donations",null,"hash=?",new String[]{hash},null,null,null)){return c.moveToFirst()?read(c):null;}}
 synchronized List<Entry> recent(){List<Entry> all=new ArrayList<>();try(Cursor c=getReadableDatabase().query("donations",null,null,null,null,null,"time DESC","10")){while(c.moveToNext())all.add(read(c));}return all;}
 private Entry read(Cursor c){Entry e=new Entry();e.hash=c.getString(c.getColumnIndexOrThrow("hash"));e.receipt=c.getString(c.getColumnIndexOrThrow("receipt"));e.state=c.getString(c.getColumnIndexOrThrow("state"));e.message=c.getString(c.getColumnIndexOrThrow("message"));e.amount=c.getLong(c.getColumnIndexOrThrow("amount"));e.time=c.getLong(c.getColumnIndexOrThrow("time"));return e;}
}
