package com.alejandro.magicdeckbuilder.data.local

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Modelo de datos para un mazo guardado localmente en el dispositivo.
 * Es un "gemelo" de la clase [Deck] (usada para Firestore), pero adaptado para
 * un almacenamiento offline.
 *
 * La anotación `@Parcelize` genera automáticamente el código para que los objetos [LocalDeck]
 * puedan pasarse de forma eficiente entre componentes de Android.
 *
 * @param id El ID único del mazo. La idea es usar el mismo ID que el documento de Firestore
 * para mantener la consistencia puesto que los mazos locales se descargan de la versión en la nube.
 * @param userId El ID del usuario propietario de este mazo. Aunque sea local, mantiene
 * la referencia al usuario para quien fue creado.
 * @param name El nombre del mazo (ej., "Mi Mazo Offline").
 * @param description Una descripción opcional del mazo.
 * @param format El formato en el que se juega el mazo (ej., "Standard", "Commander").
 * @param cards Un mapa que contiene las cartas del mazo. La clave es el ID de la carta (String)
 * y el valor es un objeto [LocalCardInDeckData] que representa la carta y su cantidad.
 * `emptyMap()` es el valor por defecto si el mazo no tiene cartas al inicializarse.
 */
@Parcelize
data class LocalDeck(
    val id: String,
    val userId: String,
    val name: String,
    val description: String,
    val format: String,
    val cards: Map<String, LocalCardInDeckData> = emptyMap()
) : Parcelable {
    /**
     * Propiedad computada que calcula el número total de cartas en el mazo local.
     * Esta propiedad es idéntica a la de la clase [Deck], calculando la suma de las cantidades
     * de todas las cartas en el mapa `cards`.
     *
     * @return El número total de cartas físicas en el mazo.
     */
    val cardCount: Int
        get() = cards.values.sumOf { it.quantity }
}

/**
 * Modelo para una carta individual dentro de un mazo almacenado localmente.
 * Su estructura es muy similar a [CardInDeckData], pero con una diferencia crítica
 * en el manejo de las imágenes.
 *
 * La anotación `@Parcelize` permite que objetos [LocalCardInDeckData] sean pasados de forma eficiente
 * entre componentes de Android.
 *
 * @param id El ID único de la carta (usado como clave en el mapa 'cards' de [LocalDeck]).
 * @param quantity La cantidad de esta carta específica en el mazo local.
 * @param imagePath La **ruta de archivo local** (String) donde se guarda la imagen de la carta
 * en el almacenamiento del dispositivo. Esta es la diferencia **más importante**
 * con respecto a [CardInDeckData] que usa una URL de internet. Puede ser nulo si la imagen no se guardó.
 * @param name El nombre de la carta, útil para mostrarla en la UI local.
 * @param manaCost El coste de maná de la carta, para visualización y análisis.
 * @param cmc El Coste de Maná Convertido, útil para el ordenamiento y análisis local.
 * @param colors Una lista de los colores de la carta, para el ordenamiento y análisis local.
 * @param type El tipo de carta, esencial para categorizar y filtrar las cartas en el mazo local.
 */
@Parcelize
data class LocalCardInDeckData(
    val id: String,
    val quantity: Int,
    val imagePath: String?,
    val name: String,
    val manaCost: String?,
    val cmc: Double?,
    val colors: List<String>?,
    val type: String?
) : Parcelable

//fun LocalCardInDeckData.isCreature(): Boolean = type?.contains("Creature", ignoreCase = true) == true