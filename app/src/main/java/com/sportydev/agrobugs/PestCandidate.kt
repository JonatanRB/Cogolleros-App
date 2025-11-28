package com.sportydev.agrobugs

data class PestCandidate(
    val id: String,
    val name: String,
    val scientificName: String,
    val probability: Double,
    val attributes: Map<String, Double>
)
