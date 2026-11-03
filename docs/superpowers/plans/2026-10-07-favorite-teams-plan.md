# Plan de Implementación: Equipos Favoritos y Agregación Multiliga

- **Fecha:** 2026-10-07
- **Especificación de Referencia:** `docs/superpowers/specs/2026-10-07-favorite-teams-design.md`
- **Directrices Clave:** Material 3 Expressive (`material-3-android`), Rendimiento en Compose (`jetpack-compose-performance`) y Filosofía Minimalista (`ponytail`).

---

## 1. Desglose de Tareas de Implementación

### Tarea 1: Modelos de Dominio y Recursos de Texto
- **Objetivo:** Definir la competición virtual `League.FAVORITES`, asegurar la propiedad `league` en `Match` y centralizar todas las cadenas en `strings.xml`.
- **Archivos a modificar:**
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/java/com/ixeken/ulama/core/model/League.kt`
  - `app/src/main/java/com/ixeken/ulama/core/model/Match.kt`
  - `app/src/main/java/com/ixeken/ulama/data/repository/MatchRepositoryImpl.kt`
- **Criterios Ponytail y M3:**
  - Reutilizar el enum inmutable `League` sin crear enums duplicados de UI.
  - Asignar `League.FAVORITES` con código `"favorites"` y `tableCutoffs = emptyList()`.
  - Cero cadenas hardcodeadas: todo texto referenciado vía `@StringRes`.

### Tarea 2: Persistencia Ligera con `FavoriteTeamsRepository`
- **Objetivo:** Almacenar de forma inmediata el conjunto de identificadores `teamId` seleccionados en el archivo local de preferencias.
- **Archivos a crear/modificar:**
  - `app/src/main/java/com/ixeken/ulama/data/repository/FavoriteTeamsRepository.kt`
  - `app/src/main/java/com/ixeken/ulama/data/repository/FavoriteTeamsRepositoryImpl.kt`
  - `app/src/test/java/com/ixeken/ulama/data/repository/FavoriteTeamsRepositoryTest.kt`
- **Criterios Ponytail y Rendimiento:**
  - Almacenar en `ulama_user_preferences` bajo la clave `favorite_team_ids`.
  - Exponer un `StateFlow<Set<String>>` reactivo y síncrono en memoria.
  - Sin tablas Room ni abstracciones especulativas.
  - Prueba unitaria ligera con un doble de SharedPreferences o entorno de prueba básico.

### Tarea 3: Agregación Multiliga y Coordinación en `MatchesViewModel`
- **Objetivo:** Coordinar la consulta en paralelo de las cuatro ligas cuando la competición activa sea `League.FAVORITES`, filtrar encuentros por clubes favoritos y preparar los datos de los clubes.
- **Archivos a modificar:**
  - `app/src/main/java/com/ixeken/ulama/ui/matches/MatchesUiState.kt`
  - `app/src/main/java/com/ixeken/ulama/ui/matches/MatchesViewModel.kt`
- **Criterios de Rendimiento en Compose y Ponytail:**
  - Anotación `@Immutable` preservada en `MatchesUiState`.
  - Reutilización de llamadas existentes: los clubes disponibles por liga se obtienen de la caché en disco de `getStandings` ya construida.
  - En la vista `FAVORITES`, asignar `standings = emptyList()` para suprimir la tabla en la pantalla base.
  - Al pulsar un partido (`selectMatch`), resolver la tabla de posiciones específica de `match.league` para abastecer el detalle.

### Tarea 4: Componente Jerárquico `FavoriteTeamsBottomSheet`
- **Objetivo:** Crear la hoja modal de selección de clubes con navegación de dos niveles: Ligas ↔ Clubes.
- **Archivos a crear:**
  - `app/src/main/java/com/ixeken/ulama/ui/components/FavoriteTeamsBottomSheet.kt`
- **Criterios Material 3 y Rendimiento:**
  - Nivel 1: Cuadrícula circular de ligas con botones de 76dp e insignia de favoritos elegidos.
  - Nivel 2: Cuadrícula de clubes con botón de retroceso (←) en la cabecera.
  - Transición fluida con `AnimatedContent` basada en un simple estado `selectedLeagueForTeams: League?`.
  - Regla estricta **Clip Before Click**: `Modifier.clip(CircleShape)` antes de `Modifier.clickable`.
  - Área táctil mínima garantizada de 48dp.
  - Parámetros estables, omisión de `Modifier.composed` y claves persistentes en grids.

### Tarea 5: Integración Visual en `LeaguePickerBottomSheet`, `MatchCard` y `MainActivity`
- **Objetivo:** Conectar el selector de favoritos en la barra superior/hoja de ligas y distinguir torneos en las tarjetas de la vista agregada.
- **Archivos a modificar:**
  - `app/src/main/java/com/ixeken/ulama/ui/components/LeaguePickerBottomSheet.kt`
  - `app/src/main/java/com/ixeken/ulama/ui/components/MatchCard.kt`
  - `app/src/main/java/com/ixeken/ulama/MainActivity.kt`
- **Criterios Visuales y M3:**
  - Círculo de `League.FAVORITES` con icono de estrella y badge del conteo total.
  - Botón de texto para abrir directamente `FavoriteTeamsBottomSheet`.
  - `MatchCard` muestra un distintivo compacto con el logo de la liga cuando se encuentra en la vista Favoritos.
  - `MainActivity` despliega `FavoriteTeamsBottomSheet` de forma reactiva.

### Tarea 6: Pruebas y Verificación de Compilación
- **Objetivo:** Validar que todo el código compile sin advertencias ni errores y que la suite de pruebas pase con éxito.
- **Comandos de ejecución:**
  - `.\gradlew.bat testDebugUnitTest`
  - `.\gradlew.bat compileDebugKotlin`

---

## 2. Orden Secuencial de Ejecución

1. Ejecutar Tarea 1: Modelos y Recursos.
2. Ejecutar Tarea 2: Repositorio y Pruebas Unitarias.
3. Ejecutar Tarea 3: ViewModel y Lógica de Agregación.
4. Ejecutar Tarea 4: Componente `FavoriteTeamsBottomSheet`.
5. Ejecutar Tarea 5: Integración en Componentes y `MainActivity`.
6. Ejecutar Tarea 6: Verificación de Pruebas y Compilación.
