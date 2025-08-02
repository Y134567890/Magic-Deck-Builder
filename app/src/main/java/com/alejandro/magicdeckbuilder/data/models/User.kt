package com.alejandro.magicdeckbuilder.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId // Para que Firestore sepa que este campo es el ID del documento

/**
 * Clase de datos que representa el perfil de un usuario en la aplicación.
 * Esta clase se utiliza para almacenar información básica del usuario en Firebase Firestore.
 * Cada documento de esta colección tendrá el UID de Firebase Authentication del usuario
 * como su propio ID de documento.
 *
 * @param uid El User ID (UID) único proporcionado por Firebase Authentication para este usuario.
 * La anotación `@DocumentId` es crucial aquí, ya que le indica a Firestore que este campo
 * se debe usar como el identificador del documento en la colección de usuarios.
 * @param username El nombre de usuario que el usuario ha elegido. Este es el nombre visible
 * que otros usuarios verán, por ejemplo, al buscar amigos.
 * @param email El correo electrónico del usuario. Es opcional en el sentido de que no siempre
 * es necesario mostrarlo o usarlo en todas las funciones, pero es útil para la administración
 * o para que los usuarios se identifiquen.
 * @param createdAt La marca de tiempo que registra cuándo se creó la cuenta de este usuario.
 * Se inicializa automáticamente con la fecha y hora actuales (`Timestamp.now()`) al crear el objeto.
 * Este campo es útil para, por ejemplo, ordenar usuarios por fecha de registro.
 */
data class User(
    @DocumentId
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val createdAt: Timestamp = Timestamp.now()
)










