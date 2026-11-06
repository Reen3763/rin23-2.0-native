package com.rin23.app;

import android.content.Context;
import android.content.pm.*;
import java.io.*;

final class AppIntegrity {
 static boolean valid(Context context) {
  try {
   if((context.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0)return false;
   PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
   if(info.signingInfo==null)return false;
   Signature[] signers=info.signingInfo.getApkContentsSigners();
   if(signers.length!=1||!IntegrityPolicy.matches(SecurityConfig.SIGNER_SHA256,
      IntegrityPolicy.digest(new ByteArrayInputStream(signers[0].toByteArray()))))return false;
   File engine=new File(context.getApplicationInfo().nativeLibraryDir,"librinengine.so");
   try(InputStream in=new BufferedInputStream(new FileInputStream(engine))){
    return IntegrityPolicy.matches(SecurityConfig.ENGINE_SHA256,IntegrityPolicy.digest(in));
   }
  }catch(Exception e){return false;}
 }
}
