package com.rin23.app;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
public final class KeySyncTest {
 public static void main(String[] args)throws Exception{
  boolean rejected=false;try{KeySync.decode(new byte[16]);}catch(Exception e){rejected=true;}if(!rejected)throw new AssertionError("empty ciphertext");
  rejected=false;try{KeySync.validate("MarketUUID\tManifestUUID\tTypePack\tKey\n".getBytes(StandardCharsets.UTF_8));}catch(Exception e){rejected=true;}if(!rejected)throw new AssertionError("empty table");
  rejected=false;try{KeySync.validate(new byte[]{(byte)0xff});}catch(Exception e){rejected=true;}if(!rejected)throw new AssertionError("invalid UTF-8");
  if(args.length>0){byte[] data=KeySync.decode(Files.readAllBytes(Paths.get(args[0])));System.out.println("PASS: live AES-CBC/PKCS7 + UTF-8 + four-column table, rows="+KeySync.validate(data));}
  System.out.println("PASS: malformed ciphertext and table rejection");
 }
}
