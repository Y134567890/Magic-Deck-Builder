package com.alejandro.magicdeckbuilder.presentation.friendship

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alejandro.magicdeckbuilder.presentation.components.AppTopBar
import com.alejandro.magicdeckbuilder.data.models.Friendship
import com.alejandro.magicdeckbuilder.data.models.User
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Gray
import com.alejandro.magicdeckbuilder.ui.theme.GreenSuccess
import com.alejandro.magicdeckbuilder.ui.theme.Orange

/**
 * Pantalla principal para la gestión de amistades.
 * Permite buscar usuarios, ver la lista de amigos y solicitudes, y gestionar estas relaciones.
 *
 * @param friendshipViewModel ViewModel que gestiona la lógica de amistades.
 * @param username El nombre de usuario del usuario actual, para mostrar en la TopBar.
 * @param onNavigateBack Lambda para manejar la navegación hacia atrás.
 * @param onSignOut Lambda para manejar el cierre de sesión.
 * @param onNavigateToFriends Lambda para navegar a la propia pantalla de amigos.
 * @param onViewFriendsDecks Callback para navegar a la pantalla de mazos de un amigo,
 * recibe el UID del amigo y su nombre de usuario.
 * @param onNavigateToAccountManagement Función de callback para navegar a la pantalla de gestión de cuenta
 */
