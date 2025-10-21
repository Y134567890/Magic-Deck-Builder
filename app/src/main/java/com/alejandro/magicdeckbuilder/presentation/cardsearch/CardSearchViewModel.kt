package com.alejandro.magicdeckbuilder.presentation.cardsearch

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alejandro.magicdeckbuilder.api.MtgApi
import com.alejandro.magicdeckbuilder.api.ScryfallCardSearchResponse
import com.alejandro.magicdeckbuilder.data.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Modelo de datos que representa el estado completo de la interfaz de usuario de la pantalla de búsqueda de cartas.
 * Este objeto es inmutable, y cada cambio en la UI se refleja creando una nueva instancia con los datos actualizados.
 *
 * @param searchText El texto actual en el campo de búsqueda.
 * @param cards La lista de cartas que se muestran en la UI.
 * @param isLoading Booleano que indica si se está realizando una operación de carga (ej. búsqueda de cartas).
 * @param errorMessage Mensaje de error a mostrar al usuario si ocurre un problema. Nulo si no hay error.
 * @param currentPage La página actual de resultados que se está mostrando/solicitando a la API.
 * @param canLoadMore Booleano que indica si hay más resultados disponibles para cargar (para paginación infinita).
 * @param showFilterDialog Booleano que controla la visibilidad del diálogo de filtros.
 * @param selectedCard La carta que está actualmente seleccionada (ej. para mostrar el diálogo de carta grande).
 * @param filters Objeto [Filters] que contiene los filtros de búsqueda aplicados actualmente.
 * @param hasSearched Booleano para impedir que se muestre el texto correspondiente a no encontrar cartas antes de realizar una búsqueda.
 */
data class CardSearchUiState(
    val searchText: String = "",
    val cards: List<Card> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val canLoadMore: Boolean = true,
    val showFilterDialog: Boolean = false,
    val selectedCard: Card? = null,
    val filters: Filters = Filters(),
    val hasSearched: Boolean = false
)

/**
 * Modelo de datos que contiene todos los filtros que el usuario puede aplicar a la búsqueda de cartas.
 *
 * @param cmc Coste de Maná Convertido. Null si no se aplica el filtro.
 * @param white, green, red, black, blue, colorless. Booleanos para filtrar por colores de identidad.
 * @param isLand, isCreature, etc. Booleanos para filtrar por tipos de carta.
 * @param power, toughness. Valores para filtrar por fuerza y resistencia de criaturas. Null si no se aplican.
 */
data class Filters(
    val cmc: Int? = null,
    val white: Boolean = false,
    val green: Boolean = false,
    val red: Boolean = false,
    val black: Boolean = false,
    val blue: Boolean = false,
    val colorless: Boolean = false,
    val isLand: Boolean = false,
    val isCreature: Boolean = false,
    val power: Int? = null,
    val toughness: Int? = null,
    val isEnchantment: Boolean = false,
    val isSorcery: Boolean = false,
    val isInstant: Boolean = false,
    val isPlaneswalker: Boolean = false,
    val isArtifact: Boolean = false
) {
    /**
     * Convierte los filtros seleccionados en una cadena de consulta compatible con la API de Scryfall.
     * Scryfall tiene una sintaxis de consulta específica para sus filtros (ej. "t:creature", "id:w", "mv=3").
     *
     * @return Una cadena de consulta formateada para Scryfall.
     */
    fun toScryfallQuery(): String {
        val queryParts = mutableListOf<String>() // Lista para construir las partes de la consulta

        // --- Lógica de filtrado de colores ---
        val colorsSelected = mutableListOf<String>()
        if (white) colorsSelected.add("w")
        if (blue) colorsSelected.add("u")
        if (black) colorsSelected.add("b")
        if (red) colorsSelected.add("r")
        if (green) colorsSelected.add("g")

        if (colorsSelected.isNotEmpty()) {
            // Si se selecciona al menos un color específico (W, U, B, R, G),
            // busca cartas que contengan esos colores.
            queryParts.add("id>=" + colorsSelected.sorted().joinToString(""))
        }

        // Si el usuario seleccionó el filtro de incoloro, se incluirá en la búsqueda
        if (colorless) {
            queryParts.add("id:c")
        } else if (colorsSelected.isNotEmpty()) {
            // Si no marcó incoloro y sí seleccionó algún color, excluímos incoloro de la búsqueda
            queryParts.add("-id:c")
        }

        // --- CMC (Mana Value / Coste de Maná Convertido) ---
        cmc?.let {
            if (it == 8) {
                // Para CMC 8, Scryfall usa 'mv>=8' para significar 8 o más.
                queryParts.add("mv>=8")
            } else {
                // Para otros valores, se usa 'mv=X'.
                queryParts.add("mv=$it")
            }
        }

        // --- Tipos de carta ---
        val types = mutableListOf<String>()
        if (isCreature) types.add("creature")
        if (isLand) types.add("land")
        if (isEnchantment) types.add("enchantment")
        if (isInstant) types.add("instant")
        if (isSorcery) types.add("sorcery")
        if (isPlaneswalker) types.add("planeswalker")
        if (isArtifact) types.add("artifact")

        if (types.isNotEmpty()) {
            // Si hay tipos seleccionados, los combina con 'OR' y el prefijo 't:'.
            queryParts.add(types.joinToString(" OR ") { "t:$it" })
        }

        // --- Power y Toughness (Fuerza y Resistencia) ---
        power?.let { queryParts.add("pow=$it") } // 'pow=X' para fuerza igual a X
        toughness?.let { queryParts.add("tou=$it") } // 'tou=X' para resistencia igual a X

        // Une todas las partes de la consulta con un espacio.
        return queryParts.joinToString(" ")
    }
}

