# Vendano ProGuard rules

# Keep Cardano client library (bloxbean)
-keep class com.bloxbean.cardano.** { *; }
-dontwarn com.bloxbean.cardano.**
-keepclassmembers class com.bloxbean.cardano.** { *; }

# Keep Gson models
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Room entities
-keep class net.vendano.vendano_android.data.local.db.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Vendano domain models (Gson serialization)
-keep class net.vendano.vendano_android.domain.model.** { *; }
-keep class net.vendano.vendano_android.data.remote.dto.** { *; }

# BouncyCastle (used by cardano-client-lib)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# CBOR library
-keep class co.nstant.in.cbor.** { *; }
-dontwarn co.nstant.in.cbor.**

# Jackson (used by cardano-client-lib internally)
-dontwarn com.fasterxml.jackson.**
-keep class com.fasterxml.jackson.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ZXing (QR code generation)
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Coil image loading
-dontwarn okhttp3.internal.platform.**
