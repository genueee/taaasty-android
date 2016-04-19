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

# Eventbus
-keepclassmembers class ** {
    public void onEvent*(**);
}

# GSON
-keepattributes Signature
-keep class sun.misc.Unsafe { *; }

# Retrofit
-keepattributes *Annotation*
-keepattributes Exceptions
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn rx.**
-dontwarn com.squareup.okhttp.**
-dontwarn com.google.appengine.api.urlfetch.**


#okhttp
-dontwarn java.nio.file.*
-dontwarn com.squareup.okhttp.internal.http.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

#okhttp3
-dontwarn okhttp3.**

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

#RxJava
-dontwarn sun.misc.**

-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}

-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

# retrolambda
-dontwarn java.lang.invoke.*
