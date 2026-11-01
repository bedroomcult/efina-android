# Add project specific ProGuard rules here.

# Keep Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson was replaced by kotlinx.serialization, so those rules are dead.
# The project no longer ships com.google.gson.

# kotlinx.serialization: the generated serializers are looked up reflectively.
# Without these, every @Serializable DTO fails at runtime in release builds with
# "Serializer for class ... is not found".
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.efina.app.data.remote.dto.**$$serializer { *; }
-dontwarn kotlinx.serialization.**

# Keep data classes
-keep class com.efina.app.data.remote.dto.** { *; }
-keep class com.efina.app.domain.model.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Compose
-keep class androidx.compose.** { *; }

# Tink (pulled in by androidx.security:security-crypto for EncryptedSharedPreferences)
# references errorprone annotations that are compile-time only. R8 fails the
# release build on the missing classes unless told to ignore them.
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.crypto.tink.**
-keep class com.google.crypto.tink.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
