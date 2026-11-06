package com.rin23.app;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.json.*;

/** Inspects and repacks archives without extracting user-controlled paths to disk. */
final class PackArchive {
 static final long MAX_INPUT=256L*1024*1024,MAX_EXPANDED=512L*1024*1024;
 static final int MAX_ENTRIES=12000,MAX_PACKS=32;
 static final class Budget {long bytes;int entries;void add(int n)throws IOException{bytes+=n;if(bytes>MAX_EXPANDED||Thread.currentThread().isInterrupted())throw new IOException("ไฟล์ใหญ่เกินขีดจำกัด หรือยกเลิกการทำงาน");}}
 static File prepare(File source,File dir,String name)throws Exception{return prepare(source,dir,name,0,new Budget());}
 private static File prepare(File source,File dir,String name,int depth,Budget budget)throws Exception{
  if(depth>2)throw new IOException("ZIP ซ้อนหลายชั้นเกินไป ยังไม่รองรับ");if(source.length()>MAX_INPUT)throw new IOException("รองรับไฟล์ต้นฉบับไม่เกิน 256 MB");
  List<File> packs=new ArrayList<>();File output=null;
  try(ZipFile zip=new ZipFile(source)){
   Map<String,ZipEntry> files=new LinkedHashMap<>();Set<String> seen=new HashSet<>();List<String> roots=new ArrayList<>();List<ZipEntry> nested=new ArrayList<>();
   Enumeration<? extends ZipEntry> it=zip.entries();long declared=0;
   while(it.hasMoreElements()){ZipEntry e=it.nextElement();if(++budget.entries>MAX_ENTRIES)throw new IOException("ZIP มีไฟล์ย่อยมากเกินไป");String p=e.getName();validatePath(p);if(!seen.add(p.toLowerCase(Locale.ROOT)))throw new IOException("ZIP มีชื่อไฟล์ซ้ำ");if(e.isDirectory())continue;if(e.getSize()>0){if(e.getSize()>MAX_EXPANDED-declared)throw new IOException("ไฟล์เมื่อคลาย ZIP ใหญ่เกิน 512 MB");declared+=e.getSize();if(declared>MAX_EXPANDED)throw new IOException("ไฟล์เมื่อคลาย ZIP ใหญ่เกิน 512 MB");}files.put(p,e);if(p.equals("manifest.json")||p.endsWith("/manifest.json"))roots.add(p.substring(0,p.length()-"manifest.json".length()));String lower=p.toLowerCase(Locale.ROOT);if(lower.endsWith(".mcpack")||lower.endsWith(".mcaddon")||lower.endsWith(".zip"))nested.add(e);}
   if(roots.size()>MAX_PACKS||nested.size()>MAX_PACKS)throw new IOException("แพ็กใน ZIP มากเกินไป");
   if(!roots.isEmpty()){
    for(String root:roots){for(String other:roots)if(!root.equals(other)&&other.startsWith(root))throw new IOException("พบ manifest ซ้อนกัน โครงสร้างแพ็กไม่ชัดเจน");
     manifest(zip,files.get(root+"manifest.json"),budget);
     File pack=File.createTempFile("pack-",".mcpack",dir);packs.add(pack);
     try(ZipOutputStream out=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(pack)))){for(Map.Entry<String,ZipEntry> e:files.entrySet())if(e.getKey().startsWith(root)){String relative=e.getKey().substring(root.length());ZipEntry target=new ZipEntry(relative);target.setTime(0);out.putNextEntry(target);copyEntry(zip,e.getValue(),out,budget);out.closeEntry();}}
    }
   }else if(!nested.isEmpty()){
    for(ZipEntry entry:nested){File child=File.createTempFile("nested-",".zip",dir);try{try(OutputStream out=new BufferedOutputStream(new FileOutputStream(child))){copyEntry(zip,entry,out,budget);}packs.add(prepare(child,dir,entry.getName(),depth+1,budget));}finally{child.delete();}}
   }else throw new IOException("ไม่พบ manifest.json ของแพ็ก Minecraft ใน ZIP นี้");
   if(packs.size()==1){output=File.createTempFile("ready-",extension(packs.get(0).getName()),dir);output.delete();if(!packs.get(0).renameTo(output))throw new IOException("เตรียมไฟล์ไม่สำเร็จ");packs.clear();}
   else{output=File.createTempFile("ready-",".mcaddon",dir);try(ZipOutputStream out=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)))){int i=0;for(File pack:packs){if(pack.getName().endsWith(".mcaddon")){try(ZipFile children=new ZipFile(pack)){Enumeration<? extends ZipEntry> each=children.entries();while(each.hasMoreElements()){ZipEntry child=each.nextElement();if(++i>MAX_PACKS)throw new IOException("แพ็กใน ZIP มากเกินไป");ZipEntry entry=new ZipEntry("pack-"+i+".mcpack");entry.setTime(0);out.putNextEntry(entry);copyEntry(children,child,out,budget);out.closeEntry();}}}else{if(++i>MAX_PACKS)throw new IOException("แพ็กใน ZIP มากเกินไป");ZipEntry entry=new ZipEntry("pack-"+i+".mcpack");entry.setTime(0);out.putNextEntry(entry);try(InputStream in=new FileInputStream(pack)){copy(in,out,MAX_INPUT);}out.closeEntry();}}}}
   if(output.length()>MAX_INPUT)throw new IOException("ไฟล์แพ็กที่เตรียมใหญ่เกิน 256 MB");return output;
  }catch(ZipException e){if(output!=null)output.delete();throw new IOException("ไฟล์ ZIP เสีย เข้ารหัสไว้ หรือไม่ใช่ ZIP ที่รองรับ",e);}catch(Exception e){if(output!=null)output.delete();throw e;}finally{for(File p:packs)p.delete();}
 }
 private static String extension(String name){return name.toLowerCase(Locale.ROOT).endsWith(".mcaddon")?".mcaddon":".mcpack";}
 static String safeName(String name){String n=name==null?"Imported Pack":name.replace('\\','/');n=n.substring(n.lastIndexOf('/')+1).replaceAll("(?i)(\\.(zip|mcpack|mcaddon))+$","").replaceAll("[\\p{Cntrl}:*?\"<>|]","_").trim();if(n.isEmpty()||n.equals(".")||n.equals(".."))n="Imported Pack";return n.substring(0,Math.min(n.length(),80));}
 private static void validatePath(String p)throws IOException{if(p.isEmpty()||p.length()>1024||p.startsWith("/")||p.indexOf('\\')>=0||p.contains("//")||p.indexOf(':')>=0||p.indexOf('\0')>=0)throw new IOException("เส้นทางไฟล์ใน ZIP ไม่ถูกต้อง");for(String part:p.split("/"))if(part.equals("..")||part.equals("."))throw new IOException("เส้นทางไฟล์ใน ZIP ไม่ถูกต้อง");}
 private static void manifest(ZipFile zip,ZipEntry entry,Budget budget)throws Exception{ByteArrayOutputStream buffer=new ByteArrayOutputStream();try(InputStream in=zip.getInputStream(entry)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(buffer.size()+n>1048576)throw new IOException("manifest.json ใหญ่เกินไป");budget.add(n);buffer.write(b,0,n);}}String text=new String(buffer.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);if(text.startsWith("\ufeff"))text=text.substring(1);JSONObject m;try{m=new JSONObject(text);}catch(JSONException e){throw new IOException("manifest.json ไม่ใช่ JSON ที่อ่านได้");}JSONObject h=m.optJSONObject("header");JSONArray modules=m.optJSONArray("modules");if(h==null||h.optString("name").isEmpty()||!uuid(h.optString("uuid"))||!version(h.opt("version"))||modules==null||modules.length()==0)throw new IOException("manifest.json ไม่ใช่แพ็ก Minecraft ที่สมบูรณ์");Set<String> ids=new HashSet<>();ids.add(h.getString("uuid").toLowerCase(Locale.ROOT));for(int i=0;i<modules.length();i++){JSONObject mod=modules.optJSONObject(i);if(mod==null||!uuid(mod.optString("uuid"))||!version(mod.opt("version"))||!ids.add(mod.optString("uuid").toLowerCase(Locale.ROOT))||!Arrays.asList("resources","data","script","skin_pack").contains(mod.optString("type")))throw new IOException("ชนิดโมดูลหรือ UUID ใน manifest.json ไม่ถูกต้อง");}}
 private static boolean version(Object value){if(value instanceof String)return ((String)value).matches("[0-9]+\\.[0-9]+\\.[0-9]+");if(!(value instanceof JSONArray))return false;JSONArray a=(JSONArray)value;if(a.length()!=3)return false;for(int i=0;i<3;i++){Object n=a.opt(i);if(!(n instanceof Number)||((Number)n).doubleValue()!=((Number)n).longValue()||((Number)n).longValue()<0)return false;}return true;}
 private static boolean uuid(String s){return s.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");}
 private static void copyEntry(ZipFile zip,ZipEntry e,OutputStream out,Budget budget)throws Exception{CRC32 crc=new CRC32();long size=0;try(InputStream in=zip.getInputStream(e)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1){budget.add(n);size+=n;crc.update(b,0,n);out.write(b,0,n);}}if(e.getSize()!=size||e.getCrc()!=crc.getValue())throw new IOException("ข้อมูลไฟล์ ZIP ไม่ครบหรือเสียหาย");}
 static long copy(InputStream in,OutputStream out,long max)throws IOException{byte[] b=new byte[65536];int n;long total=0;while((n=in.read(b))!=-1){total+=n;if(total>max||Thread.currentThread().isInterrupted())throw new IOException("ไฟล์ใหญ่เกินขีดจำกัด หรือหยุดนำเข้า");out.write(b,0,n);}return total;}
}
