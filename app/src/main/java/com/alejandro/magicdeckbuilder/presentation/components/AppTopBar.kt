package com.alejandro.magicdeckbuilder.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DensityMedium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Gray
import kotlinx.coroutines.launch

/**
 * Componente Composable que representa la barra superior de la aplicación (TopBar).
 * Muestra el nombre de usuario, permite navegación hacia atrás, ofrece opciones de menú
 * (cerrar sesión, ir a amigos) y puede mostrar un subtítulo.
 *
 * @param username El nombre de usuario a mostrar. Puede ser nulo si no se ha cargado.
 * @param canNavigateBack Booleano que indica si el botón de "volver" debe ser visible.
 * @param onNavigateBack Función de callback que se invoca al presionar el botón de "volver".
 * @param onSignOut Función de callback que se invoca al seleccionar "Cerrar Sesión" del menú.
 * @param onNavigateToFriends Función de callback que se invoca al seleccionar "Amigos" del menú.
 * @param modifier Modificador opcional para aplicar a la TopBar (ej. padding, tamaño).
 * @param subtitle Un subtítulo opcional para mostrar debajo del nombre de usuario.
 * @param hideSection Booleano que, si es true, oculta la sección de acciones (botón de menú).
 * Se usa para ocultar el menú desplegable en el modo offline
 * @param onNavigateToAccountManagement Callback para navegar a la pantalla de gestión de cuenta.
 */
@OptIn(ExperimentalMaterial3Api::class) // Se requiere para usar TopAppBar de Material 3
@Composable
fun AppTopBar(
    username: String?,
    canNavigateBack: Boolean,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToFriends: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    hideSection: Boolean = false,
    onNavigateToAccountManagement: () -> Unit
) {
    // Estado mutable para controlar la visibilidad del menú desplegable.
    var showMenu by remember { mutableStateOf(false) }

    // Usa `rememberCoroutineScope` para obtener un alcance de corrutina.
    // Esto permite lanzar corrutinas que están vinculadas al ciclo de vida del composable,
    // asegurando que las operaciones asíncronas se cancelen automáticamente cuando el
    // composable sale de la composición.
    val coroutineScope = rememberCoroutineScope()

    // Variable para registro del momento del último clic.
    var lastClickTime by remember { mutableStateOf(0L) }

    // Define el tiempo mínimo (en milisegundos) que debe transcurrir entre dos clics.
    val clickThreshold = 500L

    // Componente TopAppBar de Material Design 3.
    TopAppBar(
        title = {
            // Contenido del título: Envuelve el nombre de usuario y el subtítulo en una Column.
            Column {
                Text(
                    text = username ?: "Cargando...", // Muestra el nombre de usuario o "Cargando..."
                    color = White, // Color del texto del nombre de usuario
                    style = MaterialTheme.typography.headlineSmall, // Estilo de texto definido en el tema
                )
                // Si se proporciona un subtítulo, lo muestra debajo del nombre de usuario.
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = White.copy(alpha = 0.7f), // Un blanco semi-transparente para el subtítulo
                        style = MaterialTheme.typography.bodySmall, // Estilo de texto más pequeño
                    )
                }
            }
        },
        navigationIcon = {
            // Icono de navegación (botón de volver)
            if (canNavigateBack) { // Solo muestra el botón si canNavigateBack es true
                IconButton(
                    onClick = {
                        // Lanza una nueva corrutina en el alcance (`coroutineScope`) del composable
                        // para ejecutar lógica asíncrona dentro del onClick.
                        coroutineScope.launch {
                            // Se obtiene el tiempo actual del sistema en milisegundos.
                            val currentTime = System.currentTimeMillis()

                            // Se comprueba si el tiempo transcurrido desde el último clic es mayor
                            // que el umbral de 500 ms.
                            if (currentTime - lastClickTime > clickThreshold) {
                                // Si ha pasado suficiente tiempo, actualiza `lastClickTime` al tiempo actual.
                                lastClickTime = currentTime
                                // Se ejecuta la acción de navegación.
                                // Esto previene el mal funcionamiento por realizar varias pulsaciones seguidas en muy poco tiempo
                                onNavigateBack()
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Icono de flecha hacia atrás
                        contentDescription = "Volver", // Descripción para accesibilidad
                        tint = White // Color del icono
                    )
                }
            }
        },
        actions = {
            // Acciones/iconos en el lado derecho de la TopBar.
            if (!hideSection) { // Solo muestra las acciones si hideSection es false; es decir, si NO hay que ocultar la sección
                // Botón para mostrar/ocultar el menú desplegable de opciones.
                IconButton(onClick = { showMenu = !showMenu }) {
                    Icon(
                        imageVector = Icons.Filled.DensityMedium, // Icono de tres líneas (hamburguesa)
                        contentDescription = "Opciones", // Descripción para accesibilidad
                        tint = White // Color del icono
                    )
                }
                // Menú desplegable (DropdownMenu)
                DropdownMenu(
                    expanded = showMenu, // Controla la visibilidad del menú
                    onDismissRequest = { showMenu = false }, // Al cerrar el menú (ej. tocar fuera), lo oculta
                    modifier = Modifier.background(Black) // Fondo negro para el menú
                ) {
                    // Ítem del menú: "Amigos"
                    DropdownMenuItem(
                        text = { Text("Amigos", color = White) }, // Texto del ítem
                        onClick = {
                            showMenu = false // Oculta el menú
                            onNavigateToFriends() // Navega a la pantalla de amigos
                        }
                    )
                    // Ítem del menú: "Gestión de cuenta"
                    DropdownMenuItem(
                        text = { Text("Gestión de cuenta", color = White) },
                        onClick = {
                            showMenu = false
                            onNavigateToAccountManagement() // NUEVO
                        }
                    )
                    // Ítem del menú: "Cerrar Sesión"
                    DropdownMenuItem(
                        text = { Text("Cerrar Sesión", color = White) }, // Texto del ítem
                        onClick = {
                            onSignOut() // Invoca el callback para cerrar sesión
                            showMenu = false // Oculta el menú
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent, // Fondo transparente para la barra
            titleContentColor = White, // Color del contenido del título
            actionIconContentColor = White, // Color de los iconos de acción
            navigationIconContentColor = White // Color del icono de navegación
        ),
        modifier = modifier // Aplica el modificador pasado como parámetro
            .fillMaxWidth() // Asegura que la TopBar ocupe el ancho disponible
            .background(
                Brush.verticalGradient(listOf(Gray, Black), startY = 0f, endY = 600f) // Fondo con degradado
            )
            .windowInsetsPadding(WindowInsets.statusBars) // Añade padding para no superponerse con la barra de estado del sistema
    )
}