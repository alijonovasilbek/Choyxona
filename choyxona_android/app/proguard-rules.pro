# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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

# Keep Retrofit models
-keep class uz.choyxona.app.data.model.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*

# ==================================================================
# R8 full mode (default since AGP 8) strips the generic signature of
# every class it is not told to keep. Retrofit reads those generics
# at runtime to work out what a service method returns, so without the
# rules below every API call fails on a release build while the debug
# build (no R8) works fine.
#
# Retrofit 2.9.0 ships META-INF/proguard/retrofit2.pro, but that file
# predates full mode and is missing exactly these three keeps. They
# were added upstream in Retrofit 2.10.
# ==================================================================

# Suspend service methods carry the response type in
# Continuation<? super Response<T>>. Continuation was being renamed and
# its generic erased, leaving Retrofit with a raw type.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep the service interfaces themselves (and their signatures).
-keep,allowobfuscation interface uz.choyxona.app.data.api.** { *; }

# Gson resolves types through TypeToken, whose generics full mode also strips.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Signature needs InnerClasses + EnclosingMethod to be usable.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

# Readable stack traces from a release build.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
