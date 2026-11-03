# Especificación de Diseño: Notificaciones en Vivo, Soporte Samsung Now Bar y Alertas de Gol On-Device

- **Fecha:** 2026-10-06
- **Estado:** Aprobado
- **Módulo:** Notificaciones en Vivo y Seguimiento de Partidos

## 1. Resumen y Objetivos
Implementar un sistema de notificaciones en vivo y seguimiento de partidos 100% on-device para Ulama, sin intermediación de servidores externos ni Firebase.
El sistema soporta:
1. Notificación en curso permanente en cortina y pantalla de bloqueo con minuto, marcador y estado del juego.
2. Integración nativa con Android 16 Live Updates y Samsung Now Bar en One UI 7 mediante el uso de `setRequestPromotedOngoing` y `setShortCriticalText`.
3. Alertas flotantes prioritarias con sonido y vibración ante eventos de gol.
4. Activación diferida para partidos programados mediante alarmas exactas al silbatazo inicial.
5. Persistencia local de partidos seguidos vinculada al botón existente en el encabezado de detalle.

## 2. Arquitectura de Componentes

### 2.1 Modelo de Datos y Persistencia
- `MatchFollowRepository`: Gestiona la lista de partidos seguidos mediante `SharedPreferences` y `kotlinx.serialization`. Expone un `StateFlow<Set<String>>` con los identificadores de los partidos activos.
- `FollowedMatch`: Estructura inmutable con identificador, liga, nombres de los equipos, marcador, estado y marca de tiempo UTC de inicio.

### 2.2 Despertador y Alarmas
- `MatchAlarmScheduler`: Programa alarmas exactas con `AlarmManager.setExactAndAllowWhileIdle` para la hora precisa del silbatazo inicial en partidos futuros.
- `MatchAlarmReceiver`: `BroadcastReceiver` que captura la alarma al comenzar el partido y arranca `MatchLiveNotificationService`.

### 2.3 Servicio Foreground de Polling
- `MatchLiveNotificationService`: Servicio de tipo `dataSync` que ejecuta un bucle de corrutinas con `SupervisorJob`.
  - Intervalo en vivo: 35 segundos.
  - Intervalo en descanso o medio tiempo: 60 segundos.
  - Detección de gol: Compara el marcador acumulado del ciclo previo contra el actual. Si hay incremento, invoca `GoalAlertManager`.
  - Cierre al silbatazo final: Publica la tarjeta definitiva de tiempo completo, retira la bandera permanente y finaliza con `stopSelf`.

### 2.4 Presentación Visual y Canales
- `MatchNotificationHelper`:
  - Canal `ulama_live_matches`: Importancia media o por defecto, sin sonido y sin vibración repetitiva.
  - Canal `ulama_goal_alerts`: Importancia alta, con sonido del sistema y vibración corta.
  - Compatibilidad Android 16 / One UI 7 Now Bar: Activa `setRequestPromotedOngoing(true)` y `setShortCriticalText` si la versión del sistema operativo lo soporta.

## 3. Plan de Verificación
- Pruebas unitarias de serialización y repositorio de seguimiento (`MatchFollowRepositoryTest`).
- Pruebas unitarias del detector de goles y cambios de estado (`GoalDetectionTest`).
- Verificación de compilación limpia de la suite y del binario de depuración.
