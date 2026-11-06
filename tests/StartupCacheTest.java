package com.rin23.app;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import org.json.*;
public class StartupCacheTest {
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("rin-cache-test").toFile();CatalogCache cache=new CatalogCache(root);
  cache.put("a","first");if(!cache.get("a").equals("first"))throw new AssertionError();cache.put("a","second");if(!cache.get("a").equals("second"))throw new AssertionError();
  try{cache.get("missing");throw new AssertionError();}catch(IOException expected){}
  for(File f:new File(root,"catalog-cache").listFiles())f.setLastModified(System.currentTimeMillis()-8L*24*3600000);
  try{cache.get("a");throw new AssertionError();}catch(IOException expected){}
  Files.write(new File(root,"keys.tsv").toPath(),"MarketUUID\tManifestUUID\tTypePack\tKey\n".getBytes(StandardCharsets.UTF_8));
  JSONObject body=new JSONObject();body.put("count",true);body.put("filter",CatalogClient.filter("default"));body.put("search","");body.put("orderBy","startDate desc");body.put("scid","4fc10100-5f7a-4470-899b-280835760c07");body.put("select","contents");body.put("top",20);body.put("skip",0);
  cache.put("/Catalog/Search\n"+body.toString(),"{\"Count\":1,\"Items\":[{\"Id\":\"test-id\",\"Title\":\"Offline title\"}]}");
  CatalogClient.Page page=new CatalogClient(root).cachedSearch("","default",0,20,true);
  if(page.items.size()!=1||!page.items.get(0).title.equals("Offline title"))throw new AssertionError("cached startup");
  if(new File(root,"api.json").exists())throw new AssertionError("test must have no token");
  for(int i=0;i<45;i++)cache.put("item"+i,"{}");if(new File(root,"catalog-cache").listFiles().length>40)throw new AssertionError("cache count bound");
  System.out.println("PASS: cache overwrite, expiry, missing entries, bounded retention and cached catalog without token/network");
 }
}
