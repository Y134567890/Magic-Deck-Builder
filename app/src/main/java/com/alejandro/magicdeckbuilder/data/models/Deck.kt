package com.alejandro.magicdeckbuilder.data.models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.android.parcel.Parcelize

/**
 * Clase de datos que representa un mazo de Magic: The Gathering.
 * Esta clase está diseñada para ser almacenada en Firebase Firestore.
 *
 * @param id El ID único del documento en Firestore. La anotación `@DocumentId`
 * le indica a Firestore que este campo debe usarse como el ID del documento.
 * Si no se proporciona un valor al crear un mazo nuevo, Firestore lo generará.
 * @param userId El ID del usuario propietario de este mazo. Fundamental para filtrar mazos por usuario.
 * @param name El nombre del mazo (ej., "Dimir Control", "Boros Aggro").
 * @param description Una descripción opcional del mazo, su estrategia, etc.
 * @param format El formato en el que se juega el mazo (ej., "Standard", "Commander", "Modern").
 * @param lastModified Una marca de tiempo que indica la última vez que el mazo fue modificado.
 * La anotación `@ServerTimestamp` le indica a Firestore que debe rellenar automáticamente
 * este campo con la fecha y hora del servidor cuando el documento se guarda o actualiza.
 * Es nullable (`Timestamp?`) porque puede no estar presente en la primera creación o al leer datos.
 * @param cards Un mapa que contiene las cartas del mazo. La clave es el ID de la carta (String)
 * y el valor es un objeto [CardInDeckData] que representa la carta y su cantidad.
 * `emptyMap()` es el valor por defecto si el mazo no tiene cartas al inicializarse.
 */
data class Deck(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val format: String = "",
    @ServerTimestamp
    val lastModified: Timestamp? = null,
    val cards: Map<String, CardInDeckData> = emptyMap()
) {
    /**
     * Propiedad computada que calcula el número total de cartas en el mazo.
     * La anotación `@get:Exclude` le indica a Firestore que **no** debe intentar
     * guardar este campo en la base de datos, ya que es un valor derivado y no un campo real del documento.
     *
     * @return El número total de cartas sumando las cantidades de todas las [CardInDeckData] en el mapa 'cards'.
     */
    @get:Exclude
    val cardCount: Int
        get() = cards.values.sumOf { it.quantity }
}

/**
 * Clase de datos que representa una carta dentro de un mazo.
 * Contiene solo la información esencial de una carta necesaria cuando forma parte de un mazo,
 * incluyendo su cantidad y algunos atributos clave para visualización y filtrado.
 *
 * La anotación `@Parcelize` permite que objetos [CardInDeckData] sean pasados de forma eficiente
 * entre componentes de Android sin necesidad de escribir código boilerplate.
 *
 * @param id El ID único de la carta (usado como clave en el mapa 'cards' de [Deck]).
 * La anotación `@DocumentId` no es estrictamente necesaria aquí si este objeto
 * es parte de un mapa dentro de otro documento de Firestore, pero es útil mantenerlo en caso
 * de tener la intención de usarlo como un documento independiente en otro contexto.
 * @param quantity La cantidad de esta carta específica en el mazo.
 * @param imageUrl La URL de la imagen de la carta, para mostrarla en la lista de cartas del mazo.
 * @param name El nombre de la carta, útil para ordenar alfabéticamente las cartas dentro del mazo.
 * @param manaCost El coste de maná de la carta (ej., "{1}{R}"), útil para análisis y visualización.
 * @param cmc El Coste de Maná Convertido, útil para el ordenamiento y el análisis de la curva de maná del mazo.
 * @param colors Una lista de los colores de la carta, útil para el ordenamiento y el análisis de la identidad de color del mazo.
 * @param type El tipo de carta (ej., "Creature", "Instant"), esencial para categorizar y filtrar las cartas del mazo.
 */
@Parcelize
data class CardInDeckData(
    @DocumentId val id: String = "", // Mantener el ID del documento si lo usas
    val quantity: Int = 0,
    val imageUrl: String? = null,
    val name: String = "", // Añadir nombre para ordenamiento alfabético
    val manaCost: String? = null, // Para colores y coste de maná
    val cmc: Double? = null, // Converted Mana Cost para ordenamiento
    val colors: List<String>? = null, // Para ordenamiento por colores y estadísticas
    val type: String? = null, // Para tipo de carta (criatura, conjuro, tierra, etc.)
    // No necesitamos power/toughness para estas funcionalidades
) : Parcelable

//fun CardInDeckData.isCreature(): Boolean = type?.contains("Creature", ignoreCase = true) == true
//fun CardInDeckData.isLand(): Boolean = type?.contains("Land", ignoreCase = true) == true
//fun CardInDeckData.isEnchantment(): Boolean = type?.contains("Enchantment", ignoreCase = true) == true
//fun CardInDeckData.isInstant(): Boolean = type?.contains("Instant", ignoreCase = true) == true
//fun CardInDeckData.isSorcery(): Boolean = type?.contains("Sorcery", ignoreCase = true) == true
//fun CardInDeckData.isPlaneswalker(): Boolean = type?.contains("Planeswalker", ignoreCase = true) == true
//fun CardInDeckData.isArtifact(): Boolean = type?.contains("Artifact", ignoreCase = true) == true

//fun CardInDeckData.hasColor(colorName: String): Boolean {
//    return colors?.any { it.equals(colorName, ignoreCase = true) } == true
//}
//
//fun CardInDeckData.isTrulyColorless(): Boolean {
//    return colors.isNullOrEmpty() && !isLand()
//}