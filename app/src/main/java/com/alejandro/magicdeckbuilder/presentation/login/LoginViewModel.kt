package com.alejandro.magicdeckbuilder.presentation.login

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel para la pantalla de inicio de sesión.
 * Hereda de [BaseAuthViewModel] para gestionar el estado de carga común en operaciones de autenticación.
 * Se encarga de la lógica de negocio para:
 * - Iniciar sesión con email y contraseña.
 * - Iniciar sesión con credenciales de Google.
 * - Validar campos de entrada (email y contraseña).
 * - Manejar el estado de la UI (errores, éxito, habilitación del botón).
 * - Enviar emails de restablecimiento de contraseña.
 */
class LoginViewModel : BaseAuthViewModel() {

    // LiveData para el email ingresado por el usuario.
    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email

    // LiveData para la contraseña ingresada por el usuario.
    private val _password = MutableLiveData<String>()
    val password: LiveData<String> = _password

    // LiveData para controlar la habilitación del botón de login.
    private val _loginEnable = MutableLiveData<Boolean>()
    val loginEnable: LiveData<Boolean> = _loginEnable

    // LiveData para indicar el éxito del inicio de sesión.
    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    // LiveData para mensajes de error a mostrar en la UI. Null si no hay error.
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Intenta iniciar sesión en Firebase usando credenciales de Google.
     * Esta función es llamada después de que el usuario ha seleccionado una cuenta de Google
     * y se ha obtenido la [AuthCredential] correspondiente.
     *
     * Utiliza `withLoading` (de [BaseAuthViewModel]) para mostrar un indicador de carga.
     *
     * @param auth La instancia de [FirebaseAuth].
     * @param credential Las credenciales de autenticación de Google.
     */
    fun firebaseAuthWithGoogle(auth: FirebaseAuth, credential: AuthCredential) {
        viewModelScope.launch { // Lanza una corrutina dentro del ViewModelScope.
            withLoading {
                try {
                    // Intenta iniciar sesión en Firebase con las credenciales de Google.
                    auth.signInWithCredential(credential).await()
                    _loginSuccess.value = true // Si tiene éxito, actualiza el estado de éxito.
                    Log.i("Alejandro", "Google sign in success")
                } catch (e: ApiException) {
                    // Captura errores específicos de Google Play Services.
                    _errorMessage.value = "Error al iniciar sesión con Google: ${e.localizedMessage}" // Establece el mensaje de error.
                    Log.i("Alejandro", "Google sign in failed", e)
                }
            }
        }
    }

    /**
     * Se llama cuando el email o la contraseña en los campos de texto de la UI cambian.
     * Actualiza los LiveData correspondientes y recalcula si el botón de login debe estar habilitado.
     *
     * @param email El email actual en el campo de texto.
     * @param password La contraseña actual en el campo de texto.
     */
    fun onLoginChanged(email: String, password: String) {
        _email.value = email // Actualiza el email en el ViewModel.
        _password.value = password // Actualiza la contraseña en el ViewModel.
        // Habilita el botón de login si tanto el email como la contraseña son válidos.
        _loginEnable.value = isValidEmail(email) && isValidPassword(password)
    }

    /**
     * Valida si la contraseña no está vacía.
     * @param password La contraseña a validar.
     * @return `true` si la contraseña no está vacía, `false` en caso contrario.
     */
    private fun isValidPassword(password: String): Boolean = password.isNotEmpty()

    /**
     * Valida si el email tiene un formato válido utilizando [Patterns.EMAIL_ADDRESS].
     * @param email El email a validar.
     * @return `true` si el email tiene un formato válido, `false` en caso contrario.
     */
    private fun isValidEmail(email: String): Boolean = Patterns.EMAIL_ADDRESS.matcher(email).matches()

    /**
     * Intenta iniciar sesión en Firebase con el email y la contraseña actuales.
     * Esta función es llamada cuando el usuario hace clic en el botón de "Login".
     *
     * Utiliza `withLoading` (de [BaseAuthViewModel]) para mostrar un indicador de carga.
     *
     * @param auth La instancia de [FirebaseAuth].
     */
    fun onLoginSelected(auth: FirebaseAuth) {
        // Obtiene los valores actuales del email y la contraseña, usando cadenas vacías como fallback.
        val currentEmail = _email.value ?: ""
        val currentPassword = _password.value ?: ""

        viewModelScope.launch { // Lanza una corrutina.
            withLoading { // Gestiona el estado de carga.
                try {
                    // Intenta iniciar sesión con email y contraseña.
                    auth.signInWithEmailAndPassword(currentEmail, currentPassword).await()
                    _loginSuccess.value = true // Actualiza el estado de éxito.
                    Log.i("Alejandro", "Login OK")
                } catch (e: Exception) {
                    // Captura cualquier excepción durante el proceso de login.
                    _errorMessage.value = "Error al iniciar sesión: ${e.localizedMessage}"
                    Log.i("Alejandro", "Login KO", e)
                }
            }
        }
    }

    /**
     * Limpia el mensaje de error actual.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Envía un email de restablecimiento de contraseña a la dirección de email actual.
     * Muestra un mensaje de éxito o error al usuario.
     *
     * Utiliza `withLoading` para mostrar un indicador de carga.
     *
     * @param auth La instancia de [FirebaseAuth].
     */
    fun sendPasswordResetEmail(auth: FirebaseAuth) {
        val currentEmail = _email.value // Se obtiene el email del campo de texto (LiveData).

        // Valida que el email no sea nulo/vacío y tenga un formato válido.
        if (currentEmail.isNullOrEmpty() || !isValidEmail(currentEmail)) {
            _errorMessage.value = "Por favor, introduce un email válido para restablecer la contraseña."
            return // Sale de la función si el email no es válido.
        }

        viewModelScope.launch { // Lanza una corrutina.
            withLoading { // Gestiona el estado de carga.
                try {
                    // Intenta enviar el email de restablecimiento de contraseña.
                    auth.sendPasswordResetEmail(currentEmail).await()
                    _errorMessage.value = "Se ha enviado un correo electrónico a $currentEmail con instrucciones para restablecer tu contraseña. Revisa tu bandeja de entrada y spam."
                    Log.i("LoginViewModel", "Password reset email sent to $currentEmail")
                } catch (e: Exception) {
                    // Captura cualquier excepción durante el envío del email.
                    _errorMessage.value = "Error al enviar el correo de restablecimiento: ${e.localizedMessage}"
                    Log.e("LoginViewModel", "Error sending password reset email", e)
                }
            }
        }
    }
}