package com.alejandro.magicdeckbuilder.presentation.friendship

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alejandro.magicdeckbuilder.data.models.Friendship
import com.alejandro.magicdeckbuilder.data.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Estado de la UI para la pantalla de amistades (`FriendshipScreen`).
 * Contiene toda la información necesaria para renderizar la UI de gestión de amigos y solicitudes.
 */
data class FriendshipUiState(
    val currentUserUid: String? = null, // UID del usuario actualmente autenticado.
    val friendsAndRequests: List<Friendship> = emptyList(), // Lista de amigos y solicitudes.
    val searchResults: List<User> = emptyList(), // Resultados de la búsqueda de usuarios.
    val isSearching: Boolean = false, // Indica si se está realizando una búsqueda.
    val isLoadingFriends: Boolean = false, // Indica si se están cargando los amigos/solicitudes.
    val errorMessage: String? = null, // Mensaje de error general.
    val infoMessage: String? = null,  // Mensaje de información/éxito para el usuario.
    val showSearchResultsDialog: Boolean = false, // Controla la visibilidad del diálogo de resultados de búsqueda.
    val showCancelDialog: Boolean = false, // Controla la visibilidad del diálogo de confirmación de cancelar solicitud.
    val showRejectDialog: Boolean = false, // Controla la visibilidad del diálogo de confirmación de rechazar solicitud.
    val showRemoveDialog: Boolean = false, // Controla la visibilidad del diálogo de confirmación de eliminar amigo.
    val selectedFriendshipId: String? = null // ID de la amistad seleccionada para acciones de confirmación.
)

/**
 * ViewModel para la pantalla de gestión de amistades.
 * Maneja la lógica de autenticación, la interacción con Firestore para amigos y solicitudes,
 * y la lógica de búsqueda de usuarios.
 *
 * @param auth La instancia de [FirebaseAuth] para la autenticación.
 * @param firestore La instancia de [FirebaseFirestore] para la base de datos remota.
 */
