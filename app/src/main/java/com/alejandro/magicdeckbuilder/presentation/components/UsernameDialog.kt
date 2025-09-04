package com.alejandro.magicdeckbuilder.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alejandro.magicdeckbuilder.presentation.signup.PrivacyPolicyCheckbox
import com.alejandro.magicdeckbuilder.presentation.user.UserUiState
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Orange

/**
 * Componente Composable que muestra un diálogo modal para que el usuario elija un nombre de usuario.
 * Se utiliza al inicio si el usuario no tiene un nombre de usuario asignado.
 *
 * @param uiState El [UserUiState] actual que contiene el estado del diálogo (texto de entrada, errores, etc.).
 * @param onUsernameInputChange Función de callback que se invoca cuando el texto del nombre de usuario cambia.
 * @param onSaveUsername Función de callback que se invoca cuando el usuario intenta guardar el nombre de usuario.
 * @param onDismiss Función de callback que se invoca cuando el diálogo debería ser cerrado.
 * Aunque en este caso el diálogo no se cierra por interacción del usuario, es buena práctica mantener este callback
 * para que el componente sea más flexible y reutilizable si en el futuro se desea permitir el cierre por otros medios.
 */
@OptIn(ExperimentalMaterial3Api::class) // Se requiere para usar OutlinedTextField de Material 3
@Composable
fun UsernameDialog(
    uiState: UserUiState,
    onUsernameInputChange: (String) -> Unit,
    onPrivacyCheckChange: (Boolean) -> Unit,
    onSaveUsername: () -> Unit,
    onDismiss: () -> Unit
) {
    // El diálogo solo se muestra si `uiState.showUsernameDialog` es true.
    if (uiState.showUsernameDialog) {
        Dialog(
            onDismissRequest = onDismiss, // El callback onDismiss se llama si el diálogo se cierra (ej. por código externo).
            // Propiedades del diálogo que impiden que se cierre al presionar el botón de atrás o al tocar fuera.
            // Esto obliga al usuario a introducir un nombre de usuario.
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            // `Surface` es un contenedor de Material Design para el contenido del diálogo.
            Surface(
                modifier = Modifier
                    .fillMaxWidth() // Ocupa el ancho disponible.
                    .wrapContentHeight(), // Ajusta su altura al contenido.
                shape = RoundedCornerShape(16.dp), // Esquinas redondeadas para el diálogo.
                color = Black.copy(alpha = 0.85f), // Fondo del diálogo: Negro semi-transparente.
                shadowElevation = 24.dp, // Sombra para darle profundidad y destacarlo.
            ) {
                // Columna para organizar los elementos dentro del diálogo.
                Column(
                    modifier = Modifier.padding(24.dp), // Padding interno para el contenido.
                    verticalArrangement = Arrangement.spacedBy(16.dp) // Espacio vertical entre los elementos hijos.
                ) {
                    // Título de bienvenida.
                    Text(
                        text = "Bienvenido a Magic Deck Builder",
                        style = MaterialTheme.typography.titleLarge.copy(color = White)  // Estilo y color personalizados.
                    )
                    // Mensaje explicativo para el usuario.
                    Text(
                        text = "Antes de continuar, por favor elige un nombre de usuario.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = White.copy(alpha = 0.7f)) // Estilo y color semi-transparente.
                    )

                    // Campo de texto para que el usuario introduzca el nombre de usuario.
                    OutlinedTextField(
                        value = uiState.usernameInput, // El texto actual del campo, desde el estado del ViewModel.
                        onValueChange = onUsernameInputChange, // Callback para actualizar el texto en el ViewModel.
                        label = { Text("Nombre de usuario", color = White.copy(alpha = 0.7f)) }, // Etiqueta del campo.
                        isError = !uiState.isUsernameUnique, // Muestra el estado de error si el nombre de usuario no es único.
                        singleLine = true, // Limita el input a una sola línea.
                        modifier = Modifier.fillMaxWidth(), // Ocupa el ancho.
                        colors = TextFieldDefaults.outlinedTextFieldColors( // Colores personalizados para el TextField.
                            focusedBorderColor = Orange,
                            unfocusedBorderColor = White.copy(alpha = 0.5f),
                            focusedLabelColor = Orange,
                            unfocusedLabelColor = White.copy(alpha = 0.5f),
                            cursorColor = White,
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            containerColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), // Muestra "Hecho" en el teclado.
                        keyboardActions = KeyboardActions(
                            onDone = { // Al presionar "Hecho" en el teclado, intenta guardar el nombre.
                                onSaveUsername()
                            }
                        )
                    )

                    // Mensaje de error si el nombre de usuario no es único.
                    if (!uiState.isUsernameUnique) {
                        Text(
                            text = uiState.errorMessage ?: "Este nombre de usuario ya está en uso.", // Mensaje de error (si existe, sino un default).
                            color = MaterialTheme.colorScheme.error, // Color de error del tema.
                            style = MaterialTheme.typography.bodySmall, // Estilo de texto pequeño.
                            modifier = Modifier.padding(start = 16.dp) // Padding para alinear con la entrada de texto.
                        )
                    }

                    // Mensaje de error genérico si existe y NO es un error de unicidad.
                    // Esto maneja otros tipos de errores que puedan surgir al guardar el nombre.
                    if (uiState.errorMessage != null && uiState.isUsernameUnique) {
                        Text(
                            text = uiState.errorMessage, // Muestra el mensaje de error.
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    if (uiState.isGoogleUser) {
                        PrivacyPolicyCheckbox(
                            isChecked = uiState.privacyAccepted,
                            onCheckedChange = onPrivacyCheckChange
                        )
                    }

                    // Botón para guardar el nombre de usuario.
                    Button(
                        onClick = onSaveUsername, // Invoca el callback para guardar el nombre.
                        // El botón está habilitado solo si:
                        //      - El input no está vacío
                        //      - No hay una carga en progreso
                        //      - El usuario no inició sesión con cuenta de Google o aceptó la política de privacidad
                        enabled = uiState.usernameInput.isNotBlank()
                                && !uiState.isLoading
                                && (!uiState.isGoogleUser || uiState.privacyAccepted),
                        modifier = Modifier.fillMaxWidth(), // Ocupa el ancho.
                        colors = ButtonDefaults.buttonColors( // Colores personalizados para el botón.
                            containerColor = Orange,
                            contentColor = Color.Black // Color del texto/icono dentro del botón.
                        )
                    ) {
                        // Si está cargando, muestra un indicador de progreso dentro del botón.
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Color.Black, // Color del indicador.
                                modifier = Modifier.size(24.dp) // Tamaño del indicador.
                            )
                        } else {
                            // Si no está cargando, muestra el texto del botón.
                            Text("Guardar nombre de usuario")
                        }
                    }
                }
            }
        }
    }
}