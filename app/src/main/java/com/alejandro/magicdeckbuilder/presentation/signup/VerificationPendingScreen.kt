package com.alejandro.magicdeckbuilder.presentation.signup

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.google.firebase.auth.FirebaseAuth

/**
 * Pantalla que se muestra a los usuarios mientras esperan la verificación de su correo electrónico.
 * Ofrece opciones para reenviar el correo, verificar el estado o volver al login.
 *
 * @param auth Instancia de [FirebaseAuth] para interactuar con el sistema de autenticación de Firebase.
 * @param viewModel Instancia de [SignUpViewModel].
 * @param navigateToLogin Callback para navegar de vuelta a la pantalla de inicio de sesión.
 * @param navigateToHome Callback para navegar a la pantalla principal de la aplicación una vez verificado el email.
 */
@Composable
fun VerificationPendingScreen(
    auth: FirebaseAuth,
    viewModel: SignUpViewModel,
    navigateToLogin: () -> Unit,
    navigateToHome: () -> Unit
) {
    // Observa el mensaje de error y el estado de carga del ViewModel.
    val errorMessage by viewModel.errorMessage.observeAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Efecto lanzado una única vez al entrar en la Composición (debido a `Unit`).
    // Intenta recargar el usuario de Firebase para verificar su estado de email.
    LaunchedEffect(Unit) {

        auth.currentUser?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("Verification", "User reloaded. Email verified: ${auth.currentUser?.isEmailVerified}")
                // Si el usuario recargado tiene el email verificado, navega a Home.
                if (auth.currentUser?.isEmailVerified == true) {
                    navigateToHome()
                }
            } else {
                // Registra un error si la recarga del usuario falla.
                Log.e("Verification", "Failed to reload user", task.exception)
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize() // Ocupa el tamaño de la pantalla.
            .background(Black) // Fondo negro.
            .padding(24.dp), // Relleno general para la columna.
        horizontalAlignment = Alignment.CenterHorizontally, // Centra los elementos horizontalmente.
        verticalArrangement = Arrangement.Center // Centra los elementos verticalmente.
    ) {
        Text(
            text = "¡Gracias por registrarte!",
            fontSize = 24.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Hemos enviado un correo electrónico de verificación a tu dirección. Por favor, haz clic en el enlace del correo para activar tu cuenta.",
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Botón para reenviar el correo de verificación.
        Button(
            onClick = {
                viewModel.resendVerificationEmail(auth) // Llama a la función del ViewModel.
            },
            enabled = !isLoading // Deshabilita el botón mientras una operación está cargando.
        ) {
            Text("Reenviar correo de verificación")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Botón para comprobar que el usuario ha verificado su correo.
        Button(
            onClick = {
                // Intenta recargar el usuario y verifica el estado del email.
                auth.currentUser?.reload()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (auth.currentUser?.isEmailVerified == true) {
                            navigateToHome() // Si está verificado, navega a Home.
                        } else {
                            viewModel.clearError() // Limpia cualquier error previo.
                            viewModel.newError("Tu correo aún no ha sido verificado. Por favor, revisa tu bandeja de entrada.")
                        }
                    } else {
                        viewModel.newError("Error al verificar el estado de tu cuenta.")
                    }
                }
            },
            enabled = !isLoading
        ) {
            Text("Ya verifiqué mi correo / Entrar")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Botón para volver al login (ej. si el usuario quiere usar otra cuenta).
        Button(
            onClick = {
                auth.signOut() // Cierra la sesión actual de Firebase.
                navigateToLogin() // Navega a la pantalla de login.
            },
            enabled = !isLoading
        ) {
            Text("Volver a Iniciar Sesión (o usar otra cuenta)")
        }

        // Muestra el mensaje de error si no es nulo.
        errorMessage?.let {
            Text(
                text = it,
                color = Color.Red,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        // Muestra un indicador de progreso circular si `isLoading` es true.
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }
    }
}