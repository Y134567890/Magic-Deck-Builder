package com.alejandro.magicdeckbuilder.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alejandro.magicdeckbuilder.data.Card
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Componente Composable que muestra una cuadrícula vertical de cartas.
 * Soporta la carga infinita (paginación) y maneja los clics en cada carta.
 *
 * @param cards La lista de objetos [Card] a mostrar en la cuadrícula.
 * @param isLoading Booleano que indica si se están cargando más cartas (para mostrar un indicador de carga).
 * @param onCardClick Función de callback que se invoca cuando se hace clic en una [Card] individual.
 * @param onLoadMore Función de callback que se invoca cuando el usuario se acerca al final de la lista
 * y se deben cargar más elementos.
 */
@Composable
fun CardGrid(
    cards: List<Card>,
    isLoading: Boolean,
    onCardClick: (Card) -> Unit,
    onLoadMore: () -> Unit
) {
    // Guarda el estado del desplazamiento de la cuadrícula, permitiendo observar su posición.
    val listState = rememberLazyGridState()

    // `LazyVerticalGrid` es un componente de Compose optimizado para mostrar listas de elementos
    // en una cuadrícula vertical de manera eficiente, cargando solo los elementos visibles.
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // Define 2 columnas fijas.
        modifier = Modifier.fillMaxSize(), // Ocupa el tamaño disponible de su padre.
        verticalArrangement = Arrangement.spacedBy(8.dp), // Espaciado vertical entre filas.
        horizontalArrangement = Arrangement.spacedBy(8.dp), // Espaciado horizontal entre columnas.
        // Relleno alrededor del contenido de la cuadrícula. El padding inferior de 64.dp
        // ayuda a que el último elemento no quede oculto bajo la barra de navegación o elementos inferiores.
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 64.dp),
        state = listState // Asigna el estado de la cuadrícula para observar el desplazamiento.
    ) {
        // Itera sobre la lista de `cards` para crear un `CardItem` para cada una.
        // `key = { it.id }` es importante para la eficiencia de recomposición de LazyGrid,
        // ya que ayuda a Compose a identificar elementos únicos cuando la lista cambia.
        items(cards, key = { it.id }) { card ->
            CardItem(card = card, onClick = onCardClick) // Crea un CardItem para cada carta.
        }

        // Si se está cargando, añade un indicador de progreso al final de la cuadrícula.
        if (isLoading) {
            item { // `item` se usa para añadir un solo elemento a LazyGrid/LazyColumn.
                Box(
                    modifier = Modifier
                        .fillMaxWidth() // Ocupa el ancho disponible.
                        .padding(16.dp), // Padding alrededor del indicador.
                    contentAlignment = Alignment.Center // Centra el contenido (el CircularProgressIndicator).
                ) {
                    CircularProgressIndicator() // Muestra el indicador de carga.
                }
            }
        }
    }

    // `LaunchedEffect` se usa para ejecutar efectos secundarios (como observar el estado de desplazamiento)
    // que se deben iniciar cuando el Composable entra en la composición y detenerse cuando sale.
    // Se ejecuta cada vez que `listState` cambia (aunque en este caso, `listState` es recordado
    // y solo cambia su contenido interno, no la referencia en sí, por lo que el efecto se lanzará una vez).
    LaunchedEffect(listState) {
        // `snapshotFlow` crea un Kotlin Flow que emite valores cada vez que el estado de Compose
        // observado dentro del bloque cambia. Aquí observa el `index` del último elemento visible.
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            // `map` transforma el índice del último elemento visible.
            // La condición `it != null && it >= cards.size - 5 && !isLoading` significa:
            // - `it != null`: Asegura que hay al menos un elemento visible.
            // - `it >= cards.size - 5`: Comprueba si el usuario se ha desplazado cerca del final de la lista
            //   (cuando quedan 5 o menos elementos para llegar al final).
            // - `!isLoading`: Evita llamadas repetidas a `onLoadMore` si ya se está cargando.
            .map { it != null && it >= cards.size - 5 && !isLoading }
            // `distinctUntilChanged` asegura que `collect` solo se ejecute cuando el valor emitido
            // (`shouldLoadMore`) cambie realmente (de false a true, o de true a false),
            // evitando llamadas redundantes a `onLoadMore`.
            .distinctUntilChanged()
            // `collect` consume los valores del Flow.
            .collect { shouldLoadMore ->
                // Si la condición `shouldLoadMore` es verdadera, invoca el callback para cargar más cartas.
                if (shouldLoadMore) {
                    onLoadMore()
                }
            }
    }
}

