# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /opt/androidstudio/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-optimizations !code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable
-dontobfuscate

-dontwarn org.slf4j.**

-keep class ru.taaasty.rest.model.** { *; }

#Retrofit
-dontwarn android.net.http.AndroidHttpClient
-dontwarn retrofit.client.ApacheClient$GenericEntityHttpRequest
-dontwarn retrofit.client.ApacheClient$GenericHttpRequest
-dontwarn retrofit.client.ApacheClient$TypedOutputEntity

# Eventbus
-keepclassmembers class ** {
    public void onEvent*(**);
}

# GSON
-keepattributes Signature
-keep class sun.misc.Unsafe { *; }

# Retrofit
-keepattributes *Annotation*
-keep class retrofit.** { *; }
-keepclasseswithmembers class * {
    @retrofit.http.* <methods>;
}
-dontwarn rx.**
-dontwarn com.squareup.okhttp.**
-dontwarn com.google.appengine.api.urlfetch.**

#okhttp
-dontwarn java.nio.file.*
-dontwarn com.squareup.okhttp.internal.http.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

#android-gif-drawable
-keep public class pl.droidsonroids.gif.GifIOException{<init>(int);}
-keep class pl.droidsonroids.gif.GifInfoHandle{<init>(long,int,int,int);}

#Square otto
-keepattributes *Annotation*
-keepclassmembers class ** {
    @com.squareup.otto.Subscribe public *;
    @com.squareup.otto.Produce public *;
}

#adobe sdk
-keepnames class com.adobe.creativesdk.foundation.internal.auth.** {
   *;
}

#Android support v4
-keep class android.support.v7.widget.SearchView { *; }

#Android design library
-keep public class * extends android.support.design.widget.CoordinatorLayout.Behavior { *; }
-keep public class * extends android.support.design.widget.ViewOffsetBehavior { *; }
-keepclassmembers class android.support.design.widget.FloatingActionButton$Behavior { public <init>(); }
-keep public class * extends android.support.v7.preference.Preference { *; }


# vk
#-keep class com.vk.** { *;}
#-dontskipnonpubliclibraryclassmembers

#Aviary
-dontwarn android.util.FloatMath**

-dontwarn android.app.Notification**


# pusher
#-dontwarn javax.management.**
#-dontwarn javax.xml.**
#-dontwarn org.apache.log4j.**
#-dontwarn org.slf4j.*