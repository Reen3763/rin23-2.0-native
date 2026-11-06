package com.rin23.app;
import android.content.Context;
import android.os.*;
import java.util.*;
import java.util.concurrent.*;

/** One process-wide worker survives leaving the screen. Persistent checkpoints survive process loss. */
final class DonationManager {
 interface Observer{void changed();}
 private static DonationManager instance;
 static synchronized DonationManager get(Context c){if(instance==null)instance=new DonationManager(c.getApplicationContext());return instance;}
 private final DonationStore store;private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler main=new Handler(Looper.getMainLooper());private final List<Observer> observers=new ArrayList<>();
 boolean busy=false;String message="บริจาคโดยสมัครใจ แอปใช้ฟรีเหมือนเดิม";boolean succeeded=false;
 private DonationManager(Context c){store=new DonationStore(c);}
 void observe(Observer o){if(!observers.contains(o))observers.add(o);o.changed();}void unobserve(Observer o){observers.remove(o);}private void changed(){for(Observer o:new ArrayList<>(observers))o.changed();}
 List<DonationStore.Entry> history(){return store.recent();}
 void donate(String link){if(busy)return;final String canonical="https://gift.truemoney.com/campaign/?v="+DonationClient.code(link),hash=DonationClient.hash(DonationClient.code(link));
  busy=true;succeeded=false;message="กำลังตรวจสอบและส่งซอง กรุณารอ…";changed();worker.execute(()->{
   String msg;boolean success=false;boolean[] sent={false};
   try{
    DonationStore.Entry entry=store.begin(hash);
    if(entry==null){DonationStore.Entry old=store.find(hash);msg=old!=null&&old.state.equals("succeeded")?"ซองนี้รับบริจาคแล้ว ไม่มีการส่งซ้ำ":"ซองนี้เคยส่งแล้ว กรุณาตรวจผลจากประวัติ ยังไม่มีการส่งซ้ำ";}
    else{
     try{
      DonationClient.Result result=new DonationClient().redeem(canonical,true,()->{store.submitted(hash);sent[0]=true;});
      store.finish(hash,result.success?"succeeded":"rejected",result.satang,result.message);success=result.success;msg=result.message;
     }catch(Exception e){String reason=e instanceof DonationClient.ProviderError?e.getMessage():"การเชื่อมต่อขัดข้อง หรือยังอ่านผลจากผู้ให้บริการไม่ได้";msg=sent[0]?reason+"\nยังยืนยันการรับเงินไม่ได้ กรุณาตรวจยอดกับผู้รับก่อนส่งซองใหม่":reason+"\nยังไม่ได้ส่งคำขอรับซอง สามารถลองใหม่ภายหลัง";try{store.finish(hash,sent[0]?"unknown":"not_sent",0,msg);}catch(Exception ignored){msg+="\nบันทึกสถานะไม่ได้ กรุณาอย่าส่งซ้ำ";}}
    }
   }catch(Exception e){msg="ไม่สามารถบันทึกรายการได้ กรุณาเปิดแอปใหม่และตรวจประวัติก่อนส่งซ้ำ";}
   final String finalMessage=msg;final boolean ok=success;main.post(()->{busy=false;succeeded=ok;message=finalMessage;changed();});
  });
 }
}
