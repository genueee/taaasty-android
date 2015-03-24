
# common library
-keep class com.aviary.android.feather.sdk.AviaryIntent
-keep class com.aviary.android.feather.sdk.internal.tracking.AviaryTracker
-keep class com.aviary.android.feather.sdk.internal.tracking.AbstractTracker
-keep class com.aviary.android.feather.sdk.log.LoggerFactory
-keep class com.aviary.android.feather.sdk.internal.headless.gl.GLUtils
-keep class com.aviary.android.feather.sdk.internal.services.BaseContextService
-keep class com.aviary.android.feather.sdk.internal.tracking.TrackerFactory


# headless library
-keep interface com.aviary.android.feather.sdk.internal.headless.filters.IFilter
-keep class com.aviary.android.feather.sdk.internal.headless.AviaryEffect
-keep class com.aviary.android.feather.sdk.internal.headless.moa.Moa
-keep class com.aviary.android.feather.sdk.internal.headless.moa.MoaHD
-keep class com.aviary.android.feather.sdk.internal.headless.moa.MoaParameter
-keep class com.aviary.android.feather.sdk.internal.headless.utils.CameraUtils
-keep class com.aviary.android.feather.sdk.internal.headless.moa.MoaJavaUndo
-keep class com.aviary.android.feather.sdk.internal.headless.moa.MoaJavaUndo$MoaUndoBitmap

-keep class com.aviary.android.feather.sdk.BuildConfig
-keep class com.aviary.android.feather.cds.BuildConfig
-keep class com.aviary.android.feather.headless.BuildConfig
-keep class com.aviary.android.feather.common.BuildConfig

-keep class * extends com.aviary.android.feather.sdk.internal.headless.filters.IFilter
-keep class * extends com.aviary.android.feather.sdk.internal.headless.moa.MoaParameter

-keep class * extends com.aviary.android.feather.sdk.widget.AviaryStoreWrapperAbstract
-keep class * extends com.aviary.android.feather.sdk.widget.PackDetailLayout
-keep class * extends com.aviary.android.feather.sdk.internal.services.BaseContextService
-keep class * extends com.aviary.android.feather.sdk.internal.tracking.AbstractTracker
-keep public class com.android.vending.billing.IInAppBillingService
-keep class com.aviary.android.feather.sdk.internal.headless.moa.MoaResult
-keep class com.aviary.android.feather.sdk.internal.headless.filters.NativeFilterProxy
-keep class com.aviary.android.feather.sdk.utils.AviaryIntentConfigurationValidator
-keep class com.aviary.android.feather.sdk.internal.Constants
-keep class com.aviary.android.feather.sdk.AviaryIntent
-keep class com.aviary.android.feather.sdk.AviaryIntent$Builder
-keep class com.aviary.android.feather.sdk.AviaryVersion

-keepclassmembers class com.aviary.android.feather.sdk.overlays.UndoRedoOverlay {
    void setAlpha1(int);
    void setAlpha2(int);
    void setAlpha3(int);
    int getAlpha1();
    int getAlpha2();
    int getAlpha3();
}

-keepclassmembers class * extends com.aviary.android.feather.sdk.internal.graphics.drawable.FeatherDrawable {
	float getScaleX();
	void setScaleX(float);
}

-keepclassmembers class com.aviary.android.feather.sdk.AviaryIntent {*;}
-keepclassmembers class com.aviary.android.feather.sdk.AviaryIntent$Builder {*;}
-keepclassmembers class com.aviary.android.feather.sdk.AviaryVersion {*;}
-keepclassmembers class com.aviary.android.feather.sdk.utils.AviaryIntentConfigurationValidator {*;}
-keepclassmembers class com.aviary.android.feather.sdk.internal.graphics.drawable.FeatherDrawable {*;}
-keepclassmembers class com.aviary.android.feather.sdk.internal.utils.SDKUtils {*;}
-keepclassmembers class com.aviary.android.feather.sdk.internal.utils.SDKUtils$ApiKeyReader {*;}

# keep everything for native methods/fields
-keepclassmembers class com.aviary.android.feather.sdk.internal.headless.moa.Moa {*;}
-keepclassmembers class com.aviary.android.feather.sdk.internal.headless.moa.MoaHD {*;}
-keepclassmembers class com.aviary.android.feather.sdk.internal.headless.moa.MoaJavaUndo {*;}
-keepclassmembers class com.aviary.android.feather.sdk.internal.headless.utils.CameraUtils {*;}
-keepclassmembers class com.aviary.android.feather.sdk.internal.headless.moa.MoaResult {*;}
-keepclassmembers class com.aviary.android.feather.sdk.opengl.AviaryGLSurfaceView {*;}

-keepclassmembers class com.aviary.android.feather.sdk.internal.headless.filters.MoaJavaToolStrokeResult {
  <methods>;
}

-keepclassmembers class com.aviary.android.feather.sdk.internal.headless.gl.GLUtils {
  <methods>;
}

-keepclassmembers class com.aviary.android.feather.sdk.internal.headless.filters.NativeToolFilter {*;}

-keepclassmembers class com.aviary.android.feather.sdk.AviaryIntent {*;}
-keepclassmembers class com.aviary.android.feather.sdk.internal.os.AviaryIntentService {*;}
-keepclassmembers class com.aviary.android.feather.sdk.internal.os.AviaryAsyncTask {*;}

-keepclassmembers class com.aviary.android.feather.sdk.internal.tracking.AbstractTracker {
    <fields>;
}
-keepclassmembers class com.aviary.android.feather.sdk.internal.tracking.AviaryTracker {
    <fields>;
}

-keepclassmembers class com.aviary.android.feather.sdk.log.LoggerFactory {
    <fields>;
}

-keepclassmembers class com.aviary.android.feather.sdk.internal.headless.moa.MoaJavaUndo$MoaUndoBitmap {
    <fields>;
}

-keepclassmembers class com.aviary.android.feather.sdk.BuildConfig {*;}
-keepclassmembers class com.aviary.android.feather.cds.BuildConfig {*;}
-keepclassmembers class com.aviary.android.feather.headless.BuildConfig {*;}
-keepclassmembers class com.aviary.android.feather.common.BuildConfig {*;}


# keep class members
-keepclassmembers class com.aviary.android.feather.sdk.internal.tracking.AbstractTracker { *; }
-keepclassmembers class com.aviary.android.feather.sdk.internal.tracking.TrackerFactory { *; }
-keepclassmembers class com.aviary.android.feather.sdk.internal.headless.gl.GLUtils { *; }
-keepclassmembers class com.aviary.android.feather.sdk.internal.services.BaseContextService { *; }
-keepclassmembers class com.aviary.android.feather.utils.SettingsUtils { *; }

-keepclassmembers class * extends com.aviary.android.feather.sdk.internal.services.BaseContextService {
   public <init>( com.aviary.android.feather.sdk.internal.services.IAviaryController );
}

-keepclasseswithmembers class * {
    public <init>( com.aviary.android.feather.sdk.internal.services.IAviaryController );
}

# Keep all the native methods
-keepclassmembers class * {
   private native <methods>;
   public native <methods>;
   protected native <methods>;
   public static native <methods>;
   private static native <methods>;
   static native <methods>;
   native <methods>;
}

-keepclasseswithmembers class * {
    public <init>( com.aviary.android.feather.sdk.internal.services.IAviaryController );
}