/**
 * Componente Composable que representa una única carta en la cuadrícula.
 * Muestra la imagen de la carta y su nombre.
 *
 * @param card El objeto [Card] a mostrar.
 * @param onClick Función de callback que se invoca al hacer clic en la carta.
 */
@Composable
fun CardItem(card: Card, onClick: (Card) -> Unit) {
    // `Card` es un componente de Material Design que proporciona una superficie elevada.
    Card(
        modifier = Modifier
            .fillMaxWidth() // Ocupa el ancho disponible en su celda de la cuadrícula.
            .height(250.dp) // Altura fija para cada ítem de carta.
            .clickable { onClick(card) } // Hace la tarjeta clickeable y llama al callback.
            .padding(4.dp) // Padding interno dentro de la tarjeta.
    ) {
        Column(
            modifier = Modifier.fillMaxSize(), // Ocupa el tamaño de la tarjeta.
            horizontalAlignment = Alignment.CenterHorizontally, // Centra horizontalmente el contenido.
            verticalArrangement = Arrangement.SpaceBetween // Distribuye el espacio verticalmente entre sus hijos.
        ) {
            // Muestra la imagen de la carta si está disponible.
            if (card.imageUrl != null) {
                // `AsyncImage` de Coil carga la imagen de forma asíncrona desde la URL.
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(card.imageUrl) // URL de la imagen de la carta.
                        .crossfade(true) // Habilita una animación de fundido cruzado al cargar.
                        .error(android.R.drawable.ic_menu_close_clear_cancel) // Imagen a mostrar si hay un error de carga.
                        .placeholder(android.R.drawable.ic_menu_gallery) // Imagen a mostrar mientras la carga está en progreso.
                        .build(),
                    contentDescription = card.name, // Descripción para accesibilidad.
                    contentScale = ContentScale.Fit, // Escala la imagen para que quepa dentro de los límites, manteniendo su relación de aspecto.
                    modifier = Modifier
                        .weight(1f) // Hace que la imagen ocupe la mayor parte del espacio vertical disponible.
                        .fillMaxWidth() // Ocupa el ancho disponible.
                        .padding(bottom = 4.dp) // Pequeño padding inferior antes del nombre.
                )
            } else {
                // Si no hay URL de imagen, muestra un Box con un mensaje.
                Box(
                    modifier = Modifier
                        .weight(1f) // También ocupa la mayor parte del espacio vertical.
                        .fillMaxWidth(), // Ocupa el ancho.
                    contentAlignment = Alignment.Center // Centra el mensaje de texto.
                ) {
                    Text(
                        text = "Imagen no disponible", // Mensaje si no hay imagen.
                        style = MaterialTheme.typography.bodySmall, // Estilo de texto.
                        textAlign = TextAlign.Center // Centra el texto dentro del Box.
                    )
                }
            }
            // Muestra el nombre de la carta.
            Text(
                text = card.name,
                style = MaterialTheme.typography.labelSmall, // Estilo de texto más pequeño, ideal para etiquetas.
                textAlign = TextAlign.Center, // Centra el texto.
                modifier = Modifier.padding(
                    start = 4.dp,
                    end = 4.dp,
                    bottom = 4.dp
                )
            )
        }
    }
}