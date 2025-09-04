package com.alejandro.magicdeckbuilder.presentation.deckedit

import android.app.Application
import android.util.Log
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alejandro.magicdeckbuilder.presentation.components.AppTopBar
import com.alejandro.magicdeckbuilder.presentation.user.UserViewModel
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Gray
import com.alejandro.magicdeckbuilder.ui.theme.Orange
import java.io.File

/**
 * Pantalla para visualizar un mazo local.
 * Esta pantalla es de solo lectura y no permite la edición del mazo.
 *
 * @param localDeckViewViewModel ViewModel que gestiona la lógica de visualización del mazo local.
 * @param userViewModel ViewModel para obtener la información del usuario.
 * @param onNavigateBack Lambda para manejar la navegación hacia atrás.
 * @param navController Controlador de navegación de Jetpack Compose.
 * @param onSignOut Lambda para manejar el cierre de sesión (no usado si hideSection es true).
 * @param onNavigateToFriends Lambda para navegar a la pantalla de amigos (no usado si hideSection es true).
 * @param deckId El ID del mazo local a cargar y visualizar.
 * @param hideSection Booleano que indica si se deben ocultar elementos de la barra superior
 * (como el nombre de usuario y los botones de amigos/cerrar sesión)
 * para el Modo Offline.
 * @param onNavigateToAccountManagement Función de callback para navegar a la pantalla de gestión de cuenta
 */
