# Especificacion de Diseno y Arquitectura: Ulama

- **Fecha:** 2026-10-05
- **Estado:** Propuesto
- **Autor:** Staff Engineering Pair
- **Plataforma:** Android (Jetpack Compose, Material 3 Expressive, Kotlin 2.2.10)

---

## 1. Contexto y Objetivos

Ulama es una aplicacion de futbol moderna, minimalista y de alto rendimiento disenada para seguir resultados, marcadores en vivo, proximos encuentros y clasificaciones de cuatro ligas principales:
- Liga MX (mex.1)
- Premier League (eng.1)
- LaLiga (esp.1)
- UEFA Champions League (uefa.champions)

### Metas Principales
- Consumo de datos completamente gratuito sin registros ni llaves de acceso mediante la API publica de ESPN.
- Filosofia de diseno minimalista basada en la simpleza de Apple Sports.
- Rendimiento de grado industrial bajo las directrices de Jetpack Compose Performance.
- Soporte hibrido sin conexion mediante cache HTTP transparente en disco.
- Sistema de tokens de diseno de 3 niveles con la paleta de colores oficial de Ulama.

---

## 2. Arquitectura del Sistema y Modularidad

El sistema sigue una arquitectura por capas estrictamente desacoplada:

```text
app/src/main/java/com/ixeken/ulama/
├── core/
│   ├── designsystem/          # Tokens primitivos, semanticos y componentes M3 Expressive
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   ├── Type.kt
│   │   └── tokens/            # Component tokens (MatchCardTokens, etc.)
│   ├── network/               # Cliente OkHttp, Retrofit, CacheInterceptor
│   │   ├── ApiClient.kt
│   │   └── CacheInterceptor.kt
│   └── model/                 # Modelos de dominio inmutables (@Immutable)
│       ├── League.kt
│       ├── Match.kt
│       ├── Team.kt
│       └── Standings.kt
├── data/
│   ├── espn/                  # DTOs crudos y servicio Retrofit de ESPN
│   │   ├── dto/
│   │   ├── EspnApiService.kt
│   │   └── EspnMatchRepositoryImpl.kt
│   └── repository/            # Contrato del repositorio de dominio
│       └── MatchRepository.kt
└── feature/
    ├── matches/               # Pantalla principal con selector de ligas y filtros
    │   ├── MatchesScreen.kt
    │   ├── MatchesViewModel.kt
    │   ├── MatchesUiState.kt
    │   └── components/
    │       ├── MatchCard.kt
    │       ├── LeagueSelectorPill.kt
    │       ├── TemporalFilterTabs.kt
    │       └── StandingsTable.kt
    └── matchdetail/           # Hoja modal deslizable con estadísticas y alineación
        ├── MatchDetailBottomSheet.kt
        └── components/
            ├── StatComparisonBar.kt
            └── PitchLineupView.kt
```

### Reglas de Desacoplamiento
1. Los DTOs de ESPN no se exponen fuera del paquete `data/espn`.
2. La interfaz de usuario consume unicamente modelos inmutables de `core/model`.
3. Cualquier cambio en la estructura de los JSON de ESPN solo requiere adaptar el mapper del repositorio.

---

## 3. Fuentes de Datos y Estrategia de Red

### 3.1 Endpoints de ESPN
- **Marcadores y Calendario:**
  `GET https://site.api.espn.com/apis/site/v2/sports/soccer/{league}/scoreboard?dates={YYYYMMDD}`
  El campo `leagues[0].calendar` proporciona todas las fechas con partidos de la temporada.
- **Tabla de Posiciones:**
  `GET https://site.api.espn.com/apis/v2/sports/soccer/{league}/standings`
- **Detalle de Partido (Estadisticas y Alineacion):**
  `GET https://site.api.espn.com/apis/site/v2/sports/soccer/{league}/summary?event={eventId}`

### 3.2 Cache HTTP y Funcionamiento Hibrido Offline
- OkHttp configurado con un directorio de cache en disco de 10 MB en `context.cacheDir`.
- `CacheInterceptor` inyecta politicas HTTP:
  - **Conexion activa:** `max-age = 30` para refresco regular de partidos en vivo.
  - **Sin conexion:** `only-if-cached` con `max-stale = 60 * 60 * 24 * 7` (7 dias de tolerancia).
- Resultado: Apertura instantanea sin conexion mostrando los ultimos datos sincronizados sin requerir base de datos SQLite.

---

## 4. Diseno Visual y Sistema de Tokens Material 3 Expressive

### 4.1 Jerarquia de Tokens en 3 Niveles
1. **Tokens Primitivos:** Paleta hex oficial de Ulama:
   - Verde Oscuro: `#386641`
   - Verde: `#6A994E`
   - Verde Claro: `#A7C957`
   - Rojo: `#BC4749`
   - Rojo Claro: `#FB6B6E`
   - Blanco Vainilla: `#F2E8CF`
   - Blanco Vainilla Claro: `#F9F2E1`
   - Negro: `#1F1F1F`
