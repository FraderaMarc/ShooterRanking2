ShooterRanking2

Esta versión es una migración híbrida creada para acercar el proyecto original a una estructura Activity + Fragments + Navigation + SharedViewModel/LiveData.

Qué se ha hecho:
- MainActivity clásica con NavHostFragment.
- navigation graph XML.
- SharedViewModel con LiveData para ids/nombres seleccionados.
- Temporades, Equips y Ranking con RecyclerView.
- Pantallas complejas (stats, mapa de tiro y auth) conservadas en Compose dentro de Fragments para reducir el riesgo de romper diseño o lógica.
- Se ha reutilizado la lógica real de FirebaseProvider, ShooterRepository y modelos del proyecto original.

Limitación importante:
- El proyecto original es Compose. Una migración 100% a Fragments + RecyclerView manteniendo comportamiento y aspecto exactos requería una refactorización mayor y validación en Android Studio.
- Esta entrega está pensada como base de migración segura y entendible, no como garantía de equivalencia total pixel-perfect.
