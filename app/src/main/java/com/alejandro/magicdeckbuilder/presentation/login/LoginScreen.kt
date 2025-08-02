package com.alejandro.magicdeckbuilder.presentation.login

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejandro.magicdeckbuilder.R
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Orange
import com.alejandro.magicdeckbuilder.ui.theme.SelectedField
import com.alejandro.magicdeckbuilder.ui.theme.UnselectedField
import com.google.firebase.auth.FirebaseAuth

/**
 * Pantalla de inicio de sesión de la aplicación.
 * Permite al usuario introducir sus credenciales (email y contraseña) para autenticarse.
 * También maneja la navegación post-login y la recuperación de contraseña.
 *
 * @param auth Instancia de [FirebaseAuth] para interactuar con el sistema de autenticación de Firebase.
 * @param viewModel Instancia de [LoginViewModel] para gestionar la lógica de negocio y el estado de la UI.
 * @param navigateBack Callback para navegar a la pantalla anterior.
 * @param navigateToHome Callback para navegar a la pantalla principal después de un login exitoso y verificado.
 * @param navigateToVerificationPending Callback para navegar a una pantalla de "verificación pendiente" si el email no está verificado.
 */
@Composable
fun LoginScreen(
    auth: FirebaseAuth,
    viewModel: LoginViewModel,
    navigateBack: () -> Unit,
    navigateToHome: () -> Unit,
    navigateToVerificationPending: () -> Unit = {}
) {
    // Observa el estado del ViewModel usando LiveData y StateFlow, y las convierte en Composable States.
    val email: String by viewModel.email.observeAsState(initial = "")
    val password: String by viewModel.password.observeAsState(initial = "")
    val loginEnable: Boolean by viewModel.loginEnable.observeAsState(initial = false)
    val loginSuccess by viewModel.loginSuccess.observeAsState() // Estado de éxito del login.
    val errorMessage by viewModel.errorMessage.observeAsState() // Mensaje de error a mostrar.
    val isLoading by viewModel.isLoading.collectAsState() // Estado de carga (desde BaseAuthViewModel).

    // Efecto lanzado cuando `loginSuccess` cambia.
    // Maneja la navegación basada en el resultado del login y el estado de verificación del email.
    LaunchedEffect(loginSuccess) {
        if (loginSuccess == true) { // Si el login fue exitoso.
            val user = auth.currentUser // Obtiene el usuario actual de Firebase.
            if (user != null && user.isEmailVerified) {
                // Si el usuario existe y su email está verificado, navega a la Home.
                navigateToHome()
                viewModel.clearError() // Limpia cualquier error/estado para evitar re-navegación.
            } else if (user != null && !user.isEmailVerified) {
                // Si el usuario existe pero su email NO está verificado, navega a la pantalla de verificación pendiente.
                navigateToVerificationPending()
                viewModel.clearError() // Limpia el estado.
                auth.signOut() // Desconecta al usuario si el email no está verificado para forzar la verificación.
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black) // Fondo negro.
            .padding(horizontal = 32.dp), // Relleno horizontal global.
        contentAlignment = Alignment.Center // Centra el contenido de la caja.
    ) {
        Column(
            modifier = Modifier.fillMaxSize(), // La columna ocupa el tamaño disponible.
            horizontalAlignment = Alignment.CenterHorizontally // Centra los elementos horizontalmente.
        ) {
            Spacer(Modifier.height(60.dp)) // Espacio superior.
            IconoVolver(navigateBack) // Botón de retroceso.
            EmailText(Modifier.align(Alignment.Start)) // Etiqueta "Email" alineada al inicio.
            Spacer(Modifier.height(8.dp))
            EmailField(email) { viewModel.onLoginChanged(it, password) } // Campo de texto para el email.
            Spacer(Modifier.height(48.dp))
            PasswordText(Modifier.align(Alignment.Start)) // Etiqueta "Password" alineada al inicio.
            Spacer(Modifier.height(8.dp))
            PasswordField(password) { viewModel.onLoginChanged(email, it) } // Campo de texto para la contraseña.
            Spacer(Modifier.height(48.dp))
            LoginButton(loginEnable, isLoading) { viewModel.onLoginSelected(auth) } // Botón de Login.

            Spacer(modifier = Modifier.height(16.dp))

            // Muestra un indicador de progreso circular si `isLoading` es true.
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.wrapContentSize())
            }

            // Muestra el mensaje de error si no es nulo.
            errorMessage?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Texto para "Olvidaste la contraseña" con acción clickeable.
            Text(
                text = "¿Olvidaste la contraseña? Restablécela.",
                color = White,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .clickable { viewModel.sendPasswordResetEmail(auth) },  // Llama a la función del ViewModel para enviar el email de restablecimiento.
                textDecoration = TextDecoration.Underline // Subraya el texto.
            )
        }
    }
}

