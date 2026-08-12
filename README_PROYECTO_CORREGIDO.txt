ShooterRanking - HOME + MODO JUGADOR

Base de trabajo:
- GitHub FraderaMarc/ShooterRanking
- Commit base: 9025c30da9fb2fdbdf2172b7d1d611460825c9bd (Correccions taule)

NUEVO FLUJO
------------
1. Usuario autenticado/verificado -> HOME.
2. HOME: Entrenador -> Temporadas; Jugador -> perfil propio.
3. Primera entrada como Jugador: Nombre, Dorsal, Posición y Base/FIBA.
4. El perfil se guarda en la colección existente "jugadors" con un id_equip reservado,
   por lo que no aparece en ningún equipo del modo Entrenador.
5. Modo Jugador: pestañas Estadísticas / Mapa.
6. Estadísticas reutiliza PlayerStatsScreen: gráfico, tabla, nombres de sesión y PDF.
7. Mapa reutiliza ShotMapScreen: sesiones, fechas, nombres, guardado y Base/FIBA.

SE CONSERVA
-----------
- Flujo Entrenador completo.
- ES / CA / EN / FR y español por defecto.
- Base / FIBA (FIBA usa internamente "Amateur").
- nom_sessio personalizado.
- Nombres largos de sesión en una línea con elipsis.
- PDFs existentes.
- Guardado explícito de sesiones y fechas.
- Configuración y cierre de sesión.

ABRIR
-----
1. Android Studio > File > Open.
2. Seleccionar esta carpeta.
3. Sync Project with Gradle Files.
4. Build > Clean Project.
5. Build > Rebuild Project.

NOTA
----
No se ha ejecutado un build Android completo en este entorno porque no dispone de
Android SDK/Gradle. Sí se han validado XML, navegación, recursos, idiomas y sintaxis
estructural Kotlin.

El GitHub contiene algunos binarios auxiliares (logo.png, mipmaps y gradle-wrapper.jar)
que el conector no permite materializar aquí. Se conserva un logo.xml funcional con
el mismo recurso R.drawable.logo; el código fuente y configuración necesarios para
la funcionalidad solicitada están incluidos.
