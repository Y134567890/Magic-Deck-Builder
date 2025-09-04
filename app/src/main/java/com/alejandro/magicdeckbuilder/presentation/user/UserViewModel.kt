package com.alejandro.magicdeckbuilder.presentation.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alejandro.magicdeckbuilder.data.models.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Clase de datos que representa el estado de la UI para la información del usuario.
 * Contiene todos los datos que la UI necesita para renderizar la pantalla relacionada con el usuario.
 *
 * @property currentUser El objeto [User] actualmente cargado, o `null` si no hay usuario o no se ha cargado.
 * @property usernameInput El texto actual en el campo de entrada del nombre de usuario.
 * @property showUsernameDialog Indica si se debe mostrar el diálogo para establecer el nombre de usuario.
 * @property isLoading Indica si alguna operación de carga o guardado está en curso.
 * @property errorMessage Mensaje de error a mostrar al usuario, o `null` si no hay error.
 * @property isUsernameUnique Indica si el nombre de usuario propuesto es único (para validación).
 * @property usernameSaved Indica si el nombre de usuario se guardó exitosamente.
 * @property isGoogleUser Indica si el usuario inicia sesión con una cuenta de Google.
 * @property privacyAccepted Indica si el usuario ha aceptado la política de privacidad.
 */
data class UserUiState(
    val currentUser: User? = null,
    val usernameInput: String = "",
    val showUsernameDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUsernameUnique: Boolean = true,
    val usernameSaved: Boolean = false,
    val isGoogleUser: Boolean = false,
    val privacyAccepted: Boolean = false
)

/**
 * ViewModel que gestiona la lógica de negocio y el estado de la UI relacionados con el usuario.
 * Esto incluye la carga de perfiles de usuario, la gestión del nombre de usuario y la interacción con Firestore.
 *
 * @param auth La instancia de [FirebaseAuth] para obtener el usuario autenticado.
 * @param firestore La instancia de [FirebaseFirestore] para interactuar con la base de datos.
 */
class UserViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    // MutableStateFlow privado para el estado interno del ViewModel.
    private val _uiState = MutableStateFlow(UserUiState())
    // StateFlow público e inmutable para que la UI observe los cambios de estado.
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    /**
     * Bloque de inicialización del ViewModel.
     * Se llama inmediatamente después de que se crea una instancia de `UserViewModel`.
     * Aquí se inicia la carga del usuario actual.
     */
    init {
        Log.d("UserViewModel", "UserViewModel inicializado, llamando a loadCurrentUser()")
        loadCurrentUser()
    }

    /**
     * Carga la información del usuario actualmente autenticado desde Firestore.
     * Actualiza el [_uiState] con los datos del usuario y determina si se debe mostrar
     * el diálogo para establecer el nombre de usuario (si el usuario es nuevo o no tiene username).
     * También detecta si el usuario ha iniciado sesión con Google, para mostrar el checkbox de
     * aceptación de política de privacidad
     */
    fun loadCurrentUser() {
        viewModelScope.launch {
            // Establece el estado de carga y limpia cualquier mensaje de error anterior.
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val firebaseUser = auth.currentUser // Obtiene el usuario autenticado de Firebase.

            Log.d("UserViewModel", "loadCurrentUser: UID de FirebaseUser = ${firebaseUser?.uid}")

            if (firebaseUser != null) {

                val isGoogleUser = firebaseUser.providerData.any { it.providerId == "google.com" }

                try {
                    // Intenta obtener el documento del usuario de la colección "users" en Firestore.
                    val userDocRef = firestore.collection("users").document(firebaseUser.uid)
                    val userDoc = userDocRef.get().await()  // Espera a que la operación se complete.
                    val user = userDoc.toObject(User::class.java) // Convierte el documento a un objeto User.

                    Log.d("UserViewModel", "loadCurrentUser: userDoc.exists() = ${userDoc.exists()}")
                    Log.d("UserViewModel", "loadCurrentUser: Objeto de usuario cargado = $user")

                    // Actualiza el _uiState con la información cargada.
                    _uiState.update { currentState ->
                        // Determina si el diálogo de nombre de usuario debe mostrarse.
                        // Se muestra si el documento del usuario no existe o si el nombre de usuario está en blanco.
                        val shouldShowDialog = user == null || user.username.isBlank()
                        Log.d("UserViewModel", "loadCurrentUser: shouldShowDialog = $shouldShowDialog (user=$user, username.isBlank=${user?.username.isNullOrBlank()})")
                        currentState.copy(
                            currentUser = user, // Asigna el usuario cargado.
                            showUsernameDialog = shouldShowDialog, // Actualiza el estado del diálogo.
                            isLoading = false, // Desactiva el estado de carga.
                            isGoogleUser = isGoogleUser // Actualiza el estado.
                        )
                    }
                } catch (e: Exception) {
                    Log.e("UserViewModel", "loadCurrentUser: Error al cargar usuario", e)
                    // En caso de error, actualiza el estado con el mensaje de error y desactiva la carga.
                    _uiState.update { it.copy(errorMessage = "Error al cargar usuario: ${e.message}", isLoading = false) }
                }
            } else {
                // Si no hay usuario de Firebase autenticado, actualiza el estado.
                Log.d("UserViewModel", "loadCurrentUser: No se encontró usuario de Firebase autenticado.")
                _uiState.update { it.copy(isLoading = false, currentUser = null, showUsernameDialog = false) }
            }
        }
    }

    /**
     * Actualiza el estado de aceptación de la política de privacidad.
     * Esta función es llamada por el Composable cuando se marca el checkbox.
     *
     * @param isAccepted El estado del checkbox.
     */
    fun onPrivacyCheckChange(isAccepted: Boolean) {
        _uiState.update { it.copy(privacyAccepted = isAccepted) }
    }

    /**
     * Actualiza el valor del campo de entrada del nombre de usuario en el estado de la UI.
     * También resetea los indicadores de unicidad y error.
     *
     * @param newUsername El nuevo texto ingresado por el usuario.
     */
    fun onUsernameInputChange(newUsername: String) {
        _uiState.update { it.copy(usernameInput = newUsername, isUsernameUnique = true, errorMessage = null) }
    }

    /**
     * Intenta guardar el nombre de usuario ingresado por el usuario en Firestore.
     * Realiza una comprobación de unicidad antes de guardar.
     * Impide la creación del usuario si no se aceptó la política de privacidad.
     */
    fun saveUsername() {
        viewModelScope.launch {
            // Establece el estado de carga y limpia cualquier mensaje de error anterior.
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentUsername = _uiState.value.usernameInput.trim() // Obtiene y recorta el nombre de usuario.

            // Validación de campo vacío.
            if (currentUsername.isBlank()) {
                _uiState.update { it.copy(errorMessage = "El nombre de usuario no puede estar vacío.", isLoading = false) }
                return@launch // Sale de la corrutina.
            }

            // Si el usuario es de Google y no ha aceptado la política, muestra un error.
            if (_uiState.value.isGoogleUser && !_uiState.value.privacyAccepted) {
                _uiState.update { it.copy(errorMessage = "Debes aceptar la política de privacidad para continuar.", isLoading = false) }
                return@launch
            }

            try {
                // Paso 1: Verificar si el nombre de usuario ya existe en Firestore.
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("username", currentUsername) // Busca documentos con este username.
                    .get()
                    .await() // Espera a que la consulta se complete.

                // Si la consulta devuelve documentos, el nombre de usuario no es único.
                if (!querySnapshot.isEmpty) {
                    _uiState.update { it.copy(isUsernameUnique = false, errorMessage = "Este nombre de usuario ya está en uso. Por favor, elige otro.", isLoading = false) }
                    return@launch // Sale de la corrutina.
                }

                // Paso 2: Si es único, guardar el nombre de usuario para el usuario actual.
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    val userRef = firestore.collection("users").document(firebaseUser.uid) // Referencia al documento del usuario.
                    val userData = User( // Crea un objeto User con la información actualizada.
                        uid = firebaseUser.uid,
                        username = currentUsername,
                        email = firebaseUser.email ?: "",
                        // Mantiene la fecha de creación si ya existía, si no, usa la actual.
                        createdAt = _uiState.value.currentUser?.createdAt ?: Timestamp.now()
                    )
                    userRef.set(userData).await() // Guarda el objeto User en Firestore.

                    // Actualiza el _uiState tras un guardado exitoso.
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentUser = userData,  // Asigna el usuario actualizado.
                            showUsernameDialog = false, // Oculta el diálogo.
                            usernameSaved = true, // Establece la bandera de guardado.
                            isLoading = false // Desactiva el estado de carga.
                        )
                    }
                } else {
                    // Si no hay usuario autenticado, muestra un error.
                    _uiState.update { it.copy(errorMessage = "Usuario no autenticado.", isLoading = false) }
                }

            } catch (e: Exception) {
                // Captura cualquier excepción durante el proceso de guardado.
                _uiState.update { it.copy(errorMessage = "Error al guardar el nombre de usuario: ${e.message}", isLoading = false) }
            }
        }
    }

    /**
     * Realiza el reset de la bandera `usernameSaved` a `false`.
     * Esto es útil para que la UI pueda reaccionar a un nuevo guardado en el futuro
     * sin que la bandera se quede fijada en `true`.
     */
    fun resetUsernameSavedFlag() {
        _uiState.update { it.copy(usernameSaved = false) }
    }
}