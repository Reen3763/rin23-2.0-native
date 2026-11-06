package com.rin23.app;
/** A checkmark requires the native job to end AND every queued export to succeed. */
final class DownloadSession {
 enum State { IDLE, ACTIVE, COMPLETE, FAILED }
 String id="";int pending=0,saved=0;boolean ended=false,failed=false,reported=false;
 void begin(String item){id=item;pending=0;saved=0;ended=false;failed=false;reported=false;}
 void queued(){pending++;}void saved(){pending--;saved++;}void failedExport(){pending--;failed=true;}
 boolean nativeReady(){if(!id.isEmpty())ended=true;return pending>0;}
 void end(){ended=true;}void fail(){failed=true;}
 State state(){if(id.isEmpty())return State.IDLE;if(!ended||pending>0)return State.ACTIVE;return !failed&&saved>0?State.COMPLETE:State.FAILED;}
}
