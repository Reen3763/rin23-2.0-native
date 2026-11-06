package com.rin23.app;
import com.google.zxing.*;
import com.google.zxing.common.*;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.qrcode.QRCodeReader;
import java.util.*;

final class VoucherQrReader {
 static String decode(int[] pixels,int width,int height)throws Exception{
  if(width<=0||height<=0||(long)width*height!=pixels.length||pixels.length>4194304)throw new IllegalArgumentException("รูปมีขนาดไม่ถูกต้อง");
  for(int i=0;i<pixels.length;i++){int p=pixels[i],a=p>>>24;if(a<255){int r=((p>>16)&255)*a/255+255-a,g=((p>>8)&255)*a/255+255-a,b=(p&255)*a/255+255-a;pixels[i]=0xff000000|(r<<16)|(g<<8)|b;}}
  RGBLuminanceSource source=new RGBLuminanceSource(width,height,pixels);Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);hints.put(DecodeHintType.TRY_HARDER,Boolean.TRUE);
  Set<String> valid=new LinkedHashSet<>();boolean sawQr=false;
  for(int invert=0;invert<2;invert++){
   BinaryBitmap bitmap=new BinaryBitmap(new HybridBinarizer(invert==0?source:source.invert()));
   try{Result[] results=new QRCodeMultiReader().decodeMultiple(bitmap,hints);for(Result result:results){sawQr=true;try{valid.add(DonationClient.code(result.getText()));}catch(IllegalArgumentException ignored){}}}catch(NotFoundException ignored){}
   if(valid.isEmpty())try{Result result=new QRCodeReader().decode(bitmap,hints);sawQr=true;try{valid.add(DonationClient.code(result.getText()));}catch(IllegalArgumentException ignored){}}catch(ReaderException ignored){}
  }
  if(valid.size()>1)throw new IllegalArgumentException("พบหลายซองในรูป กรุณาครอปให้เหลือ QR ซองเดียว");
  if(valid.isEmpty())throw new IllegalArgumentException(sawQr?"QR นี้ไม่ใช่ลิงก์ซองอั่งเปา TrueMoney • QR พร้อมเพย์ทั่วไปใช้ไม่ได้":"อ่าน QR ไม่ได้ กรุณาเลือกรูปที่ชัด หรือครอปให้ QR ใหญ่ขึ้น");
  return "https://gift.truemoney.com/campaign/?v="+valid.iterator().next();
 }
}
