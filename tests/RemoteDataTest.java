package com.rin23.app;
import java.nio.charset.StandardCharsets;
public class RemoteDataTest {
 static final String HEADER="MarketUUID\tManifestUUID\tTypePack\tKey\n";
 static final String ROW="2249c366-c812-4fe5-a6ec-abe9d6bea6bc\t2249c366-c812-4fe5-a6ec-abe9d6bea6bd\tresource\tabc123";
 static void bad(String s)throws Exception{try{KeyDataPolicy.validate(s.getBytes(StandardCharsets.UTF_8));}catch(Exception expected){return;}throw new AssertionError("accepted invalid keys");}
 public static void main(String[] args)throws Exception{
  for(String u:new String[]{"http://example.com/a","https://127.0.0.1/a","https://[::1]/a","https://user:pass@example.com/a","https://example.com:444/a","https://host.local/a","https://localhost/a","https://example.com/a#x"})if(RemotePolicy.https(u))throw new AssertionError(u);
  if(!RemotePolicy.https("https://cdn.example.com/a.png?x=1"))throw new AssertionError("public HTTPS rejected");
  if(KeyDataPolicy.validate((HEADER+ROW).getBytes(StandardCharsets.UTF_8))!=1)throw new AssertionError();
  bad(HEADER);bad(HEADER+ROW.replace("2249c366","bad"));bad(HEADER+ROW+"\tmore");bad(HEADER+ROW+"\n"+ROW.replace("abc123","different"));bad(HEADER+ROW.replace("abc123","abc\u0000def"));
  try{KeyDataPolicy.validate(new byte[]{(byte)0xff});throw new AssertionError();}catch(java.nio.charset.CharacterCodingException expected){}
  System.out.println("PASS: remote URL restrictions, valid keys, malformed UUID/UTF-8, controls, extra columns and conflicting duplicates");
 }
}
