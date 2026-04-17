package com.marcfradera.shooterranking.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseProvider {

    private const val TAG = "FirebaseProvider"

    @Volatile
    private var initialized = false

    private lateinit var firebaseApp: FirebaseApp

    @Volatile
    private var firebaseAuth: FirebaseAuth? = null

    @Volatile
    private var firebaseFirestore: FirebaseFirestore? = null

    fun initialize(context: Context) {
        ensureAppInitialized(context)
    }

    private fun ensureAppInitialized(context: Context? = null): FirebaseApp {
        if (initialized) return firebaseApp

        synchronized(this) {
            if (initialized) return firebaseApp

            val app = if (context != null) {
                val appContext = context.applicationContext

                if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                    FirebaseApp.getInstance()
                } else {
                    FirebaseApp.initializeApp(appContext)
                        ?: throw IllegalStateException(
                            "No s'ha pogut inicialitzar Firebase. Revisa google-services.json."
                        )
                }
            } else {
                FirebaseApp.getInstance()
            }

            firebaseApp = app
            initialized = true

            Log.d(TAG, "Firebase app initialized")
            Log.d(TAG, "projectId=${app.options.projectId}")
            Log.d(TAG, "applicationId=${app.options.applicationId}")
            Log.d(TAG, "storageBucket=${app.options.storageBucket}")
            Log.d(TAG, "gcmSenderId=${app.options.gcmSenderId}")

            return firebaseApp
        }
    }

    val app: FirebaseApp
        get() = ensureAppInitialized()

    val auth: FirebaseAuth
        get() {
            val cached = firebaseAuth
            if (cached != null) return cached

            synchronized(this) {
                if (firebaseAuth == null) {
                    firebaseAuth = FirebaseAuth.getInstance(app)
                }
                return firebaseAuth!!
            }
        }

    val firestore: FirebaseFirestore
        get() {
            val cached = firebaseFirestore
            if (cached != null) return cached

            synchronized(this) {
                if (firebaseFirestore == null) {
                    firebaseFirestore = FirebaseFirestore.getInstance(app)
                }
                return firebaseFirestore!!
            }
        }

    fun runtimeProjectInfo(): String {
        return if (!initialized) {
            "Firebase no inicialitzat"
        } else {
            "projectId=${firebaseApp.options.projectId}, applicationId=${firebaseApp.options.applicationId}"
        }
    }
}