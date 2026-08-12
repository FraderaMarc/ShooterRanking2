# Guía para Data safety — revisar antes de enviar

Esta guía no sustituye la declaración final de Play Console: debes revisarla contra el comportamiento real y las versiones finales de Firebase/AdMob.

La aplicación usa Firebase Authentication y Cloud Firestore para la cuenta y los datos deportivos. El usuario puede introducir correo, username, nombres de jugadores, temporadas, equipos, sesiones y estadísticas. La app incluye Google Mobile Ads/UMP si los anuncios están activados en el build de producción.

Puntos a revisar en Play Console:

- Información personal: correo electrónico y username para gestión de cuenta.
- Contenido generado por el usuario / datos deportivos: nombres de jugadores, equipos, sesiones y estadísticas que el usuario decide introducir.
- Identificadores y datos técnicos que puedan ser tratados por Firebase y por el SDK publicitario.
- Finalidades: funcionalidad de la app, gestión de cuenta, seguridad/prevención de fraude y publicidad cuando se habilite.
- Cifrado en tránsito: Firebase/Google usan HTTPS/TLS; confirma el estado final de todos los endpoints propios si añades alguno.
- Eliminación: disponible dentro de la app y mediante la URL pública de solicitud de eliminación.

Consulta la documentación vigente de cada SDK justo antes de enviar la declaración.
