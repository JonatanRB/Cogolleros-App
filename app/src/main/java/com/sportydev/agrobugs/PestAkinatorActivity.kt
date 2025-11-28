package com.sportydev.agrobugs

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.sportydev.agrobugs.PestCandidate

class PestAkinatorActivity : AppCompatActivity() {

    private lateinit var akinator: PestAkinator

    // Views
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var tvStepCounter: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvImageQuestion: TextView
    private lateinit var btnYes: MaterialButton
    private lateinit var btnNo: MaterialButton

    // Navigation
    private lateinit var navInicio: android.view.View
    private lateinit var navBusquedaNormal: android.view.View
    private lateinit var navCamera: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var navBusquedaPro: android.view.View
    private lateinit var navAjustes: android.view.View

    private var currentQuestionId: String? = null
    private val maxQuestions = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_binary)

        initViews()
        initAkinator()
        setupNavigation()
        showNextQuestion()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        tvStepCounter = findViewById(R.id.tvStepCounter)
        progressBar = findViewById(R.id.progressBar)
        tvImageQuestion = findViewById(R.id.tvImageQuestion)
        btnYes = findViewById(R.id.btnYes)
        btnNo = findViewById(R.id.btnNo)

        // Navigation
        navInicio = findViewById(R.id.nav_inicio)
        navBusquedaNormal = findViewById(R.id.nav_busqueda_normal)
        navCamera = findViewById(R.id.nav_camera)
        navBusquedaPro = findViewById(R.id.nav_busqueda_pro)
        navAjustes = findViewById(R.id.nav_ajustes)

        // Set title
        tvTitle.text = "Identificar Plaga"

        // Set button listeners
        btnBack.setOnClickListener { finish() }
        btnYes.setOnClickListener { handleAnswer(true) }
        btnNo.setOnClickListener { handleAnswer(false) }
    }

    private fun setupNavigation() {
        navInicio.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        navBusquedaNormal.setOnClickListener {
            startActivity(Intent(this, SearchManuallyActivity::class.java))
            finish()
        }

        navCamera.setOnClickListener {
            startActivity(Intent(this, SearchImageActivity::class.java))
            finish()
        }

        navBusquedaPro.setOnClickListener {
            // Ya estamos aquí
        }

        navAjustes.setOnClickListener {
            startActivity(Intent(this, ConfigurationActivity::class.java))
        }
    }

    private fun initAkinator() {
        val pests = loadPestsFromDatabase()
        akinator = PestAkinator(pests)
    }

    private fun loadPestsFromDatabase(): List<PestCandidate> {
        // TODO: Cargar desde tu base de datos BDDBichos.db
        // Por ahora, datos de ejemplo con más plagas:
        return listOf(
            PestCandidate(
                id = "fall_armyworm",
                name = "Gusano Cogollero",
                scientificName = "Spodoptera frugiperda",
                probability = 0.0,
                attributes = mapOf(
                    "host_maize" to 0.95,
                    "chews_leaves" to 0.90,
                    "frass_present" to 0.85,
                    "nocturnal" to 0.80,
                    "larva_visible_on_whorl" to 0.85,
                    "causes_dead_hearts" to 0.30,
                    "larva_inside_stem" to 0.20,
                    "holes_in_stem" to 0.15,
                    "visible_caterpillar" to 0.90,
                    "polyphagous" to 0.70
                )
            ),
            PestCandidate(
                id = "stem_borer",
                name = "Barrenador del Tallo",
                scientificName = "Diatraea spp.",
                probability = 0.0,
                attributes = mapOf(
                    "host_maize" to 0.85,
                    "causes_dead_hearts" to 0.95,
                    "larva_inside_stem" to 0.95,
                    "holes_in_stem" to 0.90,
                    "frass_present" to 0.70,
                    "chews_leaves" to 0.30,
                    "visible_caterpillar" to 0.60,
                    "nocturnal" to 0.50
                )
            ),
            PestCandidate(
                id = "leaf_miner",
                name = "Minador de Hojas",
                scientificName = "Liriomyza spp.",
                probability = 0.0,
                attributes = mapOf(
                    "host_tomato" to 0.80,
                    "leaf_mines" to 0.95,
                    "small_holes_in_leaves" to 0.85,
                    "high_reproduction" to 0.90,
                    "larva_small_and_green" to 0.80,
                    "polyphagous" to 0.85,
                    "chews_leaves" to 0.20
                )
            ),
            PestCandidate(
                id = "whitefly",
                name = "Mosca Blanca",
                scientificName = "Bemisia tabaci",
                probability = 0.0,
                attributes = mapOf(
                    "host_tomato" to 0.85,
                    "sucking_insect" to 0.95,
                    "white_tiny_insects_on_underside" to 0.95,
                    "honeydew_exudate" to 0.90,
                    "transmits_viruses" to 0.85,
                    "high_reproduction" to 0.90,
                    "polyphagous" to 0.80,
                    "chews_leaves" to 0.05
                )
            ),
            PestCandidate(
                id = "aphid",
                name = "Pulgón",
                scientificName = "Aphididae",
                probability = 0.0,
                attributes = mapOf(
                    "sucking_insect" to 0.95,
                    "honeydew_exudate" to 0.85,
                    "transmits_viruses" to 0.70,
                    "high_reproduction" to 0.95,
                    "larva_small_and_green" to 0.60,
                    "polyphagous" to 0.90,
                    "chews_leaves" to 0.05,
                    "white_tiny_insects_on_underside" to 0.20
                )
            ),
            PestCandidate(
                id = "helicoverpa",
                name = "Gusano del Fruto",
                scientificName = "Helicoverpa armigera",
                probability = 0.0,
                attributes = mapOf(
                    "host_tomato" to 0.70,
                    "chews_flowers_and_fruits" to 0.95,
                    "polyphagous" to 0.90,
                    "visible_caterpillar" to 0.85,
                    "nocturnal" to 0.75,
                    "chews_leaves" to 0.60,
                    "frass_present" to 0.70
                )
            )
        )
    }

    private fun showNextQuestion() {
        if (akinator.shouldStop()) {
            showResult()
            return
        }

        currentQuestionId = akinator.chooseBestQuestion()

        if (currentQuestionId == null) {
            showResult()
            return
        }

        updateUI()
    }

    private fun handleAnswer(answer: Boolean) {
        currentQuestionId?.let { questionId ->
            akinator.updateWithAnswer(questionId, answer)
            showNextQuestion()
        }
    }

    private fun updateUI() {
        val questionsCount = akinator.getAskedQuestionsCount()
        val questionText = currentQuestionId?.let { akinator.getQuestionText(it) } ?: ""

        // Actualizar contador de pasos
        tvStepCounter.text = "Pregunta ${questionsCount + 1} de $maxQuestions"

        // Actualizar barra de progreso
        val progress = ((questionsCount + 1).toFloat() / maxQuestions * 100).toInt()
        progressBar.progress = progress

        // Actualizar pregunta
        tvImageQuestion.text = questionText

        // Opcional: Log para debug
        val topCandidate = akinator.getTopCandidates(1).firstOrNull()
        topCandidate?.let { (pest, prob) ->
            android.util.Log.d("PestAkinator",
                "Top: ${pest.name} (${(prob * 100).toInt()}%), Entropía: ${akinator.getCurrentEntropy()}")
        }
    }

    private fun showResult() {
        val bestCandidate = akinator.getBestCandidate()

        if (bestCandidate != null) {
            val (pest, probability) = bestCandidate

            // Navegar a InformationBugActivity con los datos de la plaga
            val intent = Intent(this, InformationBugActivity::class.java)
            intent.putExtra("PEST_ID", pest.id)
            intent.putExtra("PEST_NAME", pest.name)
            intent.putExtra("PEST_SCIENTIFIC_NAME", pest.scientificName)
            intent.putExtra("PEST_PROBABILITY", probability)
            intent.putExtra("FROM_AKINATOR", true)
            startActivity(intent)
            finish()
        } else {
            // No se pudo identificar
            showNoResultDialog()
        }
    }

    private fun showNoResultDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("No se pudo identificar")
            .setMessage("No se pudo identificar la plaga con suficiente confianza. ¿Deseas intentar de nuevo?")
            .setPositiveButton("Reintentar") { _, _ ->
                recreate()
            }
            .setNegativeButton("Volver") { _, _ ->
                finish()
            }
            .show()
    }
}