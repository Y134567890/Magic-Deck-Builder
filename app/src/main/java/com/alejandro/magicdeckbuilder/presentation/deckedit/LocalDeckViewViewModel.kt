package com.alejandro.magicdeckbuilder.presentation.deckedit

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alejandro.magicdeckbuilder.data.local.LocalCardInDeckData
import com.alejandro.magicdeckbuilder.data.local.LocalDeck
import com.alejandro.magicdeckbuilder.data.local.LocalDeckManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File // Importar File para rutas de imagen locales

/**
 * Estado de la UI para la pantalla LocalDeckViewScreen.
 * Contiene todos los datos y estados que la UI necesita para renderizarse.
 */
data class LocalDeckViewUiState(
    val currentDeck: LocalDeck? = null, // El mazo local que se está visualizando.
    val isLoading: Boolean = false, // Indica si se está cargando el mazo.
    val errorMessage: String? = null, // Mensaje de error a mostrar, si lo hay.
    val selectedCardInLocalDeckId: String? = null, // ID de la carta seleccionada en el mazo, para mostrar su detalle.
    val selectedCardInLocalDeckImagePath: String? = null, // Ruta de imagen local de la carta seleccionada.
    val sortOption: SortOption = SortOption.CMC, // Opción de ordenación actual para las cartas del mazo. Por defecto: CMC.
    val showStatsDialog: Boolean = false // Indica si el diálogo de estadísticas del mazo debe mostrarse.
)

/**
 * ViewModel para la pantalla LocalDeckViewScreen.
 * Gestiona la lógica de negocio y el estado de la UI para visualizar un mazo local.
 *
 * @param application La instancia de la aplicación, necesaria para inicializar LocalDeckManager.
 * @param localDeckManager El gestor para interactuar con los datos de mazos locales.
 */
