package com.alejandro.magicdeckbuilder.presentation.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Clase base para ViewModels que gestionan operaciones de autenticación.
 * Proporciona un mecanismo centralizado para controlar el estado de carga (`isLoading`)
 * durante operaciones asíncronas, como el inicio de sesión o el registro.
 *
 * Esta clase está marcada como `open` para permitir que otros ViewModels la extiendan
 * y hereden su funcionalidad.
 */
open class BaseAuthViewModel : ViewModel() {

    // MutableStateFlow protegido que expone el estado de carga actual.
    // Es 'protected' para que solo las clases que hereden de BaseAuthViewModel puedan modificarlo directamente,
    // pero se expone como un StateFlow inmutable para la UI.
    protected val _isLoading = MutableStateFlow(false)

    // StateFlow público de solo lectura que expone el estado de carga a la interfaz de usuario (UI).
    // La UI observará este flujo para mostrar u ocultar indicadores de carga (ej., CircularProgressIndicator).
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Función utilitaria suspendida que envuelve un bloque de código asíncrono
     * y gestiona automáticamente el estado de carga (`_isLoading`).
     *
     * Antes de ejecutar el `block`, establece `_isLoading` a `true`.
     * Después de que el `block` se complete (ya sea con éxito o lanzando una excepción),
     * establece `_isLoading` a `false` en el bloque `finally`.
     *
     * @param block Un bloque de código `suspend` que contiene la operación asíncrona.
     * @return El resultado del `block` si se ejecuta con éxito, o `null` si el `block` lanza una excepción.
     * Nota: El manejo de excepciones en `block` debe hacerse internamente si se desea un comportamiento diferente al `null`.
     */
    protected suspend fun <T> withLoading(block: suspend () -> T): T? {
        _isLoading.value = true
        return try {
            block()
        } finally {
            _isLoading.value = false
        }
    }
}