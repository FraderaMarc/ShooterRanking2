package com.marcfradera.shooterranking.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.marcfradera.shooterranking.data.model.Equip
import com.marcfradera.shooterranking.data.model.Jugador
import com.marcfradera.shooterranking.data.model.Sessio
import com.marcfradera.shooterranking.data.model.Temporada
import com.marcfradera.shooterranking.data.model.ZoneAgg
import com.marcfradera.shooterranking.ui.vm.EquipDeletePreview
import com.marcfradera.shooterranking.ui.vm.EquipUiItem
import com.marcfradera.shooterranking.ui.vm.TemporadaDeletePreview
import com.marcfradera.shooterranking.ui.vm.TemporadaUiItem
import kotlinx.coroutines.tasks.await

class ShooterRepository(
    private val auth: com.google.firebase.auth.FirebaseAuth = FirebaseProvider.auth,
    private val db: FirebaseFirestore = FirebaseProvider.firestore
) {

    private fun currentUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("No hi ha cap usuari autenticat.")
    }

    private fun normalizeUsername(username: String): String {
        return username.trim().lowercase()
    }

    private fun validateUsername(username: String) {
        val normalized = normalizeUsername(username)

        if (normalized.length < 3) {
            throw IllegalArgumentException("El nom d'usuari ha de tenir com a mínim 3 caràcters.")
        }

        if (normalized.length > 20) {
            throw IllegalArgumentException("El nom d'usuari no pot superar els 20 caràcters.")
        }

        if (!Regex("^[a-z0-9._]+$").matches(normalized)) {
            throw IllegalArgumentException(
                "El nom d'usuari només pot contenir lletres, números, punt i guió baix."
            )
        }
    }

    private fun mapFirebaseError(e: Exception): Exception {
        val runtimeInfo = FirebaseProvider.runtimeProjectInfo()
        val raw = e.message.orEmpty()

        return when (e) {
            is FirebaseFirestoreException -> {
                when {
                    raw.contains("The database (default) does not exist", ignoreCase = true) ->
                        IllegalStateException(
                            "L'app està connectant a un Firestore que no troba la base de dades default.\n" +
                                    "Info runtime: $runtimeInfo"
                        )

                    e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        IllegalStateException(
                            "Firestore ha rebutjat l'accés. Revisa les regles de seguretat.\n" +
                                    "Info runtime: $runtimeInfo"
                        )

                    e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
                        IllegalStateException(
                            "Firestore no està disponible ara mateix. Revisa la connexió.\n" +
                                    "Info runtime: $runtimeInfo"
                        )

                    else ->
                        IllegalStateException(
                            raw.ifBlank { "S'ha produït un error de Firestore." }
                        )
                }
            }

            is FirebaseAuthException -> {
                when {
                    raw.contains("email address is already in use", ignoreCase = true) ->
                        IllegalStateException("Aquest correu electrònic ja està registrat.")

                    raw.contains("password is invalid", ignoreCase = true) ||
                            raw.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
                        IllegalStateException("Credencials incorrectes.")

                    raw.contains("connection abort", ignoreCase = true) ||
                            raw.contains("software caused connection abort", ignoreCase = true) ||
                            raw.contains("network", ignoreCase = true) ->
                        IllegalStateException("Error de connexió amb Firebase. Torna-ho a provar.")

                    else ->
                        IllegalStateException(raw.ifBlank { "Error d'autenticació." })
                }
            }

            is FirebaseNetworkException ->
                IllegalStateException("No hi ha connexió a Internet o la connexió ha fallat.")

            is IllegalArgumentException -> e
            is IllegalStateException -> e

            else -> {
                if (
                    raw.contains("connection abort", ignoreCase = true) ||
                    raw.contains("software caused connection abort", ignoreCase = true) ||
                    raw.contains("network", ignoreCase = true)
                ) {
                    IllegalStateException("Error de connexió amb Firebase. Torna-ho a provar.")
                } else {
                    IllegalStateException(raw.ifBlank { "S'ha produït un error inesperat." })
                }
            }
        }
    }

    suspend fun reloadCurrentUser() {
        val user = auth.currentUser ?: return

        try {
            user.reload().await()

            if (user.isEmailVerified) {
                db.collection("users")
                    .document(user.uid)
                    .update("emailVerified", true)
                    .await()
            }
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun isEmailConfirmed(): Boolean = auth.currentUser?.isEmailVerified == true

    fun currentUserEmail(): String = auth.currentUser?.email.orEmpty()

    suspend fun signIn(email: String, password: String) {
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            reloadCurrentUser()
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    suspend fun signUp(email: String, password: String, username: String) {
        validateUsername(username)
        val usernameLower = normalizeUsername(username)

        val authResult = try {
            auth.createUserWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }

        val user = authResult.user ?: throw IllegalStateException("No s'ha pogut crear l'usuari.")

        try {
            reserveUsernameAndCreateProfile(user, username.trim(), usernameLower)
            user.sendEmailVerification().await()
        } catch (e: Exception) {
            try {
                user.delete().await()
            } catch (_: Exception) {
            }
            throw mapFirebaseError(e)
        }
    }

    suspend fun resendVerificationEmail() {
        val user = auth.currentUser ?: throw IllegalStateException("No hi ha cap usuari autenticat.")
        try {
            user.sendEmailVerification().await()
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        validateUsername(username)
        val usernameLower = normalizeUsername(username)

        return try {
            val doc = db.collection("usernames").document(usernameLower).get().await()
            !doc.exists()
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    private suspend fun reserveUsernameAndCreateProfile(
        user: FirebaseUser,
        username: String,
        usernameLower: String
    ) {
        val usernameRef = db.collection("usernames").document(usernameLower)
        val userRef = db.collection("users").document(user.uid)

        try {
            db.runTransaction { tx ->
                val usernameSnap = tx.get(usernameRef)

                if (usernameSnap.exists()) {
                    throw IllegalStateException("Aquest nom d'usuari ja existeix.")
                }

                tx.set(
                    usernameRef,
                    mapOf(
                        "uid" to user.uid,
                        "username" to username,
                        "createdAt" to System.currentTimeMillis()
                    )
                )

                tx.set(
                    userRef,
                    mapOf(
                        "email" to (user.email ?: ""),
                        "username" to username,
                        "usernameLower" to usernameLower,
                        "createdAt" to System.currentTimeMillis(),
                        "emailVerified" to user.isEmailVerified
                    )
                )
            }.await()
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    suspend fun listTemporades(): List<Temporada> {
        val uid = currentUid()
        return try {
            val docs = db.collection("temporades")
                .whereEqualTo("userId", uid)
                .get()
                .await()
                .documents

            docs.mapNotNull { it.toTemporada() }
                .sortedByDescending { it.any_inici }
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    suspend fun createTemporada(anyInici: Int, anyFi: Int): Temporada {
        val uid = currentUid()
        val data = hashMapOf(
            "any_inici" to anyInici,
            "any_fi" to anyFi,
            "userId" to uid
        )

        return try {
            val ref = db.collection("temporades").add(data).await()
            val snap = ref.get().await()
            snap.toTemporada()
                ?: throw IllegalStateException("No s'ha pogut crear la temporada.")
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    suspend fun listEquips(idTemporada: String): List<Equip> {
        val uid = currentUid()
        return try {
            val docs = db.collection("equips")
                .whereEqualTo("userId", uid)
                .whereEqualTo("id_temporada", idTemporada)
                .get()
                .await()
                .documents

            docs.mapNotNull { it.toEquip() }
                .sortedBy { it.nom_equip.lowercase() }
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    suspend fun createEquip(nomEquip: String, idTemporada: String): Equip {
        val uid = currentUid()
        val data = hashMapOf(
            "nom_equip" to nomEquip,
            "id_temporada" to idTemporada,
            "userId" to uid
        )

        return try {
            val ref = db.collection("equips").add(data).await()
            val snap = ref.get().await()
            snap.toEquip()
                ?: throw IllegalStateException("No s'ha pogut crear l'equip.")
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    suspend fun listJugadors(idEquip: String): List<Jugador> {
        val uid = currentUid()
        return try {
            val docs = db.collection("jugadors")
                .whereEqualTo("userId", uid)
                .whereEqualTo("id_equip", idEquip)
                .get()
                .await()
                .documents

            docs.mapNotNull { it.toJugador() }
                .sortedBy { it.nom_jugador.lowercase() }
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    suspend fun createJugador(
        nom: String,
        dorsal: Int,
        posicio: String,
        idEquip: String
    ): Jugador {
        val uid = currentUid()
        val data = hashMapOf(
            "nom_jugador" to nom,
            "numero_jugador" to dorsal,
            "posicio_jugador" to posicio,
            "id_equip" to idEquip,
            "userId" to uid
        )

        return try {
            val ref = db.collection("jugadors").add(data).await()
            val snap = ref.get().await()
            snap.toJugador()
                ?: throw IllegalStateException("No s'ha pogut crear la jugadora.")
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    suspend fun listSessions(idJugador: String): List<Sessio> {
        val uid = currentUid()
        return try {
            val docs = db.collection("sessions")
                .whereEqualTo("userId", uid)
                .whereEqualTo("id_jugador", idJugador)
                .get()
                .await()
                .documents

            docs.mapNotNull { it.toSessio() }
                .sortedBy { it.num_sessio }
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    suspend fun nextSessionNumber(idJugador: String): Int {
        val sessions = listSessions(idJugador)
        return (sessions.maxOfOrNull { it.num_sessio } ?: 0) + 1
    }

    suspend fun createSession(sessio: Sessio): Sessio {
        val uid = currentUid()
        val data = hashMapOf(
            "num_sessio" to sessio.num_sessio,
            "id_jugador" to sessio.id_jugador,
            "userId" to uid,
            "tirs_pos_1" to sessio.tirs_pos_1,
            "fets_pos_1" to sessio.fets_pos_1,
            "tirs_pos_2" to sessio.tirs_pos_2,
            "fets_pos_2" to sessio.fets_pos_2,
            "tirs_pos_3" to sessio.tirs_pos_3,
            "fets_pos_3" to sessio.fets_pos_3,
            "tirs_pos_4" to sessio.tirs_pos_4,
            "fets_pos_4" to sessio.fets_pos_4,
            "tirs_pos_5" to sessio.tirs_pos_5,
            "fets_pos_5" to sessio.fets_pos_5,
            "tirs_pos_6" to sessio.tirs_pos_6,
            "fets_pos_6" to sessio.fets_pos_6,
            "tirs_pos_7" to sessio.tirs_pos_7,
            "fets_pos_7" to sessio.fets_pos_7,
            "tirs_pos_8" to sessio.tirs_pos_8,
            "fets_pos_8" to sessio.fets_pos_8,
            "tirs_pos_9" to sessio.tirs_pos_9,
            "fets_pos_9" to sessio.fets_pos_9,
            "tirs_pos_10" to sessio.tirs_pos_10,
            "fets_pos_10" to sessio.fets_pos_10,
            "tirs_pos_11" to sessio.tirs_pos_11,
            "fets_pos_11" to sessio.fets_pos_11
        )

        return try {
            val ref = db.collection("sessions").add(data).await()
            val snap = ref.get().await()
            snap.toSessio()
                ?: throw IllegalStateException("No s'ha pogut guardar la sessió.")
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    suspend fun aggregateZones(idJugador: String): List<ZoneAgg> {
        val sessions = listSessions(idJugador)
        val made = IntArray(12)
        val att = IntArray(12)

        fun add(z: Int, f: Int, t: Int) {
            made[z] += f
            att[z] += t
        }

        sessions.forEach { s ->
            add(1, s.fets_pos_1, s.tirs_pos_1)
            add(2, s.fets_pos_2, s.tirs_pos_2)
            add(3, s.fets_pos_3, s.tirs_pos_3)
            add(4, s.fets_pos_4, s.tirs_pos_4)
            add(5, s.fets_pos_5, s.tirs_pos_5)
            add(6, s.fets_pos_6, s.tirs_pos_6)
            add(7, s.fets_pos_7, s.tirs_pos_7)
            add(8, s.fets_pos_8, s.tirs_pos_8)
            add(9, s.fets_pos_9, s.tirs_pos_9)
            add(10, s.fets_pos_10, s.tirs_pos_10)
            add(11, s.fets_pos_11, s.tirs_pos_11)
        }

        return (1..11).map { z -> ZoneAgg(z, made[z], att[z]) }
    }

    suspend fun totalPct(idJugador: String): Double {
        val zones = aggregateZones(idJugador)
        val made = zones.sumOf { it.made }
        val att = zones.sumOf { it.attempted }
        return if (att == 0) 0.0 else made.toDouble() / att.toDouble()
    }

    private fun DocumentSnapshot.toTemporada(): Temporada? {
        val anyInici = getLong("any_inici")?.toInt() ?: return null
        val anyFi = getLong("any_fi")?.toInt() ?: return null
        val userId = getString("userId") ?: ""
        return Temporada(
            id_temporada = id,
            any_inici = anyInici,
            any_fi = anyFi,
            userId = userId
        )
    }

    private fun DocumentSnapshot.toEquip(): Equip? {
        val nomEquip = getString("nom_equip") ?: return null
        val idTemporada = getString("id_temporada") ?: return null
        val userId = getString("userId") ?: ""
        return Equip(
            id_equip = id,
            nom_equip = nomEquip,
            id_temporada = idTemporada,
            userId = userId
        )
    }

    private fun DocumentSnapshot.toJugador(): Jugador? {
        val nomJugador = getString("nom_jugador") ?: return null
        val numeroJugador = getLong("numero_jugador")?.toInt() ?: 0
        val posicioJugador = getString("posicio_jugador") ?: ""
        val idEquip = getString("id_equip") ?: return null
        val userId = getString("userId") ?: ""
        return Jugador(
            id_jugador = id,
            nom_jugador = nomJugador,
            numero_jugador = numeroJugador,
            posicio_jugador = posicioJugador,
            id_equip = idEquip,
            userId = userId
        )
    }

    private fun DocumentSnapshot.toSessio(): Sessio? {
        fun i(name: String): Int = getLong(name)?.toInt() ?: 0

        val idJugador = getString("id_jugador") ?: return null
        val userId = getString("userId") ?: ""

        return Sessio(
            id_sessio = id,
            num_sessio = i("num_sessio"),
            id_jugador = idJugador,
            userId = userId,
            tirs_pos_1 = i("tirs_pos_1"),
            fets_pos_1 = i("fets_pos_1"),
            tirs_pos_2 = i("tirs_pos_2"),
            fets_pos_2 = i("fets_pos_2"),
            tirs_pos_3 = i("tirs_pos_3"),
            fets_pos_3 = i("fets_pos_3"),
            tirs_pos_4 = i("tirs_pos_4"),
            fets_pos_4 = i("fets_pos_4"),
            tirs_pos_5 = i("tirs_pos_5"),
            fets_pos_5 = i("fets_pos_5"),
            tirs_pos_6 = i("tirs_pos_6"),
            fets_pos_6 = i("fets_pos_6"),
            tirs_pos_7 = i("tirs_pos_7"),
            fets_pos_7 = i("fets_pos_7"),
            tirs_pos_8 = i("tirs_pos_8"),
            fets_pos_8 = i("fets_pos_8"),
            tirs_pos_9 = i("tirs_pos_9"),
            fets_pos_9 = i("fets_pos_9"),
            tirs_pos_10 = i("tirs_pos_10"),
            fets_pos_10 = i("fets_pos_10"),
            tirs_pos_11 = i("tirs_pos_11"),
            fets_pos_11 = i("fets_pos_11")
        )
    }

    suspend fun listTemporadesWithCounts(): List<TemporadaUiItem> {
        return listTemporades().map { temporada ->
            TemporadaUiItem(
                temporada = temporada,
                equipsCount = listEquips(temporada.id_temporada).size
            )
        }
    }

    suspend fun listEquipsWithCounts(idTemporada: String): List<EquipUiItem> {
        return listEquips(idTemporada).map { equip ->
            EquipUiItem(
                equip = equip,
                jugadorsCount = listJugadors(equip.id_equip).size
            )
        }
    }

    suspend fun updateTemporada(
        idTemporada: String,
        anyInici: Int,
        anyFi: Int
    ) {
        db.collection("temporades")
            .document(idTemporada)
            .update(
                mapOf(
                    "any_inici" to anyInici,
                    "any_fi" to anyFi
                )
            )
            .await()
    }

    suspend fun updateEquip(
        idEquip: String,
        nomEquip: String
    ) {
        db.collection("equips")
            .document(idEquip)
            .update("nom_equip", nomEquip.trim())
            .await()
    }

    suspend fun updateJugador(
        idJugador: String,
        nom: String,
        dorsal: Int,
        posicio: String
    ) {
        db.collection("jugadors")
            .document(idJugador)
            .update(
                mapOf(
                    "nom_jugador" to nom.trim(),
                    "numero_jugador" to dorsal,
                    "posicio_jugador" to posicio.trim()
                )
            )
            .await()
    }

    suspend fun getTemporadaDeletePreview(idTemporada: String): TemporadaDeletePreview {
        val equips = listEquips(idTemporada)
        var jugadorsCount = 0
        var sessionsCount = 0

        equips.forEach { equip ->
            val jugadors = listJugadors(equip.id_equip)
            jugadorsCount += jugadors.size
            jugadors.forEach { jugador ->
                sessionsCount += listSessions(jugador.id_jugador).size
            }
        }

        return TemporadaDeletePreview(
            equips = equips,
            jugadorsCount = jugadorsCount,
            sessionsCount = sessionsCount
        )
    }

    suspend fun getEquipDeletePreview(idEquip: String): EquipDeletePreview {
        val jugadors = listJugadors(idEquip)
        var sessionsCount = 0

        jugadors.forEach { jugador ->
            sessionsCount += listSessions(jugador.id_jugador).size
        }

        return EquipDeletePreview(
            jugadors = jugadors,
            sessionsCount = sessionsCount
        )
    }

    suspend fun deleteJugadorCascade(idJugador: String) {
        val sessions = db.collection("sessions")
            .whereEqualTo("id_jugador", idJugador)
            .get()
            .await()

        sessions.documents.forEach { doc ->
            db.collection("sessions").document(doc.id).delete().await()
        }

        db.collection("jugadors")
            .document(idJugador)
            .delete()
            .await()
    }

    suspend fun deleteEquipCascade(idEquip: String) {
        val jugadors = listJugadors(idEquip)
        jugadors.forEach { jugador ->
            deleteJugadorCascade(jugador.id_jugador)
        }

        db.collection("equips")
            .document(idEquip)
            .delete()
            .await()
    }

    suspend fun deleteTemporadaCascade(idTemporada: String) {
        val equips = listEquips(idTemporada)
        equips.forEach { equip ->
            deleteEquipCascade(equip.id_equip)
        }

        db.collection("temporades")
            .document(idTemporada)
            .delete()
            .await()
    }

    suspend fun updateSession(session: Sessio) {
        val query = db.collection("sessions")
            .whereEqualTo("id_jugador", session.id_jugador)
            .whereEqualTo("num_sessio", session.num_sessio)
            .get()
            .await()

        val doc = query.documents.first()
        doc.reference.set(session).await()
    }

    suspend fun deleteSession(idJugador: String, numSessio: Int) {
        val uid = currentUid()

        val query = db.collection("sessions")
            .whereEqualTo("userId", uid)
            .whereEqualTo("id_jugador", idJugador)
            .whereEqualTo("num_sessio", numSessio)
            .get()
            .await()

        val doc = query.documents.firstOrNull()
            ?: throw IllegalStateException("No s'ha trobat la sessió a eliminar.")

        doc.reference.delete().await()
    }

    suspend fun getJugadorDeleteSessionsCount(idJugador: String): Int {
        return listSessions(idJugador).size
    }
}