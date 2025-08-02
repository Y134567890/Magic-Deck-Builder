package com.alejandro.magicdeckbuilder

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.alejandro.magicdeckbuilder.presentation.login.LoginViewModel
import com.alejandro.magicdeckbuilder.presentation.navigation.NavigationWrapper
import com.alejandro.magicdeckbuilder.presentation.user.UserViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * La actividad principal de la aplicación.
 * Esta es la primera pantalla que se lanza y se encarga de la inicialización
 * global de Firebase y Google Sign-In, así como de configurar el gráfico de navegación principal.
 */
class MainActivity : ComponentActivity() {

    // Se declaran las variables lateinit, lo que significa que se inicializarán
    // antes de que se acceda a ellas, pero no en el constructor.
    // Esto es común para componentes que requieren un contexto o se inicializan en `onCreate`.
    private lateinit var navHostController: NavHostController // Controlador de navegación de Jetpack Compose.
    private lateinit var auth: FirebaseAuth // Instancia de Firebase Authentication.
    private lateinit var googleSignInClient: GoogleSignInClient // Cliente para el inicio de sesión con Google.
    private lateinit var launcher: ActivityResultLauncher<Intent> // Lanzador de resultados de actividad para Google Sign-In.
    private lateinit var firestore: FirebaseFirestore // Instancia de Firebase Firestore.

    /**
     * Este método se llama cuando la actividad se crea por primera vez.
     * Aquí se realiza la mayoría de la inicialización de la aplicación.
     *
     * @param savedInstanceState Si la actividad se está recreando, este Bundle contiene los datos
     * más recientes proporcionados por `onSaveInstanceState(Bundle)`.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Habilita el modo "edge-to-edge" para que la UI se extienda detrás de las barras del sistema.
        enableEdgeToEdge()

        // Inicializa las instancias de Firebase Authentication y Firestore.
        auth = Firebase.auth
        firestore = Firebase.firestore

        // Configura las opciones para Google Sign-In.
        // - DEFAULT_SIGN_IN: Opciones básicas de inicio de sesión.
        // - requestIdToken: Solicita un ID token para la integración con Firebase Authentication.
        //   Se necesita el ID del cliente web, que se obtiene de `google-services.json`.
        // - requestEmail: Solicita la dirección de correo electrónico del usuario.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // ID del cliente web para Firebase.
            .requestEmail()
            .build()

        // Obtiene una instancia del cliente de Google Sign-In con las opciones configuradas.
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Configura el ActivityResultLauncher para manejar el resultado del inicio de sesión con Google.
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Cuando la actividad de Google Sign-In devuelve un resultado:
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Intenta obtener la cuenta de Google firmada del resultado.
                val account = task.getResult(ApiException::class.java)
                // Crea una credencial de autenticación de Firebase con el ID token de Google.
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                // Llama a la función de autenticación de Google de LoginViewModel.
                LoginViewModel().firebaseAuthWithGoogle(auth, credential)

            } catch (e: ApiException) {
                // Maneja los errores de la API de Google Sign-In.
                Log.w("Alejandro", "Google sign in failed", e)
            }
        }

        // Establece el contenido de la UI de la actividad utilizando Jetpack Compose.
        setContent {

            // auth.signOut()
            // googleSignInClient.signOut().addOnCompleteListener { Log.i("Alejandro", "Sesión de Google cerrada") }

            // Obtiene una instancia de UserViewModel.
            // `viewModel()` se encarga de crear o proporcionar una instancia existente
            // del ViewModel, vinculándola al ciclo de vida de la actividad/composable.
            val userViewModel: UserViewModel = viewModel()

            // Crea y recuerda una instancia de NavHostController.
            // `rememberNavController()` crea un controlador de navegación que persiste
            // a través de las recomposiciones.
            navHostController = rememberNavController()

            // Renderiza el Composable principal de navegación, pasándole todas las dependencias.
            NavigationWrapper(navHostController, auth, googleSignInClient, launcher, userViewModel, firestore)
        }
    }
}