@OptIn(ExperimentalMaterial3Api::class) // Opt-in para usar APIs experimentales de Material3
@Composable
fun LocalDeckViewScreen(
    localDeckViewViewModel: LocalDeckViewViewModel = viewModel(factory = LocalDeckViewViewModelFactory(LocalContext.current.applicationContext as Application)), // Instancia el ViewModel usando un factory personalizado que requiere el Application Context.
    userViewModel: UserViewModel,
    onNavigateBack: () -> Unit,
    navController: NavController,
    onSignOut: () -> Unit,
    onNavigateToFriends: () -> Unit,
    deckId: String,
    hideSection: Boolean,
    onNavigateToAccountManagement: () -> Unit
) {
    // Recolecta los estados de los ViewModels como State de Compose.
    val uiState by localDeckViewViewModel.uiState.collectAsState()
    val userUiState by userViewModel.uiState.collectAsState()

    val deckStats by localDeckViewViewModel.localDeckStats.collectAsState() // Estadísticas del mazo local

    // Efecto secundario que se ejecuta cuando el `deckId` cambia.
    // Llama a la función del ViewModel para cargar el mazo local.
    LaunchedEffect(deckId) {
        Log.d("LocalDeckViewScreen", "LaunchedEffect(deckId) triggered. DeckId: $deckId")
        localDeckViewViewModel.loadLocalDeckForView(deckId)
    }

    Scaffold(
        topBar = {
            // Condicionalmente renderiza la AppTopBar.
            // Si `hideSection` es falso (modo online), muestra la barra normal.
            if (!hideSection) {
                Column { // Envuelve la barra en una Column para posible futura expansión.
                    AppTopBar(
                        username = userUiState.currentUser?.username, // Nombre de usuario autenticado
                        canNavigateBack = true, // Permite navegación hacia atrás
                        onNavigateBack = onNavigateBack, // Acción de navegación hacia atrás
                        modifier = Modifier,
                        onSignOut = onSignOut, // Acción de cerrar sesión
                        onNavigateToFriends = onNavigateToFriends, // Acción de navegar a amigos
                        subtitle = "Mazos locales", // Subtítulo fijo
                        onNavigateToAccountManagement = onNavigateToAccountManagement // Callback para navegar a gestión de cuenta
                    )
                }
            } else {
                // Si `hideSection` es true (Modo Offline), muestra una barra simplificada.
                AppTopBar(
                    username = "Modo Offline", // Texto para indicar Modo Offline
                    canNavigateBack = true,
                    onNavigateBack = onNavigateBack,
                    onSignOut = { }, // No hay acción de cerrar sesión en este modo
                    modifier = Modifier,
                    onNavigateToFriends = { }, // No hay acción de navegar a amigos en este modo
                    subtitle = "Mazos locales", // Subtítulo fijo
                    hideSection = true, // Pasa el flag `hideSection` a la barra
                    onNavigateToAccountManagement = onNavigateToAccountManagement // Callback para navegar a gestión de cuenta
                )
            }
        }
    ) { paddingValues ->
        // Contenedor principal de la pantalla con fondo degradado.
        Box(
            modifier = Modifier
                .fillMaxSize() // Ocupa el tamaño disponible
                .padding(paddingValues) // Aplica el padding de la Scaffold
                .background(Brush.verticalGradient(listOf(Gray, Black), startY = 0f, endY = 600f)) // Fondo degradado
        ) {
            // Manejo del estado de carga y error.
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
                // Botón opcional para descartar el error.
                TextButton(onClick = { localDeckViewViewModel.dismissErrorMessage() }) {
                    Text("Cerrar error", color = Orange)
                }
            } else {
                // Si no hay carga ni error, muestra el contenido del mazo.
                val currentDeck = uiState.currentDeck
                if (currentDeck == null) {
                    // Mensaje si el mazo local no se encontró.
                    Text(
                        text = "Mazo local no encontrado.",
                        color = White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    // Contenido principal: detalles del mazo y lista de cartas.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()) // Permite el desplazamiento vertical de toda la columna.
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp) // Espacio entre los elementos de la columna.
                    ) {
                        // Campo de texto para el nombre del mazo (solo lectura).
                        OutlinedTextField(
                            value = currentDeck.name,
                            onValueChange = { /* No editable */ },
                            label = { Text("Nombre del Mazo", color = White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false, // Deshabilita la edición.
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Orange,
                                unfocusedBorderColor = White.copy(alpha = 0.5f),
                                focusedLabelColor = Orange,
                                unfocusedLabelColor = White.copy(alpha = 0.5f),
                                cursorColor = White,
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                containerColor = Black.copy(alpha = 0.5f),
                                errorBorderColor = Color.Red,
                                errorLabelColor = Color.Red,
                                disabledTextColor = White.copy(alpha = 0.7f), // Color para texto deshabilitado
                                disabledLabelColor = White.copy(alpha = 0.5f), // Color para etiqueta deshabilitada
                                disabledBorderColor = White.copy(alpha = 0.3f), // Color para borde deshabilitado
                            )
                        )

                        // Campo de texto para la descripción del mazo (solo lectura).
                        OutlinedTextField(
                            value = currentDeck.description,
                            onValueChange = { /* No editable */ },
                            label = { Text("Descripción", color = White.copy(alpha = 0.7f)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            enabled = false, // Deshabilita la edición.
                            colors = TextFieldDefaults.outlinedTextFieldColors(
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

                        // Campo de texto para el formato del mazo (solo lectura).
                        OutlinedTextField(
                            value = currentDeck.format,
                            onValueChange = { /* No editable */ },
                            label = { Text("Formato (ej. Standard, Commander)", color = White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false, // Deshabilita la edición.
                            colors = TextFieldDefaults.outlinedTextFieldColors(
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

                        // Tarjeta que contiene la lista de cartas y opciones de ordenación/estadísticas.
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Black.copy(alpha = 0.7f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Cartas en el Mazo: ${currentDeck.cardCount}", color = White, style = MaterialTheme.typography.titleMedium)

                                    Spacer(modifier = Modifier.weight(1f)) // Espacio flexible

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
                                                localDeckViewViewModel.setSortOption(SortOption.CMC)
                                                showSortMenu = false
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Colores", color = White) },
                                            onClick = {
                                                localDeckViewViewModel.setSortOption(SortOption.COLORS)
                                                showSortMenu = false
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Alfabéticamente", color = White) },
                                            onClick = {
                                                localDeckViewViewModel.setSortOption(SortOption.ALPHABETICAL)
                                                showSortMenu = false
                                            },
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Botón para mostrar las estadísticas del mazo.
                                    IconButton(
                                        onClick = { localDeckViewViewModel.showStatsDialog() },
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
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Muestra un mensaje si el mazo no tiene cartas.
                                if (currentDeck.cards.isEmpty()) {
                                    Text(
                                        "No hay cartas en este mazo local.",
                                        color = White.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .padding(vertical = 16.dp)
                                    )
                                } else {
                                    // Lista de cartas del mazo, ordenada.
                                    val sortedCards by localDeckViewViewModel.sortedCardsInLocalDeck.collectAsState()
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(5), // 5 columnas fijas
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 400.dp), // Altura máxima, permite scroll si excede.
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalArrangement = Arrangement.spacedBy(0.dp) // Sin espacio vertical entre filas.
                                    ) {
                                        items(sortedCards) { (cardId, cardData) ->
                                            // Cada CardImageInDeck es un elemento clicable que muestra la imagen de la carta.
                                            LocalCardImageInDeck(
                                                cardId = cardId,
                                                imagePath = cardData.imagePath,
                                                quantity = cardData.quantity,
                                                onClick = { localDeckViewViewModel.selectCardInLocalDeck(cardId) }
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
    }

    // Diálogo para mostrar la imagen de la carta seleccionada.
    uiState.selectedCardInLocalDeckId?.let { selectedCardId ->
        val cardInDeckData = uiState.currentDeck?.cards?.get(selectedCardId)
        val imagePath = cardInDeckData?.imagePath // Obtiene la ruta de la imagen local

        if (imagePath != null) {
            Dialog(
                onDismissRequest = { localDeckViewViewModel.deselectCardInLocalDeck() }, // Cierra el diálogo al tocar fuera o presionar atrás.
                properties = DialogProperties(usePlatformDefaultWidth = false) // Permite que el diálogo use el ancho personalizado.
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)) // Fondo oscuro semitransparente.
                        .clickable { localDeckViewViewModel.deselectCardInLocalDeck() }, // Cierra el diálogo al hacer clic en el fondo.
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f) // Ocupa el 90% del ancho.
                            .background(Black, RoundedCornerShape(8.dp)) // Fondo negro con esquinas redondeadas.
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Muestra la imagen de la carta usando Coil.
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(File(imagePath)) // Carga la imagen desde un archivo local.
                                .crossfade(true)
                                .error(android.R.drawable.ic_menu_close_clear_cancel) // Imagen de error
                                .placeholder(android.R.drawable.ic_menu_gallery) // Placeholder
                                .build(),
                            contentDescription = "Carta en mazo local",
                            contentScale = ContentScale.FillWidth, // Escala la imagen para llenar el ancho.
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = false) {  } // Evita cerrar el diálogo al hacer clic en la imagen.
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { localDeckViewViewModel.deselectCardInLocalDeck() }, // Botón para cerrar el diálogo.
                            colors = ButtonDefaults.buttonColors(containerColor = Orange),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cerrar", color = Black)
                        }
                    }
                }
            }
        }
    }

    // Diálogo para mostrar las estadísticas del mazo.
    if (uiState.showStatsDialog) {
        LocalDeckStatsDialog(localDecksViewModel = localDeckViewViewModel) {
            localDeckViewViewModel.dismissStatsDialog() // Cierra el diálogo al tocar fuera o presionar el botón.
        }
    }
}

/**
 * Composable para mostrar una imagen de carta en el mazo con su cantidad.
 * Utiliza Coil para cargar la imagen desde una ruta de archivo local.
 *
 * @param cardId El ID de la carta.
 * @param imagePath La ruta de archivo local de la imagen de la carta.
 * @param quantity La cantidad de esta carta en el mazo.
 * @param onClick Lambda que se invoca cuando se hace clic en la carta.
 */
@Composable
fun LocalCardImageInDeck(
    cardId: String,
    imagePath: String?,
    quantity: Int,
    onClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(1.dp)
            .aspectRatio(0.72f) // Relación de aspecto común para las cartas de Magic.
            .clickable { onClick(cardId) } // Hace el Box clicable.
    ) {
        if (imagePath != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(imagePath)) // Carga la imagen desde un archivo.
                    .crossfade(true)
                    .error(android.R.drawable.ic_menu_close_clear_cancel) // Imagen de error si falla la carga.
                    .placeholder(android.R.drawable.ic_menu_gallery) // Placeholder mientras carga.
                    .build(),
                contentDescription = null, // No se necesita una descripción de contenido para este caso.
                contentScale = ContentScale.Crop, // Escala la imagen para recortar los bordes si es necesario.
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp)) // Recorta la imagen con esquinas redondeadas.
            )
        } else {
            // Si no hay ruta de imagen, muestra un cuadro gris con un signo de interrogación.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("?", color = White)
            }
        }

        // Insignia que muestra la cantidad de cartas.
        Badge(
            modifier = Modifier
                .align(Alignment.BottomEnd) // Alinea la insignia en la esquina inferior derecha.
                .offset(x = 4.dp, y = 4.dp) // Desplaza la insignia ligeramente.
        ) {
            Text(quantity.toString(), color = Color.White)
        }
    }
}

/**
 * Composable para mostrar un diálogo con las estadísticas de un mazo local.
 *
 * @param localDecksViewModel ViewModel que proporciona las estadísticas del mazo.
 * @param onDismiss Lambda que se invoca cuando el diálogo debe cerrarse.
 */
@Composable
fun LocalDeckStatsDialog(
    localDecksViewModel: LocalDeckViewViewModel,
    onDismiss: () -> Unit
) {
    val deckStats by localDecksViewModel.localDeckStats.collectAsState() // Recolecta las estadísticas del mazo.

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f) // El diálogo ocupa el 90% del ancho.
                .background(Black, RoundedCornerShape(8.dp)), // Fondo negro con esquinas redondeadas.
            colors = CardDefaults.cardColors(containerColor = Black.copy(alpha = 0.9f)), // Color de fondo de la tarjeta.
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Elevación de la tarjeta.
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),  // Permite el desplazamiento vertical dentro del diálogo.
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Estadísticas del Mazo Local", style = MaterialTheme.typography.headlineMedium, color = Orange)
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp)) {
                    Text("Total de Cartas: ${deckStats.totalCards}", color = White.copy(alpha = 0.8f))
                    Text("CMC Promedio: %.2f".format(deckStats.cmcAvg), color = White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Curva de Maná:", style = MaterialTheme.typography.titleMedium, color = White)
                    // Muestra la distribución por CMC, ordenada por CMC.
                    deckStats.cmcDistribution.toSortedMap().forEach { (cmc, count) ->
                        val cmcLabel = if (cmc == 7) "7+" else cmc.toString() // Etiqueta "7+" para CMC >= 7.
                        Text("  Cartas de coste $cmcLabel: $count", color = White.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Cartas por Color:", style = MaterialTheme.typography.titleMedium, color = White)
                    // Define un orden específico para mostrar los colores.
                    val colorOrder = listOf("Blanco", "Verde", "Rojo", "Negro", "Azul", "Multicolor", "Incoloras")
                    colorOrder.forEach { colorName ->
                        val count = deckStats.colorDistribution[colorName] ?: 0
                        // Solo muestra el color si tiene cartas, o si es "Incoloras" o "Multicolor" (para asegurar que siempre aparezcan).
                        if (count > 0 || colorName == "Incoloras" || colorName == "Multicolor") {
                            Text("  $colorName: $count", color = White.copy(alpha = 0.8f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Cartas por Tipo:", style = MaterialTheme.typography.titleMedium, color = White)
                    // Muestra la distribución por tipo, ordenada alfabéticamente por tipo.
                    deckStats.typeDistribution.toSortedMap().forEach { (type, count) ->
                        Text("  $type: $count", color = White.copy(alpha = 0.8f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = onDismiss, // Botón para cerrar el diálogo.
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    Text("Cerrar", color = Black)
                }
            }
        }
    }
}