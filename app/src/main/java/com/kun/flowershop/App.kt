package com.kun.flowershop

import com.kun.flowershop.data.*
import com.kun.flowershop.screen.*
import com.kun.flowershop.ui.*
import android.app.Application
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

class KunApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transparent = AndroidColor.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = SystemBarStyle.light(transparent, transparent),
        )
        setContent {
            KunTheme {
                KunNavHost()
            }
        }
    }
}

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { with(kunApplication().container) { SplashViewModel(authRepository, prefs) } }
        initializer { AuthViewModel(kunApplication().container.authRepository) }
        initializer {
            with(kunApplication().container) {
                HomeViewModel(profileRepository, productRepository, favoritesRepository, cartRepository, authRepository)
            }
        }
        initializer {
            with(kunApplication().container) {
                CatalogViewModel(productRepository, favoritesRepository, cartRepository, authRepository)
            }
        }
        initializer {
            with(kunApplication().container) {
                FavoritesViewModel(productRepository, favoritesRepository, cartRepository, authRepository)
            }
        }
        initializer {
            with(kunApplication().container) {
                CartViewModel(productRepository, favoritesRepository, cartRepository, authRepository, promoRepository)
            }
        }
        initializer {
            with(kunApplication().container) {
                ProductViewModel(productRepository, favoritesRepository, cartRepository, authRepository)
            }
        }
        initializer {
            with(kunApplication().container) {
                SearchViewModel(productRepository, favoritesRepository, cartRepository, authRepository)
            }
        }
        initializer {
            with(kunApplication().container) {
                CheckoutViewModel(
                    productRepository,
                    cartRepository,
                    ordersRepository,
                    addressRepository,
                    cardsRepository,
                    notificationsRepository,
                    profileRepository,
                )
            }
        }
        initializer { with(kunApplication().container) { OrdersViewModel(ordersRepository, productRepository) } }
        initializer { NotificationsViewModel(kunApplication().container.notificationsRepository) }
        initializer { with(kunApplication().container) { PersonalViewModel(profileRepository, authRepository) } }
        initializer { AddressesViewModel(kunApplication().container.addressRepository) }
        initializer { PaymentsViewModel(kunApplication().container.cardsRepository) }
        initializer { with(kunApplication().container) { ProfileViewModel(authRepository, profileRepository) } }
    }
}

private fun CreationExtras.kunApplication(): KunApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KunApplication
