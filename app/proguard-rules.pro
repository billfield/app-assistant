# Add project specific ProGuard rules here.

# Hilt
-keep class dagger.hilt.** { *; }
-keepclassmembers class * {
    @dagger.hilt.android.qualifiers.ApplicationContext <fields>;
}

# Moshi
-keep class com.mathcoach.app.** { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonClass interface *
-keep @com.squareup.moshi.JsonClass class **

# OkHttp / OkIO
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Compose
-dontwarn androidx.compose.**
