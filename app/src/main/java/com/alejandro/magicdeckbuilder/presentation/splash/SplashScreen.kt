package com.alejandro.magicdeckbuilder.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alejandro.magicdeckbuilder.R
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Gray
import kotlinx.coroutines.delay

/**
 * Pantalla de carga (Splash Screen) que se muestra al inicio de la aplicación.
 * Muestra un logo y, tras un breve retraso, navega automáticamente a la pantalla de inicio (`start`).
 *
 * @param navController El [NavController] utilizado para la navegación entre pantallas.
 */
@Composable
fun SplashScreen(navController: NavController) {

    /**
     * [LaunchedEffect] se utiliza para ejecutar efectos secundarios en una Composable.
     * En este caso, se ejecuta una vez cuando la Composable entra en la composición
     * (debido a la `key = Unit`, que significa que el efecto se lanza solo una vez).
     */
    LaunchedEffect(Unit) {
        // Pausa la ejecución de la corrutina durante 3 segundos.
        // Esto permite que el usuario vea el splash screen durante un tiempo determinado.
        delay(3000) // 3 segundos

        // Navega a la ruta "start".
        navController.navigate("start") {
            // Configuración de popUpTo para limpiar la pila de navegación:
            // `popUpTo("splash")` : Elimina todas las pantallas hasta la ruta "splash".
            // `inclusive = true` : Asegura que la propia "splash" también sea eliminada de la pila.
            // Esto evita que el usuario pueda volver a la SplashScreen usando el botón de retroceso.
            popUpTo("splash") { inclusive = true }
        }
    }

    /**
     * [Box] es un Composable de diseño que se utiliza para apilar elementos uno encima del otro
     * o para centrar un solo elemento.
     * Aquí se usa para ocupar toda la pantalla y centrar su contenido.
     */
    Box(
        modifier = Modifier
            .fillMaxSize() // El Box ocupa el tamaño disponible de la pantalla.
            // Aplica un fondo con un gradiente vertical de color gris a negro.
            .background(Brush.verticalGradient(listOf(Gray, Black), startY = 0f, endY = 600f)),
        contentAlignment = Alignment.Center // Centra el contenido hijo (en este caso, la Column) en el centro de la Box.
    ) {
        /**
         * [Column] es un Composable de diseño que organiza sus elementos hijos verticalmente.
         */
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Muestra el logo de la aplicación.
            Image(
                painter = painterResource(id = R.drawable.logo), // Carga la imagen del logo desde los recursos.
                contentDescription = "Logo", // Descripción para accesibilidad.
                modifier = Modifier.size(100.dp) // Establece el tamaño del logo a 100x100 dp.
            )

            Spacer(modifier = Modifier.height(16.dp)) // Añade un espacio vertical de 16dp debajo del logo.
        }
    }
}
