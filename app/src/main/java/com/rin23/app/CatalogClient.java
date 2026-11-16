package com.rin23.app;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Uses the same guest session as the bundled engine. Never forwards tokens to image hosts. */
final class CatalogClient {
 static final class Page {
  final List<Protocol.Item> items;final int total,consumed;final boolean latest;
  Page(List<Protocol.Item> i,int t,boolean l,int c){items=i;total=t;latest=l;consumed=c;}
 }
 interface Transport{JSONObject request(String endpoint,JSONObject body)throws Exception;}
 private Transport transport;
 private final Map<Thread,HttpURLConnection> active=Collections.synchronizedMap(new HashMap<>());
 void cancelInterruptedRequests(){synchronized(active){for(Map.Entry<Thread,HttpURLConnection> e:active.entrySet())if(e.getKey().isInterrupted())e.getValue().disconnect();}}

 private final ThreadLocal<Boolean> cacheOnly=new ThreadLocal<>();
 Page cachedSearch(String q,String kind,int skip,int limit,boolean latest)throws Exception{cacheOnly.set(true);try{return search(q,kind,skip,limit,latest);}finally{cacheOnly.remove();}}

 CatalogClient(File f,Transport t){folder=f;transport=t;}
 private final File folder;
 private Set<String> keys;
 CatalogClient(File f){folder=f;}
 private synchronized Set<String> availableKeys()throws Exception{
  if(keys!=null)return keys;Set<String> result=new HashSet<>();
  try(BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(new File(folder,"keys.tsv")),StandardCharsets.UTF_8))){String row;in.readLine();while((row=in.readLine())!=null){String[] f=row.split("\t",-1);if(f.length==4)result.add(f[0]);}}
  keys=result;return result;
 }
 synchronized void invalidateKeys(){keys=null;}
 static String filter(String kind){
  String base="(contentType eq 'MarketplaceDurableCatalog_V1.2')";
  String tag;switch(kind){case "addon":tag="addon";break;case "world":tag="worldtemplate";break;case "skin":tag="skinpack";break;case "texture":tag="resourcepack";break;default:return base;}
  return base+" and tags/any(t: t eq '"+tag+"')";
 }
 Page search(String query,String kind,int skip,int limit,boolean latest)throws Exception{
  return search(query,kind,skip,limit,latest,filter(kind));
 }
 Page creatorWorks(Protocol.Item creator,int skip)throws Exception{
  String field=creator.creatorId.isEmpty()?"displayProperties/creatorName":"creatorEntity/id";
  String value=creator.creatorId.isEmpty()?creator.creator:creator.creatorId;
  return search("","default",skip,20,true,
      filter("default")+" and ("+field+" eq '"+value.replace("'","''")+"')");
 }
 private Page search(String query,String kind,int skip,int limit,boolean latest,String catalogFilter)throws Exception{
  JSONObject body=new JSONObject();body.put("count",true);body.put("filter",catalogFilter);
  String normalized=SearchQuery.normalize(query);if(SearchQuery.uuid(normalized)){if(skip>0)return new Page(Collections.emptyList(),1,false,0);Protocol.Item item=details(normalized.toLowerCase(Locale.ROOT));List<Protocol.Item> result=SearchQuery.matchesType(item,kind)?Collections.singletonList(item):Collections.emptyList();return new Page(result,result.size(),false,result.size());}
  String q=SearchQuery.literal(normalized);
  body.put("search",q);body.put("orderBy",latest?"startDate desc":"");body.put("scid","4fc10100-5f7a-4470-899b-280835760c07");body.put("select","contents");body.put("top",limit);body.put("skip",skip);
  JSONObject data;boolean sorted=latest;
  try{data=post("/Catalog/Search",body);}catch(ApiException e){if(latest&&e.status==400){body.put("orderBy","");data=post("/Catalog/Search",body);sorted=false;}else throw e;}
  List<Protocol.Item> items=new ArrayList<>();JSONArray rows=data.optJSONArray("Items");
  if(rows!=null)for(int i=0;i<rows.length();i++){Protocol.Item item=parse(rows.getJSONObject(i),skip+i+1,availableKeys());if(!item.id.isEmpty())items.add(item);}
  return new Page(items,data.optInt("Count",items.size()),sorted,rows==null?0:rows.length());
 }
 Protocol.Item details(String id)throws Exception{
  if(!id.matches("[0-9a-fA-F-]{36}"))throw new IOException("invalid item id");
  JSONObject body=new JSONObject();body.put("ETag","");body.put("ItemId",id);JSONObject data=post("/Catalog/GetPublishedItem",body);JSONObject item=data.optJSONObject("Item");
  return parse(item==null?data:item,1,availableKeys());
 }
 static Protocol.Item parse(JSONObject raw,int number,Set<String> available)throws Exception{
  JSONObject dp=raw.optJSONObject("DisplayProperties");String type="DLC";JSONArray tags=raw.optJSONArray("Tags");Set<String> names=new HashSet<>();if(tags!=null)for(int i=0;i<tags.length();i++)names.add(tags.optString(i));
  if(names.contains("skinpack"))type="SkinPack";else if(names.contains("persona_piece")||raw.optString("ContentType").contains("PersonaDurable"))type="Persona";else if(names.contains("worldtemplate"))type="World";else if(names.contains("resourcepack"))type="TexturePack";else if(names.contains("mashup"))type="Mashup";else if(names.contains("addon"))type="Addon";
  String id=raw.optString("Id"),title=localized(raw.opt("Title")),creator=dp==null?"":dp.optString("creatorName");
  Protocol.Item item=new Protocol.Item(number,title.isEmpty()?"ไม่มีชื่อ":title,creator.isEmpty()?"ไม่ระบุผู้สร้าง":creator,type,available.contains(id)||type.equals("SkinPack")||type.equals("Persona"));
  JSONObject entity=raw.optJSONObject("CreatorEntity");
  if(entity!=null)item.creatorId=entity.optString("Id","");
  item.id=id;item.publishedAt=releaseTime(raw.optString("StartDate"));
  TreeMap<String,List<String>> groups=new TreeMap<>();JSONArray contents=raw.optJSONArray("Contents");
  if(contents!=null)for(int i=0;i<contents.length();i++){JSONObject c=contents.optJSONObject(i);if(c==null||!safeImageUrl(c.optString("Url")))continue;List<String> ts=new ArrayList<>();JSONArray ct=c.optJSONArray("Tags");if(ct!=null)for(int j=0;j<ct.length();j++)ts.add(ct.optString(j));Collections.sort(ts);String key=String.join("\u0001",ts);if(!groups.containsKey(key))groups.put(key,new ArrayList<>());groups.get(key).add(c.optString("Url"));}
  item.contentGroups.addAll(groups.values());item.description=localized(raw.opt("Description"));JSONObject rating=raw.optJSONObject("Rating");if(rating!=null){item.rating=rating.optDouble("Average",-1);item.ratingCount=rating.optInt("TotalCount",0);}
  JSONArray images=raw.optJSONArray("Images");String fallback="";
  if(images!=null)for(int i=0;i<images.length();i++){JSONObject image=images.optJSONObject(i);if(image==null)continue;String url=image.optString("Url"),role=image.optString("Type").toLowerCase(Locale.ROOT);if(!safeImageUrl(url))continue;
   if(fallback.isEmpty())fallback=url;
   if(role.contains("screenshot")){item.screenshots.add(url);}
   else if(role.contains("thumbnail")||role.contains("poster")||role.equals("icon")){if(item.imageUrl.isEmpty()||role.contains("thumbnail"))item.imageUrl=url;}
  }
  if(item.imageUrl.isEmpty())item.imageUrl=fallback;
  if(item.imageUrl.isEmpty()&&dp!=null)for(String field:new String[]{"thumbnailUrl","thumbnail","imageUrl"}){String url=dp.optString(field);if(safeImageUrl(url)){item.imageUrl=url;break;}}
  return item;
 }
 static long releaseTime(String date){try{return java.time.Instant.parse(date).toEpochMilli();}catch(Exception ignored){try{return java.time.OffsetDateTime.parse(date).toInstant().toEpochMilli();}catch(Exception e){return 0;}}}
 static boolean isNew(Protocol.Item item,long now){return item.publishedAt>0&&item.publishedAt<=now&&now-item.publishedAt<30L*24*60*60*1000;}
 static boolean safeImageUrl(String s){return RemotePolicy.https(s);}

 static String localized(Object value){if(value instanceof String)return (String)value;if(!(value instanceof JSONObject))return "";JSONObject obj=(JSONObject)value;for(String key:new String[]{"th-TH","th","neutral","en-US","en"}){String s=obj.optString(key);if(!s.isEmpty())return s;}return "";}
 private JSONObject post(String endpoint,JSONObject body)throws Exception{
  if(transport!=null)return transport.request(endpoint,body);
  CatalogCache cache=new CatalogCache(folder);String cacheKey=endpoint+"\n"+body.toString();
  if(Boolean.TRUE.equals(cacheOnly.get()))return new JSONObject(cache.get(cacheKey));
  if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
  String token;try(InputStream in=new FileInputStream(new File(folder,"api.json"))){token=new JSONObject(read(in,1048576)).optString("20ca2_GUEST_ENTITY_TOKEN");}
  // env_key preserves uppercase suffix in the original engine.
  if(token.isEmpty())throw new IOException("session not ready");
  HttpURLConnection c=(HttpURLConnection)new URL("https://20ca2.playfabapi.com"+endpoint).openConnection();c.setInstanceFollowRedirects(false);c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("X-EntityToken",token);c.setRequestProperty("User-Agent","libhttpclient/1.0.0.0");c.setRequestProperty("Accept-Language","en-US");
  active.put(Thread.currentThread(),c);
  try{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}int code=c.getResponseCode();if(code!=200)throw new ApiException(code);
   JSONObject response;try(InputStream in=c.getInputStream()){response=new JSONObject(read(in,12*1024*1024));}
   if(response.optInt("code",200)!=200)throw new ApiException(response.optInt("code",500));JSONObject data=response.optJSONObject("data");JSONObject result=data==null?response:data;try{cache.put(cacheKey,result.toString());}catch(Exception ignored){}return result;
  }finally{active.remove(Thread.currentThread());c.disconnect();}
 }
 private static String read(InputStream in,int max)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1){if(out.size()+n>max)throw new IOException("response too large");out.write(b,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);}
 private static class ApiException extends IOException{final int status;ApiException(int s){super("catalog request failed");status=s;}}
}
