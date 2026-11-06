package com.rin23.app;

import java.io.*;
import java.security.MessageDigest;

/** Small, host-testable checks. These raise tampering cost, not a server trust boundary. */
final class IntegrityPolicy {
 static String digest(InputStream in)throws IOException {
  try {
   MessageDigest md=MessageDigest.getInstance("SHA-256");
   byte[] buffer=new byte[65536];int n;
   while((n=in.read(buffer))!=-1)md.update(buffer,0,n);
   StringBuilder out=new StringBuilder(64);
   for(byte b:md.digest())out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
   return out.toString();
  }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 static boolean matches(String expected,String actual) {
  return expected!=null&&actual!=null&&expected.matches("[0-9a-f]{64}")&&
   MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.US_ASCII),actual.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
 }
}
