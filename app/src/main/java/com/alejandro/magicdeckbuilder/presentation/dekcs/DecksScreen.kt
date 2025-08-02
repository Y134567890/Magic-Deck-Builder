package com.alejandro.magicdeckbuilder.presentation.dekcs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.alejandro.magicdeckbuilder.data.models.Deck
import com.alejandro.magicdeckbuilder.presentation.components.AppTopBar
import com.alejandro.magicdeckbuilder.presentation.friendship.ConfirmationDialog
import com.alejandro.magicdeckbuilder.presentation.user.UserViewModel
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Gray
import com.alejandro.magicdeckbuilder.ui.theme.Orange

/**
 * Pantalla principal para visualizar los mazos del usuario o de un amigo.
 * Permite crear, editar, eliminar y descargar mazos.
 *
 * @param decksViewModel ViewModel que gestiona la lógica de los mazos.
 * @param userViewModel ViewModel que gestiona la información del usuario (para la TopBar).
 * @param onNavigateBack Lambda para manejar la navegación hacia atrás.
 * @param onNavigateToDeckEdit Lambda para navegar a la pantalla de edición/visualización de un mazo.
 * @param onSignOut Lambda para manejar el cierre de sesión.
 * @param onNavigateToFriends Lambda para navegar a la pantalla de amigos.
 * @param isFriendDecksView Flag que indica si esta vista es para mostrar los mazos de un amigo (true)
 * o los mazos propios del usuario (false). Por defecto es `false`.
 * @param friendUsername Nombre de usuario del amigo, si `isFriendDecksView` es `true`. Se usa en el subtítulo de la TopBar.
 */
