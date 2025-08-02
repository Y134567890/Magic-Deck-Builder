package com.alejandro.magicdeckbuilder.presentation.signup

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejandro.magicdeckbuilder.R
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Orange
import com.alejandro.magicdeckbuilder.ui.theme.SelectedField
import com.alejandro.magicdeckbuilder.ui.theme.UnselectedField
import com.google.firebase.auth.FirebaseAuth

/**
 * Pantalla de registro de usuarios.
 * Permite al usuario introducir un email y una contraseña para crear una nueva cuenta.
 *
 * @param auth Instancia de [FirebaseAuth] para interactuar con el sistema de autenticación de Firebase.
 * @param viewModel Instancia de [SignUpViewModel] para gestionar la lógica de negocio y el estado de la UI.
 * @param navigateBack Callback para navegar a la pantalla anterior.
 * @param navigateToVerificationPending Callback para navegar a la pantalla de verificación de email después de un registro exitoso.
 */
@Composable
fun SignUpScreen(
    auth: FirebaseAuth,
    viewModel: SignUpViewModel,
    navigateBack: () -> Unit,
    navigateToVerificationPending: () -> Unit
) {
    // Observa el estado del ViewModel usando LiveData y StateFlow, y las convierte en Composable States.
    val email: String by viewModel.email.observeAsState(initial = "")
    val password: String by viewModel.password.observeAsState(initial = "")
    val signUpEnable: Boolean by viewModel.signUpEnable.observeAsState(initial = false)
    val errorMessage by viewModel.errorMessage.observeAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val registrationComplete by viewModel.registrationComplete.observeAsState(initial = false)

    // Efecto lanzado cuando `registrationComplete` cambia.
    // Si el registro fue exitoso, navega a la pantalla de verificación pendiente.
    LaunchedEffect(registrationComplete) {
        if (registrationComplete) {
            navigateToVerificationPending()
            viewModel.clearRegistrationComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize() // Ocupa el tamaño disponible.
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
            EmailField(email) { viewModel.onSignUpChanged(it, password) } // Campo de texto para el email.
            Spacer(Modifier.height(48.dp))
            PasswordText(Modifier.align(Alignment.Start)) // Etiqueta "Password" alineada al inicio.
            Spacer(Modifier.height(8.dp))
            PasswordField(password) { viewModel.onSignUpChanged(email, it) } // Campo de texto para la contraseña.
            Spacer(Modifier.height(48.dp))
            SignUpButton(signUpEnable, isLoading) { viewModel.onSignUpSelected(auth) } // Botón de Registro.

            Spacer(modifier = Modifier.height(16.dp))

            // Muestra un indicador de progreso circular si `isLoading` es true.
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.wrapContentSize())
            }

            // Muestra el mensaje de error si no es nulo.
            errorMessage?.let {
                Text(
                    text = it,
                    color = Color.Red, // Texto de error en rojo.
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
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
        colors = TextFieldDefaults.colors( // Colores personalizados
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
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // Tipo de teclado para contraseña.
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
 * Composable privado para el botón de "Registrarse".
 *
 * @param signUpEnable Indica si el botón debe estar habilitado.
 * @param isLoading Indica si una operación de carga está en curso (deshabilita el botón).
 * @param onSignUpSelected Callback que se invoca cuando se hace clic en el botón.
 */
@Composable
private fun SignUpButton(signUpEnable: Boolean, isLoading: Boolean, onSignUpSelected: () -> Unit) {
    Button(
        onClick = { onSignUpSelected() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Orange,
            contentColor = Color.Black,
            disabledContainerColor = Color.Gray, // Color cuando el botón está deshabilitado.
        ),
        enabled = signUpEnable && !isLoading // Habilita el botón si `signUpEnable` es true y no está cargando.
    ) {
        Text(
            text = "Registrarse",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
