package com.alejandro.magicdeckbuilder.presentation.deckedit

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alejandro.magicdeckbuilder.data.Card
import com.alejandro.magicdeckbuilder.data.models.CardInDeckData
import com.alejandro.magicdeckbuilder.data.models.Deck
import com.alejandro.magicdeckbuilder.presentation.navigation.CardResult
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Enumeración para las opciones de ordenación de las cartas dentro del mazo.
 */
enum class SortOption {
    CMC,          // Coste de Maná Convertido
    COLORS,       // Colores de la carta
    ALPHABETICAL  // Alfabéticamente por nombre
}

/**
 * Data class que representa las estadísticas calculadas de un mazo.
 * Es inmutable y se actualiza cada vez que se recalculan las estadísticas.
 */
data class DeckStats(
    val totalCards: Int = 0,                                // Número total de cartas en el mazo
    val cmcDistribution: Map<Int, Int> = emptyMap(),        // Distribución de cartas por CMC (CMC -> Cantidad)
    val typeDistribution: Map<String, Int> = emptyMap(),    // Distribución de cartas por Tipo (Tipo -> Cantidad)
    val colorDistribution: Map<String, Int> = emptyMap(),   // Distribución de cartas por Color (Color -> Cantidad)
    val cmcAvg: Double = 0.0                                // Coste de Maná Convertido promedio
)

/**
 * Data class que representa el estado de la UI de la pantalla de edición de mazos.
 * Contiene todos los datos y flags necesarios para que la UI se renderice correctamente.
 */
data class DeckEditUiState(
    val deck: Deck = Deck(),                           // El objeto Deck que se está editando
    val isLoading: Boolean = false,                    // Indica si se está cargando el mazo
    val errorMessage: String? = null,                  // Mensaje de error a mostrar (si lo hay)
    val isSaving: Boolean = false,                     // Indica si se está guardando el mazo
    val isDeckSaved: Boolean = false,                  // Flag para indicar que el mazo se guardó exitosamente
    val showNameErrorDialog: Boolean = false,          // Controla la visibilidad del diálogo de error de nombre
    val nameError: String? = null,                     // Mensaje de error específico para el nombre del mazo
    val selectedCardInDeckId: String? = null,          // ID de la carta seleccionada en el mazo para el diálogo de detalles
    val selectedCardInDeckImageUrl: String? = null,    // URL de la imagen de la carta seleccionada
    val sortOption: SortOption = SortOption.CMC,       // Opción de ordenación actual para las cartas del mazo
    val showStatsDialog: Boolean = false               // Controla la visibilidad del diálogo de estadísticas
)

/**
 * ViewModel para la pantalla de edición de mazos.
 * Gestiona la lógica de negocio, la interacción con Firebase y el estado de la UI.
 *
 * @param auth Instancia de FirebaseAuth para gestionar la autenticación del usuario.
 * @param firestore Instancia de FirebaseFirestore para interactuar con la base de datos.
 * @param ownerUid Opcional. Si se proporciona, el ViewModel operará en modo de "solo lectura"
 * para mazos de este UID específico. Si es nulo, operará con los mazos del usuario autenticado actual.
 */
