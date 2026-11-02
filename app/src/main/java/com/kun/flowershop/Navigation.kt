package com.kun.flowershop

import com.kun.flowershop.data.*
import com.kun.flowershop.screen.*
import com.kun.flowershop.ui.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val REGISTER = "register"
    const val LOGIN = "login"
    const val FORGOT = "forgot"
    const val HOME = "home"
    const val CATALOG = "catalog"
    const val FAVORITES = "favorites"
    const val CART = "cart"
    const val PROFILE = "profile"
    const val SEARCH = "search"
    const val PRODUCT = "product/{id}"
    const val CHECKOUT = "checkout"
    const val THANKS = "thanks"
    const val NOTIFICATIONS = "notifications"
    const val ORDERS = "orders"
    const val PAYMENTS = "payments"
    const val ADDRESSES = "addresses"
    const val HELP = "help"
    const val PERSONAL = "personal"

    fun product(id: Int) = "product/$id"
}

@Composable
fun KunNavHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val container = (context.applicationContext as KunApplication).container

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = KunTab.fromRoute(backStackEntry?.destination?.route)
    val loggedIn by container.authRepository.isLoggedIn.collectAsStateWithLifecycle()
    val cartItems by container.cartRepository.items.collectAsStateWithLifecycle()

    var pendingRoute by remember { mutableStateOf<String?>(null) }

    fun requireLogin(next: String? = null) {
        pendingRoute = next
        navController.navigate(Routes.LOGIN) { launchSingleTop = true }
    }

    fun onAuthenticated() {
        navController.goHomeClearingStack()
        val next = pendingRoute
        pendingRoute = null
        if (next != null) {
            val tab = KunTab.fromRoute(next)
            if (tab != null) navController.navigateToTab(tab) else navController.navigate(next)
        }
    }

    fun openProduct(product: Product) {
        navController.navigate(Routes.product(product.id)) { launchSingleTop = true }
    }

    fun openNotifications() {
        if (loggedIn) navController.navigate(Routes.NOTIFICATIONS) else requireLogin(Routes.NOTIFICATIONS)
    }

    Scaffold(
        containerColor = KunColors.Background,
        bottomBar = {
            if (currentTab != null) {
                KunBottomBar(
                    selected = currentTab,
                    onSelect = { tab -> navController.navigateToTab(tab) },
                    cartCount = cartItems.values.sum(),
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(
                    onFinished = { showOnboarding ->
                        navController.navigate(if (showOnboarding) Routes.ONBOARDING else Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinish = {
                        container.prefs.onboardingSeen = true
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onBack = { navController.popBackStack() },
                    onGoToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                        }
                    },
                    onAuthenticated = { onAuthenticated() },
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onBack = { navController.popBackStack() },
                    onGoToRegister = {
                        navController.navigate(Routes.REGISTER) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onAuthenticated = { onAuthenticated() },
                    onForgot = { navController.navigate(Routes.FORGOT) },
                )
            }
            composable(Routes.FORGOT) {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onNotificationsClick = { openNotifications() },
                    onProfileClick = {
                        if (loggedIn) navController.navigate(Routes.PROFILE) else requireLogin(Routes.PROFILE)
                    },
                    onOpenCatalog = { navController.navigateToTab(KunTab.Catalog) },
                    onRequireLogin = { requireLogin() },
                    onSearchClick = { navController.navigate(Routes.SEARCH) { launchSingleTop = true } },
                    onProductClick = { openProduct(it) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onProductClick = { openProduct(it) },
                    onRequireLogin = { requireLogin() },
                )
            }
            composable(Routes.CATALOG) {
                CatalogScreen(
                    onProductClick = { openProduct(it) },
                    onRequireLogin = { requireLogin() },
                )
            }
            composable(
                route = Routes.PRODUCT,
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                ProductScreen(
                    productId = entry.arguments?.getInt("id") ?: 0,
                    onBack = { navController.popBackStack() },
                    onAddedToCart = { navController.navigateToTab(KunTab.Cart) },
                    onRequireLogin = { requireLogin() },
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    onRequireLogin = { requireLogin(Routes.FAVORITES) },
                    onProductClick = { openProduct(it) },
                )
            }
            composable(Routes.CART) {
                CartScreen(
                    onCheckout = {
                        if (loggedIn) navController.navigate(Routes.CHECKOUT) else requireLogin(Routes.CHECKOUT)
                    },
                    onOpenCatalog = { navController.navigateToTab(KunTab.Catalog) },
                    onRequireLogin = { requireLogin(Routes.CART) },
                    onProductClick = { openProduct(it) },
                )
            }
            composable(Routes.CHECKOUT) {
                CheckoutScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPayments = { navController.navigate(Routes.PAYMENTS) },
                    onOrderCreated = {
                        navController.navigate(Routes.THANKS) {
                            popUpTo(Routes.CHECKOUT) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.THANKS) {
                ThanksScreen(onHome = { navController.goHomeClearingStack() })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onLoggedOut = { navController.goHomeClearingStack() },
                    onMenu = { index ->
                        val route = when (index) {
                            0 -> Routes.ORDERS
                            1 -> Routes.NOTIFICATIONS
                            2 -> Routes.PAYMENTS
                            3 -> Routes.ADDRESSES
                            else -> Routes.HELP
                        }
                        navController.navigate(route) { launchSingleTop = true }
                    },
                    onEditProfile = { navController.navigate(Routes.PERSONAL) { launchSingleTop = true } },
                )
            }
            composable(Routes.PERSONAL) {
                PersonalScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ORDERS) {
                OrdersScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCatalog = { navController.navigateToTab(KunTab.Catalog) },
                )
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.PAYMENTS) {
                PaymentsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ADDRESSES) {
                AddressesScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.HELP) {
                HelpScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateToTab(tab: KunTab) {
    navigate(tab.route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.goHomeClearingStack() {
    navigate(Routes.HOME) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
