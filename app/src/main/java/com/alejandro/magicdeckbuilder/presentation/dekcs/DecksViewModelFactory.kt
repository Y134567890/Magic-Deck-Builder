package com.alejandro.magicdeckbuilder.presentation.dekcs

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Factory para crear instancias de [DecksViewModel].
 * Permite inyectar las dependencias necesarias (Application, FirebaseAuth, FirebaseFirestore, ownerUid)
 * en el constructor del [DecksViewModel].
 *
 * @param application La instancia de la aplicación, pasada al AndroidViewModel.
 * @param auth La instancia de FirebaseAuth para la autenticación.
 * @param firestore La instancia de FirebaseFirestore para la base de datos remota.
 * @param ownerUid El UID del propietario de los mazos que se quieren gestionar.
 * Puede ser null si se están viendo los mazos del usuario actual.
 */
class DecksViewModelFactory(
    private val application: Application,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val ownerUid: String?
) : ViewModelProvider.Factory { // Implementa la interfaz ViewModelProvider.Factory.

    /**
     * Crea una nueva instancia de un ViewModel de la clase dada.
     *
     * @param modelClass La clase del ViewModel a crear (en este caso, DecksViewModel).
     * @return Una nueva instancia del ViewModel.
     * @throws IllegalArgumentException si la clase de ViewModel solicitada no es DecksViewModel.
     */
    @Suppress("UNCHECKED_CAST") // Suprime la advertencia de casting inseguro, ya que la comprobación de tipo se realiza explícitamente.
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Se comprueba si la clase solicitada es asignable a DecksViewModel.
        if (modelClass.isAssignableFrom(DecksViewModel::class.java)) {
            // Si es así, crea una nueva instancia de DecksViewModel, pasándole todas las dependencias.
            // El 'as T' es un casting seguro después de la comprobación isAssignableFrom.
            return DecksViewModel(application, auth, firestore, ownerUid) as T
        }
        // Si se solicita una clase de ViewModel diferente, lanza una excepción.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}