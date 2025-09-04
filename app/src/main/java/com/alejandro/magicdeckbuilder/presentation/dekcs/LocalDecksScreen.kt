package com.alejandro.magicdeckbuilder.presentation.dekcs

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alejandro.magicdeckbuilder.data.local.LocalDeck
import com.alejandro.magicdeckbuilder.presentation.components.AppTopBar
import com.alejandro.magicdeckbuilder.presentation.friendship.ConfirmationDialog
import com.alejandro.magicdeckbuilder.presentation.user.UserViewModel
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Gray
import com.alejandro.magicdeckbuilder.ui.theme.Orange

/**
 * Pantalla para visualizar los mazos de Magic: The Gathering descargados localmente.
 * Permite ver los detalles de un mazo y eliminar mazos locales.
 *
 * @param userViewModel ViewModel que gestiona la información del usuario (para la TopBar).
 * @param onNavigateBack Lambda para manejar la navegación hacia atrás.
 * @param onNavigateToDeckView Lambda para navegar a la pantalla de visualización de un mazo local,
 * recibiendo el ID del mazo a mostrar.
 * @param onSignOut Lambda para manejar el cierre de sesión.
 * @param onNavigateToFriends Lambda para navegar a la pantalla de amigos.
 * @param hideSection Boolean que controla la visibilidad de ciertos elementos en la TopBar,
 * usado para el "Modo Offline".
 * @param onNavigateToAccountManagement Función de callback para navegar a la pantalla de gestión de cuenta
 */
@OptIn(ExperimentalMaterial3Api::class) // Opt-in para usar APIs experimentales de Material3.
@Composable
fun LocalDecksScreen(
    userViewModel: UserViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDeckView: (String) -> Unit,
    onSignOut: () -> Unit,
    onNavigateToFriends: () -> Unit,
    hideSection: Boolean,
    onNavigateToAccountManagement: () -> Unit
) {
    val context = LocalContext.current  // Obtiene el contexto de Android para el ViewModel Factory.
    // Obtiene una instancia de LocalDecksViewModel usando su factory.
    // Se requiere el applicationContext para LocalDeckManager dentro del ViewModel.
    val viewModel: LocalDecksViewModel = viewModel(
        factory = LocalDecksViewModelFactory(context.applicationContext as Application)
    )
    val uiState by viewModel.uiState.collectAsState() // Recolecta el estado de la UI del ViewModel.

    val userUiState by userViewModel.uiState.collectAsState() // Recolecta el estado del usuario para la TopBar.
    val topBarTitle = userUiState.currentUser?.username // Nombre de usuario para la TopBar.
    val topBarSubtitle = "Mazos locales" // Subtítulo fijo para esta pantalla.

    Scaffold(
        topBar = {
            // Condicional para mostrar la AppTopBar, adaptándose al modo offline.
            if (!hideSection) {
                // TopBar para el modo online normal.
                AppTopBar(
                    username = topBarTitle,
                    canNavigateBack = true,
                    onNavigateBack = onNavigateBack,
                    onSignOut = onSignOut,
                    modifier = Modifier,
                    onNavigateToFriends = onNavigateToFriends,
                    subtitle = topBarSubtitle,
                    onNavigateToAccountManagement = onNavigateToAccountManagement
                )
            } else {
                // TopBar adaptada para el "Modo Offline".
                // Algunas acciones (SignOut, Friends) se anulan para este modo.
                AppTopBar(
                    username = "Modo Offline", // Título específico para el modo offline.
                    canNavigateBack = true,
                    onNavigateBack = onNavigateBack,
                    onSignOut = { }, // Cierre de sesión deshabilitado en offline.
                    modifier = Modifier,
                    onNavigateToFriends = { }, // Navegación a amigos deshabilitada en offline.
                    subtitle = topBarSubtitle,
                    hideSection = true, // Indica a la TopBar que oculte secciones correspondientes.
                    onNavigateToAccountManagement = onNavigateToAccountManagement // Callback para navegar a gestión de cuenta
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize() // Ocupa el tamaño disponible.
                .padding(paddingValues) // Aplica el padding de la Scaffold.
                .background(Brush.verticalGradient(listOf(Gray, Black), startY = 0f, endY = 600f)) // Fondo degradado.
        ) {
            // Manejo del estado de carga, error y lista vacía.
            if (uiState.isLoading) {
                CircularProgressIndicator( // Muestra un indicador circular de carga.
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
                // Muestra un mensaje si no hay mazos descargados.
                Text(
                    text = "No tienes mazos descargados.",
                    color = White,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Si hay mazos, los muestra en una lista desplazable.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp), // Padding alrededor de la lista.
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Espacio entre cada elemento de la lista.
                ) {
                    items(uiState.decks, key = { it.id }) { deck ->
                        LocalDeckCard(
                            deck = deck,
                            onDelete = { viewModel.deleteLocalDeck(deck.id) }, // Callback para eliminar el mazo.
                            onViewClick = { onNavigateToDeckView(deck.id) } // Callback para ver el mazo.
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para eliminar mazo local
    if (uiState.showDeleteLocalDeckDialog) {
        ConfirmationDialog(
            title = "¿Borrar mazo local?",
            message = "¿Estás seguro de que quieres eliminar este mazo de forma local? Esta acción no se puede deshacer.",
            onConfirm = { viewModel.confirmDeleteLocalDeck() }, // Llama a la función que confirma el borrado
            onDismiss = { viewModel.dismissDeleteLocalDeckDialog() }
        )
    }
}

/**
 * Composable para mostrar una tarjeta individual de un mazo local.
 * Proporciona información básica del mazo y botones para ver y eliminar.
 *
 * @param deck El objeto [LocalDeck] a mostrar en la tarjeta.
 * @param onDelete Lambda que se invoca cuando se hace clic en el botón de borrar.
 * @param onViewClick Lambda que se invoca cuando se hace clic en el botón de ver o en la tarjeta.
 */
@Composable
fun LocalDeckCard(deck: LocalDeck, onDelete: () -> Unit, onViewClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewClick() }, // Toda la tarjeta es clickeable para ver los detalles.
        colors = CardDefaults.cardColors(containerColor = Black.copy(alpha = 0.7f)), // Color de fondo de la tarjeta.
        shape = MaterialTheme.shapes.medium, // Forma de la tarjeta.
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Elevación de la tarjeta.
    ) {
        Row(
            modifier = Modifier.padding(16.dp), // Relleno interno de la tarjeta.
            verticalAlignment = Alignment.CenterVertically // Alineación vertical de los elementos dentro de la fila.
        ) {
//            val firstCardImage = deck.cards.values.firstOrNull()?.imagePath
//            if (firstCardImage != null) {
//                Image(
//                    painter = rememberAsyncImagePainter(model = File(firstCardImage)),
//                    contentDescription = "Carta de mazo",
//                    modifier = Modifier.size(40.dp, 56.dp)
//                )
//                Spacer(modifier = Modifier.width(16.dp))
//            }

            Column(modifier = Modifier.weight(1f)) { // Columna para el nombre y la cantidad de cartas, ocupa el espacio restante.
                Text(
                    text = deck.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${deck.cardCount} cartas",
                    style = MaterialTheme.typography.bodySmall,
                    color = White.copy(alpha = 0.7f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.End, // Alinea los botones al final (derecha).
                verticalAlignment = Alignment.CenterVertically // Alineación vertical de los botones.
            ) {
                // Botón de ver el mazo.
                IconButton(onClick = onViewClick) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = "Ver mazo",
                        tint = Orange
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Botón para borrar el mazo local.
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "Borrar mazo local", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}