@OptIn(ExperimentalMaterial3Api::class) // Opt-in para usar APIs experimentales de Material3.
@Composable
fun FriendshipScreen(
    friendshipViewModel: FriendshipViewModel,
    username: String?,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onViewFriendsDecks: (String, String) -> Unit,
    onNavigateToAccountManagement: () -> Unit
) {
    val uiState by friendshipViewModel.uiState.collectAsState() // Recolecta el estado de la UI del ViewModel.

    var searchQuery by remember { mutableStateOf("") } // Estado para el texto de búsqueda.
    val focusManager = LocalFocusManager.current // Para gestionar el foco del teclado.
    val context = LocalContext.current // Para acceder al InputMethodManager.

    Scaffold(
        topBar = {
            AppTopBar(
                username = username,
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                onSignOut = onSignOut,
                onNavigateToFriends = onNavigateToFriends,
                onNavigateToAccountManagement = onNavigateToAccountManagement
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize() // Ocupa el tamaño disponible.
                .padding(paddingValues) // Aplica el padding de la Scaffold.
                .background(Brush.verticalGradient(listOf(Gray, Black), startY = 0f, endY = 600f)) // Fondo degradado.
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), // Relleno global para el contenido.
                horizontalAlignment = Alignment.CenterHorizontally // Centra los elementos horizontalmente.
            ) {
                // Campo para búsqueda de amigos por nombre de usuario
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar usuarios por nombre", color = White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(), // Ocupa el ancho disponible.
                    colors = OutlinedTextFieldDefaults.colors( // Colores personalizados para el campo de texto.
                        focusedBorderColor = White,
                        unfocusedBorderColor = White.copy(alpha = 0.5f),
                        focusedLabelColor = White,
                        unfocusedLabelColor = White.copy(alpha = 0.5f),
                        cursorColor = White,
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                    ),
                    trailingIcon = {
                        // Muestra un icono de "limpiar" si hay texto en la búsqueda.
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda", tint = White)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), // Acción "Buscar" en el teclado.
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()  // Limpia el foco del campo de texto.
                        // Oculta el teclado virtual.
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        val currentFocusedView = (context as? android.app.Activity)?.currentFocus
                        currentFocusedView?.windowToken?.let {
                            imm.hideSoftInputFromWindow(it, 0)
                        }
                        friendshipViewModel.searchUsers(searchQuery) // Realiza la búsqueda.
                    })
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Amigos y Solicitudes:",
                    style = MaterialTheme.typography.titleMedium,
                    color = White,
                    modifier = Modifier.align(Alignment.Start)  // Alinea este texto al inicio.
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Muestra un indicador de carga si se están cargando amigos/solicitudes.
                if (uiState.isLoadingFriends) {
                    CircularProgressIndicator(color = White, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (uiState.friendsAndRequests.isEmpty()) {
                    // Muestra un mensaje si no hay amigos ni solicitudes.
                    Text(
                        "No tienes amigos ni solicitudes pendientes.",
                        color = White.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp)
                    )
                } else {
                    // Muestra la lista de amigos y solicitudes.
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Ocupa el espacio restante de la columna.
                    ) {
                        items(uiState.friendsAndRequests) { friendship ->
                            FriendshipItem(
                                friendship = friendship,
                                currentUserUid = uiState.currentUserUid,
                                onAccept = friendshipViewModel::acceptFriendRequest,
                                onReject = friendshipViewModel::rejectFriendRequest,
                                onCancel = friendshipViewModel::cancelFriendRequest,
                                onRemove = friendshipViewModel::removeFriend,
                                onViewDecksClick = onViewFriendsDecks // Callback para ver mazos.
                            )
                            Divider(color = White.copy(alpha = 0.2f), thickness = 1.dp) // Divisor entre elementos.
                        }
                    }
                }
            }

            // Diálogo de resultados de búsqueda.
            if (uiState.showSearchResultsDialog) {
                Dialog(
                    onDismissRequest = { friendshipViewModel.dismissSearchResultsDialog() }, // Permite cerrar el diálogo.
                    properties = DialogProperties(usePlatformDefaultWidth = false) // Para un diálogo más ancho.
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f) // 90% del ancho de la pantalla.
                            .fillMaxHeight(0.8f) // 80% de la altura de la pantalla.
                            .clip(RoundedCornerShape(8.dp)), // Esquinas redondeadas.
                        colors = CardDefaults.cardColors(containerColor = Black.copy(alpha = 0.95f)), // Color de fondo semitransparente.
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Resultados de la búsqueda:", style = MaterialTheme.typography.headlineSmall, color = White)
                            Spacer(modifier = Modifier.height(16.dp))

                            if (uiState.isSearching) {
                                CircularProgressIndicator(color = White) // Indicador de carga para la búsqueda.
                            } else if (uiState.searchResults.isEmpty()) {
                                Text("No se encontraron usuarios.", color = White.copy(alpha = 0.6f)) // Mensaje si no hay resultados.
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(uiState.searchResults) { user ->
                                        SearchResultItem(
                                            user = user,
                                            onSendRequest = friendshipViewModel::sendFriendRequest, // Pasa la lista de amistades existentes.
                                            friendsAndRequests = uiState.friendsAndRequests
                                        )
                                        Divider(color = White.copy(alpha = 0.2f), thickness = 1.dp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { friendshipViewModel.dismissSearchResultsDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = White),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Text("Cerrar", color = Black)
                            }
                        }
                    }
                }
            }

            // Diálogos de confirmación para acciones de amistad.
            if (uiState.showCancelDialog) {
                ConfirmationDialog(
                    title = "¿Cancelar solicitud de amistad?",
                    message = "¿Estás seguro de que quieres cancelar la solicitud enviada a este usuario?",
                    onConfirm = { friendshipViewModel.confirmCancelFriendRequest() },
                    onDismiss = { friendshipViewModel.dismissCancelDialog() }
                )
            }
            if (uiState.showRejectDialog) {
                ConfirmationDialog(
                    title = "¿Rechazar solicitud de amistad?",
                    message = "¿Estás seguro de que quieres rechazar la solicitud de este usuario?",
                    onConfirm = { friendshipViewModel.confirmRejectFriendRequest() },
                    onDismiss = { friendshipViewModel.dismissRejectDialog() }
                )
            }
            if (uiState.showRemoveDialog) {
                ConfirmationDialog(
                    title = "¿Eliminar amigo?",
                    message = "¿Estás seguro de que quieres eliminar a este usuario de tus amigos?",
                    onConfirm = { friendshipViewModel.confirmRemoveFriend() },
                    onDismiss = { friendshipViewModel.dismissRemoveDialog() }
                )
            }

            // Diálogo de mensaje de información (éxito).
            if (uiState.infoMessage != null) {
                AlertDialog(
                    onDismissRequest = { friendshipViewModel.dismissErrorMessage() },
                    title = { Text("Éxito", color = GreenSuccess) },
                    text = { Text(uiState.infoMessage ?: "Operación completada.", color = White) },
                    confirmButton = {
                        TextButton(onClick = { friendshipViewModel.dismissErrorMessage() }) {
                            Text("Ok", color = White)
                        }
                    },
                    containerColor = Black, // Fondo del diálogo.
                    titleContentColor = White, // Color del texto del título.
                    textContentColor = White // Color del texto del contenido.
                )
            }

            // Diálogo de mensaje de error.
            if (uiState.errorMessage != null) {
                AlertDialog(
                    onDismissRequest = { friendshipViewModel.dismissErrorMessage() },
                    title = { Text("Error", color = MaterialTheme.colorScheme.error) },
                    text = { Text(uiState.errorMessage ?: "Ha ocurrido un error desconocido.", color = MaterialTheme.colorScheme.onSurface) },
                    confirmButton = {
                        TextButton(onClick = { friendshipViewModel.dismissErrorMessage() }) {
                            Text("Ok", color = White)
                        }
                    },
                    containerColor = Black, // Fondo del diálogo.
                    titleContentColor = White, // Color del texto del título.
                    textContentColor = White // Color del texto del contenido.
                )
            }
        }
    }
}

