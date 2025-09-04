package com.alejandro.magicdeckbuilder.presentation.navigation

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.os.Parcelable
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.alejandro.magicdeckbuilder.data.Card
import com.alejandro.magicdeckbuilder.presentation.accountmanagement.AccountManagementScreen
import com.alejandro.magicdeckbuilder.presentation.accountmanagement.AccountManagementViewModel
import com.alejandro.magicdeckbuilder.presentation.cardsearch.CardSearchScreen
import com.alejandro.magicdeckbuilder.presentation.deckedit.DeckEditScreen
import com.alejandro.magicdeckbuilder.presentation.deckedit.DeckEditViewModel
import com.alejandro.magicdeckbuilder.presentation.deckedit.LocalDeckViewScreen
import com.alejandro.magicdeckbuilder.presentation.deckedit.LocalDeckViewViewModel
import com.alejandro.magicdeckbuilder.presentation.deckedit.LocalDeckViewViewModelFactory
import com.alejandro.magicdeckbuilder.presentation.dekcs.DecksScreen
import com.alejandro.magicdeckbuilder.presentation.dekcs.DecksViewModel
import com.alejandro.magicdeckbuilder.presentation.dekcs.DecksViewModelFactory
import com.alejandro.magicdeckbuilder.presentation.dekcs.LocalDecksScreen
import com.alejandro.magicdeckbuilder.presentation.friendship.FriendshipScreen
import com.alejandro.magicdeckbuilder.presentation.friendship.FriendshipViewModel
import com.alejandro.magicdeckbuilder.presentation.home.HomeScreen
import com.alejandro.magicdeckbuilder.presentation.login.LoginScreen
import com.alejandro.magicdeckbuilder.presentation.signup.SignUpScreen
import com.alejandro.magicdeckbuilder.presentation.signup.VerificationPendingScreen
import com.alejandro.magicdeckbuilder.presentation.splash.SplashScreen
import com.alejandro.magicdeckbuilder.presentation.start.StartScreen
import com.alejandro.magicdeckbuilder.presentation.user.UserViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.parcel.Parcelize

/**
 * Clase de datos para encapsular una carta y su cantidad, utilizada para pasar resultados
 * de la búsqueda de cartas a la pantalla de edición de mazos.
 * Es Parcelable para poder ser almacenada en SavedStateHandle de NavController.
 */
@Parcelize
data class CardResult(
    val card: Card,
    val quantity: Int
) : Parcelable

/**
 * Clase sellada que define todas las rutas de navegación posibles en la aplicación.
 * Cada objeto representa una pantalla y su ruta asociada.
 * Algunas rutas incluyen argumentos que se pasan en la navegación.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash") // Pantalla de carga inicial.
    object Start : Screen("start") // Pantalla de inicio.
    object Login : Screen("logIn") // Pantalla de inicio de sesión.
    object SignUp : Screen("signUp") // Pantalla de registro.
    object Home : Screen("home") // Pantalla principal de la aplicación.
    object VerificationPending : Screen("verificationPending")  // Pantalla de verificación de email.
    object CardSearch : Screen("cardSearch") // Pantalla de búsqueda de cartas.
    object Decks : Screen("decks") // Pantalla de mazos de usuario (en la nube).
    // Ruta para mazos locales, con un argumento opcional `hideSection` (Modo Offline).
    object LocalDecks : Screen("local_decks_route?hideSection={hideSection}") {
        fun createRoute(hideSection: Boolean = false): String {
            return "local_decks_route?hideSection=$hideSection"
        }
    }
    // Ruta para ver un mazo local, con `deckId` y argumento opcional `hideSection` (Modo Offline).
    object LocalDeckView : Screen("local_deck_view/{deckId}?hideSection={hideSection}") {
        fun createRoute(deckId: String, hideSection: Boolean = false): String {
            return "local_deck_view/$deckId?hideSection=$hideSection"
        }
    }
    object Friendship : Screen("friendship") // Pantalla de gestión de amistades.
    // Ruta para ver los mazos de un amigo, con `friendUid` y `friendUsername`.
    object FriendDecks : Screen("friend_decks/{friendUid}/{friendUsername}") {
        fun createRoute(friendUid: String, friendUsername: String): String {
            return "friend_decks/$friendUid/$friendUsername"
        }
    }
    // Ruta para editar un mazo, con `deckId`.
    object DeckEdit : Screen("deckEdit/{deckId}") {
        fun createRoute(deckId: String): String {
            return "deckEdit/$deckId"
        }
    }
    // Ruta para ver un mazo (en modo lectura), con `deckId`, `ownerUid` y `ownerUsername`.
    object DeckView : Screen("deckView/{deckId}/{ownerUid}/{ownerUsername}") {
        fun createRoute(deckId: String, ownerUid: String, ownerUsername: String): String {
            return "deckView/$deckId/$ownerUid/$ownerUsername"
        }
    }
    // Ruta para la pantalla de manejo de cuenta
    object AccountManagement : Screen("accountManagement")
}

/**
 * Composable que envuelve y configura el gráfico de navegación de la aplicación.
 * Define las pantallas, sus rutas y cómo se manejan las transiciones y los argumentos.
 *
 * @param navHostController Controlador de navegación de Compose.
 * @param auth Instancia de [FirebaseAuth] para gestionar el estado de autenticación.
 * @param googleSignInClient Cliente para el inicio de sesión con Google.
 * @param launcher Lanzador de resultados de actividad para Google Sign-In.
 * @param userViewModel ViewModel para gestionar la información del usuario.
 * @param firestore Instancia de [FirebaseFirestore] para operaciones de base de datos.
 */
