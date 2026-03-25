package com.marcfradera.shooterranking.data.model

data class Temporada(
    val id_temporada: String = "",
    val any_inici: Int = 0,
    val any_fi: Int = 0,
    val userId: String = ""
)

data class Equip(
    val id_equip: String = "",
    val nom_equip: String = "",
    val id_temporada: String = "",
    val userId: String = ""
)

data class Jugador(
    val id_jugador: String = "",
    val nom_jugador: String = "",
    val numero_jugador: Int = 0,
    val posicio_jugador: String = "",
    val id_equip: String = "",
    val userId: String = ""
)

data class Sessio(
    val id_sessio: String = "",
    val num_sessio: Int = 0,
    val id_jugador: String = "",
    val userId: String = "",
    val tirs_pos_1: Int = 0, val fets_pos_1: Int = 0,
    val tirs_pos_2: Int = 0, val fets_pos_2: Int = 0,
    val tirs_pos_3: Int = 0, val fets_pos_3: Int = 0,
    val tirs_pos_4: Int = 0, val fets_pos_4: Int = 0,
    val tirs_pos_5: Int = 0, val fets_pos_5: Int = 0,
    val tirs_pos_6: Int = 0, val fets_pos_6: Int = 0,
    val tirs_pos_7: Int = 0, val fets_pos_7: Int = 0,
    val tirs_pos_8: Int = 0, val fets_pos_8: Int = 0,
    val tirs_pos_9: Int = 0, val fets_pos_9: Int = 0,
    val tirs_pos_10: Int = 0, val fets_pos_10: Int = 0,
    val tirs_pos_11: Int = 0, val fets_pos_11: Int = 0
)

data class ZoneAgg(val zone: Int, val made: Int, val attempted: Int) {
    val pct: Double get() = if (attempted == 0) 0.0 else made.toDouble() / attempted.toDouble()
}

data class JugadorRankingItem(
    val jugador: Jugador,
    val sessions: Int,
    val made: Int,
    val attempted: Int,
    val pct: Double
)