/**
 * [ViewModel] para la pantalla de búsqueda de cartas.
 * Gestiona el estado de la UI ([CardSearchUiState]), interactúa con la API de Scryfall
 * para buscar cartas y aplica los filtros definidos por el usuario.
 */
class CardSearchViewModel : ViewModel() {

    // MutableStateFlow que mantiene el estado actual de la UI de la pantalla de búsqueda.
    // Los cambios en _uiState se reflejarán automáticamente en la UI que lo observe.
    private val _uiState = MutableStateFlow(CardSearchUiState())

    // Exposición de _uiState como un StateFlow de solo lectura para la UI.
    val uiState: StateFlow<CardSearchUiState> = _uiState.asStateFlow()

    // Variables internas para mantener el término de búsqueda y los filtros aplicados
    // entre las llamadas a la API, especialmente útil para la paginación.
    private var currentSearchTerm: String = ""
    private var currentFilters: Filters = Filters()

    /**
     * Actualiza el texto de búsqueda en el estado de la UI.
     * @param text El nuevo texto de búsqueda.
     */
    fun onSearchTextChanged(text: String) {
        _uiState.update { it.copy(searchText = text) }
    }

    /**
     * Inicia una nueva búsqueda de cartas.
     * Reinicia la lista de cartas, la página actual y los indicadores de carga.
     * Captura el texto de búsqueda y los filtros actuales para la búsqueda.
     */
    fun onSearch() {
        currentSearchTerm = _uiState.value.searchText // Captura el texto actual del UI State
        currentFilters = _uiState.value.filters // Captura los filtros actuales del UI State

        // Reinicia el estado de la UI para una nueva búsqueda:
        // - Vacía la lista de cartas.
        // - Resetea la página a 1.
        // - Permite cargar más (asume que habrá resultados al principio).
        // - Establece isLoading a true.
        // - Limpia cualquier mensaje de error previo.
        _uiState.update {
            it.copy(
                cards = emptyList(),
                currentPage = 1,
                canLoadMore = true,
                isLoading = true,
                errorMessage = null,
                hasSearched = true
            )
        }
        searchCards() // Llama a la función privada que realiza la búsqueda real.
    }

    /**
     * Intenta cargar más cartas si hay más disponibles y no se está cargando ya.
     * Incrementa la página actual y luego realiza otra búsqueda.
     */
    fun loadMoreCards() {
        // Solo carga más si hay más páginas disponibles Y no se está cargando ya.
        if (_uiState.value.canLoadMore && !_uiState.value.isLoading) {
            // Incrementa el número de página y establece isLoading a true.
            _uiState.update { it.copy(currentPage = it.currentPage + 1, isLoading = true) }
            searchCards() // Realiza la búsqueda para la siguiente página.
        }
    }

