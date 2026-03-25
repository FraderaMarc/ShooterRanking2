package com.fradera.shooterranking

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity legacy.
 *
 * Esta clase existía en la versión antigua del proyecto y dependía de:
 * - databinding
 * - Supabase
 * - layouts antiguos
 *
 * En la versión nueva la app funciona con:
 * - Firebase
 * - paquete com.marcfradera.shooterranking
 * - MainActivity + Fragments
 *
 * Para no romper compatibilidad si AndroidManifest o algún enlace antiguo
 * siguen apuntando aquí, esta activity simplemente redirige a la app real.
 */
class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Redirige a la MainActivity real del proyecto Firebase nuevo.
        startActivity(
            Intent(this, com.marcfradera.shooterranking.MainActivity::class.java)
        )

        // Cerramos esta activity legacy para que no quede en la pila.
        finish()
    }
}