class DeckEditViewModel(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val ownerUid: String? = null
) : ViewModel() {

    init {
        Log.d("DeckEditViewModel", "DeckEditViewModel instance created: ${this.hashCode()} with ownerUid: $ownerUid")
    }

    // StateFlow para el estado de la UI de la pantalla de edición del mazo.
    private val _uiState = MutableStateFlow(DeckEditUiState())
    val uiState: StateFlow<DeckEditUiState> = _uiState.asStateFlow()

    // StateFlow para las estadísticas del mazo.
    private val _deckStats = MutableStateFlow(DeckStats())
    val deckStats: StateFlow<DeckStats> = _deckStats.asStateFlow()


    private var currentDeckId: String? = null // Almacena el ID del mazo que se está editando
    private var isDeckDataLoadedForCurrentId = false // Flag para saber si los datos del mazo actual ya se han cargado

    private var loadDeckJob: Job? = null // Referencia a un trabajo de corrutina de carga para poder cancelarlo
    private var deckListenerRegistration: ListenerRegistration? = null // Listener para actualizaciones en tiempo real de Firestore

    /**
     * Se llama cuando el ViewModel es destruido. Asegura que el listener de Firestore se elimine
     * para evitar fugas de memoria y que cualquier trabajo pendiente se cancele.
     */
    override fun onCleared() {
        super.onCleared()
        deckListenerRegistration?.remove() // Se quita el listener al limpiar
        loadDeckJob?.cancel() // Cancela cualquier trabajo de carga activo
        Log.d("DeckEditViewModel", "DeckEditViewModel cleared: ${this.hashCode()}. Listener removed.")
    }

    /**
     * Asegura que el ViewModel esté inicializado con el mazo correcto (nuevo o existente)
     * basándose en el `incomingDeckId` y el estado interno.
     * Gestiona el reseteo para nuevos mazos y la carga para mazos existentes.
     *
     * @param incomingDeckId El ID del mazo que se debe cargar o "new" si es un mazo nuevo.
     * @param forceReload Si es true, fuerza una recarga de los datos del mazo, incluso si ya están cargados.
     */
    fun ensureInitialized(incomingDeckId: String?, forceReload: Boolean = false) {
        // Determina si la ruta indica la creación de un nuevo mazo.
        val isNewDeckRoute = incomingDeckId == null || incomingDeckId.isBlank() || incomingDeckId == "new"

        Log.d("DeckEditViewModel", "ensureInitialized() called with incomingDeckId: $incomingDeckId. Current ViewModel ID: ${this.hashCode()}")
        Log.d("DeckEditViewModel", "ViewModel internal state: currentDeckId=$currentDeckId, isDeckDataLoadedForCurrentId=$isDeckDataLoadedForCurrentId, current UI deck ID: ${_uiState.value.deck.id}, ownerUid=$ownerUid")
        Log.d("DeckEditViewModel", "Calculated: isNewDeckRoute=$isNewDeckRoute, forceReload=$forceReload")

        // Siempre cancela cualquier carga pendiente y quita el listener antiguo al inicio.
        loadDeckJob?.cancel()
        loadDeckJob = null
        deckListenerRegistration?.remove() // Quita el listener antiguo antes de configurar uno nuevo (o ninguno)
        deckListenerRegistration = null

        // --- Lógica CLAVE para distinguir mazo NUEVO vs. mazo EXISTENTE ---

        // Caso 1: Se está intentando crear un NUEVO mazo (`isNewDeckRoute` es true).
        if (isNewDeckRoute) {
            // No se permite crear mazos nuevos si estamos en modo de "ver mazos de un amigo" (`ownerUid != null`).
            if (ownerUid != null) {
                _uiState.update { it.copy(errorMessage = "No se pueden crear mazos nuevos en el modo de visualización de mazos de amigos.", isLoading = false) }
                Log.w("DeckEditViewModel", "Intento de crear mazo nuevo en modo de visualización de amigo (ownerUid: $ownerUid).")
                return // Sale de la función, ya que no se permite la creación.
            }

            // Si el ViewModel tiene estado de un mazo anterior (o un ID de mazo en UI State),
            // y se pide un "nuevo mazo", se necesita restablecer el ViewModel.
            if (currentDeckId != null || _uiState.value.deck.id.isNotBlank()) {
                Log.d("DeckEditViewModel", "ensureInitialized(): Detectada intención de crear NUEVO mazo, pero ViewModel aún tiene estado anterior. Haciendo reset.")
                currentDeckId = null // Limpiar el ID del mazo anterior
                isDeckDataLoadedForCurrentId = false // Indicar que no hay datos cargados para el nuevo (nulo) ID
                _uiState.update { DeckEditUiState() } // Hacer reset del UI State a su estado inicial (mazo vacío, sin errores)
            }
        }
        // Caso 2: El ViewModel ya está correctamente inicializado para el `incomingDeckId` actual
        // y no se ha forzado una recarga. En este caso, no se necesita hacer nada.
        else if (currentDeckId == incomingDeckId && isDeckDataLoadedForCurrentId && !forceReload) {
            Log.d("DeckEditViewModel", "ensureInitialized() saltado: Datos del mazo ya cargados para ID $incomingDeckId. No se inició una nueva carga.")
            return // No hay nada que hacer, el estado es correcto
        }


        // --- Se procede con la inicialización si se necesita una carga/reseteo ---

        // Se actualiza `currentDeckId` al `incomingDeckId` para el seguimiento interno.
        currentDeckId = incomingDeckId
        isDeckDataLoadedForCurrentId = false // Se asume que no está cargado hasta que se confirme

        if (isNewDeckRoute) {
            Log.d("DeckEditViewModel", "ensureInitialized(): Inicializando para un mazo NUEVO. Estableciendo estado de mazo vacío.")
            _uiState.update {
                it.copy(
                    deck = Deck(), // Establece explícitamente un nuevo objeto Deck vacío
                    isLoading = false,
                    errorMessage = null,
                    isDeckSaved = false
                )
            }
            isDeckDataLoadedForCurrentId = true // Marca el mazo vacío como "cargado".
        } else {
            // Es un mazo existente, intenta cargarlo desde Firestore
            Log.d("DeckEditViewModel", "ensureInitialized(): Inicializando para mazo EXISTENTE: $incomingDeckId. Llamando a loadDeck().")
            // `incomingDeckId!!` es seguro porque `isNewDeckRoute` ya lo ha descartado como nulo/vacío.
            loadDeck(incomingDeckId!!)
        }
    }

    /**
     * Carga un mazo específico de Firestore.
     * Utiliza un listener en tiempo real para mantener el estado del mazo actualizado.
     *
     * @param deckId El ID del mazo a cargar.
     */
    private fun loadDeck(deckId: String) {
        // Determina el UID objetivo: Si `ownerUid` es proporcionado (modo vista), se usa ese.
        // Si no (`ownerUid` es nulo), se usa el UID del usuario autenticado actual.
        val targetUid = ownerUid ?: auth.currentUser?.uid

        if (targetUid == null) {
            _uiState.update { it.copy(errorMessage = "Usuario no autenticado o UID de propietario no especificado para cargar mazo.", isLoading = false) }
            Log.e("DeckEditViewModel", "loadDeck: No hay UID objetivo para cargar mazo.")
            isDeckDataLoadedForCurrentId = false
            return
        }

        // Siempre limpia el error y establece `isLoading` al inicio de cualquier operación de carga.
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        Log.d("DeckEditViewModel", "loadDeck($deckId): Iniciando nueva carga para targetUid: $targetUid. errorMessage inicializado a null.")

        // Configura un listener en tiempo real para el documento del mazo en Firestore.
        deckListenerRegistration = firestore.collection("decks").document(deckId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Manejo de errores de Firestore.
                    _uiState.update { it.copy(errorMessage = "Error al cargar mazo: ${e.message}", isLoading = false) } // Error por excepción
                    Log.e("DeckEditViewModel", "loadDeck($deckId) ERROR: ${e.message}", e)
                    isDeckDataLoadedForCurrentId = false
                    return@addSnapshotListener
                }

                // Si el snapshot existe y contiene datos.
                if (snapshot != null && snapshot.exists()) {
                    val deck = snapshot.toObject(Deck::class.java)?.copy(id = snapshot.id)
                    // Verifica que el mazo cargado pertenece al `targetUid`.
                    if (deck != null && deck.userId == targetUid) {
                        _uiState.update { currentState ->
                            currentState.copy(
                                deck = deck,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                        isDeckDataLoadedForCurrentId = true
                        Log.d("DeckEditViewModel", "loadDeck($deckId): Mazo cargado: ${deck.name} para UID: $targetUid. errorMessage: ${_uiState.value.errorMessage}")
                        // Recalcular estadísticas cuando el mazo se carga/actualiza.
                        calculateDeckStats()
                    } else {
                        // El mazo no se encontró o no pertenece al usuario esperado.
                        val msg = "Mazo no encontrado o no pertenece a este usuario ($targetUid)."
                        _uiState.update { it.copy(errorMessage = msg, isLoading = false) }
                        Log.w("DeckEditViewModel", "loadDeck($deckId): $msg")
                        isDeckDataLoadedForCurrentId = false
                    }
                } else {
                    // El documento del mazo no existe en Firestore.
                    val msg = "El mazo especificado no existe o fue eliminado."
                    _uiState.update { it.copy(errorMessage = msg, isLoading = false) }
                    Log.w("DeckEditViewModel", "loadDeck($deckId): El documento no existe en Firestore. errorMessage: $msg")
                    isDeckDataLoadedForCurrentId = false
                }
            }
    }

    /**
     * Actualiza el nombre del mazo en el estado de la UI.
     * Solo permite la modificación si el ViewModel no está en modo de visualización de amigo.
     */
    fun onDeckNameChange(newName: String) {
        if (ownerUid != null) {
            _uiState.update { it.copy(errorMessage = "No tienes permiso para modificar mazos de otros usuarios.") }
            return
        }
        _uiState.update { it.copy(deck = it.deck.copy(name = newName), nameError = null, showNameErrorDialog = false) }
    }

    /**
     * Actualiza la descripción del mazo en el estado de la UI.
     * Solo permite la modificación si el ViewModel no está en modo de visualización de amigo.
     */
    fun onDeckDescriptionChange(newDescription: String) {
        if (ownerUid != null) {
            _uiState.update { it.copy(errorMessage = "No tienes permiso para modificar mazos de otros usuarios.") }
            return
        }
        _uiState.update { it.copy(deck = it.deck.copy(description = newDescription)) }
    }

    /**
     * Actualiza el formato del mazo en el estado de la UI.
     * Solo permite la modificación si el ViewModel no está en modo de visualización de amigo.
     */
    fun onDeckFormatChange(newFormat: String) {
        if (ownerUid != null) {
            _uiState.update { it.copy(errorMessage = "No tienes permiso para modificar mazos de otros usuarios.") }
            return
        }
        _uiState.update { it.copy(deck = it.deck.copy(format = newFormat)) }
    }

    /**
     * Descarta el diálogo de error de nombre.
     */
    fun dismissNameErrorDialog() {
        _uiState.update { it.copy(showNameErrorDialog = false, nameError = null) }
    }

    /**
     * Guarda el mazo actual en Firestore.
     * Maneja tanto la creación de nuevos mazos como la actualización de mazos existentes.
     * Realiza validaciones como el nombre vacío y la unicidad del nombre para el usuario actual.
     */
    fun saveDeck() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.update { it.copy(errorMessage = "Usuario no autenticado.", isSaving = false) }
            return
        }

        // Solo permitir guardar si el ViewModel no está en modo de visualización de amigo.
        if (ownerUid != null) {
            _uiState.update { it.copy(errorMessage = "No tienes permiso para guardar mazos de otros usuarios.", isSaving = false) }
            return
        }

        val currentDeck = _uiState.value.deck.copy(userId = userId)

        // Validación: el nombre del mazo no puede estar vacío.
        if (currentDeck.name.isBlank()) {
            _uiState.update { it.copy(showNameErrorDialog = true, nameError = "El nombre del mazo no puede estar vacío.", isSaving = false) }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) } // Establece el estado de guardado.

        viewModelScope.launch {
            try {
                val decksRef = firestore.collection("decks")

                // Comprueba si ya existe un mazo con el mismo nombre para el usuario actual.
                val existingDecksQuery = decksRef
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("name", currentDeck.name)
                    .get()
                    .await()

                if (existingDecksQuery.documents.isNotEmpty()) {
                    val existingDeckDocument = existingDecksQuery.documents.first()
                    // Si ya existe un mazo con el mismo nombre y NO es el mazo que estamos editando actualmente,
                    // entonces es un error de nombre duplicado.
                    if (currentDeckId == null || existingDeckDocument.id != currentDeckId) {
                        _uiState.update { it.copy(showNameErrorDialog = true, nameError = "Ya tienes un mazo con este nombre.", isSaving = false) }
                        return@launch
                    }
                }

                Log.d("DeckEditViewModel", "saveDeck: Intentando guardar mazo con ID: $currentDeckId y ${currentDeck.cardCount} cartas.")
                Log.d("DeckEditViewModel", "saveDeck: Contenido del mapa de cartas a guardar: ${currentDeck.cards}")


                // Si `currentDeckId` es nulo/vacío, significa que es un mazo nuevo que se guarda por primera vez.
                if (currentDeckId == null || currentDeckId!!.isBlank() || currentDeckId == "new") {
                    val newDocRef = decksRef.add(currentDeck).await() // Firestore asigna un ID único.
                    val newDeckId = newDocRef.id // Obtiene el ID real generado por Firestore.

                    currentDeckId = newDeckId // Se actualiza el ID interno del ViewModel
                    _uiState.update { it.copy(deck = currentDeck.copy(id = newDeckId), isDeckSaved = true, isSaving = false, errorMessage = null) } // Actualiza el ID en el UI State
                    Log.d("DeckEditViewModel", "saveDeck: Mazo NUEVO guardado con ID: $newDeckId")
                    isDeckDataLoadedForCurrentId = true // Marcar como cargado
                } else { // Es un mazo existente, se actualiza usando su ID
                    decksRef.document(currentDeckId!!).set(currentDeck).await()
                    _uiState.update { it.copy(isDeckSaved = true, isSaving = false, errorMessage = null) }
                    Log.d("DeckEditViewModel", "saveDeck: Mazo EXISTENTE actualizado con ID: $currentDeckId")
                    isDeckDataLoadedForCurrentId = true // Marca como cargado.
                }
            } catch (e: Exception) {
                Log.e("DeckEditViewModel", "saveDeck ERROR: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "Error al guardar mazo: ${e.message}", isSaving = false) }
            }
        }
    }

    /**
     * Resetea el flag `isDeckSaved` para evitar navegaciones repetidas.
     */
    fun resetDeckSavedFlag() {
        _uiState.update { it.copy(isDeckSaved = false) }
    }

    /**
     * Añade una carta al mazo en el estado local del ViewModel.
     * Si la carta ya existe, incrementa su cantidad; de lo contrario, la añade.
     * Solo permite añadir cartas si el ViewModel no está en modo de visualización de amigo.
     *
     * @param card La carta a añadir.
     * @param quantity La cantidad de copias a añadir.
     */
    fun addCardToDeck(card: Card, quantity: Int) {
        if (ownerUid != null) {
            _uiState.update { it.copy(errorMessage = "No tienes permiso para añadir cartas a mazos de otros usuarios.") }
            return
        }

        // Cancela cualquier trabajo de carga pendiente para asegurar que no se sobrescriba
        // el estado actual con una carga antigua.
        loadDeckJob?.cancel()
        loadDeckJob = null
        // Al modificar el mazo localmente, se considera que el mazo está "cargado"
        // ya que el estado actual en memoria es el que se quiere.
        isDeckDataLoadedForCurrentId = true

        _uiState.update { currentState ->
            val currentCards = currentState.deck.cards.toMutableMap()
            val existingCardData = currentCards[card.id]

            val newCardData = if (existingCardData != null) {
                // Si la carta ya existe, actualiza su cantidad.
                existingCardData.copy(quantity = existingCardData.quantity + quantity)
            } else {
                // Si la carta es nueva, crea un nuevo CardInDeckData.
                CardInDeckData(
                    id = card.id,
                    quantity = quantity,
                    imageUrl = card.imageUrl,
                    name = card.name,
                    manaCost = card.manaCost,
                    cmc = card.cmc,
                    colors = card.colors,
                    type = card.type
                )
            }
            currentCards[card.id] = newCardData // Añade o actualiza la carta en el mapa.
            val updatedDeck = currentState.deck.copy(cards = currentCards) // Crea un nuevo Deck con el mapa actualizado.

            Log.d("DeckEditViewModel", "addCardToDeck: Mazo actualizado LOCALMENTE. Cartas en el estado: ${updatedDeck.cards.size} - CardCount: ${updatedDeck.cardCount}")
            Log.d("DeckEditViewModel", "addCardToDeck: Contenido del mapa de cartas actualizado: ${updatedDeck.cards}")

            currentState.copy(deck = updatedDeck, errorMessage = null) // Actualiza el UI State.
        }
        calculateDeckStats() // Recalcular estadísticas cada vez que se añade una carta.
    }

    /**
     * Elimina una cantidad específica de copias de una carta del mazo en el estado local del ViewModel.
     * Si la cantidad restante es cero o menos, la carta se elimina por completo.
     * Solo permite eliminar cartas si el ViewModel no está en modo de visualización de amigo.
     *
     * @param cardId El ID de la carta a eliminar.
     * @param quantityToRemove La cantidad de copias a eliminar.
     */
    fun removeCardFromDeck(cardId: String, quantityToRemove: Int) {
        if (ownerUid != null) {
            _uiState.update { it.copy(errorMessage = "No tienes permiso para eliminar cartas de mazos de otros usuarios.") }
            return
        }

        loadDeckJob?.cancel()
        loadDeckJob = null
        isDeckDataLoadedForCurrentId = true

        _uiState.update { currentState ->
            val currentCards = currentState.deck.cards.toMutableMap()
            val existingCardData = currentCards[cardId]

            if (existingCardData != null) {
                val newQuantity = (existingCardData.quantity - quantityToRemove).coerceAtLeast(0) // Asegura que la cantidad no sea negativa.
                if (newQuantity <= 0) {
                    currentCards.remove(cardId) // Elimina la carta si la cantidad es cero o menos.
                } else {
                    currentCards[cardId] = existingCardData.copy(quantity = newQuantity) // Actualiza la cantidad.
                }
            }
            val updatedDeck = currentState.deck.copy(cards = currentCards)
            Log.d("DeckEditViewModel", "removeCardFromDeck: Mazo actualizado LOCALMENTE. Cartas en el estado: ${updatedDeck.cards.size} - CardCount: ${updatedDeck.cardCount}")
            Log.d("DeckEditViewModel", "removeCardFromDeck: Contenido del mapa de cartas actualizado: ${updatedDeck.cards}")

            currentState.copy(deck = updatedDeck, errorMessage = null)
        }
        calculateDeckStats() // Recalcular estadísticas cada vez que se quita una carta.
    }

    /**
     * Selecciona una carta en el mazo para mostrar sus detalles.
     *
     * @param cardId El ID de la carta seleccionada.
     */
    fun selectCardInDeck(cardId: String) {
        _uiState.update { currentState ->
            val cardData = currentState.deck.cards[cardId]
            currentState.copy(
                selectedCardInDeckId = cardId,
                selectedCardInDeckImageUrl = cardData?.imageUrl,
                errorMessage = null
            )
        }
    }

    /**
     * Deselecciona cualquier carta que estuviera seleccionada en el mazo.
     * Esto se usa para cerrar el diálogo de detalles de la carta.
     */
    fun deselectCardInDeck() {
        _uiState.update { it.copy(selectedCardInDeckId = null, selectedCardInDeckImageUrl = null, errorMessage = null) }
    }

    /**
     * Establece la opción de ordenación para las cartas del mazo.
     *
     * @param option La [SortOption] deseada.
     */
    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    /**
     * [StateFlow] que emite la lista de cartas del mazo, ordenadas según la [SortOption] actual.
     * Observa los cambios en `_uiState` para reaccionar a actualizaciones en el mazo o en la opción de ordenación.
     */
    val sortedCardsInDeck: StateFlow<List<Pair<String, CardInDeckData>>> = _uiState
        .map { uiState -> // Mapea el flujo del estado de la UI a una lista ordenada de cartas.
            val cardsMap = uiState.deck.cards // Obtiene el mapa de cartas del mazo.
            val sortedList = when (uiState.sortOption) {
                SortOption.CMC -> cardsMap.entries.sortedWith(compareBy<Map.Entry<String, CardInDeckData>> { entry ->
                    val cardData = entry.value
                    val isLand = cardData.type?.contains("Land", ignoreCase = true) == true

                    // Si es una tierra, se le asigna un CMC muy alto (Double.MAX_VALUE) para que vaya al final
                    // de la lista cuando se ordena por CMC. De lo contrario, se usa su CMC real.
                    if (isLand) Double.MAX_VALUE else (cardData.cmc ?: 0.0)
                }.thenBy { it.value.name }) // Criterio secundario: si los CMC son iguales, ordenar por nombre alfabéticamente.

                SortOption.COLORS -> cardsMap.entries.sortedWith(compareBy<Map.Entry<String, CardInDeckData>> { entry ->
                    val cardData = entry.value
                    val manaCost = cardData.manaCost
                    val type = cardData.type // String (ej. "Land", "Creature")

                    // Funciones auxiliares para verificar la presencia de un símbolo de color en el `manaCost`.
                    fun CardInDeckData.hasColorSymbol(colorSymbol: String): Boolean {
                        return this.manaCost?.contains("{$colorSymbol}", ignoreCase = true) == true
                    }

                    // Función auxiliar para detectar símbolos híbridos {X/Y}
                    fun CardInDeckData.hasHybridManaSymbol(): Boolean {
                        return this.manaCost?.contains("""\{[WUBRG]/[WUBRG]\}""".toRegex()) == true
                    }

                    // Función para determinar si una carta es verdaderamente incolora basándose en su `manaCost` y tipo.
                    fun CardInDeckData.isTrulyColorlessByManaCost(): Boolean {
                        val hasAnyColorSymbol = this.hasColorSymbol("W") ||
                                this.hasColorSymbol("U") ||
                                this.hasColorSymbol("B") ||
                                this.hasColorSymbol("R") ||
                                this.hasColorSymbol("G") ||
                                // También se considera incolora si solo tiene maná genérico X.
                                this.manaCost?.contains("{X}", ignoreCase = true) == true

                        // Una carta es incolora si no tiene símbolos de color (excepto X) y no es una tierra.
                        return !hasAnyColorSymbol && !(type?.contains("Land", ignoreCase = true) == true)
                    }

                    // Define el orden deseado para los colores básicos usando sus símbolos.
                    // Este orden determina la prioridad en la ordenación: BLANCO (0), VERDE (1), ROJO (2), NEGRO (3), AZUL (4).
                    val colorSymbolsOrder = listOf("W", "G", "R", "B", "U")

                    val isLand = type?.contains("Land", ignoreCase = true) == true

                    // Determina si la carta es monocolor, multicolor, incolora o tierra para la ordenación.
                    val cardColorsFound = colorSymbolsOrder.filter { cardData.hasColorSymbol(it) }

                    // Una carta es multicolor si tiene más de un color básico O si tiene algún símbolo de maná híbrido.
                    val isMulticolor = cardColorsFound.size > 1 || cardData.hasHybridManaSymbol()

                    // Una carta es monocolor si tiene exactamente un color básico y NO es híbrida.
                    val isMonocolor = cardColorsFound.size == 1 && !cardData.hasHybridManaSymbol()
                    val isColorless = cardData.isTrulyColorlessByManaCost() // Usa la función auxiliar.

                    // Asigna una prioridad de ordenación numérica basada en la categoría de color.
                    val sortPriority = when {
                        isLand -> 7 // 8º lugar: Tierras  (al final)
                        isColorless -> 6 // 7º lugar: Incoloras
                        isMulticolor -> 5 // 6º lugar: Multicolor
                        isMonocolor -> { // 1º-5º lugar: Monocolor según el orden definido en `colorSymbolsOrder`.
                            val primarySymbol = cardColorsFound.first() // Obtiene el primer símbolo de color encontrado.
                            colorSymbolsOrder.indexOf(primarySymbol) // Asigna la prioridad basada en el índice.
                        }
                        else -> { // Caso por defecto para cartas que no encajan claramente en las categorías anteriores.
                            if (manaCost.isNullOrBlank()) {
                                6 // Si no tiene `manaCost`, se asume incolora y se agrupa con ellas.
                            } else {
                                // Advertencia para cartas con `manaCost` complejo que no se categorizan.
                                Log.w("DeckEditViewModel", "sortedCardsInDeck: Carta '${cardData.name}' con manaCost '${manaCost}' no encaja en las categorías de color. Asignando prioridad baja.")
                                99 // Muy alta prioridad para ponerla al final
                            }
                        }
                    }

                    sortPriority // Retorna la prioridad calculada para la comparación.
                }.thenBy { it.value.name }) // Criterio secundario: si los colores son iguales, ordenar por nombre.

                SortOption.ALPHABETICAL -> cardsMap.entries.sortedBy { it.value.name } // Ordenar solo por nombre.
            }
            sortedList.map { it.key to it.value } // Convierte de vuelta a una lista de pares (ID -> CardInDeckData).
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000), // Inicia el flujo cuando hay suscriptores y lo detiene 5 segundos después de que no los haya.
            initialValue = emptyList() // Valor inicial de la lista ordenada.
        )

    /**
     * Calcula las estadísticas del mazo (total de cartas, distribución por CMC, tipo y color, CMC promedio).
     * Actualiza el [_deckStats] StateFlow con los resultados.
     */
    fun calculateDeckStats() {
        val deck = _uiState.value.deck
        val cards = deck.cards.values

        val totalCards = cards.sumOf { it.quantity }

        // Distribución por CMC.
        val cmcDistribution = mutableMapOf<Int, Int>().apply {
            // Inicializa los "cubos" para CMC de 0 a 7+.
            for (i in 0..6) put(i, 0)
            put(7, 0) // Para CMC de 7 o más.
        }
        var totalCmcSum = 0.0
        var totalCmcCards = 0 // Contador para el promedio de CMC, excluyendo tierras.

        cards.forEach { card ->
            val cmc = card.cmc?.toInt() ?: 0
            // Las tierras no cuentan para el promedio de CMC ni para la distribución de CMC.
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

        val cmcAvg = if (totalCmcCards > 0) totalCmcSum / totalCmcCards else 0.0

        // Distribución por Tipo de Carta.
        val typeDistribution = mutableMapOf<String, Int>()
        cards.forEach { card ->
            val quantity = card.quantity
            val type = card.type ?: "Desconocido"

            // Categoriza las cartas en tipos principales.
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
                // Para otros tipos más complejos (ej. "Legendary Artifact Creature"),
                // intenta obtener la última palabra como tipo base.
                val parts = type.split(" ").map { it.trim() }.filter { it.isNotBlank() }
                val baseType = parts.lastOrNull() ?: "Otro"
                typeDistribution[baseType] = typeDistribution.getOrDefault(baseType, 0) + quantity
            }
        }

        // Distribución por Color de Carta.
        val colorDistribution = mutableMapOf<String, Int>()
        cards.forEach { card ->
            val quantity = card.quantity
            val isLand = card.type?.contains("Land", ignoreCase = true) == true
            if (isLand) { // Las tierras no cuentan para la distribución de color basada en el coste de maná.
                return@forEach
            }

            // Comprueba la presencia de símbolos de maná de color.
            val hasWhite = card.manaCost?.contains("{W}", ignoreCase = true) == true
            val hasBlue = card.manaCost?.contains("{U}", ignoreCase = true) == true
            val hasBlack = card.manaCost?.contains("{B}", ignoreCase = true) == true
            val hasRed = card.manaCost?.contains("{R}", ignoreCase = true) == true
            val hasGreen = card.manaCost?.contains("{G}", ignoreCase = true) == true
            val hasAnyColor = hasWhite || hasBlue || hasBlack || hasRed || hasGreen

            val isColorless = !hasAnyColor
            val isMulticolor = listOf(hasWhite, hasBlue, hasBlack, hasRed, hasGreen).count { it } > 1

            when {
                isColorless -> {
                    colorDistribution["Incoloras"] = colorDistribution.getOrDefault("Incoloras", 0) + quantity
                }
                isMulticolor -> {
                    colorDistribution["Multicolor"] = colorDistribution.getOrDefault("Multicolor", 0) + quantity
                }
                else -> { // Monocolor
                    if (hasWhite) colorDistribution["Blanco"] = colorDistribution.getOrDefault("Blanco", 0) + quantity
                    if (hasBlue) colorDistribution["Azul"] = colorDistribution.getOrDefault("Azul", 0) + quantity
                    if (hasBlack) colorDistribution["Negro"] = colorDistribution.getOrDefault("Negro", 0) + quantity
                    if (hasRed) colorDistribution["Rojo"] = colorDistribution.getOrDefault("Rojo", 0) + quantity
                    if (hasGreen) colorDistribution["Verde"] = colorDistribution.getOrDefault("Verde", 0) + quantity
                }
            }
        }

        // Actualiza el StateFlow de estadísticas con los nuevos cálculos.
        _deckStats.value = DeckStats(
            totalCards = totalCards,
            cmcDistribution = cmcDistribution,
            typeDistribution = typeDistribution,
            colorDistribution = colorDistribution,
            cmcAvg = cmcAvg
        )
        Log.d("DeckEditViewModel", "calculateDeckStats: Estadísticas recalculadas. Total Cartas: $totalCards, CMC Avg: $cmcAvg")
    }

