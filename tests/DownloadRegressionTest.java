package com.rin23.app;
import java.util.*;
/** Reproduces native ready arriving while a MediaStore export is still in flight. */
public final class DownloadRegressionTest implements Protocol.Listener {
 final DownloadSession session=new DownloadSession();int saves=0,ready=0;
 public void searchReady(){ready++;session.nativeReady();}public void saved(String name){saves++;session.queued();}
 public void results(List<Protocol.Item> i,int p,int ps,int t){}public void selectItems(){}public void variants(List<String> l){}public void failure(String m){session.fail();}public void status(String m){}
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  DownloadRegressionTest t=new DownloadRegressionTest();t.session.begin("portable-bases");Protocol protocol=new Protocol(t);
  String transcript="\r\u001b[2K  ✓  Portable Bases (addon).mcaddon saved to \"packs\"\n› Search [Filter: Default]: ";
  for(int i=0;i<transcript.length();i++)protocol.feed(transcript.substring(i,i+1));
  ok(t.saves==1&&t.ready==1,"real saved and return-to-search protocol");ok(t.session.ended&&t.session.pending==1,"native done remembered during export");ok(t.session.state()==DownloadSession.State.ACTIVE,"wait for actual publication");t.session.saved();ok(t.session.state()==DownloadSession.State.COMPLETE,"published file becomes checkmark without a second native prompt");
  DownloadSession fast=new DownloadSession();fast.begin("fast");fast.queued();fast.saved();ok(!fast.nativeReady()&&fast.state()==DownloadSession.State.COMPLETE,"export-before-prompt path also works");
  DownloadSession multi=new DownloadSession();multi.begin("multi");multi.queued();multi.queued();ok(multi.nativeReady(),"multiple exports pending");multi.saved();ok(multi.state()==DownloadSession.State.ACTIVE,"wait remaining export");multi.failedExport();ok(multi.state()==DownloadSession.State.FAILED,"never mark failed export complete");
  ok(DownloadHistory.recoverPublished(false,false,true,516516,516516,true),"recover old 100-percent-stuck history from actual published archive");
  ok(!DownloadHistory.recoverPublished(false,false,false,100,100,true),"pending MediaStore row not completed");ok(!DownloadHistory.recoverPublished(false,false,true,99,100,true),"truncated export not recovered");ok(!DownloadHistory.recoverPublished(false,false,true,100,100,false),"not an archive");ok(!DownloadHistory.recoverPublished(false,true,true,100,100,true),"known partial failure not falsely recovered");
  ok(DownloadHistory.sameFile("id","pack.mcaddon",100,"id","pack.mcaddon",100),"reuse exact existing export");ok(!DownloadHistory.sameFile("other","pack.mcaddon",100,"id","pack.mcaddon",100),"same title for another UUID is not a duplicate");ok(!DownloadHistory.sameFile("id","standard.mcpack",100,"id","vibrant.mcpack",100),"different output not discarded");
  ok(GalleryRules.panoramaWidth(360,203,3000,300)==2030,"wide picture fills height and extends horizontally");ok(GalleryRules.panoramaWidth(360,203,3000,300)>360,"picture must pan rather than shrink");
  System.out.println("PASS: saved/search prompt during async export, both completion orders, multipart failure, existing-file recovery/dedup and enlarged horizontal panorama");
 }
}
