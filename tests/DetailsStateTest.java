package com.rin23.app;
public final class DetailsStateTest {
 static void ok(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  DownloadSession s=new DownloadSession();ok(s.state()==DownloadSession.State.IDLE,"initial");s.begin("a");s.queued();s.saved();ok(s.state()==DownloadSession.State.ACTIVE,"no checkmark before native job ends");s.end();ok(s.state()==DownloadSession.State.COMPLETE,"saved then native end");
  s.begin("b");s.queued();s.queued();s.end();ok(s.state()==DownloadSession.State.ACTIVE,"wait for both exports");s.saved();ok(s.state()==DownloadSession.State.ACTIVE,"one export is not whole job");s.saved();ok(s.state()==DownloadSession.State.COMPLETE,"all exported before checkmark");
  s.begin("c");s.queued();s.queued();s.saved();s.failedExport();s.end();ok(s.state()==DownloadSession.State.FAILED,"partial export must not show checkmark");
  s.begin("d");s.fail();s.end();ok(s.state()==DownloadSession.State.FAILED,"download failure");s.begin("e");s.end();ok(s.state()==DownloadSession.State.FAILED,"empty output not success");s.begin("f");ok(s.state()==DownloadSession.State.ACTIVE&&!s.failed&&s.saved==0,"retry resets outcome");
  ok(!GalleryRules.belowDescription(1920,1080),"normal screenshot in carousel");ok(GalleryRules.belowDescription(600,1600),"portrait below description");ok(GalleryRules.belowDescription(3200,700),"panorama below description");ok(!GalleryRules.belowDescription(0,0),"unknown dimensions stay pending");ok(GalleryRules.naturalHeight(360,600,1600)==960,"long image keeps natural ratio");ok(GalleryRules.naturalHeight(360,3200,700)==79,"panorama not stretched to 16:9");
  System.out.println("PASS: checkmark requires native completion and every export, partial failure/empty output/retry, intrinsic gallery classification and natural image proportions");
 }
}