//    /**
//     * Función auxiliar para extraer colores de una cadena `manaCost`.
//     * @param manaCost La cadena de coste de maná (ej. "{2}{W/U}").
//     * @return Un conjunto de cadenas que representan los colores encontrados (ej. "W", "U").
//     */
//    private fun extractColorsFromManaCost(manaCost: String?): Set<String> {
//        if (manaCost.isNullOrBlank()) return emptySet()
//        val colors = mutableSetOf<String>()
//        // Simple regex for basic color symbols.
//        // For more complex cases (hybrids like {W/U}, {B/P}, etc.), this would need to be more robust.
//        val regex = Regex("\\{[WUBRG]\\}") // Matches {W}, {U}, etc.
//        regex.findAll(manaCost).forEach { matchResult ->
//            val symbol = matchResult.value.replace("{", "").replace("}", "")
//            colors.add(symbol)
//        }
//        return colors
//    }

    /**
     * Muestra el diálogo de estadísticas del mazo.
     * Recalcula las estadísticas justo antes de mostrar el diálogo para asegurar que estén actualizadas.
     */
    fun showStatsDialog() {
        calculateDeckStats()
        _uiState.update { it.copy(showStatsDialog = true) }
    }

    /**
     * Oculta el diálogo de estadísticas del mazo.
     */
    fun dismissStatsDialog() {
        _uiState.update { it.copy(showStatsDialog = false) }
    }

//    /**
//     * Descarta cualquier mensaje de error genérico que se esté mostrando en la UI.
//     */
//    fun dismissErrorMessage() {
//        _uiState.update { it.copy(errorMessage = null) }
//    }
}