package com.rin23.app;
import org.json.*;
import java.util.*;
public final class DonationClientTest {
 static void ok(boolean b,String message){if(!b)throw new AssertionError(message);}
 interface Action{void run()throws Exception;}static void fails(Action a,String name)throws Exception{try{a.run();throw new AssertionError(name);}catch(IllegalArgumentException|java.io.IOException expected){}}
 static JSONObject success(String amount)throws Exception{return new JSONObject().put("status",new JSONObject().put("code","SUCCESS")).put("data",new JSONObject().put("my_ticket",new JSONObject().put("amount_baht",amount)));}
 static final String LINK="https://gift.truemoney.com/campaign/?v=0123456789abcdef";
 public static void main(String[] args)throws Exception{
  ok(DonationClient.code(LINK+"#test").equals("0123456789abcdef"),"canonical code");
  for(String bad:new String[]{"https://gift.truemoney.com.evil.test/campaign/?v=0123456789abcdef","http://gift.truemoney.com/campaign/?v=0123456789abcdef",LINK+"&v=otherlongcode123",LINK.replace("/campaign/","/other/"),LINK.replace("gift.truemoney.com","user:password@gift.truemoney.com"),LINK.replace("0123456789abcdef","abc%20def12345"),"0123456789abcdef","https://example.org/"})fails(()->DonationClient.code(bad),"reject fake link");
  ok(DonationClient.amount("10.05")==1005&&DonationClient.amount("0.01")==1&&DonationClient.baht(1005).equals("10.05"),"exact integer money");
  for(Object bad:new Object[]{"10abc","1e3","Infinity","0","-1","1.001",true,null})fails(()->DonationClient.amount(bad),"reject invalid amount");
  List<String> order=new ArrayList<>();DonationClient c=new DonationClient((method,url,body)->{order.add(method);if(method.equals("POST")){ok(body.getString("mobile").equals("0955136110"),"fixed recipient");ok(body.getString("voucher_hash").equals("0123456789abcdef"),"selected voucher");return success("10.05");}return new JSONObject("{\"status\":{\"code\":\"SUCCESS\"}}");});
  DonationClient.Result r=c.redeem(LINK,true,()->order.add("PERSIST"));ok(r.success&&r.satang==1005,"verified result");ok(order.equals(Arrays.asList("GET","PERSIST","POST")),"durable checkpoint before only POST");
  order.clear();fails(()->c.redeem(LINK,false,()->{}),"consent required");ok(order.isEmpty(),"no network without explicit confirmation");
  order.clear();fails(()->c.redeem(LINK,true,()->{throw new java.io.IOException("disk full");}),"checkpoint failed");ok(order.equals(Arrays.asList("GET")),"no POST when persistence fails");
  int[] calls={0};DonationClient maintenance=new DonationClient((m,u,b)->{calls[0]++;return new JSONObject("{\"status\":{\"code\":\"MAINTENANCE\"}}");});fails(()->maintenance.redeem(LINK,true,()->{throw new AssertionError("no submit");}),"maintenance stops");ok(calls[0]==1,"preflight only");
  calls[0]=0;DonationClient blocked=new DonationClient((m,u,b)->{calls[0]++;throw new DonationClient.ProviderError(403);});fails(()->blocked.redeem(LINK,true,()->{}),"blocked stops");ok(calls[0]==1,"no retries or alternate transport");
  for(JSONObject raw:new JSONObject[]{new JSONObject(),success("10abc"),new JSONObject("{\"status\":{\"code\":\"SUCCESS\"}}")}){DonationClient invalid=new DonationClient((m,u,b)->m.equals("GET")?new JSONObject("{\"status\":{\"code\":\"SUCCESS\"}}"):raw);fails(()->invalid.redeem(LINK,true,()->{}),"HTTP200 not success without status and exact received amount");}
  DonationClient rejected=new DonationClient((m,u,b)->new JSONObject(m.equals("GET")?"{\"status\":{\"code\":\"SUCCESS\"}}":"{\"status\":{\"code\":\"VOUCHER_OUT_OF_STOCK\"}}"));ok(!rejected.redeem(LINK,true,()->{}).success,"provider rejection remains failure");
  int[] posts={0};DonationClient timeout=new DonationClient((m,u,b)->{if(m.equals("POST")){posts[0]++;throw new java.io.IOException("timeout");}return new JSONObject("{\"status\":{\"code\":\"SUCCESS\"}}");});fails(()->timeout.redeem(LINK,true,()->{}),"post timeout");ok(posts[0]==1,"never automatically retry a money request");
  ok(DonationClient.hash(DonationClient.code(LINK)).length()==64,"voucher hash stored instead of code");
  System.out.println("PASS: fixed recipient, consent, durable submit ordering, strict link/amount, maintenance/403, ambiguous results and no network retries (all mocked)");
 }
}
