package com.sportydev.agrobugs

/**
 * Mapea los nombres detectados por YOLO a los nombres en la base de datos
 */

object PestMapper {

    private val pestMapping = mapOf(
        // Mapeo de nombres en inglés de YOLO a nombres en español de la BD
        "roya-" to "Gusano cogollero",
        "fall armyworm" to "gusano cogollero",
        "cogollero" to "gusano cogollero",
        "aphid" to "pulgón",
        "hoja" to "Trips",
        "whitefly" to "mosca blanca",
        "mosca blanca" to "mosca blanca",
        "leafhopper" to "chicharrita",
        "corn leafhopper" to "chicharrita",
        "beetle" to "escarabajo",
        "corn beetle" to "escarabajo",
        // Agrega más mapeos según tus etiquetas de YOLO
    )

    /**
     * Convierte el nombre detectado por YOLO al nombre en la base de datos
     */
    fun mapToDbName(yoloLabel: String): String {
        val normalized = yoloLabel.lowercase().trim()
        return pestMapping[normalized] ?: yoloLabel
    }

    /**
     * Busca una plaga en la BD usando el nombre detectado por YOLO
     */
    fun findPestInDatabase(yoloLabel: String, adminBd: AdminBd): Plaga? {
        // Primero intentar con el mapeo
        val mappedName = mapToDbName(yoloLabel)
        var pest = adminBd.getPestByName(mappedName)

        // Si no se encuentra, intentar con el nombre original
        if (pest == null) {
            pest = adminBd.getPestByName(yoloLabel)
        }

        return pest
    }

    fun findPestInDatabaseAkinator(akinatorId: String, dbHelper: AdminBd): Plaga? {
        val dbName = getDatabaseName(akinatorId)
        return dbHelper.getPestByName(dbName)
    }

    /**
     * Obtiene el nombre de la base de datos a partir del ID del Akinator
     */
    fun getDatabaseName(akinatorId: String): String {
        return pestMapping[akinatorId] ?: akinatorId
    }

}