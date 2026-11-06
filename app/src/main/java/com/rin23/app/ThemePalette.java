package com.rin23.app;
/** Semantic colors shared by all screens. No Android dependency. */
final class ThemePalette {
 final int background,surface,text,muted,accent,onAccent,border,soft,success,star;final boolean light;
 ThemePalette(boolean light){this.light=light;
 background=light?0xfff5f5f7:0xff101114;surface=light?0xffffffff:0xff1b1d23;
 text=light?0xff202127:0xfff6f5f2;muted=light?0xff656976:0xffb0b3be;
 accent=light?0xffb92d29:0xffff6b61;onAccent=light?0xffffffff:0xff21100f;
 border=light?0xffdedfe5:0xff30333c;soft=light?0xffffe9e6:0xff382323;
 success=light?0xff247343:0xff9de1b0;star=light?0xff956000:0xfff0d9a8;
 }
}
