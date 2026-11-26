# ==============================================================================
# Reglas de Optimizacion R8 y ProGuard para Ulama
# ==============================================================================

# Atributos esenciales para anotaciones y tipos genericos de Retrofit / Serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, SourceFile, LineNumberTable

# ------------------------------------------------------------------------------
# 1. Kotlinx Serialization y DTOs de ESPN
# ------------------------------------------------------------------------------
# Conservar serializadores estaticos generados permitiendo inlining y eliminacion de metodos DTO sin uso
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class **$$serializer {
    *;
}
-keepclassmembers,allowobfuscation class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,allowobfuscation,allowshrinking @kotlinx.serialization.Serializable class *
-keepclassmembers,allowobfuscation @kotlinx.serialization.Serializable class * {
    static *** Companion;
    <fields>;
}

# ------------------------------------------------------------------------------
# 2. Retrofit 3 & OkHttp 5
# ------------------------------------------------------------------------------
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# Conservar metodos anotados en la interfaz del servicio HTTP
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep interface com.ixeken.ulama.data.espn.EspnApiService { *; }

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
