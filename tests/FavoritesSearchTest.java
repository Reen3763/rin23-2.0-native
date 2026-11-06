package com.rin23.app;
import java.util.*;
public final class FavoritesSearchTest {
 static final String A="2249c366-c812-4fe5-a6ec-abe9d6bea6bc",B="b0123456-c812-4fe5-a6ec-abe9d6bea6bc";
 static final class Disk implements FavoriteStore.Storage {String value="[]";boolean fail;public String load(){return value;}public boolean save(String s){if(fail)return false;value=s;return true;}}
 static void check(boolean c,String m){if(!c)throw new AssertionError(m);}
 static Protocol.Item item(String id,String title){Protocol.Item p=new Protocol.Item(1,title,"Rin23","Addon",true);p.id=id;p.imageUrl="https://example.com/cover.png";p.description="not stored";return p;}
 public static void main(String[] args)throws Exception{
  Disk disk=new Disk();FavoriteStore store=new FavoriteStore(disk);Protocol.Item a=item(A.toUpperCase(Locale.ROOT),"บ้านต้นไม้ ภาษาไทย");check(store.toggle(a),"add");check(store.contains(A),"UUID normalized");a.imageUrl="mutated";check(!store.get(A).imageUrl.equals("mutated"),"snapshot is independent");
  FavoriteStore reopened=new FavoriteStore(disk);check(reopened.items().size()==1&&reopened.get(A).title.equals("บ้านต้นไม้ ภาษาไทย"),"Thai persisted across reopen");check(!reopened.get(A).available&&reopened.get(A).contentGroups.isEmpty(),"bookmarks never authorize stale downloads");
  check(reopened.toggle(item(B,"Mob Food")),"add distinct UUID");check(reopened.items().get(0).id.equals(B),"newest first");check(!reopened.toggle(item(A,"other name"))&&!reopened.contains(A),"toggle same UUID removes not duplicates");check(new FavoriteStore(disk).items().size()==1,"remove persisted");
  disk.fail=true;try{reopened.toggle(item(A,"retry"));throw new AssertionError("write error hidden");}catch(java.io.IOException expected){}check(!reopened.contains(A)&&reopened.items().size()==1,"failed write rolls back");
  disk.fail=false;check(reopened.toggle(item(A,"retry")),"retry save");List<Protocol.Item> snapshot=reopened.items();snapshot.get(0).id=B;check(reopened.contains(A),"returned values cannot mutate store");
  try{reopened.toggle(item("bad","invalid"));throw new AssertionError("bad id accepted");}catch(IllegalArgumentException expected){}
  Disk partial=new Disk();partial.value="[null,{}, {\"id\":\""+A+"\",\"title\":\"ok\"},{\"id\":\""+A.toUpperCase(Locale.ROOT)+"\",\"title\":\"dedup\"}]";check(new FavoriteStore(partial).items().size()==1,"malformed entries skipped and duplicate UUID deduped");
  Disk corrupt=new Disk();corrupt.value="broken";try{new FavoriteStore(corrupt);throw new AssertionError("corrupt file ignored");}catch(org.json.JSONException expected){}check(corrupt.value.equals("broken"),"corruption not overwritten");
  check(FileSearch.matches("Mob Food (addon).mcaddon"," FOOD mob "),"word search case insensitive");check(FileSearch.matches("บ้านต้นไม้.mcaddon","ต้นไม้"),"Thai substring");check(FileSearch.matches("FOOD.mcpack","ｆｏｏｄ"),"unicode width normalization");check(FileSearch.matches("anything","   "),"empty restores all");check(!FileSearch.matches("Mob Food.mcaddon","world"),"no match");check(!FileSearch.matches("Food.mcaddon","food world"),"all query words required");check(FileSearch.matches("PACK.MCPACK","mcpack"),"extension search");
  System.out.println("PASS: favorites persist, UUID dedup/toggle, newest first, safe snapshots, stale availability, failed-write rollback, corrupt data protection; Thai/case/word/extension file search");
 }
}