@OptIn(ExperimentalMaterial3Api::class) // Opt-in para usar APIs experimentales de Material3
@Composable
fun DecksScreen(
    decksViewModel: DecksViewModel,
    userViewModel: UserViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDeckEdit: (String?) -> Unit,
    onSignOut: () -> Unit,
    onNavigateToFriends: () -> Unit,
    isFriendDecksView: Boolean = false,
    friendUsername: String? = null
) {
    // Recolecta los estados de los ViewModels como State de Compose.
    val uiState by decksViewModel.uiState.collectAsState()
    val userUiState by userViewModel.uiState.collectAsState()

    val downloadState by decksViewModel.downloadState.collectAsState() // Estado de descarga de mazos
    val snackbarHostState = remember { SnackbarHostState() } // Estado para mostrar SnackBar

    var showCreateDeckDialog by remember { mutableStateOf(false) } // Controla la visibilidad del diálogo de creación
    var newDeckName by remember { mutableStateOf("") } // Campo para el nombre del nuevo mazo
    var newDeckDescription by remember { mutableStateOf("") } // Campo para la descripción del nuevo mazo
    var newDeckFormat by remember { mutableStateOf("") } // Campo para el formato del nuevo mazo
    var newDeckNameError by remember { mutableStateOf<String?>(null) } // Mensaje de error para el nombre del mazo

    // Define el título y subtítulo de la barra superior basados en si es vista de amigo o no.
    val topBarTitle = userUiState.currentUser?.username
    val topBarSubtitle = if (isFriendDecksView && friendUsername != null) {
        "Mazos de $friendUsername" // Si es vista de amigo, el subtítulo es "Mazos de {Nombre del Amigo}"
    } else null // Si no es vista de amigo, no hay subtítulo

    // Efecto secundario que se ejecuta cuando el `downloadState` cambia.
    // Muestra un SnackBar con el resultado de la descarga.
    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is DownloadStatus.Success -> {
                snackbarHostState.showSnackbar("Mazo '${state.deckName}' guardado localmente.")
                decksViewModel.clearDownloadState() // Limpiar el estado para evitar que el SnackBar se muestre de nuevo
            }
            is DownloadStatus.Error -> {
                snackbarHostState.showSnackbar("Error al descargar: ${state.message}")
                decksViewModel.clearDownloadState()
            }
            else -> {} // Ignorar otros estados (Idle, Downloading)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Define el host para los SnackBar
        topBar = {
            Column {
                AppTopBar(
                    username = topBarTitle,
                    canNavigateBack = true,
                    onNavigateBack = onNavigateBack,
                    onSignOut = onSignOut,
                    modifier = Modifier,
                    onNavigateToFriends = onNavigateToFriends,
                    subtitle = topBarSubtitle // Pasa el subtítulo calculado a la barra superior
                )
            }
        },
        floatingActionButton = {
            // El Floating Action Button (FAB) solo se muestra si NO es la vista de mazos de un amigo.
            if (!isFriendDecksView) {
                FloatingActionButton(
                    // Al hacer clic en el FAB, se muestra el diálogo de creación de mazo.
                    onClick = {
                        // Resetear los campos y errores del diálogo antes de mostrarlo.
                        newDeckName = ""
                        newDeckDescription = ""
                        newDeckFormat = ""
                        newDeckNameError = null
                        showCreateDeckDialog = true
                    },
                    containerColor = Orange, // Color de fondo del FAB
                    contentColor = Black // Color del icono dentro del FAB
                ) {
                    Icon(Icons.Filled.Add, "Añadir nuevo mazo") // Icono de añadir
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End // Posiciona el FAB al final (derecha)
    ) { paddingValues ->
        // Contenedor principal de la pantalla con fondo degradado.
        Box(
            modifier = Modifier
                .fillMaxSize() // Ocupa el tamaño disponible
                .padding(paddingValues) // Aplica el padding de la Scaffold
                .background(Brush.verticalGradient(listOf(Gray, Black), startY = 0f, endY = 600f))  // Fondo degradado
        ) {
            // Manejo del estado de carga, error y lista vacía.
            if (uiState.isLoading) {
                CircularProgressIndicator( // Muestra un indicador circular de carga
                    modifier = Modifier.align(Alignment.Center),
                    color = Orange
                )
            } else if (uiState.errorMessage != null) {
                // Muestra un mensaje de error si existe.
                Text(
                    text = "Error: ${uiState.errorMessage}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.decks.isEmpty()) {
                // Muestra un mensaje si no hay mazos. El mensaje varía si es vista de amigo.
                Text(
                    text = if (isFriendDecksView) "Este amigo no tiene mazos aún." else "No tienes mazos aún. ¡Crea uno!",
                    color = White,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Si hay mazos, los muestra en una lista.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp), // Padding alrededor de la lista
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Espacio entre cada elemento de la lista
                ) {
                    items(uiState.decks, key = { it.id }) { deck ->
                        // Determina si el mazo actual se está descargando para mostrar el indicador circular.
                        val isDownloading = downloadState is DownloadStatus.Downloading &&
                                (downloadState as DownloadStatus.Downloading).deckId == deck.id

                        DeckCard(
                            deck = deck,
                            onEditClick = { onNavigateToDeckEdit(deck.id) },
                            onDeleteClick = { decksViewModel.deleteDeck(deck.id) },
                            isFriendDecksView = isFriendDecksView,
                            isDownloading = isDownloading,
                            onDownloadClick = { decksViewModel.downloadDeck(deck) }
                        )
                    }
                }
            }
        }

        // Diálogo de confirmación para eliminar mazo
        if (uiState.showDeleteDeckDialog) {
            ConfirmationDialog(
                title = "¿Borrar mazo?",
                message = "¿Estás seguro de que quieres eliminar este mazo? Esta acción no se puede deshacer.",
                onConfirm = { decksViewModel.confirmDeleteDeck() }, // Llama a la función que confirma el borrado
                onDismiss = { decksViewModel.dismissDeleteDeckDialog() }
            )
        }
    }

    // --- Diálogo de creación de mazo ---
    if (showCreateDeckDialog) {
        val focusRequester = remember { FocusRequester() } // Para solicitar el foco al primer campo
        val focusManager = LocalFocusManager.current // Para gestionar el foco del teclado

        AlertDialog(
            onDismissRequest = { showCreateDeckDialog = false }, // Permite cerrar el diálogo al tocar fuera o presionar atrás
            properties = DialogProperties(usePlatformDefaultWidth = false), // Permite que el diálogo use un ancho personalizado
            title = {
                Text(
                    text = "Crear Nuevo Mazo",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Orange
                )
            },
            text = {
                Column {
                    // Campo para el nombre del mazo
                    OutlinedTextField(
                        value = newDeckName,
                        onValueChange = {
                            newDeckName = it
                            if (it.isNotBlank()) newDeckNameError = null // Limpiar el error si el usuario empieza a escribir
                        },
                        label = { Text("Nombre del Mazo *", color = White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester), // Asocia el FocusRequester para enfocar este campo
                        isError = newDeckNameError != null, // Indica si hay un error en el campo
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), // Acción "Siguiente" en el teclado
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(
                            FocusDirection.Down) }), // Mueve el foco al siguiente campo
                        colors = TextFieldDefaults.outlinedTextFieldColors( // Colores personalizados para el campo
                            focusedBorderColor = Orange,
                            unfocusedBorderColor = White.copy(alpha = 0.5f),
                            focusedLabelColor = Orange,
                            unfocusedLabelColor = White.copy(alpha = 0.5f),
                            cursorColor = White,
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            containerColor = Black.copy(alpha = 0.5f),
                            errorBorderColor = Color.Red,
                            errorLabelColor = Color.Red
                        )
                    )
                    if (newDeckNameError != null) {
                        Text(
                            text = newDeckNameError!!, // Muestra el mensaje de error
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Campo para la descripción del mazo
                    OutlinedTextField(
                        value = newDeckDescription,
                        onValueChange = { newDeckDescription = it },
                        label = { Text("Descripción", color = White.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Orange,
                            unfocusedBorderColor = White.copy(alpha = 0.5f),
                            focusedLabelColor = Orange,
                            unfocusedLabelColor = White.copy(alpha = 0.5f),
                            cursorColor = White,
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            containerColor = Black.copy(alpha = 0.5f),
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Campo para el formato del mazo
                    OutlinedTextField(
                        value = newDeckFormat,
                        onValueChange = { newDeckFormat = it },
                        label = { Text("Formato (ej. Standard)", color = White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Orange,
                            unfocusedBorderColor = White.copy(alpha = 0.5f),
                            focusedLabelColor = Orange,
                            unfocusedLabelColor = White.copy(alpha = 0.5f),
                            cursorColor = White,
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            containerColor = Black.copy(alpha = 0.5f),
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Validación simple: el nombre del mazo no puede estar vacío.
                        if (newDeckName.isBlank()) {
                            newDeckNameError = "El nombre no puede estar vacío."
                        } else {
                            // Llama a la función del ViewModel para crear el mazo con los datos ingresados.
                            decksViewModel.createNewDeck(newDeckName, newDeckDescription, newDeckFormat)
                            showCreateDeckDialog = false // Oculta el diálogo después de la creación
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    Text("Crear Mazo", color = Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDeckDialog = false }) {
                    Text("Cancelar", color = White)
                }
            },
            containerColor = Black, // Fondo del diálogo
            tonalElevation = 8.dp, // Elevación tonal
            shape = RoundedCornerShape(8.dp) // Forma del diálogo
        )

        // El foco se pone en el primer campo al abrir el diálogo.
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    // --- LaunchedEffect para navegar después de la creación del mazo ---
    // Observa el estado `newlyCreatedDeckId` del ViewModel. Cuando se establece un ID,
    // navega a la pantalla de edición del nuevo mazo y luego limpia el ID.
    LaunchedEffect(uiState.newlyCreatedDeckId) {
        uiState.newlyCreatedDeckId?.let { deckId ->
            onNavigateToDeckEdit(deckId)
            decksViewModel.clearNewlyCreatedDeckId()
        }
    }
}

/**
 * Composable para mostrar una tarjeta individual de un mazo.
 * Proporciona información del mazo y acciones como editar, borrar o descargar/ver.
 *
 * @param deck El objeto `Deck` a mostrar.
 * @param onEditClick Lambda que se invoca cuando se hace clic en el botón de "Editar" o en la tarjeta.
 * @param onDeleteClick Lambda que se invoca cuando se hace clic en el botón de "Borrar".
 * @param isFriendDecksView Flag que indica si esta tarjeta se muestra en la vista de mazos de un amigo.
 * Esto cambia la visibilidad y funcionalidad de los botones (ver, descargar, editar, borrar).
 * @param isDownloading Flag que indica si este mazo específico se está descargando.
 * @param onDownloadClick Lambda que se invoca cuando se hace clic en el botón de "Descargar".
 */
@Composable
fun DeckCard(
    deck: Deck,
    onEditClick: () -> Unit, // Actuará como "ver" si isFriendDecksView es true
    onDeleteClick: () -> Unit,
    isFriendDecksView: Boolean = false,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() }, // Al hacer clic en la tarjeta también se activa la acción de edición/visualización
        colors = CardDefaults.cardColors(containerColor = Black.copy(alpha = 0.7f)), // Color de fondo de la tarjeta
        shape = MaterialTheme.shapes.medium, // Forma de la tarjeta
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Elevación de la tarjeta
    ) {
        Column(
            modifier = Modifier.padding(16.dp) // Relleno interno de la tarjeta
        ) {
            Text(
                text = deck.name,
                style = MaterialTheme.typography.titleMedium,
                color = White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Total de cartas: ${deck.cardCount}",
                style = MaterialTheme.typography.bodySmall,
                color = White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End, // Alinea los elementos al final (derecha)
                verticalAlignment = Alignment.CenterVertically // Para alinear bien el indicador circular de descarga
            ) {
                // Los botones de descargar, editar y borrar solo aparecen si NO es la vista de mazo de amigo.
                if (!isFriendDecksView) {
                    // Botón de descarga
                    if (isDownloading) {
                        CircularProgressIndicator( // Muestra un indicador circular si se está descargando
                            modifier = Modifier.size(24.dp),
                            color = Orange,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onDownloadClick) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Descargar Mazo",
                                tint = White // Color del icono de descarga
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Botón de editar
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Editar mazo",
                            tint = Orange // Color del icono de editar
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Botón de borrar
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Filled.Delete, "Borrar mazo", tint = MaterialTheme.colorScheme.error) // Color del icono de borrar
                    }
                } else {
                    // Si es la vista de un amigo, solo mostramos el botón de "Ver".
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "Ver mazo",
                            tint = Orange // Color del icono de ver
                        )
                    }
                }
            }
        }
    }
}