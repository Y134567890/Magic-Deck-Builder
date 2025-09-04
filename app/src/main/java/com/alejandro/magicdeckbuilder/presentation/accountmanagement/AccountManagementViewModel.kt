package com.alejandro.magicdeckbuilder.presentation.accountmanagement

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel que gestiona la lógica de la pantalla de gestión de cuenta.
 *
 * Se encarga de las operaciones relacionadas con la cuenta del usuario como el borrado,
 * el restablecimiento de contraseña y la gestión de la reautenticación para operaciones seguras.
 */
class AccountManagementViewModel : ViewModel() {

    // Estado para controlar la visibilidad del diálogo de confirmación de borrado.
    private val _showDeleteConfirmationDialog = MutableLiveData(false)
    val showDeleteConfirmationDialog: LiveData<Boolean> = _showDeleteConfirmationDialog

    // Estado para controlar la visibilidad del diálogo de reautenticación.
    private val _showReauthDialog = MutableLiveData<Boolean>()
    val showReauthDialog: LiveData<Boolean> = _showReauthDialog

    // Estado para controlar la visibilidad del indicador de carga.
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Estado para el mensaje de éxito
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    // Estado para el mensaje de error
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Estado para el texto introducido en el campo de contraseña del diálogo de reautenticación.
    private val _reauthPasswordInput = MutableLiveData<String>()
    val reauthPasswordInput: LiveData<String> = _reauthPasswordInput

    // Estado que indica si la cuenta se ha eliminado correctamente.
    private val _isAccountDeleted = MutableLiveData(false)
    val isAccountDeleted: LiveData<Boolean> = _isAccountDeleted

    // Estado que indica si el usuario puede restablecer su contraseña (solo para usuarios de email/contraseña).
    private val _canResetPassword = MutableLiveData(false)
    val canResetPassword: LiveData<Boolean> = _canResetPassword

    init {
        // Al inicializar el ViewModel, se comprueba el método de autenticación del usuario.
        checkAuthenticationMethod()
    }

    /**
     * Actualiza el valor de la contraseña en el diálogo de reautenticación.
     *
     * @param password La contraseña que el usuario ha introducido.
     */
    fun setReauthPasswordInput(password: String) {
        _reauthPasswordInput.value = password
    }

    /**
     * Oculta el diálogo de reautenticación y limpia el campo de contraseña.
     */
    fun onDismissReauthDialog() {
        _showReauthDialog.value = false
        _reauthPasswordInput.value = ""
    }

