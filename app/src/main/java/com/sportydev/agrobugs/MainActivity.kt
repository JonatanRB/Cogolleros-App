package com.sportydev.agrobugs

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : BaseActivity() {

    private lateinit var admin: AdminBd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // La instancia de AdminBd puede ser nula si la base de datos no es necesaria en el onCreate
        // Considera inicializarla solo si se va a usar.
//        admin = AdminBd(this)
//        admin.writableDatabase

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupCards()
        setupBottomNavigation()
    }

    /**
     * Configura los listeners para las cards principales.
     */
    private fun setupCards() {
        // Enlaza la nueva Card principal para escanear con la cámara
        findViewById<MaterialCardView>(R.id.cardScanWithCamera).setOnClickListener {
            startActivity(Intent(this, SearchImageActivity::class.java))
            overridePendingTransition(0, 0)

        }

        // Enlaza la nueva Card para búsqueda guiada (antes cardBusquedaBinaria)
        findViewById<MaterialCardView>(R.id.cardGuidedSearch).setOnClickListener {
            startActivity(Intent(this, PestAkinatorActivity::class.java))
            overridePendingTransition(0, 0)
        }

        // Enlaza la nueva Card para la enciclopedia (antes cardBusquedaManual)
        findViewById<MaterialCardView>(R.id.cardEncyclopedia).setOnClickListener {
            startActivity(Intent(this, SearchManuallyActivity::class.java))
            overridePendingTransition(0, 0)

        }
        val cardBitacora = findViewById<MaterialCardView>(R.id.cardBitacora)
        cardBitacora.setOnClickListener {
            val intent = Intent(this, MonitoringDashboardActivity::class.java)
            startActivity(intent)
        }


    }

    /**
     * Configura los listeners para la barra de navegación inferior.
     */
    private fun setupBottomNavigation() {
        // Botón de Inicio -> No hace nada porque ya está en MainActivity
        findViewById<LinearLayout>(R.id.nav_inicio).setOnClickListener {
            // Ya estás en esta pantalla, puedes dejarlo vacío o mostrar un Toast.
        }

        // Botón de Búsqueda Normal -> SearchManuallyActivity
        findViewById<LinearLayout>(R.id.nav_busqueda_normal).setOnClickListener {
            startActivity(Intent(this, SearchManuallyActivity::class.java))
            overridePendingTransition(0, 0)
        }

        // Botón de Cámara (FloatingActionButton) -> SearchImageActivity
        findViewById<FloatingActionButton>(R.id.nav_camera).setOnClickListener {
            startActivity(Intent(this, SearchImageActivity::class.java))
            overridePendingTransition(0, 0)
        }

        // Botón Pro -> SearchBinaryActivity
        findViewById<LinearLayout>(R.id.nav_bitacora).setOnClickListener {
            startActivity(Intent(this, MonitoringDashboardActivity::class.java))
            overridePendingTransition(0, 0)
        }

        // Botón de Ajustes -> AHORA ABRE CONFIGURACIÓN
        findViewById<LinearLayout>(R.id.nav_ajustes).setOnClickListener {
            startActivity(Intent(this, ConfigurationActivity::class.java))
            overridePendingTransition(0, 0)
        }

    }

}