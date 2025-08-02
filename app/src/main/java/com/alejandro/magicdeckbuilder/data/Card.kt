package com.alejandro.magicdeckbuilder.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

//data class CardResponse(
//    val cards: List<Card>
//)

/**
 * Clase de datos que representa una carta individual de Magic: The Gathering en la aplicación.
 * Esta clase contiene un subconjunto simplificado de los datos disponibles en la API de Scryfall,
 * adaptado a las necesidades específicas de la aplicación.
 *
 * La anotación `@Parcelize` genera automáticamente el código necesario
 * para que los objetos [Card] puedan ser pasados de forma eficiente entre componentes de Android
 * (como entre Activities o Fragments/Composables mediante navegación).
 *
 * @param id El ID único de Scryfall para la carta. Es un identificador global persistente.
 * @param name El nombre completo de la carta (ej., "Lightning Bolt").
 * @param imageUrl La URL de la imagen de la carta. Se selecciona una URL específica (ej., 'normal')
 * del campo 'image_uris' de Scryfall a través del [CardDeserializer]. Puede ser nulo si no hay imagen.
 * @param manaCost El coste de maná de la carta en su formato de texto (ej., "{1}{R}"). Puede ser nulo.
 * @param cmc El Coste de Maná Convertido (Converted Mana Cost) de la carta, como un número decimal (ej., 1.0).
 * Puede ser nulo para cartas sin coste de maná.
 * @param colors Una lista de los colores de la carta. Puede ser nulo para cartas incoloras o sin color.
 * @param type El tipo de carta, incluyendo subtipos (ej., "Creature — Human Soldier", "Instant", "Planeswalker — Jace").
 * Puede ser nulo.
 * @param power El valor de fuerza de una criatura (ej., "2"). Solo aplica a criaturas. Puede ser nulo.
 * @param toughness El valor de resistencia de una criatura (ej., "3"). Solo aplica a criaturas. Puede ser nulo.
 * @param oracleText El texto de la carta, que describe sus habilidades. Puede ser nulo.
 * @param setCode El código de 3-4 letras de la expansión a la que pertenece la carta (ej., "ELD" para "Throne of Eldraine").
 * Puede ser nulo.
 * @param setName El nombre completo de la expansión (ej., "Throne of Eldraine"). Puede ser nulo.
 * @param rarity La rareza de la carta (ej., "common", "uncommon", "rare", "mythic"). Puede ser nulo.
 */
@Parcelize
data class Card(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val manaCost: String?,
    val cmc: Double?,
    val colors: List<String>?,
    val type: String?,
    val power: String?,
    val toughness: String?,
    val oracleText: String? = null,
    val setCode: String? = null,
    val setName: String? = null,
    val rarity: String? = null
) : Parcelable

//fun Card.isCreature(): Boolean = type?.contains("Creature", ignoreCase = true) == true
//fun Card.isLand(): Boolean = type?.contains("Land", ignoreCase = true) == true
//fun Card.isEnchantment(): Boolean = type?.contains("Enchantment", ignoreCase = true) == true
//fun Card.isInstant(): Boolean = type?.contains("Instant", ignoreCase = true) == true
//fun Card.isSorcery(): Boolean = type?.contains("Sorcery", ignoreCase = true) == true
//fun Card.isPlaneswalker(): Boolean = type?.contains("Planeswalker", ignoreCase = true) == true
//fun Card.isArtifact(): Boolean = type?.contains("Artifact", ignoreCase = true) == true

//fun Card.hasColorByManaCost(colorSymbol: String): Boolean {
//    return manaCost?.contains("{$colorSymbol}", ignoreCase = true) == true
//}
//
//fun Card.isTrulyColorless(): Boolean {
//    val hasColorSymbol = manaCost?.contains("{W}", ignoreCase = true) == true ||
//            manaCost?.contains("{U}", ignoreCase = true) == true ||
//            manaCost?.contains("{B}", ignoreCase = true) == true ||
//            manaCost?.contains("{R}", ignoreCase = true) == true ||
//            manaCost?.contains("{G}", ignoreCase = true) == true
//
//    val hasColorInList = !colors.isNullOrEmpty()
//
//    return !hasColorSymbol && !hasColorInList
//}