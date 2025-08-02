package com.alejandro.magicdeckbuilder.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.alejandro.magicdeckbuilder.data.Card
import java.lang.reflect.Type

/**
 * Deserializador personalizado para la clase [Card].
 * Implementa [JsonDeserializer] para indicar a Gson cómo convertir un [JsonElement] (el JSON bruto)
 * en un objeto [Card] de Kotlin.
 *
 * Este deserializador es esencial porque:
 * 1. La API de Scryfall tiene una estructura JSON muy detallada que no mapea directamente
 * a los campos simplificados de la clase [Card].
 * 2. Se necesita lógica personalizada, como seleccionar la URL de imagen correcta
 * o manejar cartas de doble cara.
 * 3. Permite transformar los nombres de los campos de Scryfall (ej. `mana_cost`)
 * a los nombres de la clase (ej. `manaCost`).
 */
class CardDeserializer : JsonDeserializer<Card> {

    /**
     * Método principal para deserializar un elemento JSON en un objeto [Card].
     * Gson llama a este método cuando encuentra un JSON que necesita ser convertido a un objeto [Card].
     *
     * @param json El elemento JSON bruto a deserializar.
     * @param typeOfT El tipo de objeto que se está deserializando (en este caso, [Card]).
     * @param context El contexto de deserialización de Gson, útil para delegar la deserialización de sub-objetos.
     * @return Una instancia de la clase [Card] populada con los datos del JSON.
     * @throws IllegalStateException Si el elemento JSON de entrada es nulo o no es un objeto JSON válido.
     */
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Card {
        // Asegura que el elemento JSON no es nulo y es un objeto JSON.
        // Si no lo es, lanza una excepción indicando un problema en el formato de datos.
        val jsonObject = json?.asJsonObject ?: throw IllegalStateException("JSON element is null or not an object")

        // --- Extracción y Mapeo de la URL de la Imagen (Lógica Personalizada) ---
        // Este es uno de los puntos clave del deserializador, ya que Scryfall proporciona varias URLs.
        var imageUrl: String? = null

        // Intenta obtener la URL de imagen de los 'image_uris' de la carta principal.
        val imageUrisObject = jsonObject.getAsJsonObject("image_uris")
        if (imageUrisObject != null) {
            // Se busca la URL 'normal' para las imágenes de cartas.
            imageUrl = imageUrisObject.get("normal")?.asString
        }

        // Si 'imageUrl' sigue siendo nulo (lo que ocurre en cartas de doble cara sin 'image_uris' directos),
        // intenta obtener la URL de imagen de la primera "cara" de la carta.
        if (imageUrl == null) {
            val cardFacesArray = jsonObject.getAsJsonArray("card_faces")
            // Se verifica que haya un array de 'card_faces' y que no esté vacío.
            if (cardFacesArray != null && cardFacesArray.size() > 0) {
                // Se obtiene el primer objeto de cara de la carta.
                val firstFace = cardFacesArray.get(0).asJsonObject
                // Dentro de la primera cara, se busca de nuevo los 'image_uris'.
                val faceImageUris = firstFace.getAsJsonObject("image_uris")
                // Y se extrae la URL 'normal' de esa cara.
                imageUrl = faceImageUris?.get("normal")?.asString
            }
        }


        // --- Mapeo de Otros Campos (Ajustando Nombres de Scryfall a la clase Card) ---
        // Aquí se extraen los valores de los campos JSON usando los nombres de Scryfall
        // y se asignan a las propiedades correspondientes de la clase Card.
        // Se usa el operador Elvis (?:) para proporcionar valores por defecto (ej. "" para String, null para otros)
        // si el campo no existe en el JSON o es nulo, evitando NullPointerExceptions.

        val id = jsonObject.get("id")?.asString ?: "" // ID único de la carta.
        val name = jsonObject.get("name")?.asString ?: "" // Nombre de la carta.
        val manaCost = jsonObject.get("mana_cost")?.asString // Coste de maná (Scryfall usa 'mana_cost').
        val cmc = jsonObject.get("cmc")?.asDouble // Coste de maná convertido.
        val colorsJsonArray = jsonObject.getAsJsonArray("colors") // Array de colores (ej. ["W", "U"]).
        val colors: List<String>? = colorsJsonArray?.map { it.asString } // Mapea cada elemento del array JSON a un String, resultando en List<String>?.

        val type = jsonObject.get("type_line")?.asString // Tipo de la carta (Scryfall usa 'type_line').
        val power = jsonObject.get("power")?.asString // Poder de la criatura.
        val toughness = jsonObject.get("toughness")?.asString // Resistencia de la criatura.
        val oracleText = jsonObject.get("oracle_text")?.asString  // Texto de la carta.
        val setCode = jsonObject.get("set")?.asString // Código de la colección (Scryfall usa 'set').
        val setName = jsonObject.get("set_name")?.asString // Nombre de la colección (Scryfall usa 'set_name').
        val rarity = jsonObject.get("rarity")?.asString // Rareza de la carta.

        // --- Creación de la Instancia de Card ---
        // Finalmente, se crea y devuelve una nueva instancia de la clase Card,
        // poblando sus propiedades con los valores extraídos y mapeados.
        return Card(
            id = id,
            name = name,
            imageUrl = imageUrl,
            manaCost = manaCost,
            cmc = cmc,
            colors = colors,
            type = type,
            power = power,
            toughness = toughness,
            oracleText = oracleText,
            setCode = setCode,
            setName = setName,
            rarity = rarity
        )
    }
}