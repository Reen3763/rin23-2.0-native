package com.rin23.app;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class SecurityPolicyTest {
 static void ok(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception {
  String expected="ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
  String actual=IntegrityPolicy.digest(new ByteArrayInputStream("abc".getBytes(StandardCharsets.US_ASCII)));
  ok(IntegrityPolicy.matches(expected,actual),"known SHA-256 vector");
  ok(!IntegrityPolicy.matches(expected,IntegrityPolicy.digest(new ByteArrayInputStream("abd".getBytes(StandardCharsets.US_ASCII)))),"modified engine rejected");
  ok(!IntegrityPolicy.matches("",actual)&&!IntegrityPolicy.matches(null,actual),"unconfigured signing rejected");
  ok(SharePolicy.active(1000,1000)&&SharePolicy.active(1000,1000+SharePolicy.TTL_MS-1),"valid grant lifetime");
  ok(!SharePolicy.active(1000,1000+SharePolicy.TTL_MS),"expired grant rejected");
  ok(!SharePolicy.active(0,1000)&&!SharePolicy.active(2000,1000),"legacy and future timestamps rejected");
  ok(SharePolicy.token("00000000-0000-4000-8000-000000000001"),"valid token");
  ok(!SharePolicy.token("../escape")&&!SharePolicy.token(null),"invalid token rejected");
  System.out.println("Security policies: PASS (digest mutation, signing configuration, grant expiry and rollback)");
 }
}
