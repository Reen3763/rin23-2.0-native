package com.rin23.app;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.database.Cursor;
import org.json.*;
import java.io.*;
import java.lang.Process;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class EngineService extends Service {
 public interface Observer{void changed();}
 public interface DetailCallback{void loaded(Protocol.Item item);}
 public final class LocalBinder extends Binder{EngineService get(){return EngineService.this;}}
 public static final class Download {
  public final String name,uri,image,itemId;public final long size;public boolean complete;Download(String n,String u,String i,long bytes,String id,boolean done){name=n;uri=u;image=i;size=bytes;itemId=id;complete=done;}
 }
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor(),disk=Executors.newSingleThreadExecutor(),catalogIo=Executors.newFixedThreadPool(2),searchIo=Executors.newFixedThreadPool(2),sizeIo=Executors.newSingleThreadExecutor();
 private Future<?> searchTask;
 private final List<Observer> observers=new ArrayList<>();
 public final FeedState feed=new FeedState();
 public final List<Protocol.Item> items=feed.items,homeLatest=new ArrayList<>(),homeAddons=new ArrayList<>(),homeWorlds=new ArrayList<>();
 public final List<Download> downloads=new ArrayList<>();
 public volatile String state="STARTING",status="กำลังเตรียม Rin23…",keyStatus="กำลังอัปเดตฐานข้อมูล…",lastQuery="",lastFilter="addon";
 public int page=0,pages=0,total=0,revision=0;public boolean homeLoaded=false,latestSorted=true;
 public List<String> variantLabels=new ArrayList<>();
 private volatile Process engine;private volatile boolean stopped=false;private volatile int epoch=0;
 private File folder,transferFolder;private final TransferMeter meter=new TransferMeter();private final ScheduledExecutorService telemetry=Executors.newSingleThreadScheduledExecutor();private int transferGeneration=0;public long downloadedBytes=0,downloadTotal=-1;public int downloadPercent=-1;public String transferStage="";private CatalogClient catalog;private volatile int catalogGeneration=0;
 private boolean historyChecked=false;private boolean catalogAvailable=false;private final Set<String> failedItems=new HashSet<>();private final Set<String> outputNames=new HashSet<>();
 private int saving=0;private boolean nativeReady=false,variantDelivered=false;
 final DownloadSession session=new DownloadSession();private boolean resolvingDownload=false;private final Map<String,Protocol.Item> detailCache=new HashMap<>();
 private Protocol.Item downloading;private Integer pendingNumber;private boolean pendingLookup=false;
 @Override public void onCreate(){super.onCreate();folder=new File(getFilesDir(),"engine");folder.mkdirs();catalog=new CatalogClient(folder);transferFolder=new File(getCacheDir(),"transfers");transferFolder.mkdirs();removeTransferFiles(transferFolder);disk.execute(()->{File[] dirs=getCacheDir().listFiles();if(dirs!=null)for(File d:dirs)if(d.isDirectory()&&d.getName().startsWith("import-")){File[] files=d.listFiles();if(files!=null)for(File f:files)f.delete();d.delete();}});loadHistory();refreshHistorySizes();telemetry.scheduleWithFixedDelay(()->{if(!(state.equals("DOWNLOADING")||state.equals("VARIANT"))||session.saved>0)return;final int sampleId=transferGeneration;long bytes; synchronized(meter){bytes=meter.sample(transferFolder);}main.post(()->{if(sampleId==transferGeneration&&session.saved==0&&(state.equals("DOWNLOADING")||state.equals("VARIANT"))&&bytes>downloadedBytes){downloadedBytes=bytes;downloadPercent=TransferMeter.percent(bytes,downloadTotal);transferStage=downloadPercent==100?"ดาวน์โหลดครบแล้ว • กำลังจัดเตรียมแพ็ก":"กำลังดาวน์โหลด";changed();}});},500,500,TimeUnit.MILLISECONDS);
  getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("rin23_tasks","งานของ Rin23",NotificationManager.IMPORTANCE_LOW));startForeground(23,notification("กำลังเตรียมแอป…"));startEngine();}
 private Notification notification(String text){PendingIntent pi=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);return new Notification.Builder(this,"rin23_tasks").setContentTitle("Rin23").setContentText(text).setSmallIcon(R.drawable.ui_download).setContentIntent(pi).setOnlyAlertOnce(true).setOngoing(true).setProgress((state.equals("DOWNLOADING")||state.equals("SAVING"))?100:0,(state.equals("DOWNLOADING")||state.equals("SAVING"))?Math.max(0,downloadPercent):0,(state.equals("DOWNLOADING")||state.equals("SAVING"))&&downloadPercent<0).build();}
 @Override public IBinder onBind(Intent i){return new LocalBinder();}
 public void observe(Observer o){if(!observers.contains(o))observers.add(o);o.changed();}
 public void unobserve(Observer o){observers.remove(o);}
 private void changed(){if(stopped)return;for(Observer o:new ArrayList<>(observers))o.changed();getSystemService(NotificationManager.class).notify(23,notification(status));}
 private void update(String s,String message){if(s.equals("ERROR")&&session.state()==DownloadSession.State.ACTIVE){session.fail();session.end();finishSession();}state=s;status=message;changed();}
 public boolean canBrowse(){return catalogAvailable;}
 public boolean isReady(){return historyChecked&&(state.equals("SEARCH")||state.equals("RESULTS"));}
 public boolean isBusy(){return !isReady()&&!state.equals("ERROR");}
 public void retry(){if(saving>0){status="กำลังบันทึกไฟล์ กรุณารอสักครู่";changed();return;}if(state.equals("ERROR")){stopEngine();stopped=false;homeLoaded=false;++catalogGeneration;++transferGeneration;feed.reset();downloading=null;startEngine();}}
 public void loadHome(){if(!canBrowse())return;homeLoaded=true;search("","default",true,1);}
 public void search(String query,String filter){search(query,filter,true,1);}
 public void search(String query,String filter,boolean latest,int ignoredPage){
  if(!canBrowse())return;query=SearchQuery.normalize(query);if(query.length()>200||query.contains("\n")||query.contains("\r")){status="คำค้นยาวเกินไป กรุณาลองใหม่";changed();return;}
  lastQuery=query;lastFilter=filter;this.latest=latest;++catalogGeneration;if(searchTask!=null){searchTask.cancel(true);catalog.cancelInterruptedRequests();}feed.reset();page=0;revision++;fetchPage(false,true);
 }
 private boolean latest=true;
 public void loadMore(boolean retry){if(!canBrowse())return;fetchPage(retry,false);}
 private void fetchPage(boolean retry,boolean first){
  if(!feed.begin(retry))return;final int generation=catalogGeneration,offset=feed.offset;final String q=lastQuery,f=lastFilter;final boolean sort=latest;
  if(first&&isReady())update("CATALOG","กำลังค้นหาผลงาน…");else changed();
  searchTask=searchIo.submit(()->{if(stopped||generation!=catalogGeneration)return;
   if(first)try{CatalogClient.Page cached=catalog.cachedSearch(q,f,offset,20,sort);main.post(()->{if(stopped||generation!=catalogGeneration)return;feed.reset();feed.accept(cached.items,cached.total,cached.consumed);feed.loading=true;total=cached.total;revision++;status="แสดงข้อมูลที่บันทึกไว้ • กำลังตรวจรายการใหม่…";changed();});}catch(Exception ignored){}
   try{CatalogClient.Page result=catalog.search(q,f,offset,20,sort);main.post(()->{if(stopped||generation!=catalogGeneration)return;if(first)feed.reset();feed.accept(result.items,result.total,result.consumed);total=feed.total;page++;latestSorted=result.latest;revision++;if(first&&state.equals("CATALOG"))state="RESULTS";if(isReady())status=total==0?"ไม่พบผลงาน ลองเปลี่ยนคำค้นหรือหมวดหมู่":"สำรวจแล้ว "+items.size()+" จาก "+total+" รายการ";changed();});}catch(Exception e){main.post(()->{if(stopped||generation!=catalogGeneration)return;feed.fail();if(first&&state.equals("CATALOG")){state="RESULTS";status="โหลดผลงานไม่สำเร็จ";}revision++;changed();});}});
 }
 public Protocol.Item cachedItem(String id){if(detailCache.containsKey(id))return detailCache.get(id);for(Protocol.Item item:items)if(item.id.equals(id))return item;return null;}
 public DownloadSession.State downloadState(String id){if(session.id.equals(id)&&(session.state()==DownloadSession.State.ACTIVE||session.state()==DownloadSession.State.FAILED))return session.state();for(Download d:downloads)if(d.itemId.equals(id)&&d.complete)return DownloadSession.State.COMPLETE;return DownloadSession.State.IDLE;}
 public long savedSize(String id){long n=0;for(Download d:downloads)if(d.itemId.equals(id))n+=Math.max(0,d.size);return n>0?n:-1;}
 public void estimateSize(Protocol.Item item,java.util.function.LongConsumer callback){sizeIo.execute(()->{long n=item.contentGroups.isEmpty()?-1:TransferMeter.size(item.contentGroups.get(0));main.post(()->{if(!stopped)callback.accept(n);});});}
 public void details(Protocol.Item item,DetailCallback callback){catalogIo.execute(()->{Protocol.Item detail=item;try{detail=catalog.details(item.id);}catch(Exception ignored){}final Protocol.Item result=detail;main.post(()->{if(!stopped){detailCache.put(result.id,result);callback.loaded(result);}});});}
 public void download(Protocol.Item item){if(item.id.isEmpty())return;if(downloadState(item.id)==DownloadSession.State.COMPLETE){status="ผลงานนี้โหลดแล้ว • กดเปิดเพื่อดูไฟล์";changed();return;}if(!isReady()||session.state()==DownloadSession.State.ACTIVE)return;outputNames.clear();failedItems.remove(item.id);downloading=item;session.begin(item.id);resolvingDownload=true;startTransfer(item,0);pendingLookup=true;update("DOWNLOADING","กำลังเตรียม "+item.title+"…");if(nativeReady){pendingLookup=false;nativeReady=false;send(item.id);}else send("s");}
 public void chooseVariant(int n){if(!state.equals("VARIANT"))return;variantDelivered=true;variantLabels.clear();startTransfer(downloading,n-1);update("DOWNLOADING","กำลังดาวน์โหลดและเตรียมไฟล์…");send(String.valueOf(n));}
 public boolean consumeVariantDialog(){if(!state.equals("VARIANT")||variantDelivered)return false;variantDelivered=true;return true;}
 private void send(String s){try{Process p=engine;if(p==null)throw new IOException();OutputStream out=p.getOutputStream();out.write((s+"\n").getBytes(StandardCharsets.UTF_8));out.flush();}catch(Exception e){update("ERROR","ติดต่อโปรแกรมไม่ได้ กดเริ่มใหม่");}}
 private void postFor(int id,Runnable action){main.post(()->{if(!stopped&&epoch==id)action.run();});}
 private void startEngine(){final long startupAt=android.os.SystemClock.elapsedRealtime();final int runId=++epoch;main.postDelayed(()->{if(epoch==runId&&state.equals("STARTING")){stopEngine();update("ERROR","เชื่อมต่อนานเกินไป กรุณาตรวจเน็ตแล้วกดเริ่มใหม่");}},150000);update("STARTING","กำลังอัปเดตฐานข้อมูล…");worker.execute(()->{try{
  if(!AppIntegrity.valid(this))throw new IOException("ตรวจสอบแอปไม่ผ่าน กรุณาติดตั้งรุ่นจากผู้พัฒนา");
  // Catalog can use its existing guest session while the native engine connects.
  if(new File(folder,"api.json").isFile()&&new File(folder,"keys.tsv").isFile())postFor(runId,()->{catalogAvailable=true;loadHome();});
  String ks=KeySync.sync(folder);android.util.Log.i("Rin23Timing","keys_ready_ms="+(android.os.SystemClock.elapsedRealtime()-startupAt));postFor(runId,()->{keyStatus=ks;status="กำลังเชื่อมต่อแค็ตตาล็อก…";changed();});if(stopped||epoch!=runId)return;
  try(FileOutputStream out=new FileOutputStream(new File(folder,"settings.json"))){out.write("{\"show_ratings\":false,\"show_id\":true}".getBytes(StandardCharsets.UTF_8));}
  ProcessBuilder pb=new ProcessBuilder(new File(getApplicationInfo().nativeLibraryDir,"librinengine.so").getAbsolutePath()).directory(folder).redirectErrorStream(true);pb.environment().put("KEYS_URL",KeySync.URL);pb.environment().put("KEYS_DECRYPTION_KEY",KeySync.KEY);pb.environment().put("HOME",folder.getAbsolutePath());pb.environment().put("TMPDIR",transferFolder.getAbsolutePath());
  Process running=pb.start();if(stopped||epoch!=runId){running.destroy();return;}engine=running;
  Protocol protocol=new Protocol(new Protocol.Listener(){
   public void searchReady(){android.util.Log.i("Rin23Timing","engine_ready_ms="+(android.os.SystemClock.elapsedRealtime()-startupAt));postFor(runId,()->{nativeReady=true;catalogAvailable=true;catalog.invalidateKeys();
    if(pendingLookup&&downloading!=null){pendingLookup=false;nativeReady=false;send(downloading.id);return;}
    if(state.equals("STARTING")){update("SEARCH","พร้อมใช้งาน");if(!homeLoaded||(!feed.loading&&items.isEmpty()))loadHome();return;}
    boolean waitingForExports=session.nativeReady();finishSession();
    if(waitingForExports){update("SAVING","กำลังบันทึกลง Download/rin23…");return;}
    if(state.equals("DOWNLOADING")||state.equals("VARIANT"))update("RESULTS","พร้อมใช้งาน");
   });}
   public void results(List<Protocol.Item> list,int p,int ps,int t){postFor(runId,()->{nativeReady=false;
    if(session.state()==DownloadSession.State.ACTIVE&&!resolvingDownload){session.end();finishSession();return;}
    if(downloading!=null&&resolvingDownload&&state.equals("DOWNLOADING")){resolvingDownload=false;Protocol.Item match=null;for(Protocol.Item row:list)if(row.id.equalsIgnoreCase(downloading.id)){match=row;break;}
     if(match!=null&&match.available){pendingNumber=match.number;send("d");}else{session.fail();session.end();finishSession();downloading=null;update("RESULTS","ไม่พบไฟล์ที่พร้อมดาวน์โหลดสำหรับผลงานนี้");send("s");}
    }
   });}
   public void selectItems(){postFor(runId,()->{if(pendingNumber!=null){send(pendingNumber.toString());pendingNumber=null;}else send("");});}
   public void variants(List<String> labels){postFor(runId,()->{variantLabels=labels;variantDelivered=false;update("VARIANT","เลือกรุ่นที่ต้องการดาวน์โหลด");});}
   public void saved(String name){postFor(runId,()->{if(outputNames.add(name))saveDownload(name);});}
   public void failure(String msg){postFor(runId,()->{if(session.state()==DownloadSession.State.ACTIVE)session.fail();status=msg;changed();});}
   public void status(String msg){postFor(runId,()->{status=msg;changed();});}
  });
  try(Reader in=new InputStreamReader(running.getInputStream(),StandardCharsets.UTF_8)){char[] b=new char[2048];int n;while((n=in.read(b))!=-1&&!stopped&&epoch==runId)protocol.feed(new String(b,0,n));}
  if(stopped||epoch!=runId)running.destroy();int exit=running.waitFor();if(!stopped)postFor(runId,()->update("ERROR","โปรแกรมหยุดทำงาน ("+exit+") กดเริ่มใหม่"));
 }catch(Exception e){if(!stopped)postFor(runId,()->update("ERROR","เปิดโปรแกรมไม่ได้ กรุณาตรวจเน็ตแล้วกดเริ่มใหม่"));}});}
 private void startTransfer(Protocol.Item item,int variant){
  final int generation=++transferGeneration;downloadedBytes=0;downloadTotal=-1;downloadPercent=-1;transferStage="กำลังตรวจสอบขนาดไฟล์";
  synchronized(meter){meter.reset();} // Native temp directories are removed by the engine after each transfer.
  catalogIo.execute(()->{Protocol.Item detail=item;try{if(detail.contentGroups.isEmpty())detail=catalog.details(item.id);}catch(Exception ignored){}final Protocol.Item result=detail;final List<String> urls=variant<detail.contentGroups.size()?detail.contentGroups.get(variant):Collections.emptyList();long total=TransferMeter.size(urls);main.post(()->{if(generation!=transferGeneration||stopped||session.saved>0||session.ended||downloading==null||!downloading.id.equals(item.id)||!(state.equals("DOWNLOADING")||state.equals("VARIANT")))return;downloading.contentGroups=result.contentGroups;downloadTotal=total;downloadPercent=TransferMeter.percent(downloadedBytes,total);transferStage=total>0?"กำลังดาวน์โหลด":"กำลังดาวน์โหลด • เซิร์ฟเวอร์ไม่แจ้งขนาด";changed();});});
 }
 private void removeTransferFiles(File dir){File[] fs=dir.listFiles();if(fs!=null)for(File f:fs){if(f.isDirectory())removeTransferFiles(f);f.delete();}}
 private void saveDownload(String name){
  final Protocol.Item source=downloading;final List<Download> existing=new ArrayList<>(downloads);
  saving++;session.queued();transferStage="กำลังบันทึกไฟล์";downloadPercent=0;downloadedBytes=0;downloadTotal=-1;update("SAVING","กำลังบันทึกลง Download/rin23…");
  disk.execute(()->{
   Uri uri=null;
   try{
    File base=new File(folder,"packs").getCanonicalFile(),file=new File(base,name).getCanonicalFile();
    if(!file.getParentFile().equals(base)||!file.isFile())throw new IOException("invalid output");
    final long size=file.length();main.post(()->{downloadTotal=size;downloadedBytes=0;downloadPercent=0;transferStage="กำลังบันทึกไฟล์";changed();});
    if(source!=null)for(Download previous:existing)if(DownloadHistory.sameFile(previous.itemId,previous.name,previous.size,source.id,file.getName(),size)){try(ParcelFileDescriptor check=getContentResolver().openFileDescriptor(Uri.parse(previous.uri),"r")){if(check!=null&&check.getStatSize()==size){file.delete();main.post(()->exportFinished(previous,true));return;}}catch(Exception ignored){}}

    ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,file.getName());values.put(MediaStore.Downloads.MIME_TYPE,"application/octet-stream");values.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/rin23");values.put(MediaStore.Downloads.IS_PENDING,1);
    uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new IOException();
    try(InputStream in=new FileInputStream(file);OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException();byte[] b=new byte[65536];int n;long copied=0,last=0;while((n=in.read(b))!=-1){out.write(b,0,n);copied+=n;long now=SystemClock.elapsedRealtime();if(now-last>300){last=now;final long done=copied;main.post(()->{downloadedBytes=done;downloadPercent=TransferMeter.percent(done,size);changed();});}}}
    values.clear();values.put(MediaStore.Downloads.IS_PENDING,0);if(getContentResolver().update(uri,values,null,null)!=1)throw new IOException();
    Download result=new Download(file.getName(),uri.toString(),source==null?"":source.imageUrl,size,source==null?"":source.id,false);file.delete();main.post(()->exportFinished(result,false));
   }catch(Exception e){if(uri!=null){try{getContentResolver().delete(uri,null,null);}catch(Exception ignored){}}main.post(()->{saving--;session.failedExport();finishSession();update(saving>0?"SAVING":session.state()==DownloadSession.State.ACTIVE?"DOWNLOADING":"RESULTS","บันทึกไม่สำเร็จ ไฟล์ยังอยู่ในแอป กรุณาลองดาวน์โหลดอีกครั้ง");});}
  });
 }
 private void exportFinished(Download result,boolean reused){if(!reused){downloads.add(0,result);while(downloads.size()>100)downloads.remove(downloads.size()-1);}persistHistory();saving--;session.saved();downloadedBytes=result.size;downloadTotal=result.size;downloadPercent=100;transferStage="บันทึกสำเร็จ";finishSession();if(session.state()==DownloadSession.State.ACTIVE)update(saving>0?"SAVING":"DOWNLOADING",saving>0?"กำลังบันทึกไฟล์ที่เหลือ…":"บันทึกไฟล์แล้ว • รอตัวดาวน์โหลดจบงาน");else changed();recoverPrompt();}
 private void recoverPrompt(){final String id=session.id;final int generation=epoch;main.postDelayed(()->{if(stopped||epoch!=generation||!session.id.equals(id)||session.state()!=DownloadSession.State.ACTIVE||saving>0||session.saved==0||session.ended)return;send("");main.postDelayed(()->{if(stopped||epoch!=generation||!session.id.equals(id)||session.state()!=DownloadSession.State.ACTIVE||saving>0||session.ended)return;session.fail();session.end();finishSession();status="ไฟล์บันทึกแล้ว แต่ตัวดาวน์โหลดไม่ตอบกลับ • รายการและเมนูยังใช้ได้ กรุณาเปิดแอปใหม่ก่อนโหลดรายการอื่น";state="ERROR";changed();},10000);},2000);}
 private void finishSession(){DownloadSession.State outcome=session.state();if(session.reported||outcome==DownloadSession.State.ACTIVE||outcome==DownloadSession.State.IDLE)return;session.reported=true;if(outcome==DownloadSession.State.COMPLETE){for(Download d:downloads)if(d.itemId.equals(session.id))d.complete=true;failedItems.remove(session.id);status="โหลดและบันทึกสำเร็จ • กดเปิดเพื่อดูไฟล์";}else{failedItems.add(session.id);status="ดาวน์โหลดหรือบันทึกไม่ครบ กรุณาลองใหม่";}persistHistory();if(!state.equals("ERROR"))state="RESULTS";revision++;changed();}
 private void loadHistory(){failedItems.addAll(getSharedPreferences("rin23",0).getStringSet("failedItems",Collections.emptySet()));try{JSONArray a=new JSONArray(getSharedPreferences("rin23",0).getString("downloads","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);downloads.add(new Download(o.getString("name"),o.getString("uri"),o.optString("image"),o.optLong("size",-1),o.optString("itemId"),o.optBoolean("complete",false)));}}catch(Exception ignored){}}
 private void refreshHistorySizes(){final List<Download> old=new ArrayList<>(downloads);final Set<String> knownFailures=new HashSet<>(failedItems);disk.execute(()->{List<Download> refreshed=new ArrayList<>();for(Download d:old){long size=d.size;boolean published=false,zip=false;try(Cursor c=getContentResolver().query(Uri.parse(d.uri),new String[]{android.provider.OpenableColumns.SIZE,MediaStore.Downloads.IS_PENDING},null,null,null)){if(c!=null&&c.moveToFirst()){if(!c.isNull(0))size=c.getLong(0);published=c.getInt(1)==0;}}catch(Exception ignored){}if(published&&size>0)try(InputStream in=getContentResolver().openInputStream(Uri.parse(d.uri))){byte[] header=new byte[4];int pos=0,n;while(pos<4&&(n=in.read(header,pos,4-pos))>0)pos+=n;zip=pos==4&&header[0]=='P'&&header[1]=='K'&&header[2]==3&&header[3]==4;}catch(Exception ignored){}boolean done=!d.itemId.isEmpty()&&DownloadHistory.recoverPublished(d.complete,knownFailures.contains(d.itemId),published,size,d.size,zip);refreshed.add(new Download(d.name,d.uri,d.image,size,d.itemId,done));}main.post(()->{for(Download d:refreshed)for(int i=0;i<downloads.size();i++)if(downloads.get(i).uri.equals(d.uri)){downloads.set(i,d);break;}historyChecked=true;persistHistory();revision++;changed();});});}
 private void persistHistory(){JSONArray a=new JSONArray();for(Download d:downloads){try{JSONObject o=new JSONObject();o.put("name",d.name);o.put("uri",d.uri);o.put("image",d.image);o.put("size",d.size);o.put("itemId",d.itemId);o.put("complete",d.complete);a.put(o);}catch(Exception ignored){}}getSharedPreferences("rin23",0).edit().putString("downloads",a.toString()).putStringSet("failedItems",new HashSet<>(failedItems)).apply();}
 public void deleteDownload(Download item){disk.execute(()->{try{
  Uri uri=Uri.parse(item.uri);if(!"content".equals(uri.getScheme())||!"media".equals(uri.getAuthority())||uri.getPathSegments().size()!=3||!"downloads".equals(uri.getPathSegments().get(1))||ContentUris.parseId(uri)<=0)throw new IOException("invalid download uri");
  int count=getContentResolver().delete(uri,null,null);
  if(count==0){try(Cursor c=getContentResolver().query(uri,new String[]{"_id"},null,null,null)){if(c==null||c.moveToFirst())throw new IOException("not deleted");}}
  main.post(()->{downloads.remove(item);persistHistory();status="ลบไฟล์แล้ว • "+item.name;changed();});
 }catch(Exception e){main.post(()->{status="ลบไฟล์ไม่ได้ ไฟล์อาจถูกย้าย หรือแอปไม่มีสิทธิ์";changed();});}});}
 public boolean importing=false,randomBusy=false;public String importStatus="";
 public interface RandomCallback{void done(Protocol.Item item,String error);}
 private final LinkedHashSet<String> recentRandom=new LinkedHashSet<>();
 public void randomPick(String kind,RandomCallback callback){if(randomBusy||!canBrowse())return;randomBusy=true;changed();catalogIo.execute(()->{Protocol.Item chosen=null;String error="";try{java.security.SecureRandom rng=new java.security.SecureRandom();CatalogClient.Page first=catalog.search("",kind,0,20,false);List<Protocol.Item> pool=new ArrayList<>();int pages=Math.max(1,(first.total+19)/20);for(int attempt=0;attempt<4&&pool.isEmpty();attempt++){int offset=attempt==3?0:rng.nextInt(pages)*20;CatalogClient.Page page=offset==0?first:catalog.search("",kind,offset,20,false);pool.addAll(RandomDiscovery.eligible(page.items,recentRandom));}if(pool.isEmpty())pool.addAll(RandomDiscovery.eligible(first.items,Collections.emptySet()));if(!pool.isEmpty())chosen=pool.get(rng.nextInt(pool.size()));else error="ยังสุ่มไม่พบผลงานที่พร้อมใช้งาน ลองหมวดอื่นอีกครั้ง";}catch(Exception e){error="สุ่มไม่สำเร็จ กรุณาตรวจอินเทอร์เน็ตแล้วลองใหม่";}final Protocol.Item item=chosen;final String message=error;main.post(()->{randomBusy=false;if(stopped)return;if(item!=null){detailCache.put(item.id,item);recentRandom.add(item.id);while(recentRandom.size()>30)recentRandom.remove(recentRandom.iterator().next());}changed();callback.done(item,message);});});}
 public void importLocal(Uri source,String displayName){if(importing||stopped)return;importing=true;importStatus="กำลังอ่านไฟล์จากเครื่อง…";changed();final List<Download> history=new ArrayList<>(downloads);
  disk.execute(()->{File work=null;Uri target=null;try{work=new File(getCacheDir(),"import-"+java.util.UUID.randomUUID());if(!work.mkdirs())throw new IOException("เตรียมพื้นที่นำเข้าไม่ได้");File input=new File(work,"input.zip");try(InputStream in=getContentResolver().openInputStream(source);OutputStream out=new BufferedOutputStream(new FileOutputStream(input))){if(in==null)throw new IOException("เปิดไฟล์ไม่ได้");PackArchive.copy(in,out,PackArchive.MAX_INPUT);}main.post(()->{importStatus="กำลังตรวจและเตรียมแพ็ก Minecraft…";changed();});File ready=PackArchive.prepare(input,work,displayName);String name=PackArchive.safeName(displayName)+(ready.getName().endsWith(".mcaddon")?".mcaddon":".mcpack");String id="local:"+sha256(ready);long size=ready.length();
   for(Download old:history)if(old.itemId.equals(id)){try(ParcelFileDescriptor fd=getContentResolver().openFileDescriptor(Uri.parse(old.uri),"r")){if(fd!=null&&fd.getStatSize()==size){main.post(()->{importing=false;importStatus="ไฟล์นี้อยู่ในรายการแล้ว • เปิดด้วย Minecraft ได้เลย";changed();});return;}}catch(Exception ignored){}}
   ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,name);values.put(MediaStore.Downloads.MIME_TYPE,PackProvider.mime(name));values.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/rin23");values.put(MediaStore.Downloads.IS_PENDING,1);target=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(target==null)throw new IOException("สร้างไฟล์ดาวน์โหลดไม่ได้");try(InputStream in=new FileInputStream(ready);OutputStream out=getContentResolver().openOutputStream(target)){if(out==null)throw new IOException("บันทึกไฟล์ไม่ได้");PackArchive.copy(in,out,PackArchive.MAX_INPUT);}values.clear();values.put(MediaStore.Downloads.IS_PENDING,0);if(getContentResolver().update(target,values,null,null)!=1)throw new IOException("บันทึกไฟล์ไม่สำเร็จ");Download imported=new Download(name,target.toString(),"",size,id,true);main.post(()->{downloads.add(0,imported);persistHistory();revision++;importing=false;importStatus="นำเข้าสำเร็จ • "+name+" • เปิดด้วย Minecraft ได้เลย";changed();});target=null;
  }catch(Exception e){if(target!=null)try{getContentResolver().delete(target,null,null);}catch(Exception ignored){}final String error=e instanceof IOException?e.getMessage():"อ่านไฟล์ไม่ได้ หรือรูปแบบยังไม่รองรับ";main.post(()->{importing=false;importStatus="นำเข้าไม่สำเร็จ • "+error;changed();});}finally{if(work!=null){File[] files=work.listFiles();if(files!=null)for(File f:files)f.delete();work.delete();}try{getContentResolver().releasePersistableUriPermission(source,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}}});
 }
 private static String sha256(File file)throws Exception{java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}StringBuilder out=new StringBuilder();for(byte b:digest.digest())out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();}

 private void stopEngine(){epoch++;Process p=engine;engine=null;if(p!=null)p.destroy();}
 public void shutdown(){stopped=true;stopEngine();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}
 @Override public void onDestroy(){stopped=true;stopEngine();worker.shutdownNow();disk.shutdownNow();catalogIo.shutdownNow();searchIo.shutdownNow();sizeIo.shutdownNow();telemetry.shutdownNow();super.onDestroy();}
 @Override public void onTimeout(int startId,int fgsType){shutdown();}
}
