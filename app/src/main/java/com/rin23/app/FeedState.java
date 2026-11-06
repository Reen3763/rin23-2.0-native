package com.rin23.app;
import java.util.*;
/** Paging cursor advances by server rows, independently of deduplicated visible cards. */
final class FeedState {
 int offset=0,total=0;boolean loading=false,exhausted=false;String error="";
 final List<Protocol.Item> items=new ArrayList<>();private final Set<String> ids=new HashSet<>();
 void reset(){offset=0;total=0;loading=false;exhausted=false;error="";items.clear();ids.clear();}
 boolean begin(boolean retry){if(loading||exhausted||(!retry&&!error.isEmpty()))return false;loading=true;error="";return true;}
 void accept(List<Protocol.Item> rows,int count,int consumed){total=Math.max(0,count);offset+=consumed;int before=items.size();for(Protocol.Item row:rows)if(!row.id.isEmpty()&&ids.add(row.id))items.add(row);loading=false;error="";exhausted=consumed==0||offset>=total||items.size()==before;}
 void fail(){loading=false;error="โหลดต่อไม่สำเร็จ แตะเพื่อลองอีกครั้ง";}
}
