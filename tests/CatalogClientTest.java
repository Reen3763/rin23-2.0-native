package com.rin23.app;
import org.json.*;
import java.util.*;
public final class CatalogClientTest {
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
 public static void main(String[] args)throws Exception{
  JSONObject raw=new JSONObject("{\"Id\":\"2249c366-c812-4fe5-a6ec-abe9d6bea6bc\",\"Title\":{\"neutral\":\"English\",\"th-TH\":\"ชื่อไทย\"},\"Description\":{\"neutral\":\"Description\"},\"DisplayProperties\":{\"creatorName\":\"King Cube\"},\"Tags\":[\"addon\"],\"Rating\":{\"Average\":4.7,\"TotalCount\":12},\"Images\":[{\"Type\":\"Screenshot\",\"Url\":\"https://example.org/screen.jpg\"},{\"Type\":\"Thumbnail\",\"Url\":\"https://example.org/cover.jpg\"},{\"Type\":\"Thumbnail\",\"Url\":\"http://example.org/unsafe.jpg\"}]}");
  Protocol.Item item=CatalogClient.parse(raw,7,Collections.singleton(raw.getString("Id")));
  check(item.id.equals(raw.getString("Id"))&&item.number==7,"stable identity");check(item.title.equals("ชื่อไทย"),"localization");check(item.creator.equals("King Cube"),"creator");check(item.type.equals("Addon")&&item.available,"type and key");check(item.imageUrl.equals("https://example.org/cover.jpg"),"cover selection");check(item.screenshots.size()==1,"screenshot gallery");check(item.rating==4.7&&item.ratingCount==12,"rating");
  JSONObject minimal=new JSONObject("{\"Id\":\"other\",\"Title\":\"Simple\",\"Tags\":[\"worldtemplate\"]}");Protocol.Item missing=CatalogClient.parse(minimal,1,Collections.emptySet());check(missing.imageUrl.isEmpty()&&missing.type.equals("World")&&!missing.available,"missing metadata fallback");
  check(!CatalogClient.safeImageUrl("file:///data/secret")&&!CatalogClient.safeImageUrl("https://user:pass@example.org/x")&&!CatalogClient.safeImageUrl("http://example.org/x"),"image URL safety");
  check(CatalogClient.filter("addon").contains("t eq 'addon'"),"fixed category filter");check(!CatalogClient.filter("bad' or true").contains("bad"),"filter injection rejection");
  System.out.println("PASS: catalog UUID, Thai title, creator, cover and screenshots, rating, key availability, missing-image fallback, URL validation and fixed filters");
 }
}
