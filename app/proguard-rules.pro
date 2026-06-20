# ProGuard rules for Premium Portal
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class org.jetbrains.kotlin.** { *; }

# Keep our app classes
-keep class com.portalapp.** { *; }
