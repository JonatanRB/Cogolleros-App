package com.sportydev.agrobugs

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView

class InformationBugActivity : BaseActivity() {

    companion object {
        const val EXTRA_PLAGA_ID = "extra_plaga_id"
    }

    // Vistas principales
    private lateinit var tvPestName: TextView
    private lateinit var tvScientificName: TextView
    private lateinit var tvPestDescription: TextView

    // Vistas para el header de imágenes
    private lateinit var ivMainImage: ShapeableImageView
    private lateinit var ivSubImage1: ShapeableImageView
    private lateinit var ivSubImage2: ShapeableImageView

    // ✅ Vistas para el badge de detección
    private lateinit var detectionBadge: MaterialCardView
    private lateinit var tvDetectionLabel: TextView
    private lateinit var tvDetectionConfidence: TextView

    // Vistas para el Bottom Sheet
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<MaterialCardView>
    private lateinit var tvSheetTitle: TextView
    private lateinit var tvSheetContent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_information_bug)

        // Inicializar vistas
        tvPestName = findViewById(R.id.tvBugName)
        tvScientificName = findViewById(R.id.tvScientificName)
        tvPestDescription = findViewById(R.id.tvBugDescription)
        ivMainImage = findViewById(R.id.ivMainImage)
        ivSubImage1 = findViewById(R.id.ivSubImage1)
        ivSubImage2 = findViewById(R.id.ivSubImage2)

        // Inicializar badge de detección
        detectionBadge = findViewById(R.id.detectionBadge)
        tvDetectionLabel = findViewById(R.id.tvDetectionLabel)
        tvDetectionConfidence = findViewById(R.id.tvDetectionConfidence)

        // ✅ VERIFICAR SI VIENE DEL AKINATOR
        val fromAkinator = intent.getBooleanExtra("FROM_AKINATOR", false)

        if (fromAkinator) {
            handleAkinatorResult()
        } else {
            handleNormalFlow()
        }
    }

    // ✅ NUEVO MÉTODO: Manejar resultado del Akinator
    private fun handleAkinatorResult() {
        val pestName = intent.getStringExtra("PEST_NAME")
        val pestId = intent.getStringExtra("PEST_ID")
        val probability = intent.getDoubleExtra("PEST_PROBABILITY", 0.0)

        if (pestName != null) {
            val dbHelper = AdminBd(this)

            // Intentar buscar por ID primero (más confiable)
            var pest: Plaga? = null
            if (pestId != null) {
                pest = PestMapper.findPestInDatabaseAkinator(pestId, dbHelper)
            }

            // Si no se encuentra por ID, buscar por nombre
            if (pest == null) {
                pest = dbHelper.getPestByName(pestName)
            }

            if (pest != null) {
                populateUI(pest)
                showAkinatorConfidence(probability)
            } else {
                // No se encontró en la BD, mostrar info básica
                showBasicAkinatorInfo()
            }
        } else {
            Toast.makeText(this, "Error: No se recibió información de la plaga", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // ✅ NUEVO MÉTODO: Flujo normal (desde detección de imagen o búsqueda manual)
    private fun handleNormalFlow() {
        val pestId = intent.getIntExtra(EXTRA_PLAGA_ID, -1)
        val detectedName = intent.getStringExtra("detected_name")
        val confidence = intent.getFloatExtra("confidence", 0f)

        if (pestId != -1) {
            val dbHelper = AdminBd(this)
            val pest = dbHelper.getPestById(pestId)

            if (pest != null) {
                populateUI(pest)

                // Mostrar badge si viene de detección automática
                if (detectedName != null && confidence > 0) {
                    showDetectionBadge(detectedName, confidence)
                }
            } else {
                Toast.makeText(this, "Error: No se encontró la plaga.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Error: No se recibió un ID válido", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun populateUI(pest: Plaga) {
        tvPestName.text = pest.nomPlaga
        tvScientificName.text = pest.nomcientifico
        tvPestDescription.text = pest.descPlaga

        setupImageHeader(pest.imageName)
        setupViewsAndListeners(pest)
    }

    // ✅ MÉTODO PARA MOSTRAR EL BADGE DE DETECCIÓN POR IMAGEN
    private fun showDetectionBadge(detectedName: String, confidence: Float) {
        val confidencePercent = String.format("%.1f%%", confidence * 100)

        // Hacer visible el badge
        detectionBadge.visibility = View.VISIBLE

        // Establecer textos
        tvDetectionLabel.text = "✓ Detectado: $detectedName"
        tvDetectionConfidence.text = "Confianza: $confidencePercent"

        // Cambiar colores según nivel de confianza
        when {
            confidence >= 0.8f -> {
                // Alta confianza - Verde brillante
                detectionBadge.setCardBackgroundColor(0xFFE8F5E9.toInt())
                detectionBadge.strokeColor = 0xFF4CAF50.toInt()
                tvDetectionLabel.setTextColor(0xFF1B5E20.toInt())
                tvDetectionConfidence.setTextColor(0xFF2E7D32.toInt())
            }
            confidence >= 0.6f -> {
                // Confianza media - Amarillo
                detectionBadge.setCardBackgroundColor(0xFFFFF9C4.toInt())
                detectionBadge.strokeColor = 0xFFFBC02D.toInt()
                tvDetectionLabel.setTextColor(0xFF827717.toInt())
                tvDetectionConfidence.setTextColor(0xFF9E9D24.toInt())
            }
            else -> {
                // Baja confianza - Naranja
                detectionBadge.setCardBackgroundColor(0xFFFFE0B2.toInt())
                detectionBadge.strokeColor = 0xFFFF9800.toInt()
                tvDetectionLabel.setTextColor(0xFFE65100.toInt())
                tvDetectionConfidence.setTextColor(0xFFEF6C00.toInt())
            }
        }

        // Toast de confirmación
        Toast.makeText(
            this,
            "Plaga detectada automáticamente",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ✅ MÉTODO PARA MOSTRAR CONFIANZA DEL AKINATOR
    private fun showAkinatorConfidence(probability: Double) {
        val confidencePercent = (probability * 100).toInt()
        val confidenceFloat = probability.toFloat()

        // Hacer visible el badge
        detectionBadge.visibility = View.VISIBLE

        // Establecer textos
        tvDetectionLabel.text = "✓ Identificado por Akinator"
        tvDetectionConfidence.text = "Confianza: $confidencePercent%"

        // Cambiar colores según nivel de confianza
        when {
            confidenceFloat >= 0.8f -> {
                // Alta confianza - Verde brillante
                detectionBadge.setCardBackgroundColor(0xFFE8F5E9.toInt())
                detectionBadge.strokeColor = 0xFF4CAF50.toInt()
                tvDetectionLabel.setTextColor(0xFF1B5E20.toInt())
                tvDetectionConfidence.setTextColor(0xFF2E7D32.toInt())
            }
            confidenceFloat >= 0.6f -> {
                // Confianza media - Amarillo
                detectionBadge.setCardBackgroundColor(0xFFFFF9C4.toInt())
                detectionBadge.strokeColor = 0xFFFBC02D.toInt()
                tvDetectionLabel.setTextColor(0xFF827717.toInt())
                tvDetectionConfidence.setTextColor(0xFF9E9D24.toInt())
            }
            else -> {
                // Baja confianza - Naranja
                detectionBadge.setCardBackgroundColor(0xFFFFE0B2.toInt())
                detectionBadge.strokeColor = 0xFFFF9800.toInt()
                tvDetectionLabel.setTextColor(0xFFE65100.toInt())
                tvDetectionConfidence.setTextColor(0xFFEF6C00.toInt())
            }
        }

        // Toast de confirmación
        Toast.makeText(
            this,
            "Plaga identificada por Akinator con $confidencePercent% de confianza",
            Toast.LENGTH_LONG
        ).show()
    }

    // ✅ NUEVO MÉTODO: Mostrar info básica cuando no se encuentra en BD
    private fun showBasicAkinatorInfo() {
        val pestName = intent.getStringExtra("PEST_NAME")
        val scientificName = intent.getStringExtra("PEST_SCIENTIFIC_NAME")
        val probability = intent.getDoubleExtra("PEST_PROBABILITY", 0.0)

        tvPestName.text = pestName ?: "Plaga no identificada"
        tvScientificName.text = scientificName ?: ""
        tvPestDescription.text = "Esta plaga fue identificada mediante el sistema Akinator, pero no se encontró información detallada en la base de datos local."

        // Mostrar badge
        showAkinatorConfidence(probability)

        // Ocultar imágenes o poner placeholder
        setupImageHeader(listOf("img_question"))

        // Configurar botón de volver
        findViewById<MaterialCardView>(R.id.btnBack).setOnClickListener { finish() }

        Toast.makeText(
            this,
            "Información limitada: Plaga no encontrada en base de datos",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun setupImageHeader(imageNames: List<String>) {
        val validImageNames = imageNames.filter { it.isNotBlank() }
        val defaultImage = "img_question"

        setImage(ivMainImage, validImageNames.getOrNull(0) ?: defaultImage)
        setImage(ivSubImage1, validImageNames.getOrNull(1) ?: defaultImage)
        setImage(ivSubImage2, validImageNames.getOrNull(2) ?: defaultImage)
    }

    private fun setImage(imageView: ImageView, imageName: String) {
        val imageId = resources.getIdentifier(imageName.trim(), "drawable", packageName)
        if (imageId != 0) {
            imageView.setImageResource(imageId)
        } else {
            imageView.setImageResource(R.drawable.img_question)
        }
    }

    private fun setupViewsAndListeners(pest: Plaga) {
        findViewById<MaterialCardView>(R.id.btnBack).setOnClickListener { finish() }

        val bottomSheetCard: MaterialCardView = findViewById(R.id.bottomSheetCard)
        tvSheetTitle = findViewById(R.id.tvSheetTitle)
        tvSheetContent = findViewById(R.id.tvSheetContent)

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetCard)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        findViewById<ImageButton>(R.id.btnCloseSheet).setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        // Configuración de Items CON ICONOS
        setupInfoItem(R.id.itemDamage, R.drawable.ic_damage, "Daños", {
            showBottomSheetWithContent("Daños a los Cultivos", pest.damage)
        })
        setupInfoItem(R.id.itemSampling, R.drawable.ic_sampling, "Muestreo", {
            showBottomSheetWithContent("Cómo Realizar el Muestreo", pest.muestreo)
        })
        setupInfoItem(R.id.itemControlBiological, R.drawable.ic_biological, "Control Biológico", {
            showBottomSheetWithContent("Control Biológico", pest.biolControl)
        })
        setupInfoItem(R.id.itemControlCultural, R.drawable.ic_cultural, "Control Cultural", {
            showBottomSheetWithContent("Control Cultural", pest.cultControl)
        })
        setupInfoItem(R.id.itemControlEthological, R.drawable.ic_ethological, "Control Etológico", {
            showBottomSheetWithContent("Control Etológico", pest.etolControl)
        })
        setupInfoItem(R.id.itemControlChemical, R.drawable.ic_chemical, "Control Químico", {
            showBottomSheetWithContent("Control Químico", pest.quimControl)
        })
        setupInfoItem(R.id.itemResistance, R.drawable.ic_resistance, "Manejo de Resistencia", {
            showBottomSheetWithContent("Manejo de Resistencia", pest.resistManejo)
        })
    }

    private fun showBottomSheetWithContent(title: String, content: String) {
        tvSheetTitle.text = title
        tvSheetContent.text = content
        findViewById<ImageView>(R.id.ivSheetImage).visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun setupInfoItem(itemId: Int, iconRes: Int, title: String, onClickAction: () -> Unit) {
        val itemLayout: View = findViewById(itemId)
        val ivIcon: ImageView = itemLayout.findViewById(R.id.ivIcon)
        val tvTitle: TextView = itemLayout.findViewById(R.id.tvTitle)

        ivIcon.setImageResource(iconRes)
        tvTitle.text = title

        itemLayout.setOnClickListener { onClickAction() }
    }
}
