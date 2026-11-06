package com.rin23.app;
import java.util.*;
public final class ProtocolTest implements Protocol.Listener {
 int ready=0,actions=0,selections=0,versionPrompts=0,failures=0;String savedName;List<Protocol.Item> list;List<String> versions;
 public void searchReady(){ready++;}
 public void results(List<Protocol.Item> items,int p,int ps,int total){actions++;list=items;check(p==1&&ps==2&&total==11,"pagination");}
 public void selectItems(){selections++;}
 public void variants(List<String> labels){versionPrompts++;versions=labels;}
 public void saved(String n){savedName=n;}
 public void failure(String s){failures++;}
 public void status(String s){}
 static void check(boolean yes,String label){if(!yes)throw new AssertionError(label);}
 public static void main(String[] args){
  ProtocolTest t=new ProtocolTest();Protocol p=new Protocol(t);
  String transcript="\u001b[2J\u001b[HKeys updated +10520 added\n› Search [Filter: Default]: "+
   "Showing results 1-10 of 11 [Filter: Default] [Page 1/2]:\n"+
   " 1. Mob Food  (King Cube) [Addon] ✓\n    ID: 2249c366-c812-4fe5-a6ec-abe9d6bea6bc\n"+
   " 2. A title with [brackets]  (Maker) [World] ★ 4.8 (123) X\n"+
   " [n]ext [d]ownload\n› Action: "+
   "› Download item(s) (e.g. 1,2): \n"+
   "Multiple variants available for item:\n [1] Default\n [2] Vibrant\n› Select version [1-2]: \n"+
   "\r\u001b[2K  ✓  Mob Food (addon).mcaddon saved to \"packs\"\n› Search [Filter: Default]: ";
  // Feed one character at a time to exercise ANSI and UTF-16 boundaries.
  for(int i=0;i<transcript.length();i++)p.feed(transcript.substring(i,i+1));
  check(t.ready==2,"ready prompts");check(t.actions==1,"action prompt");check(t.list.size()==2,"result rows");
  check(t.list.get(0).title.equals("Mob Food"),"title");check(t.list.get(0).creator.equals("King Cube"),"creator");check(t.list.get(0).available,"available");check(t.list.get(0).id.equals("2249c366-c812-4fe5-a6ec-abe9d6bea6bc"),"stable UUID");
  check(!t.list.get(1).available,"unavailable");check(t.list.get(1).title.equals("A title with [brackets]"),"bracket title");
  check(t.selections==1&&t.versionPrompts==1,"download prompts");check(t.versions.size()==2,"variants");check("Mob Food (addon).mcaddon".equals(t.savedName),"saved path");
  ProtocolTest whole=new ProtocolTest();new Protocol(whole).feed(transcript);check(whole.actions==1&&whole.ready==2,"whole-buffer parity");
  System.out.println("PASS: stream chunks, ANSI, catalog rows, creator, availability, pagination, download prompts, variants and output file detection");
 }
}
