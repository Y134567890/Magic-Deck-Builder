package com.alejandro.magicdeckbuilder.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.alejandro.magicdeckbuilder.data.models.Deck
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Gestor para guardar, cargar y eliminar mazos y sus imágenes localmente en el dispositivo.
 * Utiliza los modelos [LocalDeck] y [LocalCardInDeckData] para almacenar los datos,
 * y Gson para la serialización/deserialización a JSON.
 *
 * @param context El contexto de la aplicación, necesario para acceder a los directorios de archivos privados.
 */
class LocalDeckManager(private val context: Context) {

    // Instancia de Gson para convertir objetos Kotlin a JSON y viceversa.
    private val gson = Gson()
    // Instancia de Coil ImageLoader para descargar imágenes de forma eficiente.
    private val imageLoader = ImageLoader(context)

    // Directorios privados de la aplicación donde se guardarán los datos y las imágenes.
    private val decksDir = File(context.filesDir, "decks")
    private val imagesDir = File(context.filesDir, "deck_images")

    // Instancia del gestor de contadores de referencia de imágenes.
    private val imageReferenceCounterManager = ImageReferenceCounterManager(imagesDir)

    init {
        // Bloque de inicialización: Asegura que los directorios necesarios existan.
        if (!decksDir.exists()) decksDir.mkdirs()
        if (!imagesDir.exists()) imagesDir.mkdirs()
    }

    /**
     * Descarga un mazo completo (datos JSON e imágenes de cartas) y lo guarda para uso offline.
     * Esta función es 'suspend' porque realiza operaciones de E/S bloqueantes (red y disco)
     * y se ejecuta en un [Dispatchers.IO] para no bloquear el hilo principal (UI).
     *
     * @param deck El objeto [Deck] (proveniente de Firestore) a descargar y guardar localmente.
     * @return Un [Result.success(Unit)] si la operación fue exitosa, o [Result.failure(Exception)] si hubo un error.
     */
    suspend fun downloadAndSaveDeck(deck: Deck): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Paso previo: Si el mazo ya existe localmente, cargamos el mazo antiguo para
            // decrementar los contadores de sus imágenes antes de sobrescribirlo.
            // Esto es crucial para liberar referencias a imágenes que ya no serán usadas por este mazo.
            val existingLocalDeck = loadLocalDeckById(deck.id)
            existingLocalDeck?.cards?.values?.forEach { oldCardData ->
                oldCardData.imagePath?.let { path ->
                    // Solo intentamos decrementar si el archivo de imagen aún existe físicamente
                    // y si el path no es nulo/vacío.
                    if (File(path).exists()) {
                        val shouldDelete = imageReferenceCounterManager.decrementReference(path)
                        if (shouldDelete) {
                            // Si el contador llega a 0, borramos la imagen física.
                            File(path).delete()
                        }
                    }
                }
            }

            val localCards = mutableMapOf<String, LocalCardInDeckData>()

            // 1. Iterar sobre cada carta en el mazo remoto para descargar sus imágenes y crear datos locales.
            for ((cardId, cardData) in deck.cards) {
                val imagePath = if (cardData.imageUrl != null) {
                    // Si la carta tiene una URL de imagen, intentar descargarla y gestionar su referencia.
                    downloadImageAndIncrementReference(cardData.imageUrl, cardId)
                } else {
                    // Si no hay URL, no hay imagen local.
                    null
                }

                // 2. Crear el objeto [LocalCardInDeckData] con la ruta local de la imagen.
                localCards[cardId] = LocalCardInDeckData(
                    id = cardId,
                    quantity = cardData.quantity,
                    imagePath = imagePath,
                    name = cardData.name,
                    manaCost = cardData.manaCost,
                    cmc = cardData.cmc,
                    colors = cardData.colors,
                    type = cardData.type
                )
            }

            // 3. Crear el objeto [LocalDeck] con las cartas ya transformadas a su versión local.
            val localDeck = LocalDeck(
                id = deck.id,
                userId = deck.userId,
                name = deck.name,
                description = deck.description,
                format = deck.format,
                cards = localCards
            )

