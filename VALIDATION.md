# Validación estática de la copia

Fecha: 11 de agosto de 2026

- [x] **XML parse** — 25 XML files; errors=0
- [x] **String resources** — 198 referenced / 198 defined; missing=[]
- [x] **Drawable resources** — 4 referenced; missing=[]
- [x] **Firebase package** — com.marcfradera.shooterranking
- [x] **SDK 36** — compileSdk/targetSdk
- [x] **No signing key included** — No .jks/.keystore files
- [x] **Firestore rules included** — firestore.rules
- [x] **Privacy/delete pages included** — docs/privacy.html + docs/delete-account.html
- [x] **Kotlin delimiter sanity** — balanced

## Limitación de esta validación

El entorno usado para preparar esta copia no dispone de Android SDK y no tiene acceso DNS saliente para descargar Gradle/Maven, por lo que no fue posible ejecutar `assembleDebug`, `lintDebug` o `bundleRelease` aquí. El bootstrap de Gradle sí se ejecutó hasta el intento de descarga de Gradle 8.11.1, confirmando que el JAR/clase de arranque es invocable.

La primera acción en tu equipo debe ser abrir el proyecto con Android Studio + JDK 17 + SDK 36 y ejecutar las tareas indicadas en `README.md`.
