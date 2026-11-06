package com.rin23.app;
final class GalleryRules {
 static boolean belowDescription(int width,int height){if(width<=0||height<=0)return false;double ratio=(double)width/height;return ratio<1.0||ratio>2.4;}
 static boolean panorama(int width,int height){return width>0&&height>0&&(double)width/height>2.4;}
 static int panoramaWidth(int viewport,int height,int sourceWidth,int sourceHeight){return Math.max(viewport,(int)Math.ceil((double)height*sourceWidth/Math.max(1,sourceHeight)));}
 static int naturalHeight(int width,int sourceWidth,int sourceHeight){if(width<=0||sourceWidth<=0||sourceHeight<=0)return 1;return (int)Math.max(1,Math.min(1000000,Math.round((double)width*sourceHeight/sourceWidth)));}
}
