package com.alejandro.magicdeckbuilder.presentation.dekcs

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alejandro.magicdeckbuilder.data.local.LocalDeckManager
import com.alejandro.magicdeckbuilder.data.models.Deck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Estado de la UI para la pantalla de mazos (DecksScreen).
 * Contiene la lista de mazos, el estado de carga, mensajes de error y el ID de un mazo recién creado.
 */
data class DecksUiState(
    val decks: List<Deck> = emptyList(), // Lista de mazos a mostrar.
    val isLoading: Boolean = false, // Indica si se están cargando mazos.
    val errorMessage: String? = null, // Mensaje de error a mostrar, si lo hay.
    val newlyCreatedDeckId: String? = null, // Para comunicar el ID del mazo recién creado a la UI (para navegación).
    val showDeleteDeckDialog: Boolean = false, // Para mostrar el emergente de confirmación de borrado.
    val selectedDeckId: String? = null // Para gestionar el id del mazo a borrar.
)

/**
 * ViewModel para la pantalla de mazos (`DecksScreen`).
 * Gestiona la carga, creación, eliminación y descarga de mazos, interactuando con Firestore y el almacenamiento local.
 *
 * @param application La instancia de la aplicación, necesaria para `AndroidViewModel`.
 * @param auth La instancia de `FirebaseAuth` para la autenticación.
 * @param firestore La instancia de `FirebaseFirestore` para interactuar con la base de datos remota.
 * @param ownerUid El UID del propietario de los mazos que se quieren ver/gestionar.
 * Si es `null`, el ViewModel cargará los mazos del usuario actualmente autenticado.
 * Si no es `null`, cargará los mazos de ese `ownerUid` (usado para ver mazos de amigos).
 */
