package com.alejandro.magicdeckbuilder.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.lang.reflect.Type

/**
 * Modelo de datos para almacenar el contador de referencias de una imagen.
 */
data class ImageReference(
    val imagePath: String, // La ruta absoluta al archivo de imagen.
    var referenceCount: Int // Cuántos mazos locales usan esta imagen.
)

/**
 * Gestor para persistir y acceder a los contadores de referencia de las imágenes locales.
 * Guarda los datos en un archivo JSON.
 */
class ImageReferenceCounterManager(private val imagesDir: File) {

    private val gson = Gson()
    private val counterFile = File(imagesDir, "image_references.json")
    private val imageReferences: MutableMap<String, ImageReference> = mutableMapOf()

    init {
        // Cargar los contadores existentes al inicializar.
        loadReferences()
    }

    /**
     * Carga las referencias de imágenes desde el archivo JSON.
     */
    private fun loadReferences() {
        if (counterFile.exists()) {
            try {
                val json = counterFile.readText()
                val type: Type = object : TypeToken<List<ImageReference>>() {}.type
                val loadedList: List<ImageReference> = gson.fromJson(json, type)
                loadedList.forEach { ref ->
                    imageReferences[ref.imagePath] = ref
                }
            } catch (e: Exception) {
                e.printStackTrace()
                imageReferences.clear()
            }
        }
    }

    /**
     * Guarda las referencias de imágenes actuales al archivo JSON.
     */
    private fun saveReferences() {
        try {
            val json = gson.toJson(imageReferences.values.toList())
            counterFile.writeText(json)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Incrementa el contador de referencia para una imagen.
     * Si la imagen no está registrada, la añade con un contador de 1.
     */
    fun incrementReference(imagePath: String) {
        val ref = imageReferences.getOrPut(imagePath) { ImageReference(imagePath, 0) }
        ref.referenceCount++
        saveReferences()
    }

    /**
     * Decrementa el contador de referencia para una imagen.
     * Si el contador llega a 0, devuelve true indicando que la imagen debería ser borrada.
     * Si la imagen no está registrada, se ignora.
     */
    fun decrementReference(imagePath: String): Boolean {
        val ref = imageReferences[imagePath]
        return if (ref != null) {
            ref.referenceCount--
            if (ref.referenceCount <= 0) {
                imageReferences.remove(imagePath) // Elimina la entrada si ya no se usa.
                saveReferences()
                true // Indica que la imagen puede ser borrada.
            } else {
                saveReferences()
                false // La imagen aún está en uso.
            }
        } else {
            false
        }
    }

    /**
     * Limpia todas las referencias y el archivo. Usar con precaución.
     * Esto NO borra las imágenes físicas, solo los contadores.
     * Solo para fines de desarrollo o de restablecimiento.
     */
    fun clearAllReferences() {
        imageReferences.clear()
        counterFile.delete()
    }
}