    /**
     * Comprueba el método de autenticación del usuario actual (ej. email/contraseña o Google).
     *
     * Este método se usa para decidir si se debe mostrar la opción de restablecer la contraseña.
     */
    private fun checkAuthenticationMethod() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            // Se considera un usuario de email si su proveedor es "password".
            val isEmailUser = firebaseUser.providerData.any { it.providerId == "password" }
            _canResetPassword.value = isEmailUser
        }
    }

    /**
     * Envía un email de restablecimiento de contraseña al email del usuario actual.
     *
     * @param auth La instancia de [FirebaseAuth].
     */
    fun sendPasswordResetEmail(auth: FirebaseAuth) {
        viewModelScope.launch {
            _isLoading.update { true }
            _successMessage.value = null
            _errorMessage.value = null
            val email = auth.currentUser?.email
            if (email != null) {
                try {
                    auth.sendPasswordResetEmail(email).await()
                    _successMessage.value = "Se ha enviado un correo de restablecimiento de contraseña a $email. Revisa tu bandeja de entrada y spam."
                } catch (e: Exception) {
                    _errorMessage.value = "Error al enviar el correo de restablecimiento: ${e.message}"
                } finally {
                    _isLoading.update { false }
                }
            } else {
                _errorMessage.value = "No se pudo encontrar el email del usuario."
                _isLoading.update { false }
            }
        }
    }

    /**
     * Se invoca cuando el usuario hace clic en el botón "Eliminar Cuenta".
     * Muestra el diálogo de confirmación para prevenir borrados accidentales.
     */
    fun onDeleteAccountClicked() {
        _showDeleteConfirmationDialog.value = true
        clearMessages() // Limpiamos cualquier mensaje de éxito o error previo.
    }

    /**
     * Se invoca cuando el usuario descarta el diálogo de confirmación.
     * Oculta el diálogo.
     */
    fun onDismissDeleteConfirmation() {
        _showDeleteConfirmationDialog.value = false
    }

    /**
     * Se invoca cuando el usuario confirma la eliminación de la cuenta en el diálogo.
     * Inicia el proceso de borrado de la cuenta.
     */
    fun onConfirmDeleteAccount() {
        _showDeleteConfirmationDialog.value = false
        deleteAccount()
    }

    /**
     * Elimina la cuenta de autenticación de Firebase y todos los datos asociados en Firestore.
     *
     * La lógica de borrado sigue este orden:
     * 1. Elimina todos los mazos del usuario.
     * 2. Elimina el documento de usuario.
     * 3. Elimina la cuenta de autenticación.
     * Esto asegura que los datos se borren correctamente antes de que se pierda el acceso al ID de usuario.
     */
    fun deleteAccount() {
        viewModelScope.launch {
            _isLoading.update { true }
            _successMessage.value = null
            _errorMessage.value = null

            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            val firestore = FirebaseFirestore.getInstance()

            // Si no hay un usuario autenticado, no hay nada que borrar.
            if (currentUser == null) {
                _errorMessage.value = "No se pudo encontrar el usuario para eliminar."
                _isLoading.update { false }
                return@launch
            }

            try {
                // Paso 1: Obtener todos los mazos del usuario.
                val decksRef = firestore.collection("decks").whereEqualTo("userId", currentUser.uid)
                val decksQuerySnapshot = decksRef.get().await()

                // Paso 2: Crear un lote de escritura para las operaciones de borrado.
                val batch = firestore.batch()

                // Paso 3: Añadir la eliminación de cada mazo al lote.
                for (document in decksQuerySnapshot.documents) {
                    batch.delete(document.reference)
                }

                // Paso 4: Añadir la eliminación del documento del usuario en la colección "users".
                val userRef = firestore.collection("users").document(currentUser.uid)
                batch.delete(userRef)

                // Paso 5: Ejecutar el lote de escritura. Si falla, se lanzará una excepción aquí.
                batch.commit().await()

                // Paso 6: Si los datos de Firestore se eliminaron con éxito, procedemos a borrar la cuenta de autenticación.
                currentUser.delete().await()

                // Paso 7: Si el proceso ha tenido éxito, actualizamos el estado.
                _isAccountDeleted.value = true
            } catch (e: Exception) {
                // Maneja el caso en que el usuario debe reautenticarse por seguridad.
                if (e is FirebaseAuthRecentLoginRequiredException) {
                    _showReauthDialog.value = true
                    _errorMessage.value = "Tu sesión ha caducado. Por favor, vuelve a iniciar sesión para confirmar la acción."
                } else {
                    _errorMessage.value = "Error al eliminar la cuenta: ${e.message}"
                }
            } finally {
                _isLoading.update { false }
            }
        }
    }

    /**
     * Intenta reautenticar al usuario con sus credenciales y luego elimina su cuenta.
     *
     * @param credential Las credenciales del usuario (ej. email y contraseña o Google).
     */
    fun reauthenticateAndDelete(credential: AuthCredential) {
        viewModelScope.launch {
            _isLoading.update { true }
            _errorMessage.value = null
            try {
                // Primero reautentica al usuario
                FirebaseAuth.getInstance().currentUser?.reauthenticate(credential)?.await()
                // Si la reautenticación es exitosa, ocultamos el diálogo y reintentamos el borrado.
                _showReauthDialog.value = false
                deleteAccount()
            } catch (e: Exception) {
                _errorMessage.value = "Error al reautenticar: ${e.message}"
            } finally {
                _isLoading.update { false }
            }
        }
    }

    /**
     * Función de utilidad para limpiar los mensajes de éxito y error.
     */
    private fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }
}