    /**
     * Función privada que realiza la llamada a la API de Scryfall para buscar cartas.
     * Se ejecuta dentro de una corrutina en el `viewModelScope`.
     */
    private fun searchCards() {
        // viewModelScope.launch lanza una corrutina que está vinculada al ciclo de vida del ViewModel.
        // Se cancelará automáticamente cuando el ViewModel sea destruido, evitando fugas de memoria.
        viewModelScope.launch {
            // Establece isLoading a true y limpia cualquier mensaje de error al inicio de la búsqueda.
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Combina el término de búsqueda de texto con la consulta de filtros.
                // .trim() elimina espacios en blanco al principio o al final de la cadena combinada.
                val filterQuery = currentFilters.toScryfallQuery()

                val finalQuery = if (currentSearchTerm.isNotBlank()) {
                    "$currentSearchTerm $filterQuery".trim()
                } else {
                    filterQuery.trim() // Si no hay término de búsqueda, solo usa los filtros.
                }

                // Validación: Si la consulta final está vacía, no tiene sentido hacer la llamada a la API.
                // Esto podría ocurrir si el usuario no ha introducido texto y no ha aplicado filtros.
                if (finalQuery.isBlank()) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Introduce un término de búsqueda o aplica filtros.",
                            isLoading = false,
                            cards = emptyList(),
                            canLoadMore = false
                        )
                    }
                    // return@launch sale de la corrutina actual.
                    return@launch
                }

                // Realiza la llamada a la API de Scryfall para buscar cartas.
                // MtgApi.retrofitService es la interfaz generada por Retrofit.
                val response: ScryfallCardSearchResponse = MtgApi.retrofitService.searchCards(
                    query = finalQuery, // La consulta combinada (texto + filtros)
                    page = _uiState.value.currentPage // El número de página actual para la paginación
                )

                val receivedCards =
                    response.data // Extrae la lista de cartas del cuerpo de la respuesta.

                // --- Manejo de resultados de la API ---
                if (receivedCards.isEmpty()) {
                    if (_uiState.value.currentPage == 1) {
                        // Caso 1: No se encontraron cartas en la primera página.
                        // Significa que la búsqueda no arrojó ningún resultado.
                        _uiState.update {
                            it.copy(
                                cards = emptyList(), // Asegura que la lista de cartas esté vacía.
                                errorMessage = "No se encontraron cartas que coincidan con la búsqueda.",
                                canLoadMore = false, // No hay más cartas, por lo tanto, no se puede cargar más.
                                isLoading = false // La carga ha terminado.
                            )
                        }
                    } else {
                        // Caso 2: No hay más cartas en las páginas siguientes (la búsqueda en una página posterior no devolvió nada).
                        // Esto indica el final de la paginación.
                        _uiState.update {
                            it.copy(
                                canLoadMore = false, // No hay más páginas disponibles.
                                isLoading = false // La carga ha terminado.
                            )
                        }
                    }
                } else {
                    // Caso 3: Se recibieron cartas.
                    // Filtrar por cartas que tienen una URL de imagen.
                    // Aunque Scryfall casi siempre proporciona imágenes, es una buena práctica.
                    val cardsWithImages = receivedCards.filter { it.imageUrl != null }

                    // Obtiene la lista actual de cartas del estado de la UI y la convierte en mutable.
                    val currentCards = _uiState.value.cards.toMutableList()
                    // Añade las nuevas cartas recibidas (y filtradas por imagen) a la lista actual.
                    currentCards.addAll(cardsWithImages)

                    // Actualiza el estado de la UI con la lista de cartas combinada.
                    _uiState.update {
                        it.copy(
                            cards = currentCards, // La lista de cartas actualizada.
                            // Usa el campo `has_more` de la respuesta de Scryfall
                            // para saber si hay más páginas disponibles.
                            canLoadMore = response.has_more,
                            isLoading = false, // La carga ha terminado.
                            errorMessage = null // Limpia cualquier mensaje de error previo si la búsqueda fue exitosa.
                        )
                    }
                }
            } catch (e: Exception) {
                // Manejo de errores: Si ocurre una excepción durante la llamada a la API (ej. sin conexión).
                // Establece el mensaje de error y desactiva el indicador de carga.
                // Importante: Se ha optado por no mostrar un posible mensaje de error al usuario que le haga
                // pensar que hay algún problema en la app. De este modo, se mostrará únicamente que no se
                // han encontrado cartas, de forma que el usuario pueda hacer otra búsqueda sin tener que
                // preocuparse.
                _uiState.update { it.copy(errorMessage = null, isLoading = false) }
                Log.e("CardSearchViewModel", "Error buscando cartas: ${e.message}", e)
            }
        }
    }

    /**
     * Controla la visibilidad del diálogo de filtros.
     * @param show Booleano: `true` para mostrar el diálogo, `false` para ocultarlo.
     */
    fun showFilterDialog(show: Boolean) {
        _uiState.update { it.copy(showFilterDialog = show) }
    }

    /**
     * Aplica nuevos filtros a la búsqueda.
     * Actualiza los filtros en el estado de la UI, oculta el diálogo de filtros y reinicia la búsqueda.
     * @param newFilters El nuevo objeto [Filters] con los filtros seleccionados.
     */
    fun applyFilters(newFilters: Filters) {
        // Actualiza los filtros en el estado de la UI y oculta el diálogo.
        _uiState.update { it.copy(filters = newFilters, showFilterDialog = false) }
        currentFilters = newFilters // Actualiza la variable interna de filtros.
        // Reinicia el estado para una nueva búsqueda con los filtros aplicados.
        _uiState.update {
            it.copy(
                cards = emptyList(),
                currentPage = 1,
                canLoadMore = true,
                isLoading = true,
                errorMessage = null
            )
        }
        onSearch() // Vuelve a buscar cartas con los nuevos filtros.
    }

    /**
     * Limpia todos los filtros aplicados.
     * Reinicia los filtros a su estado por defecto y reinicia la búsqueda.
     */
    fun clearFilters() {
        // Restablece los filtros a una instancia nueva (vacía) de Filters.
        _uiState.update { it.copy(filters = Filters()) }
        currentFilters = Filters() // Actualiza la variable interna.
        // Reinicia el estado para una nueva búsqueda sin filtros.
        _uiState.update {
            it.copy(
                cards = emptyList(),
                currentPage = 1,
                canLoadMore = true,
                isLoading = true,
                errorMessage = null
            )
        }
        onSearch() // Vuelve a buscar cartas sin filtros.
    }

    /**
     * Establece la carta actualmente seleccionada en el estado de la UI.
     * Esto se usa para mostrar el diálogo de carta grande con los detalles de la carta.
     * @param card La [Card] seleccionada, o `null` para deseleccionar.
     */
    fun selectCard(card: Card?) {
        _uiState.update { it.copy(selectedCard = card) }
    }
}