class DecksViewModel(
    application: Application,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val ownerUid: String? = null
) : AndroidViewModel(application) {

    // MutableStateFlow privado para gestionar el estado de la UI.
    private val _uiState = MutableStateFlow(DecksUiState())
    // StateFlow público de solo lectura que expone el estado de la UI a la Composable.
    val uiState: StateFlow<DecksUiState> = _uiState.asStateFlow()

    // Referencias a los listeners de Firestore y FirebaseAuth para poder removerlos en onCleared().
    private var deckListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    // Instancia del gestor local de mazos.
    private val localDeckManager = LocalDeckManager(getApplication())

    // Estado para gestionar el progreso y resultado de la descarga de un mazo.
    private val _downloadState = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadState: StateFlow<DownloadStatus> = _downloadState.asStateFlow()

    /**
     * Inicia la descarga de un mazo remoto y lo guarda localmente.
     * Actualiza `_downloadState` con el progreso y resultado.
     *
     * @param deck El objeto `Deck` remoto a descargar.
     */
    fun downloadDeck(deck: Deck) {
        viewModelScope.launch {
            _downloadState.value = DownloadStatus.Downloading(deck.id)  // Indica que la descarga está en curso.
            val result = localDeckManager.downloadAndSaveDeck(deck) // Llama al gestor local para la descarga.
            if (result.isSuccess) {
                _downloadState.value = DownloadStatus.Success(deck.name) // Éxito en la descarga.
            } else {
                val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                _downloadState.value = DownloadStatus.Error(error) // Error en la descarga.
            }
        }
    }

    /**
     * Hace reset al estado de la descarga a `Idle` para ocultar mensajes de descarga completada/error en la UI.
     * Debe llamarse después de que la UI haya procesado el mensaje de éxito o error.
     */
    fun clearDownloadState() {
        _downloadState.value = DownloadStatus.Idle
    }

    init {
        // Solo añade el AuthStateListener si este ViewModel está gestionando los mazos del propio usuario (ownerUid es null).
        // Si ownerUid no es null, significa que estamos viendo los mazos de un amigo, y la autenticación del usuario actual
        // no afectaría a la lista de mazos del amigo.
        if (ownerUid == null) {
            authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val newUserId = firebaseAuth.currentUser?.uid
                // Recarga los mazos si el estado de autenticación cambia de manera relevante para el usuario actual.
                // Esto es útil si el usuario inicia sesión o cierra sesión mientras el ViewModel está activo.
                if (newUserId != auth.currentUser?.uid || (auth.currentUser?.uid == null && newUserId != null)) {
                    Log.d("DecksViewModel", "Auth state changed. New user ID: $newUserId. Reloading decks.")
                    loadUserDecks()  // Recarga los mazos.
                }
            }
            auth.addAuthStateListener(authStateListener!!) // Añade el listener.
        }
        // Carga los mazos iniciales al iniciar el ViewModel, independientemente de si hay ownerUid o no.
        loadUserDecks()
    }

    /**
     * Carga los mazos desde Firestore.
     * Si `ownerUid` no es `null`, cargará los mazos de ese UID.
     * Si `ownerUid` es `null`, cargará los mazos del usuario actualmente autenticado.
     * Establece un `SnapshotListener` para escuchar cambios en tiempo real.
     */
    fun loadUserDecks() {
        // Determina el UID objetivo: si ownerUid es proporcionado (para ver mazos de amigos), se usa.
        // Si no, usa el UID del usuario actualmente autenticado.
        val targetUid = ownerUid ?: auth.currentUser?.uid

        if (targetUid == null) {
            // Si no hay un UID objetivo (ni ownerUid ni usuario autenticado), no se pueden cargar mazos.
            _uiState.update { it.copy(decks = emptyList(), errorMessage = "Usuario no autenticado o UID de propietario no especificado.", isLoading = false) }
            deckListener?.remove() // Remueve cualquier listener activo.
            deckListener = null
            Log.w("DecksViewModel", "loadUserDecks: No hay UID objetivo para cargar mazos. Mazos vaciados.")
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) } // Establece estado de carga.

        // Remueve el listener anterior antes de crear uno nuevo para evitar múltiples listeners
        // y para asegurar que siempre haya un solo listener activo para el UID actual.
        deckListener?.remove()
        deckListener = null

        // Accede a la colección "decks" y filtra por el campo 'userId' (propietario del mazo).
        // Ordena los mazos por nombre alfabéticamente.
        deckListener = firestore.collection("decks") // Colección principal 'decks'
            .whereEqualTo("userId", targetUid) // Filtra por el userId del propietario
            .orderBy("name", Query.Direction.ASCENDING) // Ordena los mazos por nombre ascendente.
            .addSnapshotListener { snapshot, e -> // Añade un listener en tiempo real.
                if (e != null) {
                    // Manejo de errores en la escucha de Firestore.
                    Log.e("DecksViewModel", "Error al escuchar cambios en mazos para UID $targetUid: ${e.message}", e)
                    _uiState.update { it.copy(errorMessage = "Error al cargar mazos: ${e.message}", isLoading = false) }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Mapea los documentos de Firestore a objetos `Deck`.
                    // Es crucial asignar el ID del documento de Firestore al campo `id` de la clase `Deck`.
                    val decks = snapshot.documents.mapNotNull { document ->
                        document.toObject(Deck::class.java)?.copy(id = document.id)
                    }
                    _uiState.update { it.copy(decks = decks, isLoading = false) }  // Actualiza el estado con los mazos cargados.
                    Log.d("DecksViewModel", "Mazos actualizados para UID $targetUid: ${decks.size}")
                } else {
                    _uiState.update { it.copy(isLoading = false) } // En caso de snapshot nulo (aunque raro con Listener).
                }
            }
    }

    /**
     * Crea un nuevo mazo en Firestore para el usuario actualmente autenticado.
     * Realiza validaciones como nombre no vacío y no duplicado para el mismo usuario.
     *
     * @param name Nombre del nuevo mazo.
     * @param description Descripción del nuevo mazo.
     * @param format Formato del nuevo mazo (ej. "Standard", "Commander").
     */
    fun createNewDeck(name: String, description: String, format: String) {
        val userId = auth.currentUser?.uid // Obtiene el UID del usuario autenticado.
        if (userId == null) {
            _uiState.update { it.copy(errorMessage = "Usuario no autenticado para crear mazo.") }
            return
        }

        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El nombre del mazo no puede estar vacío.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Verifica si ya existe un mazo con el mismo nombre para este usuario.
                val existingDecksQuery = firestore.collection("decks")
                    .whereEqualTo("userId", userId) // Filtra por el propietario.
                    .whereEqualTo("name", name)
                    .get()
                    .await()

                if (existingDecksQuery.documents.isNotEmpty()) {
                    _uiState.update { it.copy(errorMessage = "Ya tienes un mazo con este nombre.", isLoading = false) }
                    return@launch
                }

                // Crea una nueva instancia de Deck con los datos proporcionados.
                val newDeck = Deck(
                    // El campo 'id' se deja vacío; Firestore lo generará automáticamente y se asignará con @DocumentId.
                    userId = userId, // Asigna el UID del propietario al campo 'userId' del mazo
                    name = name,
                    description = description,
                    format = format,
                    cards = emptyMap() // Un nuevo mazo se inicia sin cartas.
                    // 'lastModified' será llenado automáticamente por @ServerTimestamp
                )

                // Añade el nuevo mazo a la colección 'decks' principal en Firestore.
                val docRef = firestore.collection("decks")
                    .add(newDeck)
                    .await()

                val newDeckId = docRef.id // Obtiene el ID real generado por Firestore para el nuevo documento.
                Log.d("DecksViewModel", "Mazo creado con ID: $newDeckId")

                // Actualiza el estado de la UI para comunicar el ID del mazo recién creado.
                // Esto permite que la UI navegue a la pantalla de edición de este mazo.
                _uiState.update { it.copy(newlyCreatedDeckId = newDeckId, isLoading = false, errorMessage = null) }

                // El `SnapshotListener` (`loadUserDecks`) ya se encargará de actualizar la lista de mazos
                // en la UI automáticamente cuando el nuevo mazo sea añadido a Firestore.
            } catch (e: Exception) {
                Log.e("DecksViewModel", "Error al crear mazo: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "Error al crear mazo: ${e.message}", isLoading = false) }
            }
        }
    }

    /**
     * Limpia el `newlyCreatedDeckId` del estado de la UI.
     * Esta función debe ser llamada por la UI *después* de que haya utilizado este ID para navegar
     * a la pantalla de edición del mazo, para evitar re-navegaciones accidentales en futuras recomposiciones.
     */
    fun clearNewlyCreatedDeckId() {
        _uiState.update { it.copy(newlyCreatedDeckId = null) }
    }

    /**
     * Muestra el diálogo de confirmación para eliminar un mazo y guarda el ID del mazo seleccionado.
     *
     * @param deckId El ID del mazo que se pretende eliminar.
     */
    fun showDeleteDeckDialog(deckId: String) {
        _uiState.update {
            it.copy(
                showDeleteDeckDialog = true, // Establece la visibilidad del diálogo a true.
                selectedDeckId = deckId // Almacena el ID del mazo que se va a eliminar.
            )
        }
    }

    /**
     * Oculta el diálogo de confirmación de eliminación de mazo y limpia el ID del mazo seleccionado.
     */
    fun dismissDeleteDeckDialog() {
        _uiState.update {
            it.copy(
                showDeleteDeckDialog = false, // Oculta el diálogo.
                selectedDeckId = null // Limpia el ID del mazo, ya que el diálogo se ha cerrado.
            )
        }
    }

    /**
     * Confirma la eliminación de un mazo.
     * Se invoca cuando el usuario acepta la eliminación en el diálogo de confirmación.
     * Llama a la función interna que realiza la eliminación real del mazo.
     */
    fun confirmDeleteDeck() {
        // Verifica si hay un ID de mazo seleccionado antes de intentar borrar.
        _uiState.value.selectedDeckId?.let { deckId ->
            // Llama a la función privada que contiene la lógica de borrado real.
            deleteDeckInternal(deckId)
        }
        // Independientemente de si se encontró un ID o no, el diálogo se cierra.
        dismissDeleteDeckDialog()
    }

    /**
     * Realiza la operación de eliminación de un mazo de Firestore.
     * Esta función es privada y solo debe ser invocada después de una confirmación del usuario.
     *
     * @param deckId El ID del mazo a eliminar.
     */
    private fun deleteDeckInternal(deckId: String) {
        viewModelScope.launch {
            val currentUid = auth.currentUser?.uid // Obtiene el UID del usuario actualmente autenticado.

            // Validaciones iniciales de permisos:
            // 1. Si no hay usuario autenticado.
            // 2. Si se está viendo mazos de un amigo (`ownerUid` no es nulo) y el propietario no es el usuario actual.
            if (currentUid == null || (ownerUid != null && ownerUid != currentUid)) {
                _uiState.update { it.copy(errorMessage = "No tienes permiso para eliminar mazos de otros usuarios.", isLoading = false) }
                Log.w("DecksViewModel", "Intento de eliminar mazo de otro usuario. UID actual: $currentUid, UID del mazo: ${ownerUid ?: "N/A"}")
                return@launch // Sale de la corrutina si no hay permiso.
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) } // Establece estado de carga y limpia errores previos.
            try {
                // Obtiene el documento del mazo de la colección 'decks' principal para verificar su propietario.
                val deckDoc = firestore.collection("decks").document(deckId).get().await()
                val deckToDelete = deckDoc.toObject(Deck::class.java)

                // Segunda verificación de permisos: Se asegura de que el `userId` del mazo en Firestore
                // coincide con el UID del usuario autenticado actual.
                if (deckToDelete?.userId == currentUid) {
                    // Si coincide el propietario, procede a eliminar el mazo de Firestore.
                    firestore.collection("decks").document(deckId).delete().await()
                    Log.d("DecksViewModel", "Mazo con ID $deckId borrado por el usuario $currentUid.")
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) } // Actualiza el estado al finalizar con éxito.
                } else {
                    // Si el `userId` del mazo no coincide, deniega el permiso.
                    _uiState.update { it.copy(errorMessage = "No tienes permiso para eliminar este mazo.", isLoading = false) }
                    Log.w("DecksViewModel", "Intento de eliminar mazo no propio. Mazo userId: ${deckToDelete?.userId}, Usuario actual: $currentUid")
                }
            } catch (e: Exception) {
                // Captura y maneja cualquier excepción durante la operación de borrado.
                Log.e("DecksViewModel", "Error al borrar mazo con ID $deckId: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "Error al borrar mazo: ${e.message}", isLoading = false) }
            }
        }
    }

    /**
     * Función pública para iniciar el proceso de eliminación de un mazo.
     * En lugar de borrar directamente, muestra el diálogo de confirmación al usuario.
     *
     * @param deckId El ID del mazo a eliminar.
     */
    fun deleteDeck(deckId: String) {
        showDeleteDeckDialog(deckId) // Llama a la función para mostrar el diálogo de confirmación.
    }

    /**
     * Limpia cualquier mensaje de error actual en el estado de la UI.
     * Puede ser llamado por la UI cuando el error ha sido mostrado y se quiere ocultar.
     */
    fun dismissErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Se llama cuando el ViewModel está a punto de ser destruido.
     * Asegura que los listeners de Firestore y FirebaseAuth sean removidos para evitar fugas de memoria
     * y el consumo innecesario de recursos.
     */
    override fun onCleared() {
        super.onCleared()
        deckListener?.remove() // Remueve el listener de Firestore.
        authStateListener?.let { auth.removeAuthStateListener(it) } // Remueve el listener de FirebaseAuth.
        Log.d("DecksViewModel", "DecksViewModel cleared. Listeners removed.")
    }
}

/**
 * Clase sellada que representa los diferentes estados del proceso de descarga de un mazo.
 * Esto permite a la UI reaccionar de forma específica a cada fase (inactiva, descargando, éxito, error).
 */
sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val deckId: String) : DownloadStatus()
    data class Success(val deckName: String) : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
}