@SuppressLint("RestrictedApi") // Suprime la advertencia sobre el uso de APIs restringidas, si las hay.
@Composable
fun NavigationWrapper(
    navHostController: NavHostController,
    auth: FirebaseAuth,
    googleSignInClient: GoogleSignInClient,
    launcher: ActivityResultLauncher<Intent>,
    userViewModel: UserViewModel,
    firestore: FirebaseFirestore,
) {

    // Inicializa el FriendshipViewModel y lo recuerda para que no se recree en cada recomposición.
    val friendshipViewModel: FriendshipViewModel = remember {
        FriendshipViewModel(auth, firestore)
    }
    val userUiState by userViewModel.uiState.collectAsState() // Recolecta el estado del usuario.
    val currentUsername = userUiState.currentUser?.username // Obtiene el nombre de usuario actual.

    // Obtiene el contexto de la aplicación, necesario para las factorías de ViewModel.
    val application = LocalContext.current.applicationContext as Application

    // Define el gráfico de navegación utilizando NavHost.
    NavHost(navHostController, startDestination = Screen.Splash.route) {

        // Pantalla de Splash
        composable(Screen.Splash.route) { SplashScreen(navHostController) }

        // Pantalla de Inicio (StartScreen)
        composable(Screen.Start.route) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                if (currentUser.isEmailVerified) {
                    // Si está logueado y verificado, va a Home y limpia la pila de navegación.
                    navHostController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                    }
                    Log.i("Alejandro", "Estoy logado y verificado")
                } else {
                    // Si está logueado pero no verificado, va a VerificationPending.
                    navHostController.navigate(Screen.VerificationPending.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                    }
                    Log.i("Alejandro", "Estoy logado pero no verificado")
                }
            } else {
                // Si no está logueado, muestra la StartScreen con opciones de login/signup.
                StartScreen(
                    auth = auth,
                    navigateToLogin = { navHostController.navigate(Screen.Login.route) },
                    navigateToSignUp = { navHostController.navigate(Screen.SignUp.route) },
                    navigateToHome = {
                        navHostController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Start.route) { inclusive = true }
                        }
                    },
                    onGoogleSignInClick = {
                        val signInIntent = googleSignInClient.signInIntent
                        launcher.launch(signInIntent) // Lanza el flujo de Google Sign-In.
                    },
                    navigateToVerificationPending = {
                        navHostController.navigate(Screen.VerificationPending.route) {
                            popUpTo(Screen.Start.route) { inclusive = true }
                        }
                    },
                    navigateToLocal = {
                        // Navega a mazos locales en modo "ocultar sección" (para usuarios no logueados).
                        navHostController.navigate(Screen.LocalDecks.createRoute(hideSection = true)) { }
                    }
                )
            }
        }

        // Pantalla de Login
        composable(Screen.Login.route) {
            LoginScreen(
                auth,
                viewModel = viewModel(), // Proporciona una instancia de LoginViewModel.
                navigateBack = { navHostController.popBackStack() }, // Pop de la pila de navegación.
                navigateToHome = {
                    navHostController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Start.route) { inclusive = true } // Vuelve a la pila de inicio, limpiando.
                    }
                },
                navigateToVerificationPending = {
                    navHostController.navigate(Screen.VerificationPending.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de Registro (SignUp)
        composable(Screen.SignUp.route) {
            SignUpScreen(
                auth,
                viewModel = viewModel(), // Proporciona una instancia de SignUpViewModel.
                navigateBack = { navHostController.popBackStack() },
                navigateToVerificationPending = {
                    navHostController.navigate(Screen.VerificationPending.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla principal (Home)
        composable(Screen.Home.route) {
            HomeScreen(
                auth = auth,
                onSignOut = {
                    // Cierra sesión en Firebase y Google, y navega a la pantalla de inicio.
                    auth.signOut()
                    googleSignInClient.signOut()
                    navHostController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Home.route) { inclusive = true } // Limpia la pila hasta Home.
                    }
                },
                navigateToCardSearch = { navHostController.navigate("${Screen.CardSearch.route}?isAddingCards=false") }, // Navega a CardSearch.
                navigateToDecks = { navHostController.navigate(Screen.Decks.route) },  // Navega a Decks (online).
                navigateToLocalDecks = { navHostController.navigate(Screen.LocalDecks.route) }, // Navega a LocalDecks.
                userViewModel = userViewModel,
                onNavigateToFriends = { navHostController.navigate(Screen.Friendship.route) }, // Navega a Friends.
                onNavigateToAccountManagement = { navHostController.navigate(Screen.AccountManagement.route) } // Navega a la pantalla de gestión de cuenta
            )
        }

        // Pantalla para la pantalla de gestión de cuenta
        composable(Screen.AccountManagement.route) {
            val viewModel: AccountManagementViewModel = viewModel()
            AccountManagementScreen(
                auth = auth,
                onNavigateBack = { navHostController.popBackStack() },
                onAccountDeleted = {
                    auth.signOut()
                    navHostController.navigate(Screen.Start.route) {
                        popUpTo(navHostController.graph.id) { inclusive = true }
                    }
                },
                googleSignInClient = googleSignInClient,
                viewModel = viewModel
            )
        }

        // Pantalla de Verificación Pendiente
        composable(Screen.VerificationPending.route) {
            VerificationPendingScreen(
                auth = auth,
                viewModel = viewModel(), // Proporciona una instancia de SignUpViewModel.
                navigateToLogin = {
                    navHostController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                    }
                },
                navigateToHome = {
                    navHostController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Pantalla de Búsqueda de Cartas
        // Soporta un argumento opcional `isAddingCards` para cambiar el comportamiento. En caso de que sea true
        // el usuario habrá llegado a la pantalla desde la de edición de mazo, por lo que se deberá permitir
        // añadir cartas al mismo. En caso contrario, solo permitirá buscar cartas.
        composable(
            route = "${Screen.CardSearch.route}?isAddingCards={isAddingCards}",
            arguments = listOf(navArgument("isAddingCards") {
                type = NavType.BoolType
                defaultValue = false
            })
        ) { backStackEntry ->
            val isAddingCards = backStackEntry.arguments?.getBoolean("isAddingCards") ?: false
            CardSearchScreen(
                viewModel = viewModel(),
                onNavigateBack = { navHostController.popBackStack() },
                onSignOut = {
                    auth.signOut()
                    googleSignInClient.signOut()
                    navHostController.navigate(Screen.Start.route) {
                        // popUpTo para limpiar la pila hasta la ruta de CardSearch.
                        popUpTo("${Screen.CardSearch.route}?isAddingCards={isAddingCards}") { inclusive = true }
                    }
                },
                userViewModel = userViewModel,
                onAddCardToDeck = { card, quantity ->
                    // Cuando se añade una carta, se guarda el resultado en el SavedStateHandle
                    // de la entrada anterior (la de DeckEditScreen) y se vuelve atrás.
                    val previousBackStackEntry = navHostController.previousBackStackEntry
                    previousBackStackEntry?.savedStateHandle?.set("cardResult", CardResult(card, quantity))
                    navHostController.popBackStack()
                },
                isAddingCards = isAddingCards,
                onNavigateToFriends = { navHostController.navigate(Screen.Friendship.route) },
                onNavigateToAccountManagement = { navHostController.navigate(Screen.AccountManagement.route) }
            )
        }

        // Pantalla de Mazos (en la nube)
        composable(Screen.Decks.route) {
            // Inicializa DecksViewModel usando una factoría para inyectar dependencias.
            val decksViewModel: DecksViewModel = viewModel(
                factory = DecksViewModelFactory(
                    application = application,
                    auth = auth,
                    firestore = firestore,
                    ownerUid = null
                )
            )
            DecksScreen(
                decksViewModel = decksViewModel,
                userViewModel = userViewModel,
                onNavigateBack = { navHostController.popBackStack() },
                // Navega a DeckEdit con el ID del mazo.
                onNavigateToDeckEdit = { deckId ->
                    if (deckId != null) {
                        navHostController.navigate(Screen.DeckEdit.createRoute(deckId))
                    } else {
                        Log.e("NavigationWrapper", "DeckEditScreen recibió un deckId nulo. Esto no debería ocurrir con el nuevo flujo de creación.")
                    }
                },
                onSignOut = {
                    auth.signOut()
                    googleSignInClient.signOut()
                    navHostController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Decks.route) { inclusive = true }
                    }
                },
                onNavigateToFriends = { navHostController.navigate(Screen.Friendship.route) },
                onNavigateToAccountManagement = { navHostController.navigate(Screen.AccountManagement.route) }
            )
        }

        // Pantalla de Mazos Locales. Con un argumento `hideSection` para adaptar la UI.
        composable(
            route = Screen.LocalDecks.route,
            arguments = listOf(navArgument("hideSection") {
                type = NavType.BoolType // Indica que es un booleano
                defaultValue = false    // Valor por defecto si no se proporciona
            })
        ) { backStackEntry ->
            val hideSection = backStackEntry.arguments?.getBoolean("hideSection") ?: false

            LocalDecksScreen(
                userViewModel = userViewModel,
                onNavigateBack = { navHostController.popBackStack() },
                onNavigateToDeckView = { deckId ->
                    // Navega a la vista de un mazo local específico, pasando la bandera `hideSection`.
                    navHostController.navigate(Screen.LocalDeckView.createRoute(deckId, hideSection = hideSection))
                },
                onSignOut = {
                    auth.signOut()
                    googleSignInClient.signOut()
                    navHostController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Decks.route) { inclusive = true }
                    }
                },
                onNavigateToFriends = { navHostController.navigate(Screen.Friendship.route) },
                hideSection = hideSection,
                onNavigateToAccountManagement = { navHostController.navigate(Screen.AccountManagement.route) }
            )
        }

        // Pantalla de Vista de Mazo Local
        composable(
            route = Screen.LocalDeckView.route,
            arguments = listOf(
                navArgument("deckId") { type = NavType.StringType },
                navArgument("hideSection") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId")
            val hideSection = backStackEntry.arguments?.getBoolean("hideSection") ?: false

            if (deckId != null) {
                // Inicializa LocalDeckViewViewModel usando una factoría.
                val localDeckViewViewModel: LocalDeckViewViewModel = viewModel(
                    factory = LocalDeckViewViewModelFactory(application)
                )
                LocalDeckViewScreen(
                    localDeckViewViewModel = localDeckViewViewModel,
                    userViewModel = userViewModel,
                    onNavigateBack = { navHostController.popBackStack() },
                    navController = navHostController,
                    onSignOut = {
                        auth.signOut()
                        googleSignInClient.signOut()
                        navHostController.navigate(Screen.Start.route) {
                            popUpTo(Screen.LocalDeckView.route) { inclusive = true }
                        }
                    },
                    onNavigateToFriends = { navHostController.navigate(Screen.Friendship.route) },
                    deckId = deckId,
                    hideSection = hideSection,
                    onNavigateToAccountManagement = { navHostController.navigate(Screen.AccountManagement.route) }
                )
            } else {
                Log.e("NavigationWrapper", "DeckId nulo para LocalDeckView.")
                Text("Error: No se pudo cargar el mazo local.", color = MaterialTheme.colorScheme.error)
            }
        }

        // Pantalla de Edición de Mazos
        composable(
            route = Screen.DeckEdit.createRoute(deckId = "{deckId}"),
            arguments = listOf(navArgument("deckId") { nullable = false })
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId")

            // Inicializa DeckEditViewModel. Se usa una factoría anónima para inyectar dependencias.
            val deckEditViewModel: DeckEditViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        if (modelClass.isAssignableFrom(DeckEditViewModel::class.java)) {
                            @Suppress("UNCHECKED_CAST")
                            return DeckEditViewModel(auth = auth, firestore = firestore) as T
                        }
                        throw IllegalArgumentException("Clase ViewModel desconocida")
                    }
                }
            )

            // Observa el resultado de la búsqueda de cartas que se pasa de vuelta desde CardSearchScreen.
            val cardResult by backStackEntry.savedStateHandle.getStateFlow<CardResult?>("cardResult", null)
                .collectAsStateWithLifecycle()

            // Efecto para inicializar el ViewModel cuando el deckId cambia.
            LaunchedEffect(key1 = deckId) {
                if (deckId != null) {
                    deckEditViewModel.ensureInitialized(deckId)  // Asegura que el ViewModel se inicialice con el deckId.
                } else {
                    Log.e("NavigationWrapper", "DeckEditScreen recibió un deckId nulo. Esto no debería ocurrir con el nuevo flujo de creación.")
                    navHostController.popBackStack()
                }
            }

            // Efecto para manejar el resultado de la búsqueda de cartas.
            LaunchedEffect(cardResult) {
                cardResult?.let { result ->
                    Log.d("DeckEditScreen", "Resultado de carta recibido: ${result.card.name}, Cantidad: ${result.quantity}")
                    deckEditViewModel.addCardToDeck(result.card, result.quantity) // Añade la carta al mazo.
                    backStackEntry.savedStateHandle.remove<CardResult>("cardResult") // Limpia el resultado para evitar re-procesamiento.
                }
            }

            DeckEditScreen(
                deckEditViewModel = deckEditViewModel,
                userViewModel = userViewModel,
                onNavigateBack = { navHostController.popBackStack() },
                onDeckSaved = { navHostController.popBackStack() }, // Al guardar el mazo, vuelve atrás.
                onNavigateToCardSearch = { currentDeckId -> // Navega a CardSearch con el ID del mazo actual.
                    if (currentDeckId != null) {
                        navHostController.navigate("${Screen.CardSearch.route}?isAddingCards=true&deckId=$currentDeckId")
                    } else {
                        Log.e("NavigationWrapper", "No se puede navegar a CardSearch, currentDeckId es nulo.")
                    }
                },
                navController = navHostController,
                onSignOut = {
                    auth.signOut()
                    googleSignInClient.signOut()
                    navHostController.navigate(Screen.Start.route) {
                        popUpTo(Screen.DeckEdit.route) { inclusive = true } // Pop up to DeckEdit route
                    }
                },
                onNavigateToFriends = { navHostController.navigate(Screen.Friendship.route) },
                isViewMode = false, // Modo edición
                ownerUsername = null, // No se necesita el nombre de usuario del propietario en modo edición.
                onNavigateToAccountManagement = { navHostController.navigate(Screen.AccountManagement.route) }
            )
        }

        // Pantalla de Amistades
        composable(Screen.Friendship.route) {
            FriendshipScreen(
                friendshipViewModel = friendshipViewModel, // Pasa la instancia de FriendshipViewModel.
                username = userViewModel.uiState.collectAsState().value.currentUser?.username,
                onNavigateBack = { navHostController.popBackStack() },
                onSignOut = {
                    auth.signOut()
                    googleSignInClient.signOut()
                    navHostController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Friendship.route) { inclusive = true }
                    }
                },
                onNavigateToFriends = {  }, // Ya estamos en Friends, no es necesario navegar a esta pantalla
                onViewFriendsDecks = { friendUid, friendUsername ->
                    // Navega a la pantalla de mazos de un amigo.
                    navHostController.navigate(Screen.FriendDecks.createRoute(friendUid, friendUsername))
                },
                onNavigateToAccountManagement = { navHostController.navigate(Screen.AccountManagement.route) }
            )
        }

        // Pantalla de Mazos de Amigos
        composable(
            route = Screen.FriendDecks.route,
            arguments = listOf(
                navArgument("friendUid") { type = NavType.StringType },
                navArgument("friendUsername") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val friendUid = backStackEntry.arguments?.getString("friendUid")
            val friendUsername = backStackEntry.arguments?.getString("friendUsername")

            if (friendUid != null && friendUsername != null) {
                // Inicializa un DecksViewModel específico para el amigo.
                val decksViewModelForFriend: DecksViewModel = viewModel(
                    key = "DecksViewModel_Friend_$friendUid", // Key única para este ViewModel.
                    factory = DecksViewModelFactory(
                        application = application,
                        auth = auth,
                        firestore = firestore,
                        ownerUid = friendUid // Pasa el UID del amigo como propietario.
                    )
                )

                DecksScreen(
                    decksViewModel = decksViewModelForFriend,
                    userViewModel = userViewModel,
                    onNavigateBack = { navHostController.popBackStack() },
                    onNavigateToDeckEdit = { deckId ->
                        if (deckId != null) {
                            // Navega a la vista de un mazo específico de un amigo (solo lectura).
                            navHostController.navigate(
                                Screen.DeckView.createRoute(deckId = deckId, ownerUid = friendUid, ownerUsername = friendUsername)
                            )
                        }
                    },
                    onSignOut = {
                        auth.signOut()
                        googleSignInClient.signOut()
                        navHostController.navigate(Screen.Start.route) {
                            popUpTo(Screen.FriendDecks.route) { inclusive = true }
                        }
                    },
                    onNavigateToFriends = { navHostController.navigate(Screen.Friendship.route) },
                    isFriendDecksView = true, // Indica que se están viendo mazos de un amigo.
                    friendUsername = friendUsername, // Pasa el nombre de usuario del amigo para mostrarlo en la UI.
                    onNavigateToAccountManagement = { navHostController.navigate(Screen.AccountManagement.route) }
                )
            } else {
                Log.e("NavigationWrapper", "UID o nombre de usuario del amigo nulos para la ruta FriendDecks.")
                Text("Error: No se pudieron cargar los mazos del amigo.", color = MaterialTheme.colorScheme.error)
            }
        }

        // Pantalla de Vista de Mazo
        composable(
            route = Screen.DeckView.route,
            arguments = listOf(
                navArgument("deckId") { type = NavType.StringType },
                navArgument("ownerUid") { type = NavType.StringType },
                navArgument("ownerUsername") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString("deckId")
            val ownerUid = backStackEntry.arguments?.getString("ownerUid")
            val ownerUsername = backStackEntry.arguments?.getString("ownerUsername")

            if (deckId != null && ownerUid != null && ownerUsername != null) {
                // Inicializa un DeckEditViewModel (pero lo usaremos en modo vista) para el mazo específico.
                val deckEditViewModelForView: DeckEditViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            if (modelClass.isAssignableFrom(DeckEditViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return DeckEditViewModel(auth = auth, firestore = firestore, ownerUid = ownerUid) as T
                            }
                            throw IllegalArgumentException("Clase ViewModel desconocida para DeckView")
                        }
                    }
                )

                val cardResult by backStackEntry.savedStateHandle.getStateFlow<CardResult?>("cardResult", null)
                    .collectAsStateWithLifecycle()

                LaunchedEffect(key1 = deckId) {
                    deckEditViewModelForView.ensureInitialized(deckId)
                }

                LaunchedEffect(cardResult) {
                    cardResult?.let { result ->
                        Log.d("DeckEditScreen", "Resultado de carta recibido (modo vista): ${result.card.name}, Cantidad: ${result.quantity}")
                        backStackEntry.savedStateHandle.remove<CardResult>("cardResult")
                    }
                }

                DeckEditScreen(
                    deckEditViewModel = deckEditViewModelForView,
                    userViewModel = userViewModel,
                    onNavigateBack = { navHostController.popBackStack() },
                    onDeckSaved = {  }, // No hay función de guardar en modo vista
                    onNavigateToCardSearch = {  }, // No hay búsqueda de cartas en modo vista
                    navController = navHostController,
                    onSignOut = {
                        auth.signOut()
                        googleSignInClient.signOut()
                        navHostController.navigate(Screen.Start.route) {
                            popUpTo(Screen.DeckView.route) { inclusive = true }
                        }
                    },
                    onNavigateToFriends = { navHostController.navigate(Screen.Friendship.route) },
                    isViewMode = true, // Establece la bandera para indicar que es modo visualización.
                    ownerUsername = ownerUsername, // Pasa el username del propietario para el título.
                    onNavigateToAccountManagement = { navHostController.navigate(Screen.AccountManagement.route) }
                )
            } else {
                Log.e("NavigationWrapper", "DeckId, Owner UID o Username son nulos para la ruta DeckView.")
                Text("Error: No se pudo cargar el mazo.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}