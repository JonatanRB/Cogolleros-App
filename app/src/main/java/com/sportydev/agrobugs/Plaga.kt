package com.sportydev.agrobugs

data class Plaga(
    val idPlaga: Int,
    val idCultivo: Int,
    val nomPlaga: String,
    val nomcientifico: String,
    val descPlaga: String,
    val imageName: List<String>,
    val damage: String = "",
    val muestreo: String = "",
    val biolControl: String = "",
    val cultControl: String = "",
    val etolControl: String = "",
    val quimControl: String = "",
    val resistManejo: String = ""
)
