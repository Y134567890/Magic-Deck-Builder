package com.alejandro.magicdeckbuilder.api

import com.alejandro.magicdeckbuilder.data.Card
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Objeto singleton que proporciona la configuración y la instancia del servicio Retrofit para la API de Scryfall.
 * Al ser un 'object', solo se crea una única instancia de MtgApi en toda la aplicación,
 * lo que es eficiente y seguro para la configuración de la red.
 */
object MtgApi {

    // Define la URL base de la API de Scryfall. Todas las peticiones se construirán a partir de esta URL.
    private const val BASE_URL = "https://api.scryfall.com/"

    /**
     * Instancia del servicio Retrofit (MtgApiService) para realizar las llamadas a la API.
     * 'by lazy' asegura que la inicialización de RetrofitService solo ocurra la primera vez que se accede a ella,
     * lo que optimiza el rendimiento al no crearla hasta que sea realmente necesaria.
     */
    val retrofitService: MtgApiService by lazy {

        // Paso 1: Configurar Gson con un deserializador personalizado.
        // Esto es necesario porque el proyecto se diseñó originariamente para la API magicthegathering.io
        // Al cambiarse a la API Scryfall (mucho más potente y actualizada) en una fase avanzada del proyecto,
        // se optó por convertir la respuesta de Scryfall al modelo de datos existente; evitando cambios masivos
        // en el código (y permitiendo volver a la API anterior con facilidad)

        val gson = GsonBuilder()
            // Registra el adaptador de tipo personalizado para la clase Card.
            // CardDeserializer contendrá la lógica para convertir el JSON de Scryfall
            // en un objeto Card del modelo de datos.
            .registerTypeAdapter(Card::class.java, CardDeserializer())
            .create() // Construye la instancia de Gson con la configuración aplicada.

        // Paso 2: Construir y configurar la instancia de Retrofit.
        Retrofit.Builder()
            // Establece la URL base para todas las peticiones de este servicio.
            .baseUrl(BASE_URL)
            // Añade un 'Converter Factory' que le dice a Retrofit cómo convertir
            // el JSON de las respuestas HTTP en objetos Kotlin y viceversa.
            // Aquí se usa GsonConverterFactory, que usa la instancia de Gson configurada previamente.
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build() // Construye la instancia de Retrofit.
            // Paso 3: Crear una implementación de la interfaz MtgApiService.
            // Retrofit genera automáticamente el código necesario para realizar las peticiones
            // definidas en MtgApiService basándose en las anotaciones (@GET, @POST, @Query, etc.).
            .create(MtgApiService::class.java)
    }
}