# ==============================================================================
# Reglas de Optimizacion R8 y ProGuard para Ulama
# ==============================================================================

# Atributos esenciales para reflexion, anotaciones y tipos genericos
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, SourceFile, LineNumberTable

# ------------------------------------------------------------------------------
# 1. Kotlinx Serialization y DTOs de ESPN
# ------------------------------------------------------------------------------
# Conservar serializadores generados y miembros companeros
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class **$$serializer {
    *;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Conservar nombres de campos en clases serializables sin necesidad de @SerialName
-keepclassmembers @kotlinx.serialization.Serializable class * {
    <fields>;
}

# Conservar explicitamente las clases de transferencia de datos (DTOs) y modelos persistidos
-keep class com.ixeken.ulama.data.espn.model.** { *; }
-keep class com.ixeken.ulama.data.update.** { *; }

# ------------------------------------------------------------------------------
# 2. Retrofit 2 & OkHttp
# ------------------------------------------------------------------------------
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Conservar metodos anotados en la interfaz del servicio HTTP
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep class com.ixeken.ulama.data.espn.EspnApiService { *; }

# Base de datos de sufijos publicos de OkHttp
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ------------------------------------------------------------------------------
# 3. Coil 3 Image Loader
# ------------------------------------------------------------------------------
-dontwarn coil3.**

# ------------------------------------------------------------------------------
# 4. Eliminacion de Logs en Produccion (Optimizacion de Bytecode)
# ------------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
