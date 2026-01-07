# LiveKit ProGuard Rules
-keep class io.livekit.** { *; }
-keep class org.webrtc.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Gson
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.zain.jordan.voiceapp.data.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Missing classes from R8
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.errorprone.annotations.MustBeClosed
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