2. **Tokens Semanticos:** Mapeo de `MaterialTheme.colorScheme`:
   - `primary`: `#386641`
   - `secondary`: `#6A994E`
   - `tertiary`: `#A7C957`
   - `error`: `#BC4749` (usado para badge Live y tarjetas rojas)
   - `surface`: `#F2E8CF` en modo claro y tonalidad oscura balanceada en modo noche
   - `onSurface`: `#1F1F1F` en modo claro
3. **Tokens de Componentes:**
   - `MatchCardTokens`: Contenedor tonal `surfaceContainerLow`, esquinas redondeadas de 16dp, padding interno de 12dp.
   - `LeaguePillTokens`: Contenedor redondeado de 24dp con borde suave `outlineVariant`.
   - `StatBarTokens`: Altura de 6dp con radio de curvatura total y color comparativo por equipo.

### 4.2 Politica de Iconos
- Descartada la dependencia pesada `material-icons-extended`.
- Se incorporan unicamente Vector Drawables puntuales en `res/drawable`:
  - `ic_star.xml` y `ic_star_filled.xml`
  - `ic_chevron_right.xml`
  - `ic_sports_soccer.xml`

---

## 5. Experiencia de Usuario Inspirada en Apple Sports

### 5.1 Pantalla Principal
1. **Cabecera Superior:**
   - Titulo Ulama a la izquierda.
   - Selector en formato pastilla a la derecha para alternar entre las 4 ligas con icono oficial.
2. **Filtro Temporal de 3 Estados:**
   - **Pasados:** Partidos de la jornada anterior o ultimos 7 dias.
   - **Hoy:** Partidos de la fecha actual con marcadores en tiempo real.
   - **Proximos:** Partidos programados para los siguientes 7 dias o siguiente jornada.
3. **Comportamiento ante Jornada Vacia:**
   - Si no existen eventos en la pestana Hoy, la pantalla muestra un contenedor de estado y debajo inserta automaticamente la tabla general de posiciones de la liga.
4. **Tarjeta de Partido:**
   - Equipo local a la izquierda: escudo de 28dp, nombre legible, registro o goles.
   - Eje central: reloj en vivo con badge rojo si esta activo, o estado Halftime / Final, o la hora prevista para juegos futuros.
   - Equipo visitante a la derecha: goles o registro, escudo de 28dp, nombre.

### 5.2 Hoja Modal de Detalle (BottomSheet)
Se despliega al seleccionar cualquier tarjeta con 3 pestanas:
1. **Stats:** Barras comparativas de posesion, tiros, tiros al arco, tiros de esquina, pases y faltas.
2. **Play-By-Play / Lineup:** Pizarra tactica con formacion en cancha (ej. 4-2-3-1), dorsales y banca de suplentes.
3. **Table:** Tabla de posiciones completa de la liga.

---

## 6. Rendimiento y Optimizaciones de Jetpack Compose

Siguiendo las directrices de `jetpack-compose-performance`:
1. **Estabilidad del Compilador:**
   - Todas las clases de estado de UI y modelos de dominio llevan la anotacion `@Immutable`.
   - Listas envueltas en colecciones estables o inmutables para permitir Strong Skipping completo.
2. **Control de Ciclo de Vida y Polling:**
   - `collectAsStateWithLifecycle` con `Lifecycle.State.STARTED` para el estado de la UI.
   - Las corrutinas de polling se ejecutan unicamente cuando:
     - La aplicacion se encuentra en primer plano.
     - La pestana activa es Hoy.
     - Al menos un partido se encuentra en estado en juego.
   - Al pasar a segundo plano, el polling se suspende al instante. Consumo de bateria en segundo plano: 0%.
3. **Eficiencia en Listas Perezosas:**
   - `key = { it.id }` explicito y unico en `items` de `LazyColumn`.
   - `contentType` diferenciado para cabeceras de fecha, tarjetas de partido y filas de tabla.
4. **Carga de Imagenes:**
   - Coil 3 con cache en memoria y disco para los escudos, previniendo parpadeos durante recomposiciones.

---

## 7. Plan de Verificacion y Calidad

- **Pruebas Unitarias de Red y Mappers:** Validacion del parseo de respuestas complejas de ESPN y su transformacion a modelos inmutables.
- **Pruebas de Cache:** Validar mediante mocks de OkHttp que ante cortes de red el cliente entrega datos en cache sin lanzar excepcion.
- **Compilacion Continua:** Ejecutar `./gradlew compileDebugKotlin` tras cada tarea.
