#!/usr/bin/env python3
"""Exercise the real consent stores and navigation gate with in-memory Android adapters.

This does not emulate Android's SMS transport, permissions dialog, or carrier service.
It checks account isolation and the dispatch decision again after an OFF transition.
"""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PRODUCTION = ROOT / "app/src/main/java/kr/pagero/calltag"
SOURCES = {
    "android/content/SharedPreferences.java": '''package android.content;
public interface SharedPreferences {
 boolean contains(String key); boolean getBoolean(String key, boolean fallback);
 String getString(String key, String fallback); int getInt(String key, int fallback);
 Editor edit();
 interface Editor { Editor putBoolean(String key,boolean value); Editor putInt(String key,int value);
 Editor putString(String key,String value); Editor remove(String key); void apply(); }
}''',
    "android/content/Context.java": '''package android.content;
import java.util.*;
public class Context {
 public static final int MODE_PRIVATE=0; public Intent lastStartedIntent;
 private final Map<String,SharedPreferences> preferences=new HashMap<>();
 public Context getApplicationContext(){return this;}
 public SharedPreferences getSharedPreferences(String name,int mode){return preferences.computeIfAbsent(name,k->new Memory());}
 public void startActivity(Intent intent){lastStartedIntent=intent;}
 private static final class Memory implements SharedPreferences {
  private final Map<String,Object> data=new HashMap<>();
  public boolean contains(String k){return data.containsKey(k);}
  public boolean getBoolean(String k,boolean d){return data.containsKey(k)?(Boolean)data.get(k):d;}
  public int getInt(String k,int d){return data.containsKey(k)?(Integer)data.get(k):d;}
  public String getString(String k,String d){return data.containsKey(k)?(String)data.get(k):d;}
  public Editor edit(){return new Editor(){
   private final Map<String,Object> pending=new HashMap<>();
   public Editor putBoolean(String k,boolean v){pending.put(k,v);return this;}
   public Editor putInt(String k,int v){pending.put(k,v);return this;}
   public Editor putString(String k,String v){pending.put(k,v);return this;}
   public Editor remove(String k){pending.put(k,null);return this;}
   public void apply(){pending.forEach((k,v)->{if(v==null)data.remove(k);else data.put(k,v);});}
  };}
 }
}''',
    "android/content/Intent.java": '''package android.content;
public class Intent { public static final int FLAG_ACTIVITY_NEW_TASK=1;
 public final Class<?> target; public Intent(Context c,Class<?> t){target=t;}
 public Intent addFlags(int flags){return this;}
}''',
    "android/content/DialogInterface.java": '''package android.content;
public interface DialogInterface { interface OnClickListener {void onClick(DialogInterface d,int which);} }''',
    "android/app/Activity.java": '''package android.app;
public class Activity extends android.content.Context { public int permissionRequests;
 public void requestPermissions(String[] p,int code){permissionRequests++;}
}''',
    "android/app/AlertDialog.java": '''package android.app;
import android.content.*;
public class AlertDialog { public static class Builder {
 public Builder(Context c,int style){} public Builder setTitle(String s){return this;}
 public Builder setMessage(String s){return this;}
 public Builder setNegativeButton(String s,DialogInterface.OnClickListener l){return this;}
 public Builder setPositiveButton(String s,DialogInterface.OnClickListener l){return this;}
 public AlertDialog show(){return new AlertDialog();}
}}''',
    "android/telephony/SubscriptionManager.java": '''package android.telephony;
public final class SubscriptionManager {public static final int INVALID_SUBSCRIPTION_ID=-1;
public static boolean isValidSubscriptionId(int id){return id>=0;}}''',
    "kr/pagero/calltag/TestAdapters.java": '''package kr.pagero.calltag;
import android.content.*;
class AuthSessionStore {static String owner="owner-A";static boolean signedIn=true;
 static boolean hasSession(Context c){return signedIn;}static String ownerId(Context c){return owner;}}
class FeatureEntitlementStore {static boolean message=true;static boolean phone=true;
 static boolean hasMessageAccess(Context c){return message;}static boolean hasPhoneAccess(Context c){return phone;}}
class SetupRequirements {static boolean core=true;
 static boolean hasSms(Context c){return false;}static boolean hasCoreRuntimePermissions(Context c){return core;}
 static Intent requiredSetupIntent(Context c){return new Intent(c,SetupRequirements.class);}}
class BillingEntitlementActivity {}
class R {static class style {static final int Theme_CallTag_Dialog=1;}}
class SimProfileManager {static int selectedOrDefault(Context c){return 1;}}
class Customer {String displayName="Test";}
class CallRecord {String phone="01000000000";long startedAt=0;}
class MessageTemplateEngine {
 static final String VAR_CUSTOMER_NAME="{customer}",VAR_PHONE="{phone}",VAR_CALL_DATE="{date}",VAR_CALL_TIME="{time}";
 static String normalizeLegacyAliases(String s){return s==null?"":s;}
 static class RenderResult {} static RenderResult render(Context c,String s,Customer x,CallRecord r){return new RenderResult();}}
class MessageAutomationManager {
 static final String TRIGGER_CONNECTED="CALL_CONNECTED_END",TRIGGER_INCOMING="CALL_INCOMING_END",
 TRIGGER_OUTGOING="CALL_OUTGOING_END",TRIGGER_MISSED="MISSED_OR_REJECTED_CALL",
 TRIGGER_DELAYED="DELAY_AFTER_CALL",TRIGGER_MANUAL="MANUAL_SEND",TRIGGER_CAMPAIGN="CAMPAIGN_SEND";}
class PageroLeadMessageAutomation {static final String TRIGGER_PAGERO_LEAD_RECEIVED="PAGERO_LEAD_RECEIVED";}
class MessageDedupeEngine {static String sha256(String value){
 try {byte[] d=java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
 StringBuilder s=new StringBuilder();for(byte b:d)s.append(String.format("%02x",b&255));return s.toString();}
 catch(Exception e){throw new IllegalStateException(e);}}}
''',
    "kr/pagero/calltag/SmsConsentRegression.java": '''package kr.pagero.calltag;
import android.app.Activity;
import android.content.Context;
public class SmsConsentRegression {
 static int assertions;
 static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
 static boolean lead(Context c){return MessageAutomationStore.allowsAutomaticTrigger(c,"PAGERO_LEAD_RECEIVED");}
 public static void main(String[] args){
  Activity c=new Activity();
  c.getSharedPreferences("calltag_pagero_lead_message",0).edit().putBoolean("enabled",true).apply();
  check(!lead(c),"Unowned legacy settings must not enable inquiry automation");
  PageroLeadMessageSettings.setEnabled(c,true);
  PageroLeadMessageSettings.setDefaultTemplate(c,"Owner A template");
  PageroLeadMessageSettings.setDelayMinutes(c,17);
  check(lead(c),"Saved inquiry rule must allow its job");
  PageroLeadMessageSettings.setEnabled(c,false);
  check(!lead(c),"An already queued inquiry job must stop after OFF");
  MessageAutomationStore.setConnectedEnabled(c,true);
  check(!MessageAutomationStore.allowsAutomaticTrigger(c,"CALL_OUTGOING_END"),"Call master OFF must block queued call SMS");
  MessageAutomationStore.setEnabled(c,true);
  check(MessageAutomationStore.allowsAutomaticTrigger(c,"CALL_OUTGOING_END"),"Enabled connected rule must still work");
  check(!lead(c),"Call ON must not override inquiry OFF");
  MessageAutomationStore.setEnabled(c,false);
  PageroLeadMessageSettings.setEnabled(c,true);
  check(lead(c),"Inquiry rule must remain independent of the call rule");
  AuthSessionStore.owner="owner-B";
  check(!lead(c),"Another account must start with inquiry SMS OFF");
  check(!PageroLeadMessageSettings.defaultTemplate(c).equals("Owner A template"),"Templates must not cross accounts");
  check(PageroLeadMessageSettings.delayMinutes(c)==0,"Delay settings must not cross accounts");
  AuthSessionStore.owner="owner-A";
  check(lead(c),"Returning owner must retain its own enabled setting");
  check(PageroLeadMessageSettings.delayMinutes(c)==17,"Returning owner must retain its delay");
  AuthSessionStore.signedIn=false;
  check(!lead(c),"Logged-out inquiry automation must be blocked");
  AuthSessionStore.signedIn=true;AuthSessionStore.owner="";
  check(!lead(c),"A missing server owner must block inquiry automation");
  AuthSessionStore.owner="owner-A";
  FeatureAccessGate.open(c,SmsConsentRegression.class,FeatureAccessGate.MESSAGE);
  check(c.lastStartedIntent!=null,"Message settings must open without SMS permission");
  check(c.permissionRequests==0,"Opening settings must not request SMS permission");
  check(FeatureAccessGate.require(c,FeatureAccessGate.MESSAGE),"Message entitlement check must not depend on SMS permission");
  check(c.permissionRequests==0,"Checking entitlement must not request SMS permission");
  FeatureEntitlementStore.message=false;c.lastStartedIntent=null;
  FeatureAccessGate.open(c,SmsConsentRegression.class,FeatureAccessGate.MESSAGE);
  check(c.lastStartedIntent==null,"Expired message entitlement must still block the feature");
  check(!FeatureAccessGate.require(c,FeatureAccessGate.MESSAGE),"Expired entitlement guard must remain");
  SetupRequirements.core=false;
  check(!FeatureAccessGate.require(c,FeatureAccessGate.PHONE),"Phone permission setup guard must remain");
  check(c.lastStartedIntent!=null&&c.lastStartedIntent.target==SetupRequirements.class,"Phone setup route must remain");
  System.out.println("SMS consent runtime regression: PASS ("+assertions+" assertions)");
 }
}''',
}

def binary(name):
    found = shutil.which(name)
    if found:
        return found
    home = os.environ.get("JAVA_HOME", "")
    candidate = Path(home) / "bin" / (name + (".exe" if os.name == "nt" else ""))
    if home and candidate.is_file():
        return str(candidate)
    raise SystemExit(f"JDK 17 is required: {name} was not found")

with tempfile.TemporaryDirectory(prefix="calltag-sms-consent-") as temp:
    temp = Path(temp)
    source_paths = []
    for relative, content in SOURCES.items():
        path = temp / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        source_paths.append(str(path))
    source_paths.extend(str(PRODUCTION / name) for name in (
        "FeatureAccessGate.java", "AccountDataScope.java",
        "MessageAutomationStore.java", "PageroLeadMessageSettings.java",
    ))
    classes = temp / "classes"
    classes.mkdir()
    subprocess.run([binary("javac"), "-encoding", "UTF-8", "-d", str(classes), *source_paths], check=True)
    subprocess.run([binary("java"), "-cp", str(classes), "kr.pagero.calltag.SmsConsentRegression"], check=True)
