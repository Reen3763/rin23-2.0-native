package com.rin23.app;
import java.net.URI;
import java.util.Locale;
/** Syntactic guard only: does not authenticate publishers or prevent DNS rebinding. */
final class RemotePolicy {
 static boolean https(String value){
  try {
   URI u=new URI(value);String host=u.getHost();
   if(!"https".equalsIgnoreCase(u.getScheme())||host==null||u.getRawUserInfo()!=null||u.getFragment()!=null||(u.getPort()!=-1&&u.getPort()!=443))return false;
   host=host.toLowerCase(Locale.ROOT);
   // Reject IP literals and local names; catalog images must use public DNS names.
   if(host.indexOf(':')>=0||host.matches("[0-9.]+")||host.endsWith(".")||!host.contains("."))return false;
   for(String suffix:new String[]{".localhost",".local",".internal",".home",".lan",".test",".invalid"})if(host.endsWith(suffix))return false;
   return true;
  }catch(Exception e){return false;}
 }
}
