package com.alejandro.magicdeckbuilder.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Clase de datos que representa una relación de amistad entre el usuario actual y otro usuario.
 * Esta clase se utiliza para almacenar y gestionar el estado de las solicitudes de amistad
 * y las relaciones aceptadas en Firebase Firestore.
 *
 * @param otherUserUid El User ID (UID) del otro usuario involucrado en esta relación de amistad.
 * La anotación `@DocumentId` indica a Firestore que este campo debe usarse como el ID del documento
 * en la colección de amistades del usuario actual. Es clave para identificar de forma única la relación.
 * @param status El estado actual de la relación de amistad. Es una cadena de texto que puede tomar
 * varios valores predefinidos para representar las diferentes fases de una solicitud de amistad:
 * - "pending_sent": La solicitud ha sido enviada por el usuario actual y está pendiente.
 * - "pending_received": La solicitud ha sido recibida por el usuario actual y está pendiente.
 * - "accepted": La solicitud ha sido aceptada y los dos usuarios son amigos.
 * - "blocked": La relación ha sido bloqueada por alguno de los usuarios.
 * @param requestedAt La marca de tiempo que registra cuándo se envió la solicitud de amistad.
 * Se inicializa automáticamente con la hora actual (`Timestamp.now()`) al crear el objeto.
 * @param acceptedAt La marca de tiempo que registra cuándo fue aceptada la solicitud de amistad.
 * Puede ser `null` si la solicitud aún está en estado "pending_sent" o "pending_received",
 * ya que todavía no ha sido aceptada.
 * @param username El nombre de usuario del `otherUserUid`. Este campo está **desnormalizado**;
 * es decir, se duplica información del perfil del otro usuario directamente en este documento de amistad.
 * Esto se hace para evitar una lectura adicional a la colección de usuarios cada vez que se muestran
 * las amistades, mejorando el rendimiento en la visualización.
 * @param email El correo electrónico del `otherUserUid`. Este campo también está **desnormalizado**
 * por la misma razón que el nombre de usuario, para facilitar el acceso rápido a la información
 * básica del otro usuario sin necesidad de consultas adicionales a Firestore.
 */
data class Friendship(
    @DocumentId
    val otherUserUid: String = "",
    val status: String = "",
    val requestedAt: Timestamp = Timestamp.now(),
    val acceptedAt: Timestamp? = null,
    val username: String = "",
    val email: String = ""
)
