package com.alejandro.magicdeckbuilder.presentation.cardsearch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alejandro.magicdeckbuilder.data.Card
import com.alejandro.magicdeckbuilder.presentation.components.AppTopBar
import com.alejandro.magicdeckbuilder.presentation.components.CardGrid
import com.alejandro.magicdeckbuilder.presentation.components.FilterDialog
import com.alejandro.magicdeckbuilder.presentation.user.UserViewModel
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Gray
import com.alejandro.magicdeckbuilder.ui.theme.Orange

/**
 * Pantalla principal para la búsqueda de cartas de Magic: The Gathering.
 * Permite a los usuarios buscar cartas, aplicar filtros, ver detalles de cartas,
 * y opcionalmente añadir cartas a un mazo existente (en caso de llegar a esta
 * pantalla desde la de edición de mazos).
 *
 * @param viewModel El [CardSearchViewModel] que gestiona el estado y la lógica de la pantalla.
 * @param onNavigateBack Función de callback para navegar hacia atrás.
 * @param onSignOut Función de callback para cerrar la sesión del usuario.
 * @param userViewModel El [UserViewModel] para acceder a los datos del usuario autenticado (ej. nombre de usuario para la TopBar).
 * @param onAddCardToDeck Función de callback que se invoca cuando se añade una carta a un mazo.
 * Recibe el objeto [Card] completo y la cantidad.
 * @param isAddingCards Booleano que indica si la pantalla está en modo de "añadir cartas a un mazo".
 * Esto cambia la UI (muestra el selector de cantidad y botones de añadir/cerrar).
 * @param onNavigateToFriends Función de callback para navegar a la pantalla de amigos.
 * @param onNavigateToAccountManagement Función de callback para navegar a la pantalla de gestión de cuenta
 */
@OptIn(ExperimentalMaterial3Api::class) // Se utiliza para elementos experimentales de Material3 como OutlinedTextField
@Composable
fun CardSearchScreen(
    viewModel: CardSearchViewModel,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    userViewModel: UserViewModel,
    onAddCardToDeck: (card: Card, quantity: Int) -> Unit,
    isAddingCards: Boolean = false,
    onNavigateToFriends: () -> Unit,
    onNavigateToAccountManagement: () -> Unit
) {
    // Recoge el estado de la UI del ViewModel como un State<T> para que los cambios en el ViewModel
    // recompongan automáticamente la UI.
    val uiState by viewModel.uiState.collectAsState()

    // Estado del usuario para la TopBar
    val userUiState by userViewModel.uiState.collectAsState()

    // Controlador del teclado virtual, permite ocultarlo programáticamente.
    val keyboardController = LocalSoftwareKeyboardController.current

    // Estado local para la cantidad de cartas a añadir, mutable y recordado entre recomposiciones.
    var quantityToAdd by remember { mutableStateOf(1) }

    // Estado local para controlar la visibilidad del diálogo de carta grande.
    var showLargeCardDialog by remember { mutableStateOf(false) }

    // Contenedor principal de la pantalla con un degradado de fondo.
    Box(
        modifier = Modifier
            .fillMaxSize() // Ocupa el tamaño disponible
            .background(
                Brush.verticalGradient(
                    listOf(Gray, Black),
                    startY = 0f,
                    endY = 600f
                )
            ), // Fondo degradado
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior de la aplicación.
            AppTopBar(
                username = userUiState.currentUser?.username, // Muestra el nombre del usuario actual
                canNavigateBack = true, // Permite navegación hacia atrás
                onNavigateBack = onNavigateBack, // Callback para navegación atrás
                onSignOut = onSignOut, // Callback para cerrar sesión
//                modifier = Modifier.weight(1f),
                modifier = Modifier, // Modificador aplicado a la barra
                onNavigateToFriends = onNavigateToFriends, // Callback para navegar a amigos
                onNavigateToAccountManagement = onNavigateToAccountManagement // Callback para navegar a gestión de cuenta
            )
            // Contenido principal de la pantalla, con padding horizontal.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Fila para el campo de búsqueda y el botón de filtro.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically // Centra verticalmente los elementos de la fila
                ) {
                    // Campo de texto para buscar cartas.
                    OutlinedTextField(
                        value = uiState.searchText, // Texto actual del campo, desde el ViewModel
                        onValueChange = { viewModel.onSearchTextChanged(it) }, // Actualiza el texto de búsqueda en el ViewModel
                        label = { Text("Buscar carta...", color = White) }, // Etiqueta del campo
                        singleLine = true, // Limita a una sola línea de texto
                        modifier = Modifier.weight(1f), // Ocupa el espacio restante en la fila
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = White
                            )
                        }, // Icono de búsqueda al inicio
                        trailingIcon = {
                            // Icono de cerrar/borrar texto si el campo de búsqueda no está vacío
                            if (uiState.searchText.isNotBlank()) {
                                IconButton(onClick = { viewModel.onSearchTextChanged("") }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Borrar texto",
                                        tint = White
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), // Configura la acción del teclado a "Buscar"
                        keyboardActions = KeyboardActions(
                            onSearch = { // Al presionar "Buscar" en el teclado
                                viewModel.onSearch() // Ejecuta la búsqueda en el ViewModel
                                keyboardController?.hide() // Oculta el teclado virtual
                            }
                        ),
                        colors = TextFieldDefaults.outlinedTextFieldColors( // Colores personalizados para el TextField
                            focusedBorderColor = White,
                            unfocusedBorderColor = White.copy(alpha = 0.5f),
                            focusedLabelColor = White,
                            unfocusedLabelColor = White.copy(alpha = 0.5f),
                            cursorColor = White,
                            focusedTextColor = White,
                            containerColor = Black.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Espacio entre el campo de texto y el botón de filtro
                    // Columna para el botón de filtro (para mejor alineación)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.padding(top = 10.dp)) // Espacio superior para alinear con el TextField
                        IconButton(onClick = { viewModel.showFilterDialog(true) }) { // Muestra el diálogo de filtro al hacer clic
                            Icon(
                                Icons.Filled.Tune, // Icono de filtro
                                contentDescription = "Filtrar",
                                modifier = Modifier.size(32.dp),
                                tint = White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) // Espacio debajo de la fila de búsqueda/filtro

                // Manejo condicional del contenido principal (indicadores de carga, mensajes de error, lista de cartas)
                if (uiState.isLoading && uiState.cards.isEmpty()) {
                    // Si está cargando y no hay cartas (carga inicial), muestra un indicador de progreso.
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Orange)
                    }
                } else if (uiState.cards.isEmpty()
                    && !uiState.isLoading
                    && uiState.errorMessage == null
//                    && uiState.searchText.isNotBlank()
                    && uiState.hasSearched
                ) {
                    // Si no hay cartas, no está cargando, no hay error y se ha buscado algo (sin resultados).
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No se encontraron cartas.",
                            style = MaterialTheme.typography.headlineSmall,
                            color = White
                        )
                    }
                } else if (uiState.errorMessage != null) {
                    // Si hay un mensaje de error, lo muestra.
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//                        Text("Error: ${uiState.errorMessage}", color = MaterialTheme.colorScheme.error)
                        Text(
                            "${uiState.errorMessage}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = White
                        )
                    }
                } else {
                    // Si hay cartas, las muestra en una cuadrícula.
                    CardGrid(
                        cards = uiState.cards, // Lista de cartas a mostrar
                        isLoading = uiState.isLoading, // Indica si se están cargando más cartas (para el "Load More")
                        onCardClick = { card -> // Callback al hacer clic en una carta
                            viewModel.selectCard(card) // Selecciona la carta en el ViewModel
                            quantityToAdd = 1 // Reinicia la cantidad a 1
                            showLargeCardDialog = true // Muestra el diálogo de carta grande
                        },
                        onLoadMore = { viewModel.loadMoreCards() } // Callback para cargar más cartas al desplazarse
                    )
                }
            }
        }
    }

    // Diálogo de Filtros (condicional, solo se muestra si uiState.showFilterDialog es true)
    if (uiState.showFilterDialog) {
        FilterDialog(
            currentFilters = uiState.filters, // Filtros actuales
            onApplyFilters = { newFilters -> viewModel.applyFilters(newFilters) }, // Callback al aplicar filtros
            onCancel = { viewModel.showFilterDialog(false) }, // Callback al cancelar
            onClearFilters = { viewModel.clearFilters() } // Callback al borrar filtros
        )
    }

    // Diálogo de Carta Grande (se muestra cuando una carta está seleccionada y showLargeCardDialog es true)
    uiState.selectedCard?.let { card -> // Solo se ejecuta si selectedCard no es nulo
        if (showLargeCardDialog) {
            Dialog(
                onDismissRequest = { // Callback cuando el usuario intenta cerrar el diálogo (ej. tocar fuera)
                    viewModel.selectCard(null) // Deselecciona la carta
                    showLargeCardDialog = false // Oculta el diálogo
                },
                properties = DialogProperties(usePlatformDefaultWidth = false) // Permite que el diálogo ocupe el ancho completo si es necesario
            ) {
                // Fondo semi-transparente que cierra el diálogo al tocarlo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)) // Fondo oscuro transparente
                        .clickable { // Hace que el Box completo sea clickeable para cerrar el diálogo
                            viewModel.selectCard(null)
                            showLargeCardDialog = false
                        },
                    contentAlignment = Alignment.Center // Centra el contenido del diálogo
                ) {
                    // Contenido real del diálogo de carta grande
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f) // Ocupa el 90% del ancho de la pantalla
                            .background(
                                Black,
                                RoundedCornerShape(8.dp)
                            ) // Fondo negro con esquinas redondeadas
                            .padding(bottom = if (isAddingCards) 16.dp else 0.dp), // Padding inferior si se están añadiendo cartas
                        horizontalAlignment = Alignment.CenterHorizontally // Centra horizontalmente los elementos
                    ) {
                        // Muestra la imagen de la carta o un mensaje si no hay imagen
                        if (card.imageUrl != null) {
                            AsyncImage( // Carga la imagen de forma asíncrona
                                model = ImageRequest.Builder(LocalContext.current) // Constructor de la solicitud de imagen
                                    .data(card.imageUrl) // URL de la imagen
                                    .crossfade(true) // Animación de fundido cruzado
                                    .error(android.R.drawable.ic_menu_close_clear_cancel) // Imagen de error
                                    .placeholder(android.R.drawable.ic_menu_gallery) // Imagen de placeholder mientras carga
                                    .build(),
                                contentDescription = card.name, // Descripción para accesibilidad
                                contentScale = ContentScale.FillWidth, // Escala la imagen para que llene el ancho
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { } // Evita que tocar la imagen cierre el diálogo
                            )
                        } else {
                            // Si no hay URL de imagen, muestra un Box con un mensaje.
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .aspectRatio(0.72f) // Mantiene la proporción de aspecto de una carta de Magic
                                    .background(Color.White, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Imagen no disponible para ${card.name}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Sección para añadir cartas a un mazo (solo visible si isAddingCards es true)
                        if (isAddingCards) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Cantidad:",
                                    color = White,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Botón para disminuir la cantidad
                                IconButton(onClick = { if (quantityToAdd > 1) quantityToAdd-- }) {
                                    Icon(
                                        Icons.Filled.Remove,
                                        contentDescription = "Disminuir cantidad",
                                        tint = Orange
                                    )
                                }
                                Text(
                                    text = quantityToAdd.toString(), // Muestra la cantidad actual
                                    color = White,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                // Botón para aumentar la cantidad
                                IconButton(onClick = { quantityToAdd++ }) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Aumentar cantidad",
                                        tint = Orange
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Botones de acción (Añadir al Mazo y Cerrar)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween // Distribuye el espacio entre los botones
                            ) {
                                // Botón para añadir al mazo
                                Button(
                                    onClick = {
                                        onAddCardToDeck(
                                            card,
                                            quantityToAdd
                                        ) // Llama al callback con la carta y la cantidad
                                        viewModel.selectCard(null) // Deselecciona la carta
                                        showLargeCardDialog = false // Cierra el diálogo
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Orange), // Color del botón
                                    modifier = Modifier.weight(1f) // Ocupa la mitad del ancho disponible
                                ) {
                                    Text("Añadir al Mazo", color = Black) // Texto del botón
                                }

                                Spacer(modifier = Modifier.width(8.dp)) // Espacio entre los botones

                                // Botón para cerrar el diálogo sin añadir
                                Button(
                                    onClick = {
                                        viewModel.selectCard(null) // Deselecciona la carta
                                        showLargeCardDialog = false // Cierra el diálogo
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), // Color del botón
                                    modifier = Modifier.weight(1f) // Ocupa la otra mitad del ancho disponible
                                ) {
                                    Text("Cerrar", color = White) // Texto del botón
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}