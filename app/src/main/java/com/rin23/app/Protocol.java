package com.rin23.app;

import java.util.*;
import java.util.regex.*;

/** Reads the bundled Rustcoin 1.2.0 interactive protocol; no shell commands. */
public final class Protocol {
 public static final class Item {
  public final int number; public final String title,creator,type; public final boolean available;
  public String id="",imageUrl="",description=""; public long publishedAt=0, updatedAt=0; public String version=""; public List<List<String>> contentGroups=new ArrayList<>(); public double rating=-1; public int ratingCount=0; public List<String> screenshots=new ArrayList<>();
  Item(int n,String t,String c,String k,boolean a){number=n;title=t;creator=c;type=k;available=a;}
 }
 public interface Listener {
  void searchReady(); void results(List<Item> items,int page,int pages,int total);
  void selectItems(); void variants(List<String> labels); void saved(String name);
  void failure(String message); void status(String message);
 }
 private final Listener listener;
 private final StringBuilder line=new StringBuilder();
 private final List<Item> items=new ArrayList<>(); private final List<String> variants=new ArrayList<>();
 private int ansi=0,page=0,pages=0,total=0;
 private static final Pattern HEADER=Pattern.compile("Showing results .*? of (\\d+) \\[Filter: .*?\\] \\[Page (\\d+)/(\\d+)\\]");
 private static final Pattern ITEM=Pattern.compile("^\\s*(\\d+)\\.\\s+(.+)\\s+\\[([^\\]]+)\\](.*)$");
 private static final Pattern VARIANT=Pattern.compile("^\\s*\\[(\\d+)\\] (.+)$");
 private static final Pattern SAVED=Pattern.compile("^\\s*✓\\s+(.+) saved to \\\".*\\\"\\s*$");
 public Protocol(Listener l){listener=l;}
 public void feed(String chunk){
  for(int i=0;i<chunk.length();i++){
   char c=chunk.charAt(i);
   if(ansi==1){ansi=c=='['?2:0;continue;}
   if(ansi==2){if(c>='@'&&c<='~')ansi=0;continue;}
   if(c==27){ansi=1;continue;}
   if(c=='\r'){line.setLength(0);continue;}
   if(c=='\n'){onLine(line.toString());line.setLength(0);continue;}
   if(c<32&&c!='\t')continue;
   line.append(c);
   if(line.length()>16000)line.delete(0,line.length()-8000);
   if(c==' '&&line.toString().endsWith(": "))checkPrompt();
  }
 }
 private void checkPrompt(){
  String s=line.toString();
  if(s.contains("Search")&&s.contains("[Filter:")&&s.endsWith(": ")){line.setLength(0);listener.searchReady();}
  else if(s.contains("Download item(s)")){line.setLength(0);listener.selectItems();}
  else if(s.contains("Select version [")){line.setLength(0);listener.variants(new ArrayList<>(variants));variants.clear();}
  else if(s.trim().endsWith("Action:")){line.setLength(0);listener.results(new ArrayList<>(items),page,pages,total);}
 }
 private void onLine(String s){
  Matcher h=HEADER.matcher(s);
  if(h.find()){total=Integer.parseInt(h.group(1));page=Integer.parseInt(h.group(2));pages=Integer.parseInt(h.group(3));items.clear();variants.clear();return;}
  Matcher m=ITEM.matcher(s);
  if(m.matches()){
   String label=m.group(2).trim(),creator="ไม่ระบุผู้สร้าง";int at=label.lastIndexOf(" (");
   if(at>=0&&label.endsWith(")")){creator=label.substring(at+2,label.length()-1);label=label.substring(0,at).trim();}
   items.add(new Item(Integer.parseInt(m.group(1)),label,creator,m.group(3),m.group(4).contains("✓")));return;
  }
  if(s.trim().startsWith("ID:")&&!items.isEmpty()){items.get(items.size()-1).id=s.trim().substring(3).trim();return;}
  Matcher v=VARIANT.matcher(s);if(v.matches()){variants.add(v.group(1)+". "+v.group(2));return;}
  Matcher saved=SAVED.matcher(s);if(saved.matches()){listener.saved(saved.group(1));return;}
  if(s.contains("Downloading:"))listener.status("กำลังดาวน์โหลดและเตรียมไฟล์…");
  if(s.contains("Failed:")||s.contains("Error executing search:")||s.contains("No decryption key"))listener.failure("ดำเนินการไม่สำเร็จ ลองใหม่อีกครั้งหรือตรวจอินเทอร์เน็ต");
 }
}
