package com.ixeken.ulama.core.util

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import com.ixeken.ulama.core.model.AppLanguage

/**
 * Gestor utilitario para la aplicacion y consulta de idiomas en tiempo de ejecucion.
 * Aprovecha la API nativa LocaleManager de Android 13+ (minSdk 35) sin dependencias pesadas.
 */
object LocaleHelper {

    /**
     * Aplica la preferencia de idioma a nivel de aplicacion en el sistema operativo.
     * Al actualizar applicationLocales, el sistema operativo recrea la actividad principal
     * con la nueva configuracion regional de recursos.
     *
     * @param context Contexto de la aplicacion o actividad.
     * @param language Preferencia de idioma seleccionada por el usuario.
     */
    fun applyLanguage(context: Context, language: AppLanguage) {
        val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
        val localeList = when (language) {
            AppLanguage.SYSTEM -> LocaleList.getEmptyLocaleList()
            AppLanguage.SPANISH -> LocaleList.forLanguageTags("es")
            AppLanguage.ENGLISH -> LocaleList.forLanguageTags("en")
        }
        localeManager.applicationLocales = localeList
    }

    /**
     * Determina el idioma configurado actualmente en la aplicacion consultando LocaleManager.
     * Si no se ha forzado ningun idioma manual, retorna AppLanguage.SYSTEM.
     *
     * @param context Contexto del sistema.
     * @return El idioma activo a nivel de aplicacion.
     */
    fun getCurrentLanguage(context: Context): AppLanguage {
        val localeManager = context.getSystemService(LocaleManager::class.java) ?: return AppLanguage.SYSTEM
        val locales = localeManager.applicationLocales
        if (locales.isEmpty) return AppLanguage.SYSTEM

        val primaryLanguage = locales[0].language.lowercase()
        return when {
            primaryLanguage.startsWith("es") -> AppLanguage.SPANISH
            primaryLanguage.startsWith("en") -> AppLanguage.ENGLISH
            else -> AppLanguage.SYSTEM
        }
    }
}
