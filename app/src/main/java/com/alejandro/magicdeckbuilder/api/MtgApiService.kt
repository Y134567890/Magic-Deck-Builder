package com.alejandro.magicdeckbuilder.api

import com.alejandro.magicdeckbuilder.data.Card
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Clase de datos que representa la estructura de la respuesta de búsqueda de cartas de la API de Scryfall.
 * Scryfall devuelve las listas de resultados dentro de un campo 'data',
 * y también incluye información de paginación como 'has_more' y 'next_page'.
 *
 * @param data Una lista de objetos [Card] que representan las cartas encontradas.
 * Es importante que la clase [Card] pueda ser deserializada correctamente
 * desde la estructura JSON de Scryfall (a través de un deserializador personalizado en este caso).
 * @param has_more Un booleano que indica si hay más resultados disponibles después de la página actual.
 * @param next_page La URL completa para la siguiente página de resultados, si 'has_more' es true.
 * Puede ser nulo si no hay más páginas.
 */
data class ScryfallCardSearchResponse(
    val data: List<Card>,
    val has_more: Boolean,
    val next_page: String?
)

/**
 * Interfaz de servicio de Retrofit para interactuar con la API de Scryfall.
 * Define los endpoints y los parámetros para las peticiones HTTP que la aplicación puede realizar.
 * Retrofit utiliza las anotaciones HTTP (como @GET, @Query) para construir las URLs y gestionar las llamadas.
 */
interface MtgApiService {

    /**
     * Realiza una petición GET a la API de Scryfall para buscar cartas.
     * El endpoint "cards/search" es utilizado para consultas complejas de cartas.
     *
     * @param query El parámetro principal de búsqueda en Scryfall (anotado con @Query("q")).
     * Permite consultas complejas, como "lightning bolt", "t:creature c:red", "power>5", etc.
     * @param page Un número de página opcional (anotado con @Query("page")).
     * Utilizado para la paginación de resultados. Si no se especifica, se asume la primera página.
     * @param unique Una cadena opcional (anotado con @Query("unique")).
     * Controla cómo Scryfall maneja las versiones únicas de cartas.
     * "prints" (por defecto): devuelve una entrada por cada impresión única de una carta.
     * "cards": devuelve una entrada por cada carta única (excluye reimpresiones exactas).
     * "art": devuelve una entrada por cada ilustración única.
     * @param order Una cadena opcional (anotado con @Query("order")).
     * Define cómo se ordenan los resultados.
     * "name" (por defecto): ordena alfabéticamente por nombre de carta.
     * Otros valores comunes incluyen "released", "cmc", "power", "toughness", "rarity", etc.
     * @return Un objeto [ScryfallCardSearchResponse] que contiene la lista de cartas encontradas
     * y la información de paginación.
     */
    @GET("cards/search")
    suspend fun searchCards(
        @Query("q") query: String,
        @Query("page") page: Int? = null,
        @Query("unique") unique: String = "prints",
        @Query("order") order: String = "name"
    ): ScryfallCardSearchResponse
}