package com.rin23.app;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;
public final class FeedTransferTest {
 static void ok(boolean b,String what){if(!b)throw new AssertionError(what);}
 static Protocol.Item row(String id){Protocol.Item r=new Protocol.Item(1,id,"Creator","Addon",true);r.id=id;return r;}
 public static void main(String[] args)throws Exception{
  FeedState f=new FeedState();ok(f.begin(false),"first page begins");ok(!f.begin(false),"prevent duplicate scroll request");f.accept(Arrays.asList(row("a"),row("b")),7,3);ok(f.offset==3&&f.items.size()==2,"advance cursor by raw server rows");ok(f.begin(false),"append starts");f.accept(Arrays.asList(row("b"),row("c")),7,2);ok(f.offset==5&&f.items.size()==3,"deduplicate page overlap without replacing earlier items");f.begin(false);f.fail();ok(!f.begin(false)&&f.begin(true),"failed scroll waits for manual retry");f.accept(Collections.emptyList(),7,0);ok(f.exhausted&&!f.begin(true),"stop empty-page loop");f.reset();ok(f.offset==0&&f.items.isEmpty()&&!f.exhausted,"search reset");f.begin(false);f.accept(Arrays.asList(row("a")),100,1);f.begin(false);f.accept(Arrays.asList(row("a")),100,1);ok(f.exhausted,"stop repeating pages");
  long now=java.time.Instant.parse("2026-10-23T07:00:00Z").toEpochMilli();Protocol.Item r=row("date");r.publishedAt=now-29L*86400000;ok(CatalogClient.isNew(r,now),"recent");r.publishedAt=now-30L*86400000;ok(!CatalogClient.isNew(r,now),"30 day boundary");r.publishedAt=now+1;ok(!CatalogClient.isNew(r,now),"future not new");r.publishedAt=0;ok(!CatalogClient.isNew(r,now),"missing date not new");
  JSONObject raw=new JSONObject("{\"Id\":\"test\",\"StartDate\":\"2026-10-22T00:00:00Z\",\"Contents\":[{\"Url\":\"https://example.org/optimized\",\"Tags\":[\"optimized\"]},{\"Url\":\"https://example.org/base1\",\"Tags\":[]},{\"Url\":\"https://example.org/base2\"}]} ");Protocol.Item parsed=CatalogClient.parse(raw,1,Collections.emptySet());ok(CatalogClient.isNew(parsed,now),"parse release timestamp");ok(parsed.contentGroups.size()==2&&parsed.contentGroups.get(0).size()==2&&parsed.contentGroups.get(0).get(0).endsWith("base1"),"multipart variant order matches engine");
  ok(TransferMeter.percent(5,10)==50&&TransferMeter.percent(0,-1)==-1&&TransferMeter.percent(11,10)==100,"percent known/unknown/clamped");ok(TransferMeter.bytes(1048576).equals("1.0 MB"),"byte units");File dir=Files.createTempDirectory("rin23-progress-test").toFile(),part=new File(dir,"part_0");part.mkdir();File zip=new File(part,"downloaded_dlc.zip");Files.write(zip.toPath(),new byte[256]);TransferMeter meter=new TransferMeter();ok(meter.sample(dir)==256,"measure actual bytes on disk");Files.write(zip.toPath(),new byte[512]);ok(meter.sample(dir)==512,"growing download");zip.delete();ok(meter.sample(dir)==512,"retain bytes while completed parts are cleaned up");meter.reset();ok(meter.sample(dir)==0,"new transfer resets");part.delete();dir.delete();
  System.out.println("PASS: infinite feed, deduplication, cursor, retry, stop conditions, release badges, variants, actual transfer bytes and percentages");
 }
}