class LocalDeckViewViewModel(
    private val application: Application,
    private val localDeckManager: LocalDeckManager
) : ViewModel() {

    init {
        Log.d("LocalDeckViewViewModel", "LocalDeckViewViewModel instance created: ${this.hashCode()}")
    }

    // MutableStateFlow privado para gestionar el estado de la UI.
    private val _uiState = MutableStateFlow(LocalDeckViewUiState())
    // StateFlow público de solo lectura que expone el estado de la UI a la Composable.
    val uiState: StateFlow<LocalDeckViewUiState> = _uiState.asStateFlow()

    // MutableStateFlow privado para las estadísticas del mazo.
    private val _localDeckStats = MutableStateFlow(DeckStats())
    // StateFlow público de solo lectura para las estadísticas.
    val localDeckStats: StateFlow<DeckStats> = _localDeckStats.asStateFlow()

    // Variable para mantener una referencia al Job de carga del mazo, permitiendo cancelarlo.
    private var loadDeckJob: Job? = null

    /**
     * Este StateFlow observa los cambios en `currentDeck` dentro de `_uiState`
     * y emite el mazo actual cada vez que cambia.
     * Se usa `SharingStarted.Eagerly` para que empiece a emitir inmediatamente
     * y `initialValue = null` para el estado inicial.
     */
    val currentLocalDeck: StateFlow<LocalDeck?> = _uiState.map { it.currentDeck }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    init {
        // Observa el mazo actual y recalcula las estadísticas cada vez que el mazo cambia.
        viewModelScope.launch {
            currentLocalDeck.collect { deck ->
                if (deck != null) {
                    calculateLocalDeckStats(deck)
                }
            }
        }
    }


    /**
     * Se llama cuando el ViewModel está a punto de ser destruido.
     * Se utiliza para cancelar cualquier operación de carga pendiente para evitar fugas de memoria.
     */
    override fun onCleared() {
        super.onCleared()
        loadDeckJob?.cancel()
        Log.d("LocalDeckViewViewModel", "LocalDeckViewViewModel cleared: ${this.hashCode()}.")
    }

    /**
     * Carga un mazo local específico por su ID.
     * Actualiza el estado de la UI con el mazo cargado o un mensaje de error si falla.
     *
     * @param deckId El ID del mazo local a cargar.
     */
    fun loadLocalDeckForView(deckId: String) {
        Log.d("LocalDeckViewViewModel", "loadLocalDeckForView($deckId): Loading local deck.")
        loadDeckJob?.cancel() // Cancela cualquier carga anterior para evitar condiciones de carrera.
        _uiState.update { it.copy(isLoading = true, errorMessage = null) } // Establece el estado de carga y limpia errores.

        loadDeckJob = viewModelScope.launch {
            try {
                // localDeckManager.loadLocalDecks() devuelve List<LocalDeck>
                // Se busca el mazo específico por su ID dentro de la lista de todos los mazos locales.
                val allLocalDecks = localDeckManager.loadLocalDecks()
                val deck = allLocalDecks.find { it.id == deckId }

                if (deck != null) {
                    // Si el mazo se encuentra, actualiza el estado con el mazo.
                    _uiState.update {
                        it.copy(
                            currentDeck = deck,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    Log.d("LocalDeckViewViewModel", "loadLocalDeckForView($deckId): Mazo local cargado: ${deck.name}")
                } else {
                    // Si el mazo no se encuentra, actualiza el estado con un mensaje de error.
                    val msg = "Mazo local no encontrado: $deckId"
                    _uiState.update { it.copy(errorMessage = msg, isLoading = false) }
                    Log.w("LocalDeckViewViewModel", "loadLocalDeckForView($deckId): $msg")
                }
            } catch (e: Exception) {
                // Captura cualquier excepción durante la carga y actualiza el estado con el error.
                val msg = "Error al cargar mazo local: ${e.message}"
                _uiState.update { it.copy(errorMessage = msg, isLoading = false) }
                Log.e("LocalDeckViewViewModel", "loadLocalDeckForView($deckId) ERROR: $msg", e)
            }
        }
    }

    /**
     * Selecciona una carta en el mazo, actualizando el estado de la UI para mostrar sus detalles.
     *
     * @param cardId El ID de la carta a seleccionar.
     */
    fun selectCardInLocalDeck(cardId: String) {
        _uiState.update { currentState ->
            val cardData = currentState.currentDeck?.cards?.get(cardId) // Obtiene los datos de la carta.
            currentState.copy(
                selectedCardInLocalDeckId = cardId, // Establece el ID de la carta seleccionada.
                selectedCardInLocalDeckImagePath = cardData?.imagePath, // Establece la ruta de la imagen.
                errorMessage = null // Limpia cualquier mensaje de error.
            )
        }
    }

    /**
     * Deselecciona la carta actualmente visualizada, ocultando sus detalles.
     */
    fun deselectCardInLocalDeck() {
        _uiState.update { it.copy(selectedCardInLocalDeckId = null, selectedCardInLocalDeckImagePath = null, errorMessage = null) }
    }

    /**
     * Establece la opción de ordenación para las cartas del mazo.
     *
     * @param option La nueva opción de ordenación (CMC, COLORS, ALPHABETICAL).
     */
    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    /**
     * Este StateFlow calcula y emite la lista de cartas del mazo actual, ordenadas
     * según la `sortOption` seleccionada en el `_uiState`.
     * Las cartas se emiten como una lista de pares (ID de carta, datos de la carta).
     */
    val sortedCardsInLocalDeck: StateFlow<List<Pair<String, LocalCardInDeckData>>> = _uiState
        .map { uiState ->
            val cardsMap = uiState.currentDeck?.cards ?: emptyMap() // Obtiene las cartas del mazo actual.
            val sortedList = when (uiState.sortOption) {
                SortOption.CMC -> cardsMap.entries.sortedWith(compareBy<Map.Entry<String, LocalCardInDeckData>> { entry ->
                    val cardData = entry.value
                    // Las tierras se colocan al final al ordenar por CMC.
                    val isLand = cardData.type?.contains("Land", ignoreCase = true) == true
                    if (isLand) Double.MAX_VALUE else (cardData.cmc ?: 0.0) // CMC nulo o de tierra va al final.
                }.thenBy { it.value.name }) // Luego ordena alfabéticamente por nombre si los CMC son iguales.

                SortOption.COLORS -> cardsMap.entries.sortedWith(compareBy<Map.Entry<String, LocalCardInDeckData>> { entry ->
                    val cardData = entry.value
                    val manaCost = cardData.manaCost
                    val type = cardData.type

                    // Función de extensión para verificar si la carta tiene un símbolo de color específico en su coste de maná.
                    fun LocalCardInDeckData.hasColorSymbol(colorSymbol: String): Boolean {
                        return this.manaCost?.contains("{$colorSymbol}", ignoreCase = true) == true
                    }

                    // Función de extensión para verificar si la carta tiene símbolos de maná híbrido.
                    fun LocalCardInDeckData.hasHybridManaSymbol(): Boolean {
                        return this.manaCost?.contains("""\{[WUBRG]/[WUBRG]\}""".toRegex()) == true
                    }

                    // Función de extensión para determinar si una carta es "realmente" incolora por su coste de maná.
                    // Excluye tierras porque no tienen coste de maná de color.
                    fun LocalCardInDeckData.isTrulyColorlessByManaCost(): Boolean {
                        val hasAnyColorSymbol = this.hasColorSymbol("W") ||
                                this.hasColorSymbol("U") ||
                                this.hasColorSymbol("B") ||
                                this.hasColorSymbol("R") ||
                                this.hasColorSymbol("G")

                        return !hasAnyColorSymbol && !(type?.contains("Land", ignoreCase = true) == true)
                    }

                    // Orden de los símbolos de color para la clasificación monocolor.
                    val colorSymbolsOrder = listOf("W", "G", "R", "B", "U")
                    val isLand = type?.contains("Land", ignoreCase = true) == true
                    val cardColorsFound = colorSymbolsOrder.filter { cardData.hasColorSymbol(it) }
                    val isMulticolor = cardColorsFound.size > 1 || cardData.hasHybridManaSymbol()
                    val isMonocolor = cardColorsFound.size == 1 && !cardData.hasHybridManaSymbol()
                    val isColorless = cardData.isTrulyColorlessByManaCost()

                    // Asigna una prioridad de ordenación basada en el tipo de color de la carta.
                    val sortPriority = when {
                        isLand -> 7 // Las tierras van al final.
                        isColorless -> 6
                        isMulticolor -> 5
                        isMonocolor -> {
                            // Para monocolor, usa el índice del primer símbolo de color en la lista predefinida.
                            val primarySymbol = cardColorsFound.first()
                            colorSymbolsOrder.indexOf(primarySymbol)
                        }
                        else -> {
                            // Caso de respaldo para cartas que no encajan en las categorías de color esperadas.
                            if (manaCost.isNullOrBlank()) {
                                6 // Si no tiene coste de maná, se considera incolora (similar).
                            } else {
                                Log.w("LocalDeckViewViewModel", "sortedCardsInLocalDeck: Carta '${cardData.name}' con manaCost '${manaCost}' no encaja en las categorías de color. Asignando prioridad baja.")
                                99 // Prioridad muy alta para que aparezcan al final.
                            }
                        }
                    }
                    sortPriority
                }.thenBy { it.value.name }) // Luego ordena alfabéticamente por nombre.

                SortOption.ALPHABETICAL -> cardsMap.entries.sortedBy { it.value.name } // Ordena solo alfabéticamente por nombre.
            }
            sortedList.map { it.key to it.value } // Convierte de Map.Entry a Pair.
        }.stateIn(
            scope = viewModelScope,
            // Emite mientras haya al menos un observador activo, y lo mantiene durante 5 segundos
            // después de que el último observador desaparezca antes de detener la recolección.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList() // Valor inicial.
        )

    /**
     * Calcula y actualiza las estadísticas del mazo (total de cartas, distribución por CMC, tipo y color, CMC promedio).
     *
     * @param deck El mazo local para el cual calcular las estadísticas.
     */
    fun calculateLocalDeckStats(deck: LocalDeck) {
        val cards = deck.cards.values // Obtiene la lista de cartas del mazo.

        val totalCards = cards.sumOf { it.quantity } // Suma la cantidad de todas las cartas.

        // Inicializa el mapa de distribución de CMC con rangos de 0 a 6 y un bucket "7+".
        val cmcDistribution = mutableMapOf<Int, Int>().apply {
            for (i in 0..6) put(i, 0)
            put(7, 0) // Para CMC 7 o más.
        }
        var totalCmcSum = 0.0
        var totalCmcCards = 0

        cards.forEach { card ->
            val cmc = card.cmc?.toInt() ?: 0
            // Excluye las tierras del cálculo de CMC y de la curva de maná.
            if (card.type?.contains("Land", ignoreCase = true) != true) {
                totalCmcSum += cmc * card.quantity
                totalCmcCards += card.quantity

                if (cmc >= 7) {
                    cmcDistribution[7] = cmcDistribution.getOrDefault(7, 0) + card.quantity
                } else {
                    cmcDistribution[cmc] = cmcDistribution.getOrDefault(cmc, 0) + card.quantity
                }
            }
        }

        val cmcAvg = if (totalCmcCards > 0) totalCmcSum / totalCmcCards else 0.0 // Calcula el CMC promedio.

        val typeDistribution = mutableMapOf<String, Int>()
        cards.forEach { card ->
            val quantity = card.quantity
            val type = card.type ?: "Desconocido" // Tipo de carta, "Desconocido" si es nulo.

            // Clasifica las cartas por tipo principal (Instantáneo, Conjuro, Criatura, etc.).
            if (type.contains("Instant", ignoreCase = true)) {
                typeDistribution["Instantáneo"] = typeDistribution.getOrDefault("Instantáneo", 0) + quantity
            } else if (type.contains("Sorcery", ignoreCase = true)) {
                typeDistribution["Conjuro"] = typeDistribution.getOrDefault("Conjuro", 0) + quantity
            } else if (type.contains("Creature", ignoreCase = true)) {
                typeDistribution["Criatura"] = typeDistribution.getOrDefault("Criatura", 0) + quantity
            } else if (type.contains("Artifact", ignoreCase = true)) {
                typeDistribution["Artefacto"] = typeDistribution.getOrDefault("Artefacto", 0) + quantity
            } else if (type.contains("Enchantment", ignoreCase = true)) {
                typeDistribution["Encantamiento"] = typeDistribution.getOrDefault("Encantamiento", 0) + quantity
            } else if (type.contains("Planeswalker", ignoreCase = true)) {
                typeDistribution["Planeswalker"] = typeDistribution.getOrDefault("Planeswalker", 0) + quantity
            } else if (type.contains("Land", ignoreCase = true)) {
                typeDistribution["Tierra"] = typeDistribution.getOrDefault("Tierra", 0) + quantity
            } else {
                // Si no encaja en los tipos principales, intenta extraer el último componente del tipo (ej. "Legendary Creature" -> "Creature").
                val parts = type.split(" ").map { it.trim() }.filter { it.isNotBlank() }
                val baseType = parts.lastOrNull() ?: "Otro"
                typeDistribution[baseType] = typeDistribution.getOrDefault(baseType, 0) + quantity
            }
        }

        val colorDistribution = mutableMapOf<String, Int>()
        cards.forEach { card ->
            val quantity = card.quantity
            val isLand = card.type?.contains("Land", ignoreCase = true) == true
            if (isLand) {
                return@forEach // Las tierras no contribuyen a la distribución de colores de maná.
            }

            // Verifica la presencia de cada símbolo de maná de color.
            val hasWhite = card.manaCost?.contains("{W}", ignoreCase = true) == true
            val hasBlue = card.manaCost?.contains("{U}", ignoreCase = true) == true
            val hasBlack = card.manaCost?.contains("{B}", ignoreCase = true) == true
            val hasRed = card.manaCost?.contains("{R}", ignoreCase = true) == true
            val hasGreen = card.manaCost?.contains("{G}", ignoreCase = true) == true
            val hasAnyColor = hasWhite || hasBlue || hasBlack || hasRed || hasGreen

            val isColorless = !hasAnyColor // Una carta es incolora si no tiene símbolos de maná de color.
            val isMulticolor = listOf(hasWhite, hasBlue, hasBlack, hasRed, hasGreen).count { it } > 1 // Es multicolor si tiene más de un color.

            when {
                isColorless -> {
                    colorDistribution["Incoloras"] = colorDistribution.getOrDefault("Incoloras", 0) + quantity
                }
                isMulticolor -> {
                    colorDistribution["Multicolor"] = colorDistribution.getOrDefault("Multicolor", 0) + quantity
                }
                else -> {
                    // Si es monocolor, suma a la categoría de su color específico.
                    if (hasWhite) colorDistribution["Blanco"] = colorDistribution.getOrDefault("Blanco", 0) + quantity
                    if (hasBlue) colorDistribution["Azul"] = colorDistribution.getOrDefault("Azul", 0) + quantity
                    if (hasBlack) colorDistribution["Negro"] = colorDistribution.getOrDefault("Negro", 0) + quantity
                    if (hasRed) colorDistribution["Rojo"] = colorDistribution.getOrDefault("Rojo", 0) + quantity
                    if (hasGreen) colorDistribution["Verde"] = colorDistribution.getOrDefault("Verde", 0) + quantity
                }
            }
        }

        // Actualiza el StateFlow de las estadísticas con los nuevos valores calculados.
        _localDeckStats.value = DeckStats(
            totalCards = totalCards,
            cmcDistribution = cmcDistribution,
            typeDistribution = typeDistribution,
            colorDistribution = colorDistribution,
            cmcAvg = cmcAvg
        )
        Log.d("LocalDeckViewViewModel", "calculateLocalDeckStats: Estadísticas recalculadas. Total Cartas: $totalCards, CMC Avg: $cmcAvg")
    }

    /**
     * Muestra el diálogo de estadísticas del mazo.
     */
    fun showStatsDialog() {
        _uiState.update { it.copy(showStatsDialog = true) }
    }

    /**
     * Oculta el diálogo de estadísticas del mazo.
     */
    fun dismissStatsDialog() {
        _uiState.update { it.copy(showStatsDialog = false) }
    }

    /**
     * Limpia cualquier mensaje de error actual.
     */
    fun dismissErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * Factory para crear instancias de LocalDeckViewViewModel.
 * Es necesario porque el ViewModel requiere el contexto de la aplicación para inicializar LocalDeckManager.
 */
class LocalDeckViewViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Verifica si la clase del modelo es asignable a LocalDeckViewViewModel.
        if (modelClass.isAssignableFrom(LocalDeckViewViewModel::class.java)) {
            // Crea una instancia de LocalDeckManager, pasándole el contexto de la aplicación.
            val localDeckManager = LocalDeckManager(application.applicationContext)
            // Instancia y devuelve el LocalDeckViewViewModel.
            @Suppress("UNCHECKED_CAST") // Suprime la advertencia de casting inseguro.
            return LocalDeckViewViewModel(application, localDeckManager) as T
        }
        // Lanza una excepción si se intenta crear una clase de ViewModel desconocida.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}