            // 4. Guardar el objeto [LocalDeck] como un archivo JSON en el directorio 'decksDir'.
            val deckFile = File(decksDir, "${deck.id}.json")
            deckFile.writeText(gson.toJson(localDeck))

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Descarga una imagen desde una URL y la guarda en el almacenamiento local de la app.
     * También gestiona el contador de referencias de la imagen.
     *
     * @param url La URL de la imagen a descargar.
     * @param cardId El ID de la carta, usado para nombrar el archivo de imagen local.
     * @return La ruta absoluta al archivo de imagen guardado si la operación fue exitosa, o `null` si falló.
     */
    private suspend fun downloadImageAndIncrementReference(url: String, cardId: String): String? {
        val imageFile = File(imagesDir, "$cardId.jpg")

        // Solo descarga si el archivo no existe.
        if (!imageFile.exists()) {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as BitmapDrawable).bitmap
                try {
                    FileOutputStream(imageFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    return null // Retorna null si hay un error al guardar el archivo.
                }
            } else {
                return null // Retorna null si la descarga falló.
            }
        }

        // Si la imagen ya existía o se descargó correctamente, incrementa su contador de referencia.
        imageFile.absolutePath.let {
            imageReferenceCounterManager.incrementReference(it) // <-- NUEVO
        }
        return imageFile.absolutePath
    }

    /**
     * Carga un mazo local específico por su ID.
     * Método auxiliar usado internamente para la lógica de sobrescritura.
     *
     * @param deckId El ID del mazo a cargar.
     * @return El [LocalDeck] si se encuentra, o `null` si no existe o hay un error.
     */
    private fun loadLocalDeckById(deckId: String): LocalDeck? {
        val deckFile = File(decksDir, "${deckId}.json")
        return if (deckFile.exists()) {
            try {
                val json = deckFile.readText()
                gson.fromJson(json, LocalDeck::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    /**
     * Carga todos los mazos guardados localmente desde los archivos JSON en el directorio 'decksDir'.
     *
     * @return Una lista de objetos [LocalDeck] cargados, ordenados alfabéticamente por nombre,
     * o una lista vacía si no hay mazos o si ocurre un error al leerlos.
     */
    suspend fun loadLocalDecks(): List<LocalDeck> = withContext(Dispatchers.IO) {
        // Lista los archivos en el directorio 'decksDir' que terminan en ".json".
        decksDir.listFiles { _, name -> name.endsWith(".json") }
            // Mapea cada archivo JSON a un objeto LocalDeck.
            ?.mapNotNull { file ->
                try {
                    val json = file.readText()
                    gson.fromJson(json, LocalDeck::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }?.sortedBy { it.name }
            ?: emptyList()
    }

    /**
     * Elimina un mazo guardado localmente y gestiona la eliminación de sus imágenes asociadas
     * utilizando el sistema de contador de referencias. Una imagen solo se borra físicamente
     * si ningún otro mazo local la referencia.
     *
     * @param deckId El ID del mazo a eliminar.
     */
    suspend fun deleteLocalDeck(deckId: String) = withContext(Dispatchers.IO) {
        val deckFile = File(decksDir, "$deckId.json")

        // Si el archivo JSON del mazo no existe, no hay nada que borrar.
        if (!deckFile.exists()) return@withContext

        try {
            // Intenta leer el JSON del mazo para obtener las rutas de las imágenes.
            val json = deckFile.readText()
            val deck = gson.fromJson(json, LocalDeck::class.java)

            // Itera sobre las cartas del mazo y decrementa el contador de referencias de cada imagen.
            deck.cards.values.forEach { card ->
                card.imagePath?.let { path ->
                    // Solo procesamos el decremento si el archivo de imagen aún existe físicamente.
                    if (File(path).exists()) {
                        val shouldDelete = imageReferenceCounterManager.decrementReference(path)
                        if (shouldDelete) {
                            // Si decrementReference indica que el contador llegó a 0, borra la imagen física.
                            File(path).delete()
                        }
                    }
                }
            }
            // Finalmente, borra el archivo JSON del mazo.
            deckFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            // Si ocurre un error durante la lectura o el procesamiento,
            // imprime el error y al menos intenta borrar el archivo JSON del mazo
            // para limpiar parcialmente.
            deckFile.delete()
        }
    }
}