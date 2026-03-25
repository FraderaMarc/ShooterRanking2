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
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore

    fun initialize(context: Context) {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            val app = FirebaseApp.initializeApp(context) ?: FirebaseApp.getInstance()
            firebaseApp = app
            firebaseAuth = FirebaseAuth.getInstance(app)
            firebaseFirestore = FirebaseFirestore.getInstance(app)

            initialized = true

            Log.d(TAG, "Firebase initialized")
            Log.d(TAG, "projectId=${app.options.projectId}")
            Log.d(TAG, "applicationId=${app.options.applicationId}")
            Log.d(TAG, "storageBucket=${app.options.storageBucket}")
            Log.d(TAG, "gcmSenderId=${app.options.gcmSenderId}")
        }
    }

    val app: FirebaseApp
        get() {
            check(initialized) {
                "FirebaseProvider no està inicialitzat. Crida FirebaseProvider.initialize(context) abans."
            }
            return firebaseApp
        }

    val auth: FirebaseAuth
        get() {
            check(initialized) {
                "FirebaseProvider no està inicialitzat. Crida FirebaseProvider.initialize(context) abans."
            }
            return firebaseAuth
        }

    val firestore: FirebaseFirestore
        get() {
            check(initialized) {
                "FirebaseProvider no està inicialitzat. Crida FirebaseProvider.initialize(context) abans."
            }
            return firebaseFirestore
        }

    fun runtimeProjectInfo(): String {
        return if (!initialized) {
            "Firebase no inicialitzat"
        } else {
            "projectId=${firebaseApp.options.projectId}, applicationId=${firebaseApp.options.applicationId}"
        }
    }
}