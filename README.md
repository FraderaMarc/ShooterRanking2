# Shooter Ranking — Play Store readiness build

Esta copia parte de `FraderaMarc/ShooterRanking` y prepara el proyecto para publicación en Google Play manteniendo la lógica de Firebase, temporadas, equipos, jugadores, sesiones, estadísticas y mapas de tiro.

## Cambios principales

- `compileSdk` y `targetSdk` 36.
- Android Gradle Plugin 8.10.1 y Gradle 8.11.1.
- JDK 17 como nivel de compilación.
- Google Mobile Ads SDK 25.4.0 y UMP 4.0.0.
- Consentimiento UMP antes de solicitar anuncios.
- IDs de anuncios separados por `debug`/`release` y fuera del código fuente para producción.
- Recuperación de contraseña con Firebase Authentication.
- Eliminación de cuenta dentro de la app con reautenticación y borrado de datos asociados.
- Firestore Security Rules de propietario por UID.
- Integración opcional de Firebase App Check + Play Integrity.
- Recursos de firma ignorados por Git.
- Política de privacidad y página de solicitud de eliminación preparadas para alojar en web.

## Requisitos locales

1. Android Studio reciente con Android SDK 36 instalado.
2. JDK 17 configurado como Gradle JDK.
3. El proyecto Firebase `shooterranking-2cfbb` debe seguir disponible.
4. Antes de producción, despliega `firestore.rules` en el proyecto Firebase y prueba con una cuenta de test.

## AdMob de producción

Los anuncios de `debug` usan IDs de prueba de Google. En `release`, los anuncios solo se activan si defines ambos valores:

```properties
SHOOTER_ADMOB_APP_ID=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
SHOOTER_ADMOB_BANNER_ID=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
```

Puedes copiarlos a un archivo local no versionado a partir de `playstore.properties.example` o proporcionarlos como propiedades/variables de entorno según tu flujo de build. No subas secretos ni claves de firma a GitHub.

También debes configurar en AdMob > Privacidad y mensajes el mensaje europeo/UMP correspondiente a las regiones donde distribuyas la app.

## Firebase App Check

No está forzado por defecto para evitar bloquear una instalación durante las pruebas. Después de registrar la app Android con Play Integrity en Firebase App Check, activa:

```properties
SHOOTER_APP_CHECK_ENABLED=true
```

Primero valida telemetría y después habilita enforcement en Firebase.

## Eliminación de cuenta

Desde el icono de configuración de las pantallas Compose el usuario puede:

- restablecer la contraseña;
- abrir política/condiciones;
- revisar opciones de privacidad de UMP cuando estén disponibles;
- eliminar su cuenta;
- cerrar sesión.

La eliminación solicita la contraseña actual, reautentica al usuario y elimina `sessions`, `jugadors`, `equips`, `temporades`, reserva de username, perfil `users/{uid}` y finalmente el usuario de Firebase Authentication.

## Validación antes de subir a Play

En tu equipo ejecuta:

```bash
./gradlew clean assembleDebug
./gradlew lintDebug
./gradlew bundleRelease
```

Después instala el debug y prueba: registro/verificación, login, recuperación de contraseña, crear/editar/borrar temporadas/equipos/jugadores, sesiones y 11 zonas, estadísticas, mapas, exportaciones, idiomas, logout y eliminación de cuenta.

El archivo `.aab` de release deberá firmarse con tu propia upload key y usar Play App Signing.