/**
 * Composable que representa un elemento individual en la lista de amigos/solicitudes.
 * Muestra el nombre de usuario y los botones de acción relevantes según el estado de la amistad.
 *
 * @param friendship El objeto [Friendship] que representa la relación.
 * @param currentUserUid El UID del usuario actual.
 * @param onAccept Callback para aceptar una solicitud de amistad.
 * @param onReject Callback para rechazar una solicitud de amistad.
 * @param onCancel Callback para cancelar una solicitud de amistad enviada.
 * @param onRemove Callback para eliminar a un amigo.
 * @param onViewDecksClick Callback para ver los mazos de un amigo, recibe el UID del amigo y su nombre.
 */
@Composable
fun FriendshipItem(
    friendship: Friendship,
    currentUserUid: String?,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRemove: (String) -> Unit,
    onViewDecksClick: (String, String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Espacio entre el nombre y los botones.
    ) {
        Column {
            Text(friendship.username, style = MaterialTheme.typography.titleMedium, color = White)
        }

        Spacer(modifier = Modifier.width(8.dp)) // Espacio entre el nombre y el grupo de botones.

        Row(horizontalArrangement = Arrangement.End) { // Botones alineados a la derecha.
            when (friendship.status) {
                "accepted" -> {
                    // Botón "Mazos" para amigos aceptados.
                    Button(
                        onClick = { onViewDecksClick(friendship.otherUserUid, friendship.username) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF424242), // Un gris oscuro para el fondo del botón.
                            contentColor = Orange // Texto naranja para visibilidad.
                        ),
                        border = BorderStroke(1.dp, Orange), // Borde para destacar.
                        modifier = Modifier.padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Mazos", color = Orange) // Texto del botón.
                    }
                    // Botón "Eliminar" para amigos.
                    Button(
                        onClick = { onRemove(friendship.otherUserUid) },
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                        modifier = Modifier.padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Eliminar", color = White)
                    }
                }
                "pending_sent" -> {
                    // Botón "Cancelar" para solicitudes enviadas pendientes.
                    Button(
                        onClick = { onCancel(friendship.otherUserUid) },
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                        modifier = Modifier.padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Cancelar", color = White)
                    }
                }
                "pending_received" -> {
                    // Botón "Aceptar" para solicitudes recibidas pendientes.
                    Button(
                        onClick = { onAccept(friendship.otherUserUid) },
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        modifier = Modifier.padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Aceptar", color = Black)
                    }
                    // Botón "Rechazar" para solicitudes recibidas pendientes.
                    Button(
                        onClick = { onReject(friendship.otherUserUid) },
                        colors = ButtonDefaults.buttonColors(containerColor = Red),
                        modifier = Modifier.padding(horizontal = 4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Rechazar", color = White)
                    }
                }
            }
        }
    }
}

/**
 * Composable que muestra un elemento de resultado de búsqueda de usuario.
 * Permite enviar una solicitud de amistad si no hay una relación existente.
 *
 * @param user El objeto [User] encontrado en la búsqueda.
 * @param onSendRequest Callback para enviar una solicitud de amistad a este usuario.
 * @param friendsAndRequests La lista actual de amistades y solicitudes del usuario logueado.
 * Esto se usa para determinar si ya existe una relación y mostrar el estado.
 */
@Composable
fun SearchResultItem(
    user: User,
    onSendRequest: (User) -> Unit,
    friendsAndRequests: List<Friendship>
) {
    // Se comprueba si ya existe una relación (incluyendo pendientes) con este usuario.
    val existingFriendship = friendsAndRequests.firstOrNull { it.otherUserUid == user.uid }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(user.username, style = MaterialTheme.typography.titleMedium, color = White)
        }

        // Muestra el botón "Enviar Solicitud" solo si no hay una relación de amistad existente.
        if (existingFriendship == null) {
            Button(
                onClick = { onSendRequest(user) },
                colors = ButtonDefaults.buttonColors(containerColor = Green),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Enviar Solicitud", color = Black)
            }
        } else {
            // Si ya existe una relación, muestra el estado de la misma.
            val statusText = when (existingFriendship.status) {
                "pending_sent" -> "Solicitud Enviada"
                "pending_received" -> "Solicitud Recibida"
                "accepted" -> "Ya Son Amigos"
                else -> "" // Fallback para estados inesperados.
            }
            Text(
                statusText,
                color = White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * Composable genérico para mostrar un diálogo de confirmación.
 *
 * @param title El título del diálogo.
 * @param message El mensaje de confirmación.
 * @param onConfirm Callback que se invoca cuando el usuario confirma la acción.
 * @param onDismiss Callback que se invoca cuando el usuario descarta el diálogo.
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Red) }, // Título en rojo para indicar precaución.
        text = { Text(message, color = White) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirmar", color = Red) // Botón de confirmar en rojo.
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = White)
            }
        },
        containerColor = Black.copy(alpha = 0.95f), // Fondo del diálogo semitransparente.
        titleContentColor = White,
        textContentColor = White
    )
}