class FriendshipViewModel(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() { // Extiende ViewModel, no AndroidViewModel, ya que no necesita el contexto de la aplicación.

    // MutableStateFlow privado para gestionar el estado de la UI.
    private val _uiState = MutableStateFlow(FriendshipUiState())
    // StateFlow público de solo lectura que expone el estado de la UI a la Composable.
    val uiState: StateFlow<FriendshipUiState> = _uiState.asStateFlow()

    // Listener para observar cambios en el estado de autenticación de Firebase.
    private val authStateListener: FirebaseAuth.AuthStateListener

    // Objeto para guardar la referencia al listener de Firestore de amigos y solicitudes.
    // Esto es crucial para poder removerlo cuando ya no sea necesario y evitar fugas de memoria.
    private var friendsListenerRegistration: ListenerRegistration? = null

    init {
        // 1. Inicializar el ViewModel con el usuario actual si ya está logueado al momento de la creación del ViewModel.
        val initialUser = auth.currentUser
        if (initialUser != null) {
            _uiState.update { it.copy(currentUserUid = initialUser.uid, errorMessage = null, infoMessage = null) }
            listenToUserFriendships(initialUser.uid) // Inicia la escucha de amistades para este usuario.
            Log.d("FriendshipViewModel", "ViewModel init: Usuario ya logueado. UID: ${initialUser.uid}")
        } else {
            // Si no hay usuario al inicio, limpiar el estado.
            _uiState.update {
                it.copy(
                    currentUserUid = null,
                    friendsAndRequests = emptyList(),
                    searchResults = emptyList(),
                    errorMessage = null,
                    infoMessage = null
                )
            }
            Log.d("FriendshipViewModel", "ViewModel init: No hay usuario logueado.")
        }

        // 2. Configurar el AuthStateListener para manejar futuros cambios de autenticación (login/logout).
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // Si hay un usuario logueado y su UID es diferente al actual en el UiState, o si antes era nulo, actualizar.
                if (_uiState.value.currentUserUid != user.uid) {
                    _uiState.update { it.copy(currentUserUid = user.uid, errorMessage = null, infoMessage = null) }
                    listenToUserFriendships(user.uid)  // Reinicia la escucha de amistades para el nuevo usuario.
                    Log.d("FriendshipViewModel", "AuthStateChanged: UID actualizado a ${user.uid}")
                }
            } else {
                // Si no hay usuario logueado (ej. el usuario ha cerrado sesión), limpiar el estado de la UI
                // y detener el listener de Firestore para evitar escuchar datos de un usuario que ya no está.
                friendsListenerRegistration?.remove()
                friendsListenerRegistration = null
                _uiState.update {
                    it.copy(
                        currentUserUid = null,
                        friendsAndRequests = emptyList(),
                        searchResults = emptyList(),
                        errorMessage = null,
                        infoMessage = null
                    )
                }
                Log.d("FriendshipViewModel", "AuthStateChanged: Usuario deslogueado. Estado limpiado y listener de amigos detenido.")
            }
        }
        auth.addAuthStateListener(authStateListener) // Añade el listener al inicio.
    }

    /**
     * Se llama cuando el ViewModel está a punto de ser destruido.
     * Asegura que los listeners de FirebaseAuth y Firestore sean removidos para evitar fugas de memoria.
     */
    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener) // Remueve el listener de autenticación.
        friendsListenerRegistration?.remove() // Remueve el listener de Firestore de amigos.
    }

    /**
     * Inicia o reinicia la escucha en tiempo real de las relaciones de amistad del usuario dado.
     * Se suscribe a la subcolección 'friends' del usuario en Firestore.
     *
     * @param uid El UID del usuario cuyas amistades se van a escuchar.
     */
    private fun listenToUserFriendships(uid: String) {
        // Se remueve cualquier listener previo antes de añadir uno nuevo para evitar duplicados.
        friendsListenerRegistration?.remove()

        _uiState.update { it.copy(isLoadingFriends = true, errorMessage = null, infoMessage = null) }
        friendsListenerRegistration = firestore.collection("users").document(uid).collection("friends")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    // Manejo de errores en la escucha de Firestore.
                    _uiState.update { it.copy(errorMessage = "Error al cargar amigos: ${e.message}", isLoadingFriends = false, infoMessage = null) }
                    Log.e("FriendshipViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Mapea los documentos de Firestore a objetos `Friendship`.
                    // Es crucial asignar el ID del documento (que es el otherUserUid) al campo `otherUserUid` de la clase `Friendship`.
                    val friends = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Friendship::class.java)?.copy(otherUserUid = doc.id)
                    }
                    _uiState.update { it.copy(friendsAndRequests = friends, isLoadingFriends = false, errorMessage = null, infoMessage = null) }
                    Log.d("FriendshipViewModel", "Amigos/Solicitudes actualizados: ${friends.size} elementos.")
                } else {
                    _uiState.update { it.copy(isLoadingFriends = false, errorMessage = null, infoMessage = null) }
                }
            }
    }

    /**
     * Busca usuarios en Firestore por nombre de usuario que contenga la consulta.
     * Excluye al usuario actual de los resultados de búsqueda.
     *
     * @param query La cadena de texto a buscar en los nombres de usuario.
     */
    fun searchUsers(query: String) {
        if (query.isBlank()) {
            // Si la consulta está vacía, limpiar resultados y ocultar diálogo de búsqueda.
            _uiState.update { it.copy(searchResults = emptyList(), showSearchResultsDialog = false, errorMessage = null, infoMessage = null) }
            return
        }

        _uiState.update { it.copy(isSearching = true, errorMessage = null, infoMessage = null) } // Establece estado de búsqueda.
        viewModelScope.launch {
            val currentUserId = _uiState.value.currentUserUid

            if (currentUserId == null) {
                _uiState.update { it.copy(errorMessage = "Usuario no autenticado para buscar.", isSearching = false, infoMessage = null) }
                Log.e("FriendshipViewModel", "Error: Intento de búsqueda sin UID autenticado.")
                return@launch
            }

            try {
                val usersRef = firestore.collection("users")
                val usersSnapshot = usersRef.get().await() // Obtiene todos los usuarios.

                val results = usersSnapshot.documents
                    .mapNotNull { doc ->
                        doc.toObject(User::class.java) // Mapea documentos a objetos User.
                    }
                    .filter { user ->
                        // Filtra para excluir al propio usuario y encontrar usuarios con nombre que contenga la query.
                        user.uid != currentUserId && user.username.contains(query, ignoreCase = true)
                    }

                // Filtra adicionalmente los resultados de búsqueda para no mostrar usuarios con los que ya hay una relación.
                val filteredResults = results.filter { searchedUser ->
                    !_uiState.value.friendsAndRequests.any { friendship ->
                        friendship.otherUserUid == searchedUser.uid
                    }
                }

                _uiState.update { it.copy(searchResults = filteredResults, isSearching = false, showSearchResultsDialog = true, errorMessage = null, infoMessage = null) }

            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error al buscar usuarios: ${e.message}", isSearching = false, infoMessage = null) }
                Log.e("FriendshipViewModel", "Error searching users.", e)
            }
        }
    }

    /**
     * Función interna para actualizar el estado de una relación de amistad en Firestore para ambos usuarios.
     * Esto asegura la coherencia en la base de datos distribuida.
     *
     * @param otherUserUid El UID del otro usuario en la relación.
     * @param currentUserFriendshipStatus El estado de amistad para el usuario actual (ej., "pending_sent", "accepted").
     * @param otherUserFriendshipStatus El estado de amistad para el otro usuario (ej., "pending_received", "accepted").
     * @param acceptedAt Marca de tiempo de aceptación, si aplica.
     * @param successMessage Mensaje a mostrar en caso de éxito.
     */
    private fun updateFriendshipStatus(
        otherUserUid: String,
        currentUserFriendshipStatus: String,
        otherUserFriendshipStatus: String,
        acceptedAt: Timestamp? = null,
        successMessage: String? = null
    ) = viewModelScope.launch {
        val currentUserUid = _uiState.value.currentUserUid ?: run {
            _uiState.update { it.copy(errorMessage = "Usuario no autenticado para actualizar amistad.") }
            return@launch // Si no hay usuario, sale de la corrutina.
        }
        try {
            // Obtener el nombre de usuario y email del otro usuario.
            val otherUserDoc = firestore.collection("users").document(otherUserUid).get().await()
            val otherUsername = otherUserDoc.getString("username") ?: "Desconocido"
            val otherUserEmail = otherUserDoc.getString("email") ?: "desconocido@example.com"

            // Obtener el nombre de usuario y email del usuario actual.
            val currentUserDoc = firestore.collection("users").document(currentUserUid).get().await()
            val currentUsername = currentUserDoc.getString("username") ?: "Desconocido"
            val currentUserEmail = currentUserDoc.getString("email") ?: "desconocido@example.com"

            // Crear el objeto Friendship para el usuario actual.
            val currentUserFriendship = Friendship(
                otherUserUid = otherUserUid,
                status = currentUserFriendshipStatus,
                requestedAt = Timestamp.now(), // La marca de tiempo de la solicitud siempre se establece en el momento actual de la acción.
                acceptedAt = acceptedAt,
                username = otherUsername,
                email = otherUserEmail
            )
            // Guardar la relación de amistad en la subcolección 'friends' del usuario actual.
            firestore.collection("users").document(currentUserUid).collection("friends")
                .document(otherUserUid).set(currentUserFriendship).await()

            // Crear el objeto Friendship para el otro usuario (perspectiva inversa).
            val otherUserFriendship = Friendship(
                otherUserUid = currentUserUid,
                status = otherUserFriendshipStatus,
                requestedAt = Timestamp.now(), // La marca de tiempo de la solicitud siempre se establece en el momento actual de la acción.
                acceptedAt = acceptedAt,
                username = currentUsername,
                email = currentUserEmail
            )
            // Guardar la relación de amistad en la subcolección 'friends' del otro usuario.
            firestore.collection("users").document(otherUserUid).collection("friends")
                .document(currentUserUid).set(otherUserFriendship).await()

            _uiState.update { it.copy(infoMessage = successMessage, errorMessage = null) } // Mensaje de éxito.
            Log.d("FriendshipViewModel", "Relación de amistad actualizada entre $currentUserUid y $otherUserUid. Status: $currentUserFriendshipStatus. Mensaje: $successMessage")
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Error al actualizar amistad: ${e.message}", infoMessage = null) } // Mensaje de error.
            Log.e("FriendshipViewModel", "Error updating friendship.", e)
        }
    }

    /**
     * Envía una solicitud de amistad a otro usuario.
     * Comprueba si ya existe una relación pendiente o aceptada antes de enviar.
     *
     * @param targetUser El objeto [User] al que se le enviará la solicitud.
     */
    fun sendFriendRequest(targetUser: User) {
        viewModelScope.launch {
            val currentUserUid = _uiState.value.currentUserUid
            if (currentUserUid == null) {
                _uiState.update { it.copy(errorMessage = "Usuario no autenticado.") }
                return@launch
            }

            // Comprueba si ya hay una relación existente (enviada, recibida o aceptada).
            val existingFriendship = _uiState.value.friendsAndRequests.firstOrNull {
                it.otherUserUid == targetUser.uid && (it.status == "pending_sent" || it.status == "pending_received" || it.status == "accepted")
            }

            if (existingFriendship != null) {
                // Si ya existe una relación, muestra un mensaje de información.
                _uiState.update { it.copy(infoMessage = "Ya existe una relación de amistad o solicitud pendiente con ${targetUser.username}.", errorMessage = null) }
                return@launch
            }
            // Llama a la función de actualización para establecer el estado de la solicitud.
            updateFriendshipStatus(
                targetUser.uid,
                "pending_sent", // Estado para el usuario actual: solicitud enviada.
                "pending_received", // Estado para el otro usuario: solicitud recibida.
                successMessage = "Solicitud de amistad enviada a ${targetUser.username}."
            )
            dismissSearchResultsDialog() // Oculta el diálogo de búsqueda después de enviar.
        }
    }

    /**
     * Acepta una solicitud de amistad.
     * Cambia el estado de la amistad a "accepted" para ambos usuarios.
     *
     * @param otherUserUid El UID del usuario cuya solicitud se acepta.
     */
    fun acceptFriendRequest(otherUserUid: String) {
        updateFriendshipStatus(otherUserUid, "accepted", "accepted", Timestamp.now(), "Solicitud aceptada. ¡Ahora sois amigos!")
    }

    /**
     * Muestra el diálogo de confirmación para rechazar una solicitud de amistad.
     *
     * @param otherUserUid El UID del usuario cuya solicitud se va a rechazar.
     */
    fun rejectFriendRequest(otherUserUid: String) {
        showRejectDialog(otherUserUid)
    }

    /**
     * Confirma y ejecuta el rechazo de una solicitud de amistad.
     * Elimina los documentos de amistad de ambas subcolecciones de Firestore.
     */
    fun confirmRejectFriendRequest() = viewModelScope.launch {
        val currentUserUid = _uiState.value.currentUserUid ?: return@launch
        val otherUserUid = _uiState.value.selectedFriendshipId ?: return@launch

        try {
            // Elimina el documento de amistad de la subcolección del usuario actual.
            firestore.collection("users").document(currentUserUid).collection("friends")
                .document(otherUserUid).delete().await()
            // Elimina el documento de amistad de la subcolección del otro usuario.
            firestore.collection("users").document(otherUserUid).collection("friends")
                .document(currentUserUid).delete().await()

            _uiState.update { it.copy(infoMessage = "Solicitud rechazada.", errorMessage = null) }
            Log.d("FriendshipViewModel", "Solicitud de amistad de $otherUserUid rechazada por $currentUserUid.")
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Error al rechazar solicitud: ${e.message}", infoMessage = null) }
            Log.e("FriendshipViewModel", "Error rejecting friend request.", e)
        } finally {
            dismissRejectDialog() // Siempre oculta el diálogo de rechazo al finalizar.
        }
    }

    /**
     * Muestra el diálogo de confirmación para cancelar una solicitud de amistad enviada.
     *
     * @param otherUserUid El UID del usuario al que se le envió la solicitud.
     */
    fun cancelFriendRequest(otherUserUid: String) {
        showCancelDialog(otherUserUid)
    }

    /**
     * Confirma y ejecuta la cancelación de una solicitud de amistad enviada.
     * Elimina los documentos de amistad de ambas subcolecciones de Firestore.
     */
    fun confirmCancelFriendRequest() = viewModelScope.launch {
        val currentUserUid = _uiState.value.currentUserUid ?: return@launch // Obtiene el UID del usuario actual.
        val otherUserUid = _uiState.value.selectedFriendshipId ?: return@launch // Obtiene el UID del amigo seleccionado.

        try {
            // Elimina el documento de amistad de la subcolección del usuario actual.
            firestore.collection("users").document(currentUserUid).collection("friends")
                .document(otherUserUid).delete().await()
            // Elimina el documento de amistad de la subcolección del otro usuario.
            firestore.collection("users").document(otherUserUid).collection("friends")
                .document(currentUserUid).delete().await()

            _uiState.update { it.copy(infoMessage = "Solicitud cancelada.", errorMessage = null) }
            Log.d("FriendshipViewModel", "Solicitud de amistad a $otherUserUid cancelada por $currentUserUid.")
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Error al cancelar solicitud: ${e.message}", infoMessage = null) }
            Log.e("FriendshipViewModel", "Error canceling friend request.", e)
        } finally {
            dismissCancelDialog() // Siempre oculta el diálogo de cancelación al finalizar.
        }
    }

    /**
     * Muestra el diálogo de confirmación para eliminar a un amigo.
     *
     * @param otherUserUid El UID del amigo a eliminar.
     */
    fun removeFriend(otherUserUid: String) {
        showRemoveDialog(otherUserUid)
    }

    /**
     * Confirma y ejecuta la eliminación de un amigo.
     * Elimina los documentos de amistad de ambas subcolecciones de Firestore.
     */
    fun confirmRemoveFriend() = viewModelScope.launch {
        val currentUserUid = _uiState.value.currentUserUid ?: return@launch // Obtiene el UID del usuario actual.
        val otherUserUid = _uiState.value.selectedFriendshipId ?: return@launch // Obtiene el UID del amigo seleccionado.

        try {
            // Elimina el documento de amistad de la subcolección del usuario actual.
            firestore.collection("users").document(currentUserUid).collection("friends")
                .document(otherUserUid).delete().await()
            // Elimina el documento de amistad de la subcolección del otro usuario.
            firestore.collection("users").document(otherUserUid).collection("friends")
                .document(currentUserUid).delete().await()

            _uiState.update { it.copy(infoMessage = "Amigo eliminado.", errorMessage = null) }
            Log.d("FriendshipViewModel", "Amistad con $otherUserUid eliminada por $currentUserUid.")
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Error al eliminar amistad: ${e.message}", infoMessage = null) }
            Log.e("FriendshipViewModel", "Error removing friend.", e)
        } finally {
            dismissRemoveDialog() // Siempre oculta el diálogo de eliminación al finalizar.
        }
    }

    /**
     * Oculta el diálogo de resultados de búsqueda y limpia los resultados.
     */
    fun dismissSearchResultsDialog() {
        _uiState.update { it.copy(showSearchResultsDialog = false, searchResults = emptyList(), errorMessage = null, infoMessage = null) }
    }

    /**
     * Muestra el diálogo de cancelar solicitud y establece el ID de la amistad seleccionada.
     *
     * @param friendshipId El ID de la amistad (UID del otro usuario) a cancelar.
     */
    fun showCancelDialog(friendshipId: String) {
        _uiState.update { it.copy(showCancelDialog = true, selectedFriendshipId = friendshipId, errorMessage = null, infoMessage = null) }
    }

    /**
     * Oculta el diálogo de cancelar solicitud y limpia el ID de la amistad seleccionada.
     */
    fun dismissCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = false, selectedFriendshipId = null, errorMessage = null, infoMessage = null) }
    }

    /**
     * Muestra el diálogo de rechazar solicitud y establece el ID de la amistad seleccionada.
     *
     * @param friendshipId El ID de la amistad (UID del otro usuario) a rechazar.
     */
    fun showRejectDialog(friendshipId: String) {
        _uiState.update { it.copy(showRejectDialog = true, selectedFriendshipId = friendshipId, errorMessage = null, infoMessage = null) }
    }

    /**
     * Oculta el diálogo de rechazar solicitud y limpia el ID de la amistad seleccionada.
     */
    fun dismissRejectDialog() {
        _uiState.update { it.copy(showRejectDialog = false, selectedFriendshipId = null) }
    }

    /**
     * Muestra el diálogo de eliminar amigo y establece el ID de la amistad seleccionada.
     *
     * @param friendshipId El ID de la amistad (UID del otro usuario) a eliminar.
     */
    fun showRemoveDialog(friendshipId: String) {
        _uiState.update { it.copy(showRemoveDialog = true, selectedFriendshipId = friendshipId) }
    }

    /**
     * Oculta el diálogo de eliminar amigo y limpia el ID de la amistad seleccionada.
     */
    fun dismissRemoveDialog() {
        _uiState.update { it.copy(selectedFriendshipId = null, showRemoveDialog = false) }
    }

    /**
     * Limpia tanto los mensajes de error como los de información/éxito del estado de la UI.
     * Esto permite a la UI ocultar cualquier notificación de mensaje.
     */
    fun dismissErrorMessage() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) } // ¡CAMBIO!
    }
}