package com.rin23.app;
import java.util.*;import java.io.*;import java.nio.file.*;import org.json.*;
public final class SearchDiscoveryTest {
 static void ok(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  ok(SearchQuery.normalize("  บ้าน\u200bต้นไม้\u00a0\n addon  ").equals("บ้านต้นไม้ addon"),"Thai whitespace normalization");ok(SearchQuery.literal("Food++ [new]: \"x\"").equals("Food\\+\\+ \\[new\\]\\: \\\"x\\\""),"search parser special characters escaped as literal");String id="2249c366-c812-4fe5-a6ec-abe9d6bea6bc";ok(SearchQuery.uuid(id)&&!SearchQuery.uuid("----"),"exact UUID validation");
  File dir=Files.createTempDirectory("rin-search-test").toFile();Files.write(new File(dir,"keys.tsv").toPath(),"MarketUUID\tManifestUUID\tTypePack\tKey\n".getBytes("UTF-8"));final List<String> endpoints=new ArrayList<>();final List<JSONObject> bodies=new ArrayList<>();JSONObject raw=new JSONObject().put("Id",id).put("Title","Mob Food").put("Tags",new JSONArray().put("skinpack"));
  CatalogClient client=new CatalogClient(dir,(endpoint,body)->{endpoints.add(endpoint);bodies.add(new JSONObject(body.toString()));return endpoint.endsWith("GetPublishedItem")?new JSONObject().put("Item",raw):new JSONObject().put("Count",1).put("Items",new JSONArray().put(raw));});
  CatalogClient.Page page=client.search(id.toUpperCase(Locale.ROOT),"default",0,20,false);ok(endpoints.get(0).endsWith("GetPublishedItem")&&bodies.get(0).getString("ItemId").equals(id),"UUID goes directly to detail endpoint");ok(page.items.size()==1&&page.consumed==1,"UUID page");int calls=endpoints.size();ok(client.search(id,"default",20,20,false).items.isEmpty()&&endpoints.size()==calls,"UUID pagination no second request");ok(client.search(id,"world",0,20,false).items.isEmpty(),"UUID honors category");
  client.search(" Food++ ","addon",40,20,false);JSONObject body=bodies.get(bodies.size()-1);ok(body.getString("search").equals("Food\\+\\+")&&body.getString("orderBy").isEmpty(),"relevance order and literal query");ok(body.getInt("skip")==40&&body.getInt("top")==20&&body.getString("filter").contains("addon"),"paging/filter preserved");
  Protocol.Item a=new Protocol.Item(1,"a","","Addon",true),b=new Protocol.Item(2,"b","","World",false);a.id="a";b.id="b";ok(RandomDiscovery.eligible(Arrays.asList(a,a,b),Collections.emptySet()).size()==1,"random avoids unavailable and duplicate candidates");ok(RandomDiscovery.eligible(Arrays.asList(a,b),Collections.singleton("a")).isEmpty(),"avoid recent draws");
  System.out.println("PASS: search body, UUID direct lookup/category/paging, Thai normalization/literal escaping/relevance; random candidate availability, deduplication and recent exclusions (mock catalog)");
 }
}
