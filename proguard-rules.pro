# Keep Android component names and platform callbacks; minify app internals.
-keep public class * extends android.app.Activity { public <init>(); }
-keep public class * extends android.app.Service { public <init>(); }
-keep public class * extends android.content.ContentProvider { public <init>(); }
-keepclassmembers class * implements android.os.Parcelable {
 public static final android.os.Parcelable$Creator CREATOR;
}
-keepclasseswithmembernames,includedescriptorclasses class * { native <methods>; }
-keepattributes Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Retain the requested owner notice in the APK, without displaying it in the UI.
-keep class com.rin23.app.OwnerPolicy { *; }
