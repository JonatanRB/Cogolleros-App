package com.sportydev.agrobugs

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream

class AdminBd(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "BDBichos.db"
        private const val DATABASE_VERSION = 1
        private const val ASSETS_PATH = "BDBichos.db"
    }

    init {
        // Copiar la base de datos desde assets si no existe
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) {
            copyDatabaseFromAssets()
        }
    }

    private fun copyDatabaseFromAssets() {
        try {
            val dbPath = context.getDatabasePath(DATABASE_NAME)
            dbPath.parentFile?.mkdirs()

            context.assets.open(ASSETS_PATH).use { inputStream ->
                FileOutputStream(dbPath).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            println("✓ Base de datos copiada exitosamente")
        } catch (e: Exception) {
            e.printStackTrace()
            println("✗ Error al copiar la base de datos: ${e.message}")
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // La base de datos ya viene creada desde assets
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Implementar si necesitas actualizar la BD en el futuro
    }

    /**
     * Obtiene una plaga por su ID
     */
    fun getPestById(id: Int): Plaga? {
        val db = readableDatabase
        var plaga: Plaga? = null

        val cursor = db.rawQuery(
            "SELECT * FROM Plaga WHERE idPlaga = ?",
            arrayOf(id.toString())
        )

        if (cursor.moveToFirst()) {
            plaga = parsePlagaFromCursor(cursor)
        }

        cursor.close()
        return plaga
    }

    /**
     * Busca una plaga por su nombre común o científico
     * Útil para buscar por el resultado de YOLO
     */
    fun getPestByName(name: String): Plaga? {
        val db = readableDatabase
        var plaga: Plaga? = null

        // Buscar por nombre común o nombre científico (case-insensitive)
        val cursor = db.rawQuery(
            """
            SELECT * FROM Plaga 
            WHERE LOWER(nomPlaga) LIKE LOWER(?) 
            OR LOWER(nomCientifico) LIKE LOWER(?)
            LIMIT 1
            """,
            arrayOf("%$name%", "%$name%")
        )

        if (cursor.moveToFirst()) {
            plaga = parsePlagaFromCursor(cursor)
        }

        cursor.close()
        return plaga
    }

    /**
     * Obtiene todas las plagas de la base de datos
     */
    fun getAllPests(): List<Plaga> {
        val db = readableDatabase
        val plagas = mutableListOf<Plaga>()

        val cursor = db.rawQuery("SELECT * FROM Plaga", null)

        while (cursor.moveToNext()) {
            val plaga = parsePlagaFromCursor(cursor)
            plagas.add(plaga)
        }

        cursor.close()
        return plagas
    }

    /**
     * Convierte un cursor en un objeto Plaga
     */
    private fun parsePlagaFromCursor(cursor: Cursor): Plaga {
        val imageNameString = cursor.getString(cursor.getColumnIndexOrThrow("imageName"))
        val imageNames = if (imageNameString.isNullOrBlank()) {
            listOf("img_question")
        } else {
            imageNameString.split(",").map { it.trim() }
        }

        return Plaga(
            idPlaga = cursor.getInt(cursor.getColumnIndexOrThrow("idPlaga")),
            idCultivo = cursor.getInt(cursor.getColumnIndexOrThrow("idCultivo")),
            nomPlaga = cursor.getString(cursor.getColumnIndexOrThrow("nomPlaga")) ?: "",
            descPlaga = cursor.getString(cursor.getColumnIndexOrThrow("descPlaga")) ?: "",
            imageName = imageNames,
            nomcientifico = cursor.getString(cursor.getColumnIndexOrThrow("nomCientifico")) ?: "",
            damage = cursor.getString(cursor.getColumnIndexOrThrow("damage")) ?: "",
            muestreo = cursor.getString(cursor.getColumnIndexOrThrow("muestreo")) ?: "",
            biolControl = cursor.getString(cursor.getColumnIndexOrThrow("biolControl")) ?: "",
            cultControl = cursor.getString(cursor.getColumnIndexOrThrow("cultControl")) ?: "",
            etolControl = cursor.getString(cursor.getColumnIndexOrThrow("etolControl")) ?: "",
            quimControl = cursor.getString(cursor.getColumnIndexOrThrow("quimControl")) ?: "",
            resistManejo = cursor.getString(cursor.getColumnIndexOrThrow("resistManejo")) ?: ""
        )
    }

    /**
     * Imprime todas las plagas en la base de datos (para debug)
     */
    fun printAllPests() {
        val plagas = getAllPests()
        println("=== PLAGAS EN LA BASE DE DATOS ===")
        println("Total de plagas: ${plagas.size}")
        plagas.forEach { plaga ->
            println("ID: ${plaga.idPlaga} - ${plaga.nomPlaga} (${plaga.nomcientifico})")
        }
        println("===================================")
    }
}