package com.rin23.app;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;
final class KeyDataPolicy {
 private static final java.util.regex.Pattern UUID_PATTERN=java.util.regex.Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
 private static final java.util.regex.Pattern TYPE_PATTERN=java.util.regex.Pattern.compile("[A-Za-z0-9_ -]{1,64}");
 private static final java.util.regex.Pattern KEY_PATTERN=java.util.regex.Pattern.compile("[!-~]{1,512}");
 static final int MAX_BYTES=20000000;
 static int validate(byte[] bytes)throws Exception{
  if(bytes.length>MAX_BYTES)throw new IOException("keys too large");
  String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
  String[] lines=text.split("\\r?\\n",-1);
  if(lines.length<2||!lines[0].equals("MarketUUID\tManifestUUID\tTypePack\tKey"))throw new IOException("invalid header");
  Map<String,String> seen=new HashMap<>();int count=0;
  String uuid="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
  for(int i=1;i<lines.length;i++){
   if(lines[i].isEmpty())continue;
   if(lines[i].length()>1024)throw new IOException("row too long");
   String[] f=lines[i].split("\t",-1);
   if(f.length!=4||!UUID_PATTERN.matcher(f[0]).matches()||!UUID_PATTERN.matcher(f[1]).matches()||!TYPE_PATTERN.matcher(f[2]).matches()||!KEY_PATTERN.matcher(f[3]).matches())throw new IOException("invalid key row");
   String identity=(f[0]+"/"+f[1]+"/"+f[2]).toLowerCase(Locale.ROOT);
   String previous=seen.put(identity,f[3]);
   if(previous!=null&&!previous.equals(f[3]))throw new IOException("conflicting keys");
   if(++count>100000)throw new IOException("too many rows");
  }
  if(count==0)throw new IOException("empty keys");return count;
 }
}
