package com.alejandro.magicdeckbuilder.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alejandro.magicdeckbuilder.presentation.cardsearch.Filters
import com.alejandro.magicdeckbuilder.ui.theme.Black
import com.alejandro.magicdeckbuilder.ui.theme.Orange
import kotlin.math.max
import kotlin.math.min

/**
 * Componente Composable que muestra un diálogo para aplicar filtros a la búsqueda de cartas.
 * Permite al usuario seleccionar opciones de filtrado como CMC, colores, tipos de carta,
 * fuerza y resistencia.
 *
 * @param currentFilters El objeto [Filters] actual que representa los filtros aplicados.
 * @param onApplyFilters Función de callback que se invoca cuando el usuario aplica los filtros.
 * Recibe un nuevo objeto [Filters] con las selecciones del usuario.
 * @param onCancel Función de callback que se invoca cuando el usuario cancela el diálogo.
 * @param onClearFilters Función de callback que se invoca cuando el usuario borra todos los filtros.
 */
@OptIn(ExperimentalMaterial3Api::class) // Se requiere para usar OutlinedTextField de Material 3
@Composable
fun FilterDialog(
    currentFilters: Filters,
    onApplyFilters: (Filters) -> Unit,
    onCancel: () -> Unit,
    onClearFilters: () -> Unit
) {

    // Estados mutables para cada filtro individual.
    // `remember(currentFilters.property)` es crucial aquí.
    // Esto asegura que el estado local de cada campo de filtro se inicialice con el
    // valor de `currentFilters` solo la primera vez que el diálogo se muestra,
    // o cuando el `currentFilters` (pasado desde el ViewModel) cambia.
    // Si no se usara `remember(key)`, al abrir el diálogo, siempre se reiniciarían a los valores por defecto
    // de `Filters()` en lugar de los filtros que ya estaban aplicados.

    var cmc by remember(currentFilters.cmc) { mutableStateOf(currentFilters.cmc?.toString() ?: "") }
    var white by remember(currentFilters.white) { mutableStateOf(currentFilters.white) }
    var green by remember(currentFilters.green) { mutableStateOf(currentFilters.green) }
    var red by remember(currentFilters.red) { mutableStateOf(currentFilters.red) }
    var black by remember(currentFilters.black) { mutableStateOf(currentFilters.black) }
    var blue by remember(currentFilters.blue) { mutableStateOf(currentFilters.blue) }
    var colorless by remember(currentFilters.colorless) { mutableStateOf(currentFilters.colorless) }
    var isLand by remember(currentFilters.isLand) { mutableStateOf(currentFilters.isLand) }
    var isCreature by remember(currentFilters.isCreature) { mutableStateOf(currentFilters.isCreature) }
    var power by remember(currentFilters.power) { mutableStateOf(currentFilters.power?.toString() ?: "") }
    var toughness by remember(currentFilters.toughness) { mutableStateOf(currentFilters.toughness?.toString() ?: "") }
    var isEnchantment by remember(currentFilters.isEnchantment) { mutableStateOf(currentFilters.isEnchantment) }
    var isSorcery by remember(currentFilters.isSorcery) { mutableStateOf(currentFilters.isSorcery) }
    var isInstant by remember(currentFilters.isInstant) { mutableStateOf(currentFilters.isInstant) }
    var isPlaneswalker by remember(currentFilters.isPlaneswalker) { mutableStateOf(currentFilters.isPlaneswalker) }
    var isArtifact by remember(currentFilters.isArtifact) { mutableStateOf(currentFilters.isArtifact) }

    // `Dialog` es un Composable de nivel superior para mostrar contenido modal que se superpone a la UI.
    Dialog(
        onDismissRequest = onCancel, // Callback invocado cuando el usuario intenta cerrar el diálogo (ej. tocar fuera).
        properties = DialogProperties(usePlatformDefaultWidth = false) // Permite que el diálogo controle su propio ancho.
    ) {
        // `Surface` proporciona una superficie de Material Design con forma, elevación y color.
        Surface(
            shape = MaterialTheme.shapes.medium, // Forma de esquinas redondeadas.
            color = Black.copy(alpha = 0.95f), // Color de fondo del diálogo.
            modifier = Modifier
                .fillMaxWidth(0.9f) // Ocupa el 90% del ancho de la pantalla.
                .padding(16.dp) // Padding exterior alrededor de la superficie.
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp) // Padding interior para el contenido de la columna.
                    .verticalScroll(rememberScrollState()) // Hace que la columna sea desplazable verticalmente si el contenido excede el tamaño.
                    .imePadding() // Para el ajuste con el teclado
            ) {
                // Título del diálogo.
                Text(
                    text = "Filtrar Cartas",
                    style = MaterialTheme.typography.headlineSmall,
                    color = White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Campo de texto para el Coste de Maná Convertido (CMC).
                OutlinedTextField(
                    value = cmc,
                    onValueChange = { newValue ->
                        // Validación de entrada:
                        // Permite vacío o solo dígitos.
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
                            val intValue = newValue.toIntOrNull()
                            // Si es numérico, valida que esté entre 0 y 8.
                            if (intValue == null || (intValue in 0..8)) {
                                cmc = newValue // Actualiza el estado local del CMC.
                            }
                        }
                    },
                    label = { Text("Coste de Maná (CMC)", color = White.copy(alpha = 0.7f)) }, // Etiqueta del campo
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Teclado numérico.
                    modifier = Modifier.fillMaxWidth(), // Ocupa el ancho disponible.
                    placeholder = { Text("0-8 (8 para 8 o más)", color = White.copy(alpha = 0.5f)) }, // Texto de ayuda.
                    colors = OutlinedTextFieldDefaults.colors( // Se utilizan los colores de la app
                        focusedBorderColor = Orange,
                        unfocusedBorderColor = White.copy(alpha = 0.5f),
                        focusedLabelColor = Orange,
                        unfocusedLabelColor = White.copy(alpha = 0.7f),
                        cursorColor = Orange,
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedContainerColor = Black, // Fondo del campo cuando está enfocado
                        unfocusedContainerColor = Black // Fondo del campo cuando no está enfocado
                    )
                )
                Spacer(Modifier.height(8.dp))

                // Sección de filtros por Colores.
                Text("Colores:", style = MaterialTheme.typography.titleMedium, color = White)
                FilterCheckbox(text = "Blanca", checked = white) { white = it }
                FilterCheckbox(text = "Azul", checked = blue) { blue = it }
                FilterCheckbox(text = "Negra", checked = black) { black = it }
                FilterCheckbox(text = "Roja", checked = red) { red = it }
                FilterCheckbox(text = "Verde", checked = green) { green = it }
                FilterCheckbox(text = "Incoloro", checked = colorless) { colorless = it }
                Spacer(Modifier.height(8.dp))

                // Sección de filtros por Tipos de Carta.
                Text("Tipos de Carta:", style = MaterialTheme.typography.titleMedium, color = White)
                FilterCheckbox(text = "Tierra", checked = isLand) { isLand = it }
                FilterCheckbox(text = "Criatura", checked = isCreature) { isCreature = it }
                FilterCheckbox(text = "Encantamiento", checked = isEnchantment) { isEnchantment = it }
                FilterCheckbox(text = "Conjuro", checked = isSorcery) { isSorcery = it }
                FilterCheckbox(text = "Instantáneo", checked = isInstant) { isInstant = it }
                FilterCheckbox(text = "Planeswalker", checked = isPlaneswalker) { isPlaneswalker = it }
                FilterCheckbox(text = "Artefacto", checked = isArtifact) { isArtifact = it } // Nuevo filtro
                Spacer(Modifier.height(8.dp))

                // Campo de texto para la Fuerza.
                OutlinedTextField(
                    value = power,
                    onValueChange = { newValue ->
                        // Validación de entrada:
                        // Permite vacío o solo dígitos.
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
                            val intValue = newValue.toIntOrNull()
                            // Si es numérico, valida que esté entre 0 y 20.
                            if (intValue == null || (intValue in 0..20)) {
                                power = newValue // Actualiza el estado local de la fuerza.
                            }
                        }
                    },
                    label = { Text("Fuerza (0-20)", color = White.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors( // Nuevos colores para OutlinedTextField
                        focusedBorderColor = Orange,
                        unfocusedBorderColor = White.copy(alpha = 0.5f),
                        focusedLabelColor = Orange,
                        unfocusedLabelColor = White.copy(alpha = 0.7f),
                        cursorColor = Orange,
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedContainerColor = Black,
                        unfocusedContainerColor = Black
                    )
                )
                Spacer(Modifier.height(8.dp))

                // Campo de texto para la Resistencia.
                OutlinedTextField(
                    value = toughness,
                    onValueChange = { newValue ->
                        // Validación de entrada:
                        // Permite vacío o solo dígitos.
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d+$"))) {
                            val intValue = newValue.toIntOrNull()
                            // Si es numérico, valida que esté entre 0 y 20.
                            if (intValue == null || (intValue in 0..20)) {
                                toughness = newValue // Actualiza el estado local de la resistencia.
                            }
                        }
                    },
                    label = { Text("Resistencia (0-20)", color = White.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors( // Se utilizan los colores de la app
                        focusedBorderColor = Orange,
                        unfocusedBorderColor = White.copy(alpha = 0.5f),
                        focusedLabelColor = Orange,
                        unfocusedLabelColor = White.copy(alpha = 0.7f),
                        cursorColor = Orange,
                        focusedTextColor = White,
                        unfocusedTextColor = White,
                        focusedContainerColor = Black,
                        unfocusedContainerColor = Black
                    )
                )
                Spacer(Modifier.height(16.dp))

                // Sección de botones de acción (Borrar, Cerrar, Aplicar).
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End // Alinea los botones a la derecha.
                ) {
                    // Botón "Borrar Filtros".
                    Button(
                        onClick = {
                            onClearFilters() // Invoca el callback para borrar filtros.
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Orange, // Fondo del botón en naranja
                            contentColor = Black
                        )
                    ) {
                        Text("Borrar Filtros")
                    }
                    Spacer(Modifier.height(8.dp))
                    // Botón "Cerrar".
                    Button(
                        onClick = onCancel, // Invoca el callback para cancelar/cerrar el diálogo.
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Orange, // Fondo del botón en naranja
                            contentColor = Black
                        )
                    ) {
                        Text("Cerrar")
                    }
                    Spacer(Modifier.height(8.dp))
                    // Botón "Aplicar".
                    Button(
                        onClick = {
                            // Construye un nuevo objeto [Filters] con los valores de los estados locales.
                            val newFilters = Filters(
                                cmc = cmc.toIntOrNull()?.let { if (it > 8) 8 else it }, // Convierte a Int, maneja nulos y limita a 8 (para Scryfall 'mv>=8')
                                white = white,
                                green = green,
                                red = red,
                                black = black,
                                blue = blue,
                                colorless = colorless,
                                isLand = isLand,
                                isCreature = isCreature,
                                power = power.toIntOrNull()?.let { max(0, min(20, it)) }, // Convierte a Int, maneja nulos y limita entre 0 y 20
                                toughness = toughness.toIntOrNull()?.let { max(0, min(20, it)) }, // Convierte a Int, maneja nulos y limita entre 0 y 20
                                isEnchantment = isEnchantment,
                                isSorcery = isSorcery,
                                isInstant = isInstant,
                                isPlaneswalker = isPlaneswalker,
                                isArtifact = isArtifact
                            )
                            onApplyFilters(newFilters) // Invoca el callback con los nuevos filtros.
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Orange, // Fondo del botón en naranja
                            contentColor = Black
                        )
                    ) {
                        Text("Aplicar")
                    }
                }
            }
        }
    }
}

/**
 * Componente Composable auxiliar para un elemento de filtro con un Checkbox y un Text.
 * Simplifica la creación de las opciones de filtro.
 *
 * @param text El texto a mostrar junto al checkbox.
 * @param checked Booleano que indica si el checkbox está marcado.
 * @param onCheckedChange Función de callback que se invoca cuando el estado del checkbox cambia.
 */
@Composable
fun FilterCheckbox(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) } // Hace que toda la fila sea clickeable para cambiar el estado.
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically // Centra verticalmente los elementos de la fila.
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Orange, // Color del checkbox cuando está marcado
                uncheckedColor = White.copy(alpha = 0.7f), // Color del checkbox cuando no está marcado
                checkmarkColor = Black // Color del checkmark
            )
        )
        Text(text, color = White) // El texto del filtro.
    }
}