/**
 * Composable privado para el icono de "volver".
 *
 * @param navigateBack Callback para ejecutar la navegación hacia atrás.
 */
@Composable
private fun IconoVolver(navigateBack: () -> Unit) {
    Row() {
        Icon(
            painter = painterResource(id = R.drawable.ic_back), // Carga el icono desde recursos.
            contentDescription = "",
            tint = White, // Color del icono.
            modifier = Modifier
                .padding(vertical = 24.dp)
                .size(24.dp)
                .clickable { navigateBack() } // Hace el icono clickeable.
        )
        Spacer(modifier = Modifier.weight(1f)) // Empuja el icono hacia la izquierda.
    }
}

/**
 * Composable privado para la etiqueta "Email".
 *
 * @param modifier Modificador para aplicar a este Composable.
 */
@Composable
private fun EmailText(modifier: Modifier) {
    Text(
        "Email",
        color = White,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        modifier = modifier
    )
}

/**
 * Composable privado para el campo de entrada de email.
 *
 * @param email El valor actual del campo de texto.
 * @param onTextFieldChanged Callback que se invoca cuando el texto del campo cambia.
 */
@Composable
private fun EmailField(email: String, onTextFieldChanged: (String) -> Unit) {
    TextField(
        value = email,
        onValueChange = { onTextFieldChanged(it) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = "Email") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), // Tipo de teclado para email.
        singleLine = true, // Permite solo una línea de texto.
        maxLines = 1,
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = UnselectedField,
            focusedContainerColor = SelectedField
        )
    )
}

/**
 * Composable privado para la etiqueta "Password".
 *
 * @param modifier Modificador para aplicar a este Composable.
 */
@Composable
private fun PasswordText(modifier: Modifier) {
    Text(
        "Password",
        color = White,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        modifier = modifier
    )
}

/**
 * Composable privado para el campo de entrada de contraseña.
 *
 * @param password El valor actual del campo de texto.
 * @param onTextFieldChanged Callback que se invoca cuando el texto del campo cambia.
 */
@Composable
private fun PasswordField(password: String, onTextFieldChanged: (String) -> Unit) {
    TextField(
        value = password,
        onValueChange = { onTextFieldChanged(it) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = "Password") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        maxLines = 1,
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = UnselectedField,
            focusedContainerColor = SelectedField
        ),
        visualTransformation = PasswordVisualTransformation()
    )
}

/**
 * Composable privado para el botón de "Login".
 *
 * @param loginEnable Indica si el botón debe estar habilitado.
 * @param isLoading Indica si una operación de carga está en curso (deshabilita el botón).
 * @param onLoginSelected Callback que se invoca cuando se hace clic en el botón.
 */
@Composable
private fun LoginButton(loginEnable: Boolean, isLoading: Boolean, onLoginSelected: () -> Unit) {
    Button(
        onClick = { onLoginSelected() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Orange,
            contentColor = Color.Black,
            disabledContainerColor = Color.Gray, // Color cuando el botón está deshabilitado.
        ),
        enabled = loginEnable && !isLoading // Habilita el botón si `loginEnable` es true y no está cargando.
    ) {
        Text(
            text = "Login",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}