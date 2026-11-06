package com.rin23.app;
public final class ThemePaletteTest {
 static double linear(int v){double s=v/255.0;return s<=0.04045?s/12.92:Math.pow((s+0.055)/1.055,2.4);}
 static double luminance(int c){return 0.2126*linear((c>>16)&255)+0.7152*linear((c>>8)&255)+0.0722*linear(c&255);}
 static void readable(int fg,int bg,String label){double a=luminance(fg),b=luminance(bg),ratio=(Math.max(a,b)+.05)/(Math.min(a,b)+.05);if(ratio<4.5)throw new AssertionError(label+" contrast "+ratio);}
 public static void main(String[] args){for(boolean light:new boolean[]{false,true}){ThemePalette p=new ThemePalette(light);for(int bg:new int[]{p.background,p.surface}){readable(p.text,bg,"body "+light);readable(p.muted,bg,"secondary "+light);readable(p.accent,bg,"accent "+light);readable(p.success,bg,"success "+light);}readable(p.onAccent,p.accent,"button "+light);readable(p.accent,p.soft,"selected "+light);}System.out.println("PASS: readable body, secondary, action, success and selected text in both themes (4.5:1)");}
}
