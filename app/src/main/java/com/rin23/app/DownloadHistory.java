package com.rin23.app;
/** The public file, not a previous progress value, is evidence for restoring a checkmark. */
final class DownloadHistory {
 static boolean recoverPublished(boolean complete,boolean explicitFailure,boolean published,long actualSize,long recordedSize,boolean zip){if(!published||!zip||actualSize<=0||(recordedSize>=0&&recordedSize!=actualSize))return false;return complete||!explicitFailure;}
 static boolean sameFile(String oldId,String oldName,long oldSize,String id,String name,long size){return !id.isEmpty()&&oldId.equals(id)&&oldName.equals(name)&&oldSize==size;}
}
