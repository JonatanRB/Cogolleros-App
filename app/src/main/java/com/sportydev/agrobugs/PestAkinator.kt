package com.sportydev.agrobugs

import com.sportydev.agrobugs.PestCandidate
import com.sportydev.agrobugs.Question

import kotlin.math.log2

class PestAkinator (private val pests: List<PestCandidate>) {

    private val questions = mapOf(
        "host_maize" to "¿El cultivo afectado es maíz?",
        "chews_leaves" to "¿Ves signos de masticación (bordes irregulares, agujeros grandes)?",
        "frass_present" to "¿Hay excremento (frass) visible cerca de las hojas o cogollos?",
        "nocturnal" to "¿El daño parece más activo durante la noche?",
        "larva_visible_on_whorl" to "¿Ves orugas dentro del cogollo/espiral de la planta?",
        "causes_dead_hearts" to "¿Las plantas presentan 'dead heart' (tallo central seco/roto)?",
        "larva_inside_stem" to "¿Hay larvas dentro del tallo?",
        "holes_in_stem" to "¿Notas agujeros en los tallos?",
        "host_tomato" to "¿El cultivo afectado es tomate?",
        "leaf_mines" to "¿Hay galerías o minas en las hojas?",
        "small_holes_in_leaves" to "¿Ves hoyitos pequeños en las hojas?",
        "high_reproduction" to "¿La plaga parece multiplicarse muy rápido?",
        "larva_small_and_green" to "¿Las larvas son pequeñas y de color verdoso?",
        "sucking_insect" to "¿El insecto parece succionar (hojas pegajosas, amarillamiento)?",
        "white_tiny_insects_on_underside" to "¿Hay insectos blancos muy pequeños en el envés de las hojas?",
        "honeydew_exudate" to "¿Notas melaza o residuo pegajoso en las hojas?",
        "transmits_viruses" to "¿Se observan síntomas de virus en plantas?",
        "chews_flowers_and_fruits" to "¿Se comen flores y frutos?",
        "polyphagous" to "¿Ataca muchos tipos de cultivos?",
        "visible_caterpillar" to "¿Ves orugas grandes y visibles?"
    )

    private val probs = mutableMapOf<String, Double>()
    private val askedQuestions = mutableSetOf<String>()

    init {
        // Inicializar probabilidades uniformes
        val uniformProb = 1.0 / pests.size
        pests.forEach { pest ->
            probs[pest.id] = uniformProb
        }
    }

    // Calcular entropía
    private fun entropy(probabilities: Map<String, Double>): Double {
        var e = 0.0
        probabilities.values.forEach { p ->
            if (p > 0) {
                e -= p * log2(p)
            }
        }
        return e
    }

    // Normalizar probabilidades
    private fun normalize(probs: Map<String, Double>): Map<String, Double> {
        val total = probs.values.sum()
        if (total == 0.0) {
            val uniform = 1.0 / probs.size
            return probs.keys.associateWith { uniform }
        }
        return probs.mapValues { it.value / total }
    }

    // Obtener probabilidad de atributo para una plaga
    private fun getAttributeProb(pestId: String, attribute: String): Double {
        val pest = pests.find { it.id == pestId } ?: return 0.5
        return pest.attributes[attribute] ?: 0.5
    }

    // Calcular entropía esperada después de una pregunta
    private fun expectedEntropyAfterQuestion(attribute: String): Double {
        var pYes = 0.0
        var pNo = 0.0

        probs.forEach { (pestId, prob) ->
            val attrProb = getAttributeProb(pestId, attribute)
            pYes += prob * attrProb
            pNo += prob * (1 - attrProb)
        }

        val postYes = mutableMapOf<String, Double>()
        val postNo = mutableMapOf<String, Double>()

        probs.forEach { (pestId, prob) ->
            val attrProb = getAttributeProb(pestId, attribute)
            postYes[pestId] = prob * attrProb
            postNo[pestId] = prob * (1 - attrProb)
        }

        val normalizedYes = normalize(postYes)
        val normalizedNo = normalize(postNo)

        val eYes = entropy(normalizedYes)
        val eNo = entropy(normalizedNo)

        return pYes * eYes + pNo * eNo
    }

    // Elegir la mejor pregunta
    fun chooseBestQuestion(): String? {
        var bestQuestion: String? = null
        var bestScore = Double.POSITIVE_INFINITY

        questions.keys.forEach { attr ->
            if (attr !in askedQuestions) {
                val expectedE = expectedEntropyAfterQuestion(attr)
                if (expectedE < bestScore) {
                    bestScore = expectedE
                    bestQuestion = attr
                }
            }
        }

        return bestQuestion
    }

    // Actualizar con respuesta
    fun updateWithAnswer(attribute: String, answer: Boolean) {
        val newProbs = mutableMapOf<String, Double>()

        probs.forEach { (pestId, prob) ->
            val attrProb = getAttributeProb(pestId, attribute)
            val likelihood = if (answer) attrProb else (1 - attrProb)
            newProbs[pestId] = prob * likelihood
        }

        probs.clear()
        probs.putAll(normalize(newProbs))
        askedQuestions.add(attribute)
    }

    // Obtener candidatos principales
    fun getTopCandidates(n: Int = 3): List<Pair<PestCandidate, Double>> {
        return probs.entries
            .sortedByDescending { it.value }
            .take(n)
            .mapNotNull { (id, prob) ->
                val pest = pests.find { it.id == id }
                pest?.let { it to prob }
            }
    }

    // Obtener mejor candidato
    fun getBestCandidate(): Pair<PestCandidate, Double>? {
        return getTopCandidates(1).firstOrNull()
    }

    // Verificar si debemos parar
    fun shouldStop(): Boolean {
        val bestProb = probs.values.maxOrNull() ?: 0.0
        return bestProb > 0.80 || askedQuestions.size >= 10
    }

    fun getQuestionText(questionId: String): String {
        return questions[questionId] ?: ""
    }

    fun getAskedQuestionsCount(): Int = askedQuestions.size

    fun getCurrentEntropy(): Double = entropy(probs)
}