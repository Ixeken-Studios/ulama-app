# Reglas de ProGuard para Ulama App

# Conservar intactas las clases de transferencia de datos (DTOs) que se serializan desde la API de ESPN/openfootball
-keep class com.ixeken.worldcupinfo.data.remote.dto.** { *; }

# Conservar intactas las entidades de Room para asegurar mapeo correcto de base de datos
-keep class com.ixeken.worldcupinfo.data.database.entities.** { *; }

# Conservar intactas las clases del modelo de dominio
-keep class com.ixeken.worldcupinfo.domain.model.** { *; }

# Preservar firmas genéricas para evitar crashes de TypeToken con Gson
-keepattributes Signature, InnerClasses, EnclosingMethod

# Prevenir advertencias y conservar clases de Gson
-dontwarn com.google.gson.**
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
