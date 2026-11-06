package com.rin23.app;
import java.text.Normalizer;
import java.util.Locale;
final class FileSearch {
 private static String normalize(String s){return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).trim();}
 static boolean matches(String name,String query){String haystack=normalize(name),needle=normalize(query);if(needle.isEmpty())return true;for(String word:needle.split("\\s+"))if(!haystack.contains(word))return false;return true;}
}
