package com.rin23.app;
import com.google.zxing.*;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import java.util.*;
public final class VoucherQrTest {
 static int[] qr(String s,int side)throws Exception{BitMatrix bits=new QRCodeWriter().encode(s,BarcodeFormat.QR_CODE,side,side);int[] p=new int[side*side];for(int y=0;y<side;y++)for(int x=0;x<side;x++)p[y*side+x]=bits.get(x,y)?0xff000000:0xffffffff;return p;}
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  String link="https://gift.truemoney.com/campaign/?v=0123456789abcdef";int n=360;int[] p=qr(link,n);ok(VoucherQrReader.decode(p.clone(),n,n).equals(link),"decode valid voucher QR");
  int[] rotated=new int[p.length];for(int y=0;y<n;y++)for(int x=0;x<n;x++)rotated[x*n+(n-1-y)]=p[y*n+x];ok(VoucherQrReader.decode(rotated,n,n).equals(link),"rotated screenshot");
  int[] inverted=p.clone();for(int i=0;i<inverted.length;i++)inverted[i]=0xff000000|(~inverted[i]&0xffffff);ok(VoucherQrReader.decode(inverted,n,n).equals(link),"inverted QR");
  for(String invalid:new String[]{"https://example.org/","00020101021129370016A00000067701011101130066955136110"}){try{VoucherQrReader.decode(qr(invalid,n),n,n);throw new AssertionError("non-voucher QR accepted");}catch(IllegalArgumentException expected){}}
  int[] blank=new int[n*n];Arrays.fill(blank,0xffffffff);try{VoucherQrReader.decode(blank,n,n);throw new AssertionError("blank accepted");}catch(IllegalArgumentException expected){}
  int[] q=qr(link.replace("0123456789abcdef","abcdef0123456789"),n);int width=n*2+100;int[] both=new int[width*n];Arrays.fill(both,0xffffffff);for(int y=0;y<n;y++){System.arraycopy(p,y*n,both,y*width,n);System.arraycopy(q,y*n,both,y*width+n+100,n);}try{VoucherQrReader.decode(both,width,n);throw new AssertionError("ambiguous multiple vouchers accepted");}catch(IllegalArgumentException expected){}
  System.out.println("PASS: real generated QR decode, rotation, inversion, invalid/PromptPay/blank images and ambiguous two-voucher rejection; no network");
 }
}
