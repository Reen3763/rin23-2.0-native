package com.rin23.app;
import java.text.Normalizer;
import java.util.Locale;
final class SearchQuery {
 static String normalize(String value){String s=Normalizer.normalize(value==null?"":value,Normalizer.Form.NFKC).replaceAll("[\\u200B-\\u200D\\uFEFF]","").replaceAll("[\\p{Z}\\s]+"," ").trim();return s;}
 static boolean uuid(String query){return query.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");}
 static String literal(String query){String q=normalize(query);StringBuilder out=new StringBuilder();for(int i=0;i<q.length();i++){char c=q.charAt(i);if("+-&|!(){}[]^\"~*?:\\/".indexOf(c)>=0)out.append('\\');out.append(c);}return out.toString();}
 static boolean matchesType(Protocol.Item item,String kind){switch(kind){case "addon":return item.type.equals("Addon");case "world":return item.type.equals("World");case "skin":return item.type.equals("SkinPack");case "texture":return item.type.equals("TexturePack");default:return true;}}
}
