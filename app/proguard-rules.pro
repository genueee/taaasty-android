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


-keep class ru.taaasty.model.** { *; }

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