package com.alejandro.magicdeckbuilder.presentation.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejandro.magicdeckbuilder.R
import com.alejandro.magicdeckbuilder.ui.theme.BackgroundButton
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Gray
import com.alejandro.magicdeckbuilder.ui.theme.Orange
import com.alejandro.magicdeckbuilder.ui.theme.ShapeButton
import com.google.firebase.auth.FirebaseAuth

/**
 * Pantalla de inicio de la aplicación, que ofrece opciones de autenticación y acceso offline.
 *
 * @param auth Instancia de [FirebaseAuth] para gestionar el estado de autenticación.
 * @param navigateToLogin Callback para navegar a la pantalla de inicio de sesión.
 * @param navigateToSignUp Callback para navegar a la pantalla de registro.
 * @param navigateToHome Callback para navegar a la pantalla principal (Home).
 * @param navigateToVerificationPending Callback para navegar a la pantalla de verificación de email.
 * @param onGoogleSignInClick Callback para iniciar el flujo de inicio de sesión con Google.
 * @param navigateToLocal Callback para navegar a la pantalla de mazos locales (Modo Offline).
 */
@Composable
fun StartScreen(
    auth: FirebaseAuth,
    navigateToLogin: () -> Unit = {},
    navigateToSignUp: () -> Unit = {},
    navigateToHome: () -> Unit = {},
    navigateToVerificationPending: () -> Unit = {},
    onGoogleSignInClick: () -> Unit = {},
    navigateToLocal: () -> Unit = {}
) {
    /**
     * [DisposableEffect] se utiliza para gestionar efectos secundarios que necesitan limpieza
     * cuando la Composable abandona la composición. Aquí se usa para añadir y remover
     * un [FirebaseAuth.AuthStateListener].
     *
     * La `key = Unit` asegura que el efecto se lance solo una vez.
     */
    DisposableEffect(Unit) {
        // Crea un listener de estado de autenticación de Firebase.
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            // Si el usuario ya ha iniciado sesión...
            if (user != null) {
                // ...y su email está verificado, navega a la pantalla Home.
                if (user.isEmailVerified) {
                    navigateToHome()
                } else {
                    // ...pero su email no está verificado, navega a la pantalla de verificación pendiente.
                    navigateToVerificationPending()
                }
            }
        }
        // Añade el listener a la instancia de FirebaseAuth.
        auth.addAuthStateListener(listener)

        // Bloque `onDispose` se ejecuta cuando la Composable es eliminada de la composición.
        onDispose {
            // Remueve el listener para evitar fugas de memoria.
            auth.removeAuthStateListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()  // Ocupa el tamaño de la pantalla.
            // Aplica un fondo con un gradiente vertical de color gris a negro.
            .background(Brush.verticalGradient(listOf(Gray, Black), startY = 0f, endY = 600f)),
        horizontalAlignment = Alignment.CenterHorizontally // Centra los elementos de la columna horizontalmente.
    ) {
        Spacer(modifier = Modifier.weight(1f)) // Espaciador flexible para empujar el contenido hacia el centro.

        // Logo de la aplicación.
        Image(
            painter = painterResource(id = R.drawable.logo), // Carga el logo.
            contentDescription = "",
            modifier = Modifier.clip(CircleShape) // Recorta la imagen en forma de círculo.
        )
        // Título de la aplicación.
        Text(
            "Magic Deck Builder",
            color = Color.White,
            style = TextStyle(
                fontFamily = FontFamily.Serif, // Fuente Serif.
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.weight(1f)) // Otro espaciador flexible.

        // Botón "Iniciar sesión" con email/contraseña.
        Button(
            onClick = { navigateToLogin() }, // Navega a la pantalla de Login al hacer clic.
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Orange, // Color de fondo del botón.
                contentColor = Color.Black // Color del texto del botón.
            )
        ) {
            Text(
                "Iniciar sesión",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))  // Espacio entre botones.

        // Botón personalizado para implementar la función "Continue with Google".
        CustomButton(
            Modifier.clickable { onGoogleSignInClick() }, // Hace el botón clickeable y llama al callback.
            painterResource(id = R.drawable.google), // Icono de Google.
            "Continuar con Google" // Texto del botón.
        )
        // Texto pulsable para llevar al registro.
        Text(
            text = "¿No tienes cuenta? Regístrate gratis.",
            color = Color.White,
            modifier = Modifier
                .padding(24.dp)
                .clickable { navigateToSignUp() }, // Navega a la pantalla de registro al hacer clic.
            textDecoration = TextDecoration.Underline // Subraya el texto.
        )

        Spacer(modifier = Modifier.height(8.dp)) // Espacio entre elementos.

        // Botón para "Modo Offline".
        Button(
            onClick = { navigateToLocal() }, // Navega a la pantalla de mazos locales.
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Orange,
                contentColor = Color.Black
            )
        ) {
            Text(
                "Modo offline",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(1f)) // Otro espaciador flexible.
    }
}

/**
 * Composable reutilizable para un botón personalizado con icono y texto.
 *
 * @param modifier Modificador para aplicar al botón.
 * @param painter [Painter] para el icono del botón.
 * @param title Texto a mostrar en el botón.
 */
@Composable
fun CustomButton(modifier: Modifier, painter: Painter, title: String) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 32.dp)
            .background(BackgroundButton) // Fondo personalizado.
            .border(2.dp, ShapeButton, CircleShape), // Borde con forma de círculo.
        contentAlignment = Alignment.CenterStart // Centra el contenido al inicio (izquierda).
    ) {
        Row() {
            // Icono del botón.
            Image(
                painter = painter,
                contentDescription = "",
                modifier = Modifier
                    .padding(start = 16.dp) // Relleno a la izquierda del icono.
                    .size(16.dp) // Tamaño del icono.
            )

            Spacer(Modifier.weight(1f))

            // Texto del botón.
            Text(
                text = title,
                color = Color.White,
                modifier = Modifier.fillMaxWidth(), // Ocupa el ancho disponible.
                textAlign = TextAlign.Center, // Centra el texto.
                fontWeight = FontWeight.Bold
            )
        }
    }
}