# Especificación de Diseño: Selección de Equipos Favoritos y Vista Agregada Multiliga

- **Fecha:** 2026-10-07
- **Estado:** En Revisión
- **Módulo:** Favoritos, Selector Jerárquico y Agregación Multiliga

---

## 1. Resumen y Objetivos

Permitir al usuario elegir sus clubes favoritos de entre las competiciones soportadas en Ulama (Liga MX, Premier League, LaLiga y UEFA Champions League) y visualizarlos en una competición virtual dedicada denominada **Favoritos**.

### Objetivos Clave:
1. **Flujo de selección jerárquico y escalable:** Diseñar `FavoriteTeamsBottomSheet` con el mismo lenguaje visual de `LeaguePickerBottomSheet`. Nivel 1 muestra las ligas con su cantidad de clubes favoritos seleccionados. Al pulsar una liga, se desglosan sus clubes con escudos, nombres y estado activo.
2. **Identidad universal de clubes:** Cada club se identifica por su `teamId` único de ESPN. Marcar un club en su liga local (ej. FC Barcelona en LaLiga) lo reconoce automáticamente en torneos internacionales (Champions League).
3. **Agregación multiliga en la vista de Favoritos:** La competición Favoritos consolida los partidos de todas las ligas para el filtro temporal activo (Pasados, Hoy, Próximos) y filtra los encuentros donde participe al menos uno de los clubes favoritos.
4. **Comportamiento de la tabla de posiciones:**
   - En la vista principal de la liga Favoritos no se muestra tabla general, ya que no existe una tabla combinada entre distintas ligas.
   - En la hoja modal de detalle de partido (`MatchDetailBottomSheet`), sí se muestra la tabla de posiciones correspondiente a la liga a la que pertenece dicho encuentro.
5. **Persistencia local ligera (Filosofía Ponytail):** Guardado inmediato del conjunto de identificadores en `SharedPreferences` sin bases de datos adicionales.

---

## 2. Arquitectura de Componentes

### 2.1 Modelo y Capa de Datos
- **`FavoriteTeamsRepository` / `FavoriteTeamsRepositoryImpl`:**
  - Archivo de preferencias: `ulama_user_preferences`.
  - Clave: `favorite_team_ids` (`Set<String>`).
  - Expone `favoriteTeamIds: StateFlow<Set<String>>`.
  - Métodos: `toggleFavorite(teamId: String)` y `isFavorite(teamId: String): Boolean`.
- **Extensión de `League`:**
  - Incorporación de `FAVORITES` en `core/model/League.kt` con código `"favorites"` y título centralizado en `R.string.league_favorites`.

### 2.2 Agregación en `MatchesViewModel`
- Cuando `selectedLeague == League.FAVORITES`:
  - Si `favoriteTeamIds` está vacío, el estado refleja una vista vacía invitando a configurar favoritos.
  - Si hay favoritos seleccionados, consulta en paralelo los calendarios/partidos de las ligas soportadas para el filtro temporal activo.
  - Filtra los partidos con la condición: `match.homeTeam.id in favoriteIds || match.awayTeam.id in favoriteIds`.
  - Ordena los partidos cronológicamente.
  - Asigna `standings = emptyList()` para la vista principal.
- Cada partido conserva la información de su liga original (`match.league`) para resolver escudos y tablas de posiciones en el detalle.

### 2.3 Componentes de Interfaz de Usuario

#### `LeaguePickerBottomSheet`
- Agrega la opción circular de Favoritos junto a las demás ligas con icono de estrella y anillo de selección.
- Muestra insignia con el número total de clubes favoritos.
- Incluye botón secundario de texto o acceso directo para gestionar clubes.
- Si el usuario selecciona Favoritos sin tener ningún club guardado, abre inmediatamente `FavoriteTeamsBottomSheet`.

#### `FavoriteTeamsBottomSheet` (Nuevo)
- **Nivel 1 (Ligas):** Cuadrícula de ligas en círculos idéntica a `LeaguePickerBottomSheet`. Cada círculo muestra el escudo de la competición y un badge numérico con los clubes elegidos de esa liga.
- **Nivel 2 (Clubes de la Liga):** Transición fluida con botón de retroceso (←) en la cabecera. Despliega los clubes de la competición seleccionada obtenidos de la caché en disco de las tablas de posiciones.
- Cada elemento de club respeta la regla **Clip Before Click** con forma circular/redondeada, escudo oficial, nombre y distintivo de selección con color primario.
- Los cambios se persisten de forma instantánea al pulsar.

#### `MatchCard`
- En la vista de Favoritos, muestra un distintivo compacto con el logotipo o código de la liga correspondiente para distinguir el torneo del partido.

#### `MatchDetailBottomSheet`
- Al abrir el detalle de un partido desde la vista Favoritos, el `ViewModel` suministra la tabla de posiciones de la liga correspondiente a ese encuentro (`match.league`), permitiendo consultar la clasificación del torneo en el modal de detalle.

---

## 3. Plan de Verificación

1. **Pruebas de Persistencia:** Verificar que `FavoriteTeamsRepository` almacene y recupere el conjunto de identificadores tras reiniciar la aplicación.
2. **Pruebas de Filtrado Multiliga:** Validar que al marcar un club (ej. Arsenal) se desplieguen sus partidos tanto de Premier League como de Champions League en la vista Favoritos.
3. **Pruebas de Interfaz de Usuario:**
   - Comprobar que en la vista Favoritos la pantalla principal oculte la tabla general.
   - Comprobar que al pulsar un partido en Favoritos, el detalle muestre la tabla de la liga a la que pertenece ese juego.
   - Navegación bidireccional limpia en `FavoriteTeamsBottomSheet` (Ligas ↔ Clubes).
4. **Verificación de Compilación:** Ejecución de `./gradlew.bat compileDebugKotlin` asegurando cero advertencias y cero errores.
