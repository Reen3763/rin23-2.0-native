package com.rin23.app;
import org.json.*;
import java.util.*;
import java.io.IOException;

/** Local bookmarks keyed by catalog UUID. Cached availability is never trusted. */
final class FavoriteStore {
 interface Storage {String load();boolean save(String json);}
 private final Storage storage;
 private LinkedHashMap<String,Protocol.Item> entries=new LinkedHashMap<>();
 FavoriteStore(Storage storage)throws Exception{this.storage=storage;JSONArray array=new JSONArray(storage.load());for(int i=0;i<array.length();i++){JSONObject o=array.optJSONObject(i);if(o==null)continue;String id=key(o.optString("id"));if(id.isEmpty()||o.optString("title").isEmpty())continue;Protocol.Item item=new Protocol.Item(0,o.optString("title"),o.optString("creator"),o.optString("type"),false);item.id=id;item.creatorId=o.optString("creatorId");item.imageUrl=o.optString("image");item.publishedAt=o.optLong("publishedAt",0);item.rating=o.optDouble("rating",-1);item.ratingCount=o.optInt("ratingCount",0);entries.put(id,item);}}
 static String key(String id){return id!=null&&id.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")?id.toLowerCase(Locale.ROOT):"";}
 boolean contains(String id){return entries.containsKey(key(id));}
 List<Protocol.Item> items(){List<Protocol.Item> list=new ArrayList<>();for(Protocol.Item p:entries.values())list.add(copy(p));Collections.reverse(list);return list;}
 Protocol.Item get(String id){Protocol.Item item=entries.get(key(id));return item==null?null:copy(item);}
 boolean toggle(Protocol.Item item)throws Exception{String id=key(item.id);if(id.isEmpty()||item.title==null||item.title.isEmpty())throw new IllegalArgumentException("ยังไม่มีข้อมูลผลงาน");LinkedHashMap<String,Protocol.Item> next=new LinkedHashMap<>(entries);boolean adding=!next.containsKey(id);if(adding)next.put(id,copy(item));else next.remove(id);JSONArray array=new JSONArray();for(Protocol.Item p:next.values()){JSONObject o=new JSONObject();o.put("id",key(p.id));o.put("title",p.title);o.put("creator",p.creator);o.put("creatorId",p.creatorId);o.put("type",p.type);o.put("image",p.imageUrl);o.put("publishedAt",p.publishedAt);o.put("rating",p.rating);o.put("ratingCount",p.ratingCount);array.put(o);}if(!storage.save(array.toString()))throw new IOException("บันทึกรายการโปรดไม่ได้");entries=next;return adding;}
 private static Protocol.Item copy(Protocol.Item p){Protocol.Item c=new Protocol.Item(0,p.title,p.creator,p.type,false);c.id=key(p.id);c.creatorId=p.creatorId;c.imageUrl=p.imageUrl;c.publishedAt=p.publishedAt;c.rating=p.rating;c.ratingCount=p.ratingCount;return c;}
}
