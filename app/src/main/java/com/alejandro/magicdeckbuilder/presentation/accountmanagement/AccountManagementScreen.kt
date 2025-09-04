package com.alejandro.magicdeckbuilder.presentation.accountmanagement

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.alejandro.magicdeckbuilder.R
import com.alejandro.magicdeckbuilder.presentation.components.AppTopBar
import com.alejandro.magicdeckbuilder.presentation.friendship.ConfirmationDialog
import com.alejandro.magicdeckbuilder.presentation.start.CustomButton
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Orange
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

/**
 * Composable que representa la pantalla de gestión de cuenta.
 * Permite al usuario restablecer su contraseña (si es un usuario de email/contraseña) y eliminar su cuenta.
 *
 * @param auth La instancia de [FirebaseAuth] para gestionar la autenticación del usuario.
 * @param onNavigateBack Función de callback para navegar a la pantalla anterior.
 * @param onAccountDeleted Función de callback que se invoca tras el borrado exitoso de la cuenta.
 * @param googleSignInClient El cliente de Google Sign-In, necesario para la reautenticación de usuarios de Google.
 * @param viewModel La instancia de [AccountManagementViewModel] que gestiona el estado y la lógica de la pantalla.
 */
@Composable
fun AccountManagementScreen(
    auth: FirebaseAuth,
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    googleSignInClient: GoogleSignInClient,
    viewModel: AccountManagementViewModel
) {
    // Observa el estado del ViewModel para saber si se ha de mostrar el diálogo para reautenticación
    val showReauthDialog by viewModel.showReauthDialog.observeAsState(initial = false)

    // Observa el estado del ViewModel para saber si la cuenta se ha borrado.
    val isAccountDeleted by viewModel.isAccountDeleted.observeAsState(initial = false)

    // Observa el estado del ViewModel para saber si el usuario puede modificar la contrasña
    // (si el usuario ha iniciado sesión mediante el método usuario-contraseña, dispone de una contraseña que poder modificar).
    val canResetPassword by viewModel.canResetPassword.observeAsState(initial = false)

    // Observa el estado del ViewModel para saber si se ha de mostrar el diálogo de confirmación.
    val showDeleteConfirmationDialog by viewModel.showDeleteConfirmationDialog.observeAsState(initial = false)

    // Observa el estado del ViewModel para los mensajes de la UI.
    val errorMessage by viewModel.errorMessage.observeAsState()
    val successMessage by viewModel.successMessage.observeAsState()

    // Si la cuenta ha sido borrada, navega fuera de esta pantalla.
    LaunchedEffect(isAccountDeleted) {
        if (isAccountDeleted) {
            onAccountDeleted()
        }
    }

    Scaffold(
        // Barra superior de la aplicación para esta pantalla
        topBar = {
            AppTopBar(
                username = "Gestión de cuenta",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                onSignOut = { /* No se usa aquí */ },
                onNavigateToFriends = { /* No se usa aquí */ },
                hideSection = true, // Ocultamos el menú desplegable en esta pantalla.
                onNavigateToAccountManagement = { /* No se usa aquí */ }
            )
        },
        containerColor = Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Muestra el texto para restablecer la contraseña solo si el usuario no es de Google.
            if (canResetPassword) {
                Button(
                    onClick = { viewModel.sendPasswordResetEmail(auth) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Orange,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Cambiar contraseña via email")
                }

                Spacer(Modifier.height(48.dp))
            }

            // Botón para eliminar la cuenta (en rojo)
            Button(
                onClick = { viewModel.onDeleteAccountClicked() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Eliminar Cuenta")
            }

            Spacer(Modifier.height(48.dp))

            // Muestra un indicador de carga si el ViewModel lo indica.
            if (viewModel.isLoading.collectAsState().value) {
                CircularProgressIndicator(modifier = Modifier.wrapContentSize())
            }

            // Muestra mensajes de éxito o error.
            successMessage?.let {
                Text(
                    text = it,
                    color = Color.Green,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            errorMessage?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }

    // Si ha pasado demasiado tiempo desde que el usuario inició sesión, Firebase bloquea el borrado de la cuenta por seguridad. Por tanto,
    // el usuario deberá iniciar sesión de nuevo. Para estos casos, abrimos el diálogo correspondiente.
    if (showReauthDialog) {
        ReauthDialog(
            auth = auth,
            viewModel = viewModel,
            googleSignInClient = googleSignInClient,
            onReauthenticateWithGoogle = { credential ->
                viewModel.reauthenticateAndDelete(credential)
            }
        )    }

    // Muestra el diálogo de confirmación antes de proceder con el borrado.
    if (showDeleteConfirmationDialog) {
        ConfirmationDialog(
            title = "¿Eliminar cuenta?",
            message = "Estás a punto de eliminar tu cuenta de forma permanente. Esta acción no se puede deshacer. ¿Estás seguro?",
            onConfirm = { viewModel.onConfirmDeleteAccount() },
            onDismiss = { viewModel.onDismissDeleteConfirmation() }
        )
    }
}

/**
 * Composable que muestra un diálogo para solicitar al usuario que se reautentique.
 * El método de reautenticación (contraseña o Google) depende del proveedor original del usuario.
 *
 * @param auth La instancia de [FirebaseAuth].
 * @param viewModel El [AccountManagementViewModel] que gestiona la lógica del diálogo.
 * @param googleSignInClient El cliente de Google Sign-In, usado para la reautenticación de usuarios de Google.
 * @param onReauthenticateWithGoogle Callback que se invoca cuando un usuario de Google se reautentica con éxito.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReauthDialog(
    auth: FirebaseAuth,
    viewModel: AccountManagementViewModel,
    googleSignInClient: GoogleSignInClient,
    onReauthenticateWithGoogle: (AuthCredential) -> Unit
) {
    // Observa el input de la contraseña en el ViewModel.
    val passwordInput by viewModel.reauthPasswordInput.observeAsState("")
    // Observa si el usuario está autenticado con email/contraseña para mostrar la UI correcta.
    val isEmailPasswordUser by viewModel.canResetPassword.observeAsState(initial = false)

    // Lanza el intent de Google Sign-In para obtener el resultado de la actividad.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            onReauthenticateWithGoogle(credential)
        } catch (e: ApiException) {
            Log.w("ReauthDialog", "Google sign in failed", e)
        }
    }

    AlertDialog(
        onDismissRequest = { viewModel.onDismissReauthDialog() },
        title = { Text("Reautenticación requerida", color = White) },
        text = {
            if (isEmailPasswordUser) {
                // UI para usuarios de email/contraseña
                Column {
                    Text("Por favor, introduce tu contraseña para confirmar la acción.", color = White)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { viewModel.setReauthPasswordInput(it) },
                        label = { Text("Contraseña", color = White) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedLabelColor = Orange,
                            focusedBorderColor = Orange,
                            cursorColor = White,
                            focusedTextColor = White
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            } else {
                // UI para usuarios de Google
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Para confirmar, necesitas volver a iniciar sesión con tu cuenta de Google.", color = White)
                    Spacer(Modifier.height(16.dp))
                    CustomButton(
                        modifier = Modifier.clickable {
                            val signInIntent = googleSignInClient.signInIntent
                            launcher.launch(signInIntent)
                        },
                        painter = painterResource(id = R.drawable.google),
                        title = "Continuar con Google"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isEmailPasswordUser) {
                        val credential = EmailAuthProvider.getCredential(auth.currentUser?.email!!, passwordInput)
                        viewModel.reauthenticateAndDelete(credential)
                    } else {
                        // La confirmación en el caso de inicio de sesión con Gmail se integra en el CustomButton
                    }
                }
            ) {
                Text("Confirmar", color = Red)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.onDismissReauthDialog() }) {
                Text("Cancelar", color = White)
            }
        },
        containerColor = Black.copy(alpha = 0.95f),
        titleContentColor = White,
        textContentColor = White
    )
}