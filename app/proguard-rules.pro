# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

############################################
# Baseline: keep useful debug info for crashes
############################################

# Keep line numbers so Crashlytics stack traces are readable
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotation/metadata that some libs (and Kotlin) rely on
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-keepattributes Exceptions
-keepattributes KotlinMetadata

############################################
# Firebase / Crashlytics
############################################

# Usually not needed (Firebase ships its own rules), but harmless:
-dontwarn com.google.firebase.**

############################################
# Kotlinx Serialization (this is the big one)
############################################

# Keep runtime serialization core
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Keep generated serializers + companion serializer() accessors
-keepclassmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class **$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# If you use @Serializable with polymorphism / sealed types, this helps:
-keepclassmembers class ** {
    public static **$Companion Companion;
}

############################################
# Hilt / Dagger
############################################

# Hilt also ships rules, but keep the generated components safe.
-dontwarn dagger.hilt.**
-dontwarn javax.inject.**

############################################
# Room
############################################

# Room ships rules too, but keep it safe if you do reflective access anywhere.
-dontwarn androidx.room.**

############################################
# ML Kit (barcode scanning) + Play Services
############################################

# Most is covered by consumer rules. These reduce noise.
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**
-dontwarn com.google.android.play.core.**

############################################
# Media3 (ExoPlayer)
############################################

-dontwarn androidx.media3.**

############################################
# CameraX
############################################

-dontwarn androidx.camera.**

############################################
# JNI / NDK (VERY IMPORTANT if native calls Java/Kotlin by name)
############################################

# Keep names/signatures of anything with native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# If your native code looks up Java methods/fields by reflection or hardcoded names,
# add specific -keep rules for those classes/methods here (more secure than keeping everything).

############################################
# General hardening (safe-ish)
############################################

# Keep enum valueOf / values (rare issues; low cost)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
