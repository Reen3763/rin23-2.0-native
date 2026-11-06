package com.rin23.app;

final class SharePolicy {
 static final long TTL_MS=60L*60*1000;
 static boolean active(long issued,long now){return issued>0&&now>=issued&&now-issued<TTL_MS;}
 static boolean token(String value){return value!=null&&value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");}
}
