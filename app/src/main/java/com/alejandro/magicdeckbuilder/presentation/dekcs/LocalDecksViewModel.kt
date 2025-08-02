package com.alejandro.magicdeckbuilder.presentation.dekcs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alejandro.magicdeckbuilder.data.local.LocalDeck
import com.alejandro.magicdeckbuilder.data.local.LocalDeckManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la UI para la pantalla de mazos locales (`LocalDecksScreen`).
 * Contiene la lista de mazos locales, el estado de carga y mensajes de error.
 */
data class LocalDecksUiState(
    val decks: List<LocalDeck> = emptyList(), // Lista de mazos locales a mostrar.
    val isLoading: Boolean = false, // Indica si se están cargando mazos.
    val errorMessage: String? = null, // Mensaje de error a mostrar, si lo hay.
    val showDeleteLocalDeckDialog: Boolean = false, // Para mostrar el emergente de confirmación de borrado.
    val selectedLocalDeckId: String? = null // Para gestionar el id del mazo a borrar.
)

/**
 * ViewModel para la pantalla de mazos locales (`LocalDecksScreen`).
 * Gestiona la carga y eliminación de mazos almacenados localmente.
 *
 * @param application La instancia de la aplicación, necesaria para [AndroidViewModel] y para inicializar [LocalDeckManager].
 */
class LocalDecksViewModel(application: Application) : AndroidViewModel(application) {

    // Instancia del gestor de mazos locales.
    private val localDeckManager = LocalDeckManager(getApplication())

    // MutableStateFlow privado para gestionar el estado de la UI.
    private val _uiState = MutableStateFlow(LocalDecksUiState())
    // StateFlow público de solo lectura que expone el estado de la UI a la Composable.
    val uiState: StateFlow<LocalDecksUiState> = _uiState.asStateFlow()

    init {
        // Al inicializar el ViewModel, se cargan automáticamente los mazos locales.
        loadLocalDecks()
    }

    /**
     * Carga la lista de mazos locales desde el almacenamiento del dispositivo.
     * Actualiza el estado de la UI con los mazos cargados o un mensaje de error si falla.
     */
    fun loadLocalDecks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // Establece estado de carga.
            try {
                val decks = localDeckManager.loadLocalDecks() // Llama al gestor local para cargar los mazos.
                _uiState.update { it.copy(decks = decks, isLoading = false) } // Actualiza el estado con los mazos cargados.
            } catch (e: Exception) {
                // En caso de error, actualiza el estado con un mensaje de error.
                _uiState.update { it.copy(errorMessage = "Error al cargar mazos locales.", isLoading = false) }
            }
        }
    }

    /**
     * Muestra el diálogo de confirmación para eliminar un mazo local y guarda el ID del mazo seleccionado.
     *
     * @param deckId El ID del mazo local que se pretende eliminar.
     */
    fun showDeleteLocalDeckDialog(deckId: String) {
        _uiState.update {
            it.copy(
                showDeleteLocalDeckDialog = true, // Establece la visibilidad del diálogo a true.
                selectedLocalDeckId = deckId // Almacena el ID del mazo local que se va a eliminar.
            )
        }
    }

    /**
     * Oculta el diálogo de confirmación de eliminación de mazo local y limpia el ID del mazo seleccionado.
     */
    fun dismissDeleteLocalDeckDialog() {
        _uiState.update {
            it.copy(
                showDeleteLocalDeckDialog = false, // Oculta el diálogo.
                selectedLocalDeckId = null // Limpia el ID del mazo, ya que el diálogo se ha cerrado.
            )
        }
    }

    /**
     * Confirma la eliminación de un mazo local.
     * Se invoca cuando el usuario acepta la eliminación en el diálogo de confirmación.
     * Llama a la función interna que realiza la eliminación real del mazo local.
     */
    fun confirmDeleteLocalDeck() {
        // Verifica si hay un ID de mazo local seleccionado antes de intentar borrar.
        _uiState.value.selectedLocalDeckId?.let { deckId ->
            // Llama a la función privada que contiene la lógica de borrado real.
            deleteLocalDeckInternal(deckId)
        }
        // Independientemente de si se encontró un ID o no, el diálogo se cierra.
        dismissDeleteLocalDeckDialog()
    }

    /**
     * Realiza la operación de eliminación de un mazo local.
     * Esta función es privada y solo debe ser invocada después de una confirmación del usuario.
     *
     * @param deckId El ID del mazo local a eliminar.
     */
    private fun deleteLocalDeckInternal(deckId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // Establece el estado de carga a true.
            try {
                localDeckManager.deleteLocalDeck(deckId) // Llama al gestor local para eliminar el mazo.
                loadLocalDecks() // Recarga la lista completa de mazos locales para asegurar que la UI se actualice.
            } catch (e: Exception) {
                // En caso de error, actualiza el estado con un mensaje de error y desactiva la carga.
                _uiState.update { it.copy(errorMessage = "Error al borrar el mazo local: ${e.message}", isLoading = false) }
            }
        }
    }

    /**
     * Función pública para iniciar el proceso de eliminación de un mazo local.
     * En lugar de borrar directamente, muestra el diálogo de confirmación al usuario.
     *
     * @param deckId El ID del mazo local a eliminar.
     */
    fun deleteLocalDeck(deckId: String) {
        showDeleteLocalDeckDialog(deckId) // Llama a la función para mostrar el diálogo de confirmación.
    }
}

/**
 * Factory para crear instancias de [LocalDecksViewModel].
 * Permite inyectar la dependencia [Application] en el constructor del [LocalDecksViewModel].
 *
 * @param application La instancia de la aplicación, pasada al AndroidViewModel.
 */
class LocalDecksViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {  // Implementa la interfaz ViewModelProvider.Factory.

    /**
     * Crea una nueva instancia de un ViewModel de la clase dada.
     *
     * @param modelClass La clase del ViewModel a crear (en este caso, LocalDecksViewModel).
     * @return Una nueva instancia del ViewModel.
     * @throws IllegalArgumentException si la clase de ViewModel solicitada no es LocalDecksViewModel.
     */
    @Suppress("UNCHECKED_CAST") // Suprime la advertencia de casting inseguro, ya que la comprobación de tipo se realiza explícitamente.
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Comprueba si la clase solicitada es asignable a LocalDecksViewModel.
        if (modelClass.isAssignableFrom(LocalDecksViewModel::class.java)) {
            // Si es así, crea una nueva instancia de LocalDecksViewModel, pasándole la dependencia 'application'.
            // El 'as T' es un casting seguro después de la comprobación isAssignableFrom.
            return LocalDecksViewModel(application) as T
        }
        // Si se solicita una clase de ViewModel diferente, lanza una excepción.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}