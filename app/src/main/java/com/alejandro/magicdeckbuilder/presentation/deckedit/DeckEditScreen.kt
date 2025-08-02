package com.alejandro.magicdeckbuilder.presentation.deckedit

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alejandro.magicdeckbuilder.presentation.components.AppTopBar
import com.alejandro.magicdeckbuilder.presentation.user.UserViewModel
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Gray
import com.alejandro.magicdeckbuilder.ui.theme.Orange

/**
 * Pantalla para la edición y visualización de mazos.
 * Permite al usuario modificar los detalles del mazo, añadir/quitar cartas y ver estadísticas.
 * También soporta un modo de solo lectura para mazos de amigos.
 *
 * @param deckEditViewModel El ViewModel que gestiona el estado y la lógica del mazo en edición.
 * @param userViewModel El ViewModel del usuario, necesario para obtener el nombre de usuario para la TopBar.
 * @param onNavigateBack Función de callback para navegar hacia atrás.
 * @param onDeckSaved Función de callback que se invoca cuando el mazo se guarda exitosamente.
 * @param onNavigateToCardSearch Función de callback para navegar a la pantalla de búsqueda de cartas,
 * pasando el ID del mazo actual para añadir cartas a este mazo.
 * @param navController El controlador de navegación, utilizado para acceder a los argumentos de la ruta.
 * @param onSignOut Función de callback para cerrar sesión.
 * @param onNavigateToFriends Función de callback para navegar a la pantalla de amigos.
 * @param isViewMode Booleano que indica si la pantalla está en modo de solo lectura (no permite editar).
 * @param ownerUsername El nombre de usuario del propietario del mazo si se está viendo un mazo de amigo.
 * Será nulo si es un mazo propio o nuevo.
 */
@OptIn(ExperimentalMaterial3Api::class) // Se requiere para usar componentes de Material 3 como OutlinedTextField
@Composable
fun DeckEditScreen(
    deckEditViewModel: DeckEditViewModel,
    userViewModel: UserViewModel,
    onNavigateBack: () -> Unit,
    onDeckSaved: () -> Unit,
    onNavigateToCardSearch: (String) -> Unit,
    navController: NavController,
    onSignOut: () -> Unit,
    onNavigateToFriends: () -> Unit,
    isViewMode: Boolean,
    ownerUsername: String? = null
) {
    // Recopila el estado de la UI del DeckEditViewModel como un State.
    val uiState by deckEditViewModel.uiState.collectAsState()
    // Recopila el estado de la UI del UserViewModel para obtener el nombre de usuario.
    val userUiState by userViewModel.uiState.collectAsState()
    // Recopila las estadísticas del mazo (para el diálogo de estadísticas).
    val deckStats by deckEditViewModel.deckStats.collectAsState()

    // Estado local para controlar si el resultado de una búsqueda de cartas (desde CardSearchScreen)
    // ha sido procesado. Esto evita reinicializar el mazo innecesariamente.
    var cardResultProcessed by remember { mutableStateOf(false) }

    // Obtiene el `deckId` de los argumentos de navegación.
    // `remember` asegura que este valor no se recalcule en cada recomposición.
    val currentDeckId = remember { navController.currentBackStackEntry?.arguments?.getString("deckId") }

    // Estado local para el selector de cantidad en el diálogo de añadir/quitar carta.
    var quantitySelectorValue by remember { mutableStateOf(1) }

    // Efecto lanzado cuando `currentDeckId` cambia.
    // Esto asegura que el ViewModel inicialice el mazo correcto al entrar a la pantalla
    // o al navegar a ella desde la búsqueda de cartas.
    LaunchedEffect(currentDeckId) {
        Log.d("DeckEditScreen", "LaunchedEffect(currentDeckId) triggered. DeckId: $currentDeckId, cardResultProcessed: $cardResultProcessed")
        // Si no se ha procesado un resultado de carta, inicializa el mazo.
        // Esto es para el caso de abrir la pantalla por primera vez o volver de otra pantalla que no sea CardSearch.
        if (!cardResultProcessed) {
            deckEditViewModel.ensureInitialized(currentDeckId)
            Log.d("DeckEditScreen", "Calling deckEditViewModel.ensureInitialized() for deckId: $currentDeckId.")
        } else {
            // Si `cardResultProcessed` es true, significa que venimos de CardSearchScreen
            // y el ViewModel ya ha manejado la adición de la carta.
            // Se resetea el flag para futuras interacciones.
            Log.d("DeckEditScreen", "Skipping ensureInitialized() because cardResult was processed. Resetting flag.")
            cardResultProcessed = false
        }
    }

    // Efecto lanzado cuando `uiState.isDeckSaved` cambia.
    // Si el mazo se ha guardado exitosamente, navega de vuelta y resetea el flag.
    LaunchedEffect(uiState.isDeckSaved) {
        if (uiState.isDeckSaved) {
            onDeckSaved() // Navega a la pantalla anterior (ej. lista de mazos).
            deckEditViewModel.resetDeckSavedFlag() // Resetea el flag en el ViewModel.
            cardResultProcessed = false // También resetea este flag por si acaso.
        }
    }

    // `Scaffold` proporciona la estructura básica de la pantalla (TopBar, FAB, contenido).
    Scaffold(
        topBar = {
            Column { // Envuelve la TopBar en una Column para poder añadir más elementos si se desea.
                AppTopBar(
                    username = userUiState.currentUser?.username, // Nombre de usuario en la TopBar.
                    canNavigateBack = true, // Permite navegación hacia atrás.
                    onNavigateBack = onNavigateBack, // Callback para navegación hacia atrás.
                    modifier = Modifier,
                    onSignOut = onSignOut, // Callback para cerrar sesión.
                    onNavigateToFriends = onNavigateToFriends, // Callback para navegar a amigos.
                    // Muestra un subtítulo si estamos en modo vista y hay un propietario especificado.
                    subtitle = if (isViewMode && ownerUsername != null) "Mazos de $ownerUsername" else null
                )
            }
        },
        floatingActionButton = {
            // El FAB (Floating Action Button) para guardar el mazo solo se muestra si NO está en modo vista.
            if (!isViewMode) {
                FloatingActionButton(
                    onClick = if (!uiState.isSaving) deckEditViewModel::saveDeck else { -> }, // Deshabilita el clic si está guardando.
                    containerColor = Orange, // Color de fondo del FAB.
                    contentColor = Black // Color del contenido (icono/texto).
                ) {
                    // Muestra un CircularProgressIndicator si se está guardando, de lo contrario muestra el icono de guardar.
                    if (uiState.isSaving) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Filled.Save, "Guardar Mazo")
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End // Posiciona el FAB al final (derecha).
    ) { paddingValues -> // `paddingValues` contiene el padding que TopBar/FAB añaden a la pantalla.
        Box(
            modifier = Modifier
                .fillMaxSize() // Ocupa el tamaño disponible.
                .padding(paddingValues) // Aplica el padding de Scaffold.
                .background(Brush.verticalGradient(listOf(Gray, Black), startY = 0f, endY = 600f)) // Fondo con degradado.
        ) {
            // --- Manejo de estados de carga y error ---
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center), // Centra el indicador de carga.
                    color = Orange // Color del indicador.
                )
            } else if (uiState.errorMessage != null) {
                Text(
                    text = "Error: ${uiState.errorMessage}", // Muestra el mensaje de error.
                    color = MaterialTheme.colorScheme.error, // Color de error del tema.
                    modifier = Modifier.align(Alignment.Center) // Centra el texto de error.
                )
            } else {
                // --- Contenido principal de la pantalla de edición del mazo ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()) // Permite el desplazamiento vertical.
                        .padding(16.dp), // Padding interno para toda la columna.
                    verticalArrangement = Arrangement.spacedBy(16.dp) // Espacio vertical entre los elementos de la columna.
                ) {
                    // Campo de texto para el Nombre del Mazo
                    OutlinedTextField(
                        value = uiState.deck.name, // El nombre actual del mazo.
                        onValueChange = deckEditViewModel::onDeckNameChange, // Callback para actualizar el nombre.
                        label = { Text("Nombre del Mazo", color = White.copy(alpha = 0.7f)) }, // Etiqueta.
                        singleLine = true, // Una sola línea de entrada.
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.nameError != null, // Indica error si hay un mensaje de error de nombre.
                        enabled = !isViewMode, // Deshabilita la edición si está en modo vista.
                        colors = TextFieldDefaults.outlinedTextFieldColors( // Colores personalizados.
                            focusedBorderColor = Orange,
                            unfocusedBorderColor = White.copy(alpha = 0.5f),
                            focusedLabelColor = Orange,
                            unfocusedLabelColor = White.copy(alpha = 0.5f),
                            cursorColor = White,
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            containerColor = Black.copy(alpha = 0.5f), // Fondo semi-transparente.
                            errorBorderColor = Color.Red,
                            errorLabelColor = Color.Red,
                            disabledTextColor = White.copy(alpha = 0.7f),  // Color del texto deshabilitado.
                            disabledLabelColor = White.copy(alpha = 0.5f), // Color de la etiqueta deshabilitada.
                            disabledBorderColor = White.copy(alpha = 0.3f), // Color del borde deshabilitado.
                        )
                    )
                    // Muestra el mensaje de error del nombre si existe y NO estamos en modo vista.
                    if (uiState.nameError != null && !isViewMode) {
                        Text(
                            text = uiState.nameError!!, // Se usa `!!` ya que ya se comprobó que no es nulo.
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    // Campo de texto para la Descripción del Mazo
                    OutlinedTextField(
                        value = uiState.deck.description,
                        onValueChange = deckEditViewModel::onDeckDescriptionChange,
                        label = { Text("Descripción", color = White.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3, // Mínimo de 3 líneas.
                        maxLines = 5, // Máximo de 5 líneas.
                        enabled = !isViewMode, // Deshabilita la edición en modo vista.
                        colors = TextFieldDefaults.outlinedTextFieldColors( // Colores personalizados.
                            focusedBorderColor = Orange,
                            unfocusedBorderColor = White.copy(alpha = 0.5f),
                            focusedLabelColor = Orange,
                            unfocusedLabelColor = White.copy(alpha = 0.5f),
                            cursorColor = White,
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            containerColor = Black.copy(alpha = 0.5f),
                            disabledTextColor = White.copy(alpha = 0.7f),
                            disabledLabelColor = White.copy(alpha = 0.5f),
                            disabledBorderColor = White.copy(alpha = 0.3f),
                        )
                    )

                    // Campo de texto para el Formato del Mazo
                    OutlinedTextField(
                        value = uiState.deck.format,
                        onValueChange = deckEditViewModel::onDeckFormatChange,
                        label = { Text("Formato (ej. Standard, Commander)", color = White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isViewMode, // Deshabilita la edición en modo vista.
                        colors = TextFieldDefaults.outlinedTextFieldColors( // Colores personalizados.
                            focusedBorderColor = Orange,
                            unfocusedBorderColor = White.copy(alpha = 0.5f),
                            focusedLabelColor = Orange,
                            unfocusedLabelColor = White.copy(alpha = 0.5f),
                            cursorColor = White,
                            focusedTextColor = White,
                            unfocusedTextColor = White,
                            containerColor = Black.copy(alpha = 0.5f),
                            disabledTextColor = White.copy(alpha = 0.7f),
                            disabledLabelColor = White.copy(alpha = 0.5f),
                            disabledBorderColor = White.copy(alpha = 0.3f),
                        )
                    )

                    // Sección de Cartas en el Mazo
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Black.copy(alpha = 0.7f)), // Fondo semi-transparente.
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cartas en el Mazo: ${uiState.deck.cardCount}", color = White, style = MaterialTheme.typography.titleMedium)

                                Spacer(modifier = Modifier.weight(1f)) // Empuja los iconos a la derecha.

                                // Botón y menú desplegable para ordenar las cartas.
                                var showSortMenu by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = { showSortMenu = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Orange.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.List,
                                        contentDescription = "Ordenar cartas",
                                        tint = Black
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    modifier = Modifier.background(Black.copy(alpha = 0.9f))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Coste de Maná Convertido (CMC)", color = White) },
                                        onClick = {
                                            deckEditViewModel.setSortOption(SortOption.CMC)
                                            showSortMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Colores", color = White) },
                                        onClick = {
                                            deckEditViewModel.setSortOption(SortOption.COLORS)
                                            showSortMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Alfabéticamente", color = White) },
                                        onClick = {
                                            deckEditViewModel.setSortOption(SortOption.ALPHABETICAL)
                                            showSortMenu = false
                                        },
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Botón para ver las estadísticas del mazo.
                                IconButton(
                                    onClick = { deckEditViewModel.showStatsDialog() },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Orange.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.BarChart,
                                        contentDescription = "Ver estadísticas del mazo",
                                        tint = Black
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Botón para añadir carta (solo visible si NO está en modo vista).
                                if (!isViewMode) {
                                    IconButton(
                                        onClick = {
                                            // Pasa el ID del mazo actual a CardSearchScreen, o "new" si es un mazo nuevo.
                                            val idToPass = uiState.deck.id.takeIf { it.isNotBlank() } ?: "new"
                                            onNavigateToCardSearch(idToPass)
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Añadir Carta", tint = Orange)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Muestra un mensaje si el mazo está vacío, de lo contrario muestra la cuadrícula de cartas.
                            if (uiState.deck.cards.isEmpty()) {
                                Text(
                                    "Aún no hay cartas en este mazo. ¡Usa el botón '+' para añadir algunas!",
                                    color = White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(vertical = 16.dp)
                                )
                            } else {
                                // Las cartas se obtienen y se ordenan según la opción seleccionada.
                                val sortedCards by deckEditViewModel.sortedCardsInDeck.collectAsState()
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(5), // 5 columnas fijas para las imágenes de cartas.
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 400.dp), // Altura máxima para la cuadrícula.
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalArrangement = Arrangement.spacedBy(0.dp) // Sin espacio vertical entre filas.
                                ) {
                                    // Itera sobre las cartas ordenadas para mostrar cada CardImageInDeck.
                                    items(sortedCards) { (cardId, cardData) ->
                                        CardImageInDeck(
                                            cardId = cardId,
                                            imageUrl = cardData.imageUrl,
                                            quantity = cardData.quantity,
                                            onClick = {
                                                    deckEditViewModel.selectCardInDeck(cardId) // Selecciona la carta para mostrar su diálogo.
                                                    quantitySelectorValue = 1 // Resetea la cantidad del selector.
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de error del nombre del mazo (solo si no es modo vista).
    if (uiState.showNameErrorDialog && !isViewMode) {
        AlertDialog(
            onDismissRequest = { deckEditViewModel.dismissNameErrorDialog() },
            title = { Text("Error al guardar mazo", color = MaterialTheme.colorScheme.error) },
            text = { Text(uiState.nameError ?: "Ha ocurrido un error desconocido.", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                TextButton(onClick = { deckEditViewModel.dismissNameErrorDialog() }) {
                    Text("Ok", color = Orange)
                }
            }
        )
    }

    // Diálogo para mostrar los detalles de una carta seleccionada en el mazo.
    uiState.selectedCardInDeckId?.let { selectedCardId ->
        val cardInDeckData = uiState.deck.cards[selectedCardId] // Obtiene los datos de la carta.
        val imageUrl = cardInDeckData?.imageUrl

        if (imageUrl != null) {
            Dialog(
                onDismissRequest = { deckEditViewModel.deselectCardInDeck() }, // Cierra el diálogo.
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)) // Fondo oscuro semi-transparente.
                        .clickable { deckEditViewModel.deselectCardInDeck() }, // Cierra al hacer click fuera de la imagen.
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f) // Ocupa el 90% del ancho.
                            .background(Black, RoundedCornerShape(8.dp)) // Fondo negro con esquinas redondeadas.
                            .padding(16.dp), // Padding interno.
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Imagen de la carta en grande.
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .error(android.R.drawable.ic_menu_close_clear_cancel)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .build(),
                            contentDescription = "Carta en mazo",
                            contentScale = ContentScale.FillWidth, // Escala la imagen para ocupar el ancho.
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = false) {  } // Deshabilita el click en la imagen misma para evitar cierre accidental.
                        )

                        // --- Contenido condicional basado en isViewMode ---
                        if (!isViewMode) { // Solo muestra el selector de cantidad y los botones si NO está en modo vista.
                            Spacer(modifier = Modifier.height(16.dp))

                            // Selector de cantidad (añadir/quitar X copias).
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Cantidad:",
                                    color = White,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { if (quantitySelectorValue > 1) quantitySelectorValue-- }) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Disminuir cantidad", tint = Orange)
                                }
                                Text(
                                    text = quantitySelectorValue.toString(),
                                    color = White,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                IconButton(onClick = { quantitySelectorValue++ }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Aumentar cantidad", tint = Orange)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Botones de acción (Añadir, Quitar, Cerrar).
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 0.dp), // No padding horizontal extra.
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Button(
                                    onClick = {
                                        cardInDeckData.let { data ->
                                            // Crea un objeto Card temporal con los datos necesarios para añadir.
                                            val tempCard = com.alejandro.magicdeckbuilder.data.Card(
                                                id = selectedCardId,
                                                name = data.name,
                                                imageUrl = data.imageUrl,
                                                manaCost = data.manaCost,
                                                cmc = data.cmc,
                                                colors = data.colors,
                                                type = data.type,
                                                power = null,
                                                toughness = null,
                                                oracleText = null,
                                                setCode = null,
                                                setName = null,
                                                rarity = null
                                            )
                                            deckEditViewModel.addCardToDeck(tempCard, quantitySelectorValue) // Añade la carta con la cantidad seleccionada.
                                        }
                                        deckEditViewModel.deselectCardInDeck() // Deselecciona la carta para cerrar el diálogo.
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                                    modifier = Modifier.weight(1f) // Ocupa un tercio del espacio.
                                ) {
                                    Text("Añadir", color = Black)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        deckEditViewModel.removeCardFromDeck(selectedCardId, quantitySelectorValue) // Quita la carta con la cantidad seleccionada.
                                        deckEditViewModel.deselectCardInDeck() // Deselecciona la carta para cerrar el diálogo.
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Quitar", color = White)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { deckEditViewModel.deselectCardInDeck() }, // Solo deselecciona (cierra el diálogo).
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cerrar", color = White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de estadísticas del mazo.
    if (uiState.showStatsDialog) {
        DeckStatsDialog(deckEditViewModel = deckEditViewModel) {
            deckEditViewModel.dismissStatsDialog() // Callback para cerrar el diálogo de estadísticas.
        }
    }
}

/**
 * Componente Composable que muestra una imagen de carta pequeña con su cantidad,
 * para ser usada dentro de la cuadrícula de cartas del mazo.
 *
 * @param cardId El ID único de la carta (se usa para el callback de clic).
 * @param imageUrl La URL de la imagen de la carta.
 * @param quantity La cantidad de esta carta en el mazo.
 * @param onClick Función de callback que se invoca al hacer clic en la imagen de la carta.
 */
@Composable
fun CardImageInDeck(
    cardId: String,
    imageUrl: String?,
    quantity: Int,
    onClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(1.dp) // Pequeño padding alrededor de cada imagen.
            .aspectRatio(0.72f) // Mantiene la relación de aspecto de una carta (aproximadamente).
            .clickable { onClick(cardId) } // Hace la imagen clickeable.
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .build(),
                contentDescription = null, // No se necesita descripción aquí, ya que el diálogo de detalles la proporciona.
                contentScale = ContentScale.Crop, // Recorta la imagen para llenar el espacio, manteniendo la relación de aspecto.
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp)) // Esquinas ligeramente redondeadas.
            )
        } else {
            // Si no hay imagen, muestra un cuadro gris con un signo de interrogación.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("?", color = White)
            }
        }

        // Insignia (Badge) para mostrar la cantidad de la carta.
        Badge(
            modifier = Modifier
                .align(Alignment.BottomEnd) // Alinea la insignia en la esquina inferior derecha.
                .offset(x = 4.dp, y = 4.dp) // Pequeño offset para que no quede justo en la esquina.
        ) {
            Text(quantity.toString(), color = Color.White) // Muestra la cantidad.
        }
    }
}

/**
 * Componente Composable que muestra un diálogo con las estadísticas detalladas del mazo.
 * Incluye el total de cartas, CMC promedio, distribución por CMC, colores y tipos.
 *
 * @param deckEditViewModel El ViewModel para obtener las estadísticas del mazo.
 * @param onDismiss Función de callback para cerrar el diálogo de estadísticas.
 */
@Composable
fun DeckStatsDialog(
    deckEditViewModel: DeckEditViewModel,
    onDismiss: () -> Unit
) {
    // Recopila las estadísticas del mazo del ViewModel.
    val deckStats by deckEditViewModel.deckStats.collectAsState()

    Dialog(onDismissRequest = onDismiss) { // Cierra el diálogo al presionar fuera o con el botón de atrás.
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Ocupa el 90% del ancho.
                .background(Black, RoundedCornerShape(8.dp)), // Fondo negro con esquinas redondeadas.
            colors = CardDefaults.cardColors(containerColor = Black.copy(alpha = 0.9f)), // Fondo semi-transparente.
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Elevación para la sombra.
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp) // Padding interno.
                    .verticalScroll(rememberScrollState()), // Permite el desplazamiento vertical.
                horizontalAlignment = Alignment.CenterHorizontally // Centra el contenido horizontalmente.
            ) {
                Text("Estadísticas", style = MaterialTheme.typography.headlineMedium, color = Orange) // Título del diálogo.

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp)) { // Columna para el texto de las estadísticas.
                    Text("Total de Cartas: ${deckStats.totalCards}", color = White.copy(alpha = 0.8f))
                    Text("CMC Promedio: %.2f".format(deckStats.cmcAvg), color = White.copy(alpha = 0.8f)) // Formatea el CMC a dos decimales.
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Curva de Maná:", style = MaterialTheme.typography.titleMedium, color = White)
                    // Muestra la distribución por CMC, ordenando por clave.
                    deckStats.cmcDistribution.toSortedMap().forEach { (cmc, count) ->
                        val cmcLabel = if (cmc == 7) "7+" else cmc.toString() // Etiqueta especial para CMC 7+.
                        Text("  Cartas de coste $cmcLabel: $count", color = White.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Cartas por Color:", style = MaterialTheme.typography.titleMedium, color = White)
                    // Define un orden específico para los colores y los muestra.
                    val colorOrder = listOf("Blanco", "Verde", "Rojo", "Negro", "Azul", "Multicolor", "Incoloras")
                    colorOrder.forEach { colorName ->
                        val count = deckStats.colorDistribution[colorName] ?: 0 // Obtiene el conteo o 0 si no existe.
                        // Solo muestra el color si tiene cartas o si es "Incoloras" o "Multicolor" (para asegurar que siempre aparezcan).
                        if (count > 0 || colorName == "Incoloras" || colorName == "Multicolor") {
                            Text("  $colorName: $count", color = White.copy(alpha = 0.8f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Cartas por Tipo:", style = MaterialTheme.typography.titleMedium, color = White)
                    // Muestra la distribución por tipo de carta, ordenando por clave.
                    deckStats.typeDistribution.toSortedMap().forEach { (type, count) ->
                        Text("  $type: $count", color = White.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Botón para cerrar el diálogo de estadísticas.
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    Text("Cerrar", color = Black)
                }
            }
        }
    }
}