package com.rin23.app;
/** Host test for dispatch MIME types; actual URI grants/import require Android. */
public final class PackProviderTest {
 public static void main(String[] args){
  String[][] cases={{"Actions Stuff (skin_pack).mcpack","application/mcpack"},{"แพ็ก.MCADDON","application/mcaddon"},{"โลก.mcworld","application/mcworld"},{"Template.mctemplate","application/mctemplate"}};
  for(String[] c:cases)if(!PackProvider.mime(c[0]).equals(c[1]))throw new AssertionError(c[0]);
  for(String name:new String[]{"file.zip","file.mcpack.exe","file"}){try{PackProvider.mime(name);throw new AssertionError(name);}catch(IllegalArgumentException expected){}}
  System.out.println("PackProvider MIME dispatch: PASS");
 }
}
