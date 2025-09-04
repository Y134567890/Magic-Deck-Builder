package com.alejandro.magicdeckbuilder.presentation.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alejandro.magicdeckbuilder.presentation.components.AppTopBar
import com.alejandro.magicdeckbuilder.presentation.components.UsernameDialog
import com.alejandro.magicdeckbuilder.presentation.user.UserViewModel
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Gray
import com.alejandro.magicdeckbuilder.ui.theme.Orange
import com.google.firebase.auth.FirebaseAuth

/**
 * Pantalla principal de la aplicación, actúa como un centro de navegación.
 * Permite al usuario acceder a las diferentes funcionalidades de la aplicación.
 *
 * @param auth Instancia de [FirebaseAuth] para gestionar el estado de autenticación.
 * @param onSignOut Callback para manejar el cierre de sesión.
 * @param navigateToCardSearch Callback para navegar a la pantalla de búsqueda de cartas.
 * @param navigateToDecks Callback para navegar a la pantalla de mazos del usuario.
 * @param navigateToLocalDecks Callback para navegar a la pantalla de mazos locales.
 * @param userViewModel ViewModel que gestiona la información y el estado del usuario.
 * @param onNavigateToFriends Callback para navegar a la pantalla de amigos.
 * @param onNavigateToAccountManagement Función de callback para navegar a la pantalla de gestión de cuenta
 */
@OptIn(ExperimentalMaterial3Api::class) // Opt-in para usar APIs experimentales de Material3.
@Composable
fun HomeScreen(
    auth: FirebaseAuth,
    onSignOut: () -> Unit,
    navigateToCardSearch: () -> Unit,
    navigateToDecks: () -> Unit,
    navigateToLocalDecks: () -> Unit,
    userViewModel: UserViewModel,
    onNavigateToFriends: () -> Unit,
    onNavigateToAccountManagement: () -> Unit
) {
    val uiState by userViewModel.uiState.collectAsState() // Recolecta el estado de la UI del UserViewModel.

    // Este LaunchedEffect observa los cambios en el objeto FirebaseUser.
    // Si el usuario inicia sesión (auth.currentUser pasa de nulo a no nulo), se dispara la carga del usuario.
    LaunchedEffect(auth.currentUser) {
        if (auth.currentUser != null) {
            Log.d("HomeScreen", "auth.currentUser ha cambiado a no nulo. Disparando loadCurrentUser.")
            userViewModel.loadCurrentUser() // Carga/refresca la información del usuario en el ViewModel.
        }
    }

    // Este efecto se ejecutará cuando la bandera `usernameSaved` en el UiState cambie a `true`.
    // Se usa para ejecutar lógica una vez que el nombre de usuario ha sido guardado.
    LaunchedEffect(uiState.usernameSaved) {
        if (uiState.usernameSaved) {
            Log.d("HomeScreen", "Nombre de usuario guardado: ${uiState.currentUser?.username}")
            userViewModel.resetUsernameSavedFlag() // Reinicia la bandera para evitar ejecuciones repetidas.
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize() // Ocupa el tamaño disponible.
            .background(Brush.verticalGradient(listOf(Gray, Black), startY = 0f, endY = 600f)), // Fondo con degradado.
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                username = uiState.currentUser?.username, // Muestra el nombre de usuario obtenido del ViewModel.
                canNavigateBack = false, // La Home Screen no tiene botón de retroceso.
                onNavigateBack = { /* No hace nada en Home */ },
                onSignOut = onSignOut,
                modifier = Modifier, // Modificador para la TopBar.
                onNavigateToFriends = onNavigateToFriends, // Callback para navegar a la pantalla de amigos.
                onNavigateToAccountManagement = onNavigateToAccountManagement // Callback para navegar a gestión de cuenta
            )
            Column(
                modifier = Modifier
                    .fillMaxSize() // Ocupa el espacio restante después de la TopBar.
                    .padding(16.dp), // Relleno alrededor de los botones.
                verticalArrangement = Arrangement.Center, // Centra los elementos verticalmente.
                horizontalAlignment = Alignment.CenterHorizontally // Centra los elementos horizontalmente.
            ) {
                // Botón para navegar a la búsqueda de cartas.
                Button(
                    onClick = navigateToCardSearch,
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
                        "Buscar Cartas",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(80.dp)) // Espacio vertical entre botones principales.
                // Botón para navegar a la gestión de mazos (en la nube).
                Button(
                    onClick = navigateToDecks,
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
                        "Mis Mazos",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp)) // Espacio entre "Mis Mazos" y "Mazos Locales".
                // Botón para navegar a la gestión de mazos locales.
                Button(
                    onClick = navigateToLocalDecks,
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
                        "Mazos Locales",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Muestra el diálogo para pedir el nombre de usuario si es la primera vez o si falta.
    UsernameDialog(
        uiState = uiState, // Pasa el estado de la UI del UserViewModel.
        onUsernameInputChange = userViewModel::onUsernameInputChange, // Callback para cambios en el input del nombre de usuario.
        onPrivacyCheckChange = userViewModel::onPrivacyCheckChange,
        onSaveUsername = userViewModel::saveUsername, // Callback para guardar el nombre de usuario.
        onDismiss = {  } // No permitir cerrar si no hay nombre de usuario (la lógica está en el diálogo)
    )
}