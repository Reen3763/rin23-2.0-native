package com.rin23.app;

import org.json.*;
import javax.net.ssl.HttpsURLConnection;
import java.net.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Android-native implementation of the voucher request flow inspected in the supplied rin-topup. */
final class DonationClient {
 static final String MOBILE="0955136110",BASE="https://gift.truemoney.com";
 interface Transport { JSONObject request(String method,String url,JSONObject body)throws Exception; }
 interface BeforeSubmit { void persist()throws Exception; }
 static final class Result { final boolean success;final long satang;final String message;Result(boolean s,long n,String m){success=s;satang=n;message=m;} }
 static final class ProviderError extends IOException {final int status;ProviderError(int s){super(s==401||s==403?"ผู้ให้บริการปฏิเสธการเชื่อมต่อจากแอป (HTTP "+s+")":"เชื่อมต่อผู้ให้บริการไม่ได้ (HTTP "+s+")");status=s;}}
 private final Transport transport;
 DonationClient(){this(new HttpsTransport());}DonationClient(Transport t){transport=t;}
 static String code(String input){
  try{
   if(input==null||input.length()>1024)throw new Exception();URI u=new URI(input.trim());
   if(!"https".equalsIgnoreCase(u.getScheme())||!"gift.truemoney.com".equalsIgnoreCase(u.getHost())||u.getPort()!=-1||u.getRawUserInfo()!=null||!("/campaign/".equals(u.getRawPath())||"/campaign".equals(u.getRawPath())))throw new Exception();
   String value=null;String query=u.getRawQuery();if(query==null)throw new Exception();for(String part:query.split("&",-1)){String[] pair=part.split("=",2);if(pair[0].equals("v")){if(value!=null||pair.length!=2)throw new Exception();value=pair[1];}}
   if(value==null||!value.matches("[A-Za-z0-9]{10,48}"))throw new Exception();return value;
  }catch(Exception e){throw new IllegalArgumentException("กรุณาใช้ลิงก์ซองจาก https://gift.truemoney.com/campaign/?v=… เท่านั้น");}
 }
 static String hash(String code){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}catch(Exception e){throw new IllegalStateException(e);}}
 static long amount(Object value)throws IOException{String s=value==null?"":String.valueOf(value);if(!s.matches("[0-9]{1,12}(\\.[0-9]{1,2})?"))throw new IOException("ผู้ให้บริการไม่ส่งยอดเงินที่ยืนยันได้");try{long n=new BigDecimal(s).movePointRight(2).longValueExact();if(n<=0)throw new ArithmeticException();return n;}catch(ArithmeticException e){throw new IOException("ยอดเงินที่ตอบกลับไม่ถูกต้อง");}}
 static String baht(long satang){return BigDecimal.valueOf(satang,2).toPlainString();}
 Result redeem(String link,boolean confirmed,BeforeSubmit before)throws Exception{
  if(!confirmed)throw new IllegalArgumentException("ต้องยืนยันบริจาคก่อน");String token=code(link);
  JSONObject config=transport.request("GET",BASE+"/campaign/vouchers/configuration",null);JSONObject status=config.optJSONObject("status");
  if(status==null||!"SUCCESS".equals(status.optString("code")))throw new IOException("ผู้ให้บริการยังไม่พร้อมรับซอง หรือรูปแบบคำตอบเปลี่ยน กรุณาลองภายหลัง");
  JSONObject body=new JSONObject();body.put("mobile",MOBILE);body.put("voucher_hash",token);
  before.persist(); // durable checkpoint before opening the one and only redeem request
  JSONObject raw=transport.request("POST",BASE+"/campaign/vouchers/"+token+"/redeem",body);status=raw.optJSONObject("status");
  if(status==null||status.optString("code").isEmpty())throw new IOException("ยังยืนยันผลการรับซองไม่ได้");
  if(!"SUCCESS".equals(status.optString("code")))return new Result(false,0,rejection(status.optString("code")));
  JSONObject data=raw.optJSONObject("data"),ticket=data==null?null:data.optJSONObject("my_ticket");long n=amount(ticket==null?null:ticket.opt("amount_baht"));
  return new Result(true,n,"ขอบคุณที่สนับสนุน Rin23 ❤️ รับบริจาคสำเร็จ "+baht(n)+" บาท");
 }
 private static String rejection(String code){String label;switch(code){case "VOUCHER_EXPIRED":label="ซองหมดอายุแล้ว";break;case "VOUCHER_OUT_OF_STOCK":label="ซองนี้ถูกใช้แล้วหรือเงินในซองหมด";break;case "VOUCHER_NOT_FOUND":label="ไม่พบซองนี้";break;case "CANNOT_GET_OWN_VOUCHER":label="ไม่สามารถรับซองของตัวเองได้";break;default:label="ผู้ให้บริการไม่อนุมัติการรับซองนี้";}return "รับบริจาคไม่สำเร็จ • "+label;}
 static final class HttpsTransport implements Transport {
  public JSONObject request(String method,String endpoint,JSONObject body)throws Exception{
   URL url=new URL(endpoint);if(!url.getProtocol().equals("https")||!url.getHost().equals("gift.truemoney.com")||url.getPort()!=-1)throw new IOException("ปลายทางไม่ถูกต้อง");
   HttpsURLConnection c=(HttpsURLConnection)url.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setRequestMethod(method);c.setRequestProperty("User-Agent",AppVersion.USER_AGENT);c.setRequestProperty("Accept","application/json");c.setRequestProperty("Accept-Language","th-TH");c.setUseCaches(false);
   try{
    if(body!=null){byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Origin",BASE);c.setRequestProperty("Referer",BASE+"/campaign/?v="+body.getString("voucher_hash"));c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
    int status=c.getResponseCode();if(status!=200)throw new ProviderError(status);
    ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1){if(out.size()+n>262144)throw new IOException("คำตอบจากผู้ให้บริการใหญ่เกินไป");out.write(bytes,0,n);}}
    try{return new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));}catch(JSONException e){throw new IOException("ผู้ให้บริการไม่ได้ตอบกลับเป็นข้อมูลรับซองที่อ่านได้");}
   }finally{c.disconnect();}
  }
 }
}
