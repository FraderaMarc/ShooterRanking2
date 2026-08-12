# Gradle wrapper bootstrap

El `gradle-wrapper.jar` incluido en esta copia es un bootstrap temporal porque el conector de GitHub no permite materializar íntegramente el JAR binario original. Descarga la distribución oficial indicada en `gradle-wrapper.properties` (8.11.1) y ejecuta Gradle.

Tras la primera ejecución correcta, sustitúyelo por el wrapper oficial:

```bash
./gradlew wrapper --gradle-version 8.11.1
```

En Windows:

```bat
gradlew.bat wrapper --gradle-version 8.11.1
```
