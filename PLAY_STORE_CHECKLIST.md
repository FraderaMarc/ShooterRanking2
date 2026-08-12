# Checklist de publicación

- [ ] Abrir con JDK 17 y SDK 36.
- [ ] Ejecutar Sync Project with Gradle Files.
- [ ] Compilar `assembleDebug` y ejecutar `lintDebug`.
- [ ] Probar registro, verificación y login.
- [ ] Probar “He olvidado mi contraseña”.
- [ ] Probar eliminación de cuenta con una cuenta de prueba.
- [ ] Desplegar y validar `firestore.rules`.
- [ ] Crear la app/placements reales en AdMob y configurar UMP.
- [ ] Definir IDs de AdMob de release fuera del repositorio.
- [ ] Registrar Play Integrity en Firebase App Check; habilitar enforcement solo tras validar.
- [ ] Alojar `docs/privacy.html` y `docs/delete-account.html` en URLs públicas HTTPS.
- [ ] Añadir URL de privacidad y URL de eliminación en Play Console.
- [ ] Completar Data safety siguiendo `PLAY_CONSOLE_DATA_SAFETY.md` y revisarlo contra los SDK finalmente incluidos.
- [ ] Crear upload key `.jks` fuera de Git y configurar Play App Signing.
- [ ] Generar `bundleRelease` y revisar el `.aab` en Android Studio > Analyze APK/App Bundle.
- [ ] Confirmar compatibilidad de páginas de memoria de 16 KB en Play Console/pre-launch report.
- [ ] Subir primero a Internal testing y revisar pre-launch report.
- [ ] Si la cuenta de desarrollador está sujeta al requisito de closed testing, completarlo antes de producción.
