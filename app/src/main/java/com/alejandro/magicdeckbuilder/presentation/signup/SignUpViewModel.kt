package com.alejandro.magicdeckbuilder.presentation.signup

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.alejandro.magicdeckbuilder.presentation.login.BaseAuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel para la pantalla de registro de usuarios.
 * Hereda de [BaseAuthViewModel] para gestionar el estado de carga común en operaciones de autenticación.
 * Se encarga de la lógica de negocio para:
 * - Registrar nuevos usuarios con email y contraseña.
 * - Enviar correos de verificación de email.
 * - Validar campos de entrada (email y contraseña).
 * - Manejar el estado de la UI (errores, éxito de registro, habilitación del botón).
 */
class SignUpViewModel : BaseAuthViewModel() {

    // LiveData para el email ingresado por el usuario.
    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email

    // LiveData para la contraseña ingresada por el usuario.
    private val _password = MutableLiveData<String>()
    val password: LiveData<String> = _password

    // LiveData para controlar la habilitación del botón de registro.
    private val _signUpEnable = MutableLiveData<Boolean>()
    val signUpEnable: LiveData<Boolean> = _signUpEnable

    // LiveData para mensajes de error a mostrar en la UI. Null si no hay error.
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // LiveData para indicar que el proceso de registro se ha completado con éxito.
    private val _registrationComplete = MutableLiveData<Boolean>()
    val registrationComplete: LiveData<Boolean> = _registrationComplete

    /**
     * Se llama cuando el email o la contraseña en los campos de texto de la UI cambian.
     * Actualiza los LiveData correspondientes y recalcula si el botón de registro debe estar habilitado.
     *
     * @param email El email actual en el campo de texto.
     * @param password La contraseña actual en el campo de texto.
     */
    fun onSignUpChanged(email: String, password: String) {
        _email.value = email // Actualiza el email en el ViewModel.
        _password.value = password // Actualiza la contraseña en el ViewModel.
        // Habilita el botón de registro si tanto el email como la contraseña son válidos.
        _signUpEnable.value = isValidEmail(email) && isValidPassword(password)
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
     * Intenta registrar un nuevo usuario en Firebase con el email y la contraseña actuales.
     * Esta función es llamada cuando el usuario hace clic en el botón de "Registrarse".
     *
     * Utiliza `withLoading` (de [BaseAuthViewModel]) para mostrar un indicador de carga.
     *
     * @param auth La instancia de [FirebaseAuth].
     */
    fun onSignUpSelected(auth: FirebaseAuth) {
        // Obtiene los valores actuales del email y la contraseña, usando cadenas vacías como fallback.
        val currentEmail = _email.value ?: ""
        val currentPassword = _password.value ?: ""

        viewModelScope.launch { // Lanza una corrutina dentro del ViewModelScope.
            withLoading { // Envuelve la operación en `withLoading` para gestionar el estado de carga.
                try {
                    // Intenta crear el usuario con email y contraseña.
                    val result = auth.createUserWithEmailAndPassword(currentEmail, currentPassword).await()
                    Log.i("Alejandro", "Registro de usuario creado con éxito")

                    // Si la creación del usuario fue exitosa, envía el correo de verificación.
                    result.user?.sendEmailVerification()?.await()
                    Log.i("Alejandro", "Correo de verificación enviado a ${result.user?.email}")

                    _registrationComplete.value = true // Indica que el registro se ha completado.

                } catch (e: Exception) {
                    // Captura cualquier excepción durante el proceso de registro.
                    _errorMessage.value = "Error al registrarse: ${e.localizedMessage}"
                    Log.i("Alejandro", "Registro KO", e)
                }
            }
        }
    }

    /**
     * Reenvía el correo electrónico de verificación al usuario actualmente autenticado.
     * Esta función es útil si el usuario no recibió el primer correo o necesita reenviarlo.
     *
     * Utiliza `withLoading` para mostrar un indicador de carga.
     *
     * @param auth La instancia de [FirebaseAuth].
     */
    fun resendVerificationEmail(auth: FirebaseAuth) {
        viewModelScope.launch { // Lanza una corrutina.
            withLoading { // Gestiona el estado de carga.
                try {
                    // Obtiene el usuario actual de Firebase y envía el correo de verificación.
                    auth.currentUser?.sendEmailVerification()?.await()
                    _errorMessage.value = "Correo de verificación reenviado. Revisa tu bandeja de entrada."
                    Log.i("Alejandro", "Correo de verificación reenviado")
                } catch (e: Exception) {
                    // Captura cualquier excepción durante el reenvío del correo.
                    _errorMessage.value = "Error al reenviar el correo: ${e.localizedMessage}"
                    Log.i("Alejandro", "Error al reenviar correo", e)
                }
            }
        }
    }

    /**
     * Establece un nuevo mensaje de error en el LiveData [errorMessage].
     * Puede ser usado por la UI para mostrar errores específicos.
     *
     * @param error El mensaje de error a establecer.
     */
    fun newError(error: String){
        _errorMessage.value = error
    }

    /**
     * Limpia el mensaje de error actual, estableciéndolo en `null`.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Limpia la bandera `registrationComplete`, estableciéndola en `false`.
     */
    fun clearRegistrationComplete() {
        _registrationComplete.value = false
    }
}