package com.marcfradera.shooterranking.data

import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.data.model.PersonalPlayerProfile
import com.marcfradera.shooterranking.localization.AppLanguageManager
import kotlinx.coroutines.tasks.await

class PersonalPlayerRepository {

    companion object {
        private const val PERSONAL_EQUIP_ID = "__personal_player__"
    }

    private val db get() = FirebaseProvider.firestore
    private val auth get() = FirebaseProvider.auth

    private fun currentUid(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException(
                AppLanguageManager.text(R.string.personal_player_auth_required)
            )

    suspend fun load(): PersonalPlayerProfile? {
        val uid = currentUid()

        val snapshot = db.collection("jugadors")
            .whereEqualTo("userId", uid)
            .whereEqualTo("id_equip", PERSONAL_EQUIP_ID)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?: return null

        return PersonalPlayerProfile(
            id_jugador = snapshot.id,
            nom_jugador = snapshot.getString("nom_jugador").orEmpty(),
            numero_jugador = snapshot.getLong("numero_jugador")?.toInt() ?: 0,
            posicio_jugador = snapshot.getString("posicio_jugador").orEmpty(),
            tipus_pista = snapshot.getString("tipus_pista")
                ?.takeIf { it.isNotBlank() }
                ?: "Base",
            userId = uid
        )
    }

    suspend fun create(
        nom: String,
        dorsal: Int,
        posicio: String,
        tipusPista: String
    ): PersonalPlayerProfile {
        // If the profile already exists, never create a duplicate.
        load()?.let { return it }

        val uid = currentUid()
        val normalizedCourt = if (tipusPista.equals("Base", ignoreCase = true)) {
            "Base"
        } else {
            // Same canonical value already used by teams for the visible FIBA option.
            "Amateur"
        }

        val data = hashMapOf(
            "nom_jugador" to nom.trim(),
            "numero_jugador" to dorsal,
            "posicio_jugador" to posicio,
            "id_equip" to PERSONAL_EQUIP_ID,
            "tipus_pista" to normalizedCourt,
            "is_personal_profile" to true,
            "userId" to uid
        )

        val ref = db.collection("jugadors").add(data).await()

        return PersonalPlayerProfile(
            id_jugador = ref.id,
            nom_jugador = nom.trim(),
            numero_jugador = dorsal,
            posicio_jugador = posicio,
            tipus_pista = normalizedCourt,
            userId = uid
        )
    }
}
