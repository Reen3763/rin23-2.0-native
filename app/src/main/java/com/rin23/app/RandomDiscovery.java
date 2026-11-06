package com.rin23.app;
import java.util.*;
final class RandomDiscovery {
 static List<Protocol.Item> eligible(List<Protocol.Item> items,Set<String> recent){List<Protocol.Item> result=new ArrayList<>();Set<String> ids=new HashSet<>();for(Protocol.Item item:items)if(item.available&&!item.id.isEmpty()&&!recent.contains(item.id)&&ids.add(item.id))result.add(item);return result;}
}
