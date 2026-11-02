package com.kun.flowershop.data

import com.kun.flowershop.BuildConfig
import com.kun.flowershop.screen.*
import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseNotConfiguredException :
    IllegalStateException("SUPABASE_URL / SUPABASE_ANON_KEY не заданы в local.properties")

object SupabaseProvider {
    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val client: SupabaseClient by lazy {
        if (!isConfigured) throw SupabaseNotConfiguredException()
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val throwable: Throwable) : UiState<Nothing>
}

@Serializable
data class Profile(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class ProfileUpsert(
    val id: String,
    val name: String?,
    val email: String?,
)

@Serializable
data class Category(
    val id: Int,
    val name: String,
)

@Serializable
data class Product(
    val id: Int,
    @SerialName("category_id") val categoryId: Int? = null,
    val type: String? = null,
    val name: String,
    val description: String? = null,
    val price: Double,
    val image: String? = null,
    val stock: Int? = null,
    val rating: Double? = null,
    @Transient val minSizePrice: Double? = null,
) {
    /** Цена для подписи «от …» на превью: самый дешёвый размер, либо цена товара, если размеров нет. */
    val fromPrice: Double get() = minSizePrice ?: price
}

@Serializable
data class FavoriteRow(@SerialName("product_id") val productId: Int)

@Serializable
data class FavoriteInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("product_id") val productId: Int,
)

@Serializable
data class ProductSize(
    val id: Int,
    @SerialName("product_id") val productId: Int,
    val name: String,
    val pieces: Int? = null,
    val price: Double,
)

@Serializable
data class PromoCode(
    val code: String,
    @SerialName("discount_percent") val discountPercent: Int,
    val active: Boolean = true,
)

@Serializable
data class OrderRow(
    val id: Int,
    @SerialName("total_price") val totalPrice: Double = 0.0,
    val status: String? = null,
    @SerialName("recipient_name") val recipientName: String? = null,
    @SerialName("gift_message") val giftMessage: String? = null,
    @SerialName("delivery_type") val deliveryType: String? = null,
    val address: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class OrderItemRow(
    val id: Int,
    @SerialName("order_id") val orderId: Int,
    @SerialName("product_id") val productId: Int,
    val quantity: Int,
    val price: Double,
    @SerialName("size_name") val sizeName: String? = null,
)

@Serializable
data class OrderInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("total_price") val totalPrice: Double,
    val status: String,
    @SerialName("recipient_name") val recipientName: String?,
    @SerialName("gift_message") val giftMessage: String?,
    @SerialName("delivery_type") val deliveryType: String,
    val address: String?,
    @SerialName("payment_method") val paymentMethod: String,
)

@Serializable
data class OrderItemInsert(
    @SerialName("order_id") val orderId: Int,
    @SerialName("product_id") val productId: Int,
    val quantity: Int,
    val price: Double,
    @SerialName("size_name") val sizeName: String?,
)

@Serializable
data class AddressRow(
    val id: Int,
    val title: String? = null,
    val address: String,
)

@Serializable
data class AddressInsert(
    @SerialName("user_id") val userId: String,
    val title: String?,
    val address: String,
)

@Serializable
data class NotificationRow(
    val id: Int,
    val title: String,
    val body: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class NotificationInsert(
    @SerialName("user_id") val userId: String,
    val title: String,
    val body: String,
)

data class OrderWithItems(val order: OrderRow, val items: List<OrderItemRow>)

data class SavedCard(val brand: String, val last4: String)

data class CartKey(val productId: Int, val sizeId: Int? = null)

enum class DeliveryType(val code: String) {
    PICKUP("pickup"),
    DELIVERY("delivery"),
}

enum class PaymentType(val code: String) {
    CARD("card"),
    SBERPAY("sberpay"),
}

const val DELIVERY_FEE = 250.0

enum class ProductType(val title: String, private val aliases: Set<String>) {
    BOUQUET("Букеты", setOf("bouquet", "bouquets", "букет", "букеты")),
    FLOWER("Цветы", setOf("flower", "flowers", "цветок", "цветы")),
    COMPOSITION("Композиции", setOf("composition", "compositions", "композиция", "композиции"));

    fun matches(rawType: String?): Boolean =
        rawType?.trim()?.lowercase() in aliases
}

object ImageUrl {
    fun resolve(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        if (value.startsWith("http://") || value.startsWith("https://")) return value
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        if (base.isEmpty()) return null
        return "$base/storage/v1/object/public/${BuildConfig.SUPABASE_IMAGES_BUCKET}/${value.trimStart('/')}"
    }
}

fun formatPrice(price: Double): String =
    String.format(Locale.US, "%.0f", price) + " ₽"

fun priceFrom(price: Double): String = "от " + formatPrice(price)

fun formatMoney(amount: Double): String =
    String.format(Locale.US, "%,.0f", amount).replace(',', '\u00A0') + " ₽"

fun itemsLabel(count: Int): String {
    val m100 = count % 100
    val m10 = count % 10
    val word = when {
        m100 in 11..14 -> "товаров"
        m10 == 1 -> "товар"
        m10 in 2..4 -> "товара"
        else -> "товаров"
    }
    return "$count $word"
}

fun formatDate(raw: String?): String {
    val parts = raw?.take(10).orEmpty().split('-')
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else raw.orEmpty()
}

fun orderStatusLabel(status: String?): String = when (status?.trim()?.lowercase()) {
    "new" -> "Новый"
    "paid" -> "Оплачен"
    "processing" -> "Собирается"
    "delivering" -> "В пути"
    "done" -> "Выполнен"
    "cancelled" -> "Отменён"
    else -> status ?: "Новый"
}

fun cardBrand(digits: String): String = when {
    digits.startsWith("2") -> "МИР"
    digits.startsWith("4") -> "Visa"
    digits.startsWith("5") -> "Mastercard"
    else -> "Карта"
}

data class CartSummary(val subtotal: Double, val discount: Double, val total: Double)

fun cartSummary(lines: List<CartLine>, promo: PromoCode?, deliveryFee: Double = 0.0): CartSummary {
    val subtotal = lines.sumOf { it.unitPrice * it.quantity }
    val discount = promo?.let { Math.round(subtotal * it.discountPercent / 100.0).toDouble() } ?: 0.0
    return CartSummary(subtotal, discount, subtotal - discount + deliveryFee)
}

fun buildCartLines(
    products: List<Product>,
    sizes: Map<Int, List<ProductSize>>,
    items: Map<CartKey, Int>,
): List<CartLine> = items.mapNotNull { (key, qty) ->
    val product = products.firstOrNull { it.id == key.productId } ?: return@mapNotNull null
    val size = key.sizeId?.let { id -> sizes[key.productId]?.firstOrNull { it.id == id } }
    CartLine(product, qty, size)
}

class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("kun_prefs", Context.MODE_PRIVATE)

    var onboardingSeen: Boolean
        get() = prefs.getBoolean("onboarding_seen", false)
        set(value) = prefs.edit().putBoolean("onboarding_seen", value).apply()
}

class CartRepository(context: Context) {
    private val prefs = context.getSharedPreferences("kun_cart", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(read())
    val items: StateFlow<Map<CartKey, Int>> = _items.asStateFlow()

    private val _promo = MutableStateFlow<PromoCode?>(null)
    val promo: StateFlow<PromoCode?> = _promo.asStateFlow()

    fun add(productId: Int, sizeId: Int? = null) {
        val key = CartKey(productId, sizeId)
        mutate { it + (key to ((it[key] ?: 0) + 1)) }
    }

    fun setQuantity(key: CartKey, quantity: Int) =
        mutate { if (quantity <= 0) it - key else it + (key to quantity) }

    fun remove(key: CartKey) = mutate { it - key }

    fun clear() {
        _promo.value = null
        mutate { emptyMap() }
    }

    fun setPromo(code: PromoCode?) {
        _promo.value = code
    }

    private fun mutate(block: (Map<CartKey, Int>) -> Map<CartKey, Int>) {
        _items.update(block)
        prefs.edit()
            .putString(
                KEY,
                _items.value.entries.joinToString(",") { "${it.key.productId}:${it.key.sizeId ?: 0}:${it.value}" },
            )
            .apply()
    }

    private fun read(): Map<CartKey, Int> =
        prefs.getString(KEY, "").orEmpty().split(',').mapNotNull { part ->
            val pieces = part.split(':')
            val id = pieces.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val sizeId: Int?
            val qty: Int?
            if (pieces.size >= 3) {
                sizeId = pieces[1].toIntOrNull()?.takeIf { it > 0 }
                qty = pieces[2].toIntOrNull()
            } else {
                sizeId = null
                qty = pieces.getOrNull(1)?.toIntOrNull()
            }
            if (qty != null && qty > 0) CartKey(id, sizeId) to qty else null
        }.toMap()

    private companion object {
        const val KEY = "items"
    }
}

class CardsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("kun_cards", Context.MODE_PRIVATE)
    private val _cards = MutableStateFlow(read())
    val cards: StateFlow<List<SavedCard>> = _cards.asStateFlow()

    fun add(number: String) {
        val digits = number.filter { it.isDigit() }
        if (digits.length < 4) return
        val card = SavedCard(cardBrand(digits), digits.takeLast(4))
        _cards.update { list -> list.filterNot { it == card } + card }
        save()
    }

    fun remove(card: SavedCard) {
        _cards.update { list -> list.filterNot { it == card } }
        save()
    }

    private fun save() {
        prefs.edit()
            .putString("cards", _cards.value.joinToString(",") { "${it.brand}:${it.last4}" })
            .apply()
    }

    private fun read(): List<SavedCard> =
        prefs.getString("cards", "").orEmpty().split(',').mapNotNull { part ->
            val pieces = part.split(':')
            if (pieces.size == 2 && pieces[1].length == 4) SavedCard(pieces[0], pieces[1]) else null
        }
}

class ProfileRepository {
    private val client get() = SupabaseProvider.client

    suspend fun getCurrentProfile(): Profile? {
        val userId = client.auth.currentUserOrNull()?.id ?: return null
        return client.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<Profile>()
    }

    suspend fun ensureProfile() {
        runCatching {
            val user = client.auth.currentUserOrNull() ?: return
            val existing = client.from("profiles")
                .select { filter { eq("id", user.id) } }
                .decodeSingleOrNull<Profile>()
            if (existing == null) {
                val name = user.userMetadata?.get("name")?.jsonPrimitive?.contentOrNull
                    ?: user.email?.substringBefore('@')
                client.from("profiles").upsert(ProfileUpsert(id = user.id, name = name, email = user.email))
            }
        }
    }

    fun currentPhone(): String? = runCatching {
        client.auth.currentUserOrNull()?.userMetadata?.get("phone")?.jsonPrimitive?.contentOrNull
    }.getOrNull()

    suspend fun saveProfile(name: String, phone: String): Result<Unit> = runCatching {
        val user = client.auth.currentUserOrNull() ?: error("not logged in")
        client.from("profiles").upsert(ProfileUpsert(id = user.id, name = name, email = user.email))
        runCatching {
            client.auth.updateUser {
                data = buildJsonObject {
                    put("name", name)
                    put("phone", phone)
                }
            }
        }
        Unit
    }

    suspend fun currentDisplayName(): String? {
        val user = client.auth.currentUserOrNull() ?: return null
        val fromProfile = runCatching { getCurrentProfile()?.name }.getOrNull()
        return fromProfile?.takeIf { it.isNotBlank() }
            ?: user.userMetadata?.get("name")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@')
    }
}

enum class SignUpOutcome { SignedIn, ConfirmEmail }

class AuthRepository(private val profiles: ProfileRepository) {
    private val auth get() = SupabaseProvider.client.auth

    private val _isLoggedIn = MutableStateFlow(false)

    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    suspend fun restoreSession(): Boolean {
        val has = runCatching {
            auth.awaitInitialization()
            auth.currentSessionOrNull() != null
        }.getOrDefault(false)
        _isLoggedIn.value = has
        return has
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        profiles.ensureProfile()
        _isLoggedIn.value = true
    }

    suspend fun signUp(
        name: String,
        email: String,
        phone: String,
        password: String,
    ): Result<SignUpOutcome> = runCatching {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("name", name)
                if (phone.isNotBlank()) put("phone", phone)
            }
        }
        if (auth.currentSessionOrNull() != null) {
            profiles.ensureProfile()
            _isLoggedIn.value = true
            SignUpOutcome.SignedIn
        } else {
            SignUpOutcome.ConfirmEmail
        }
    }

    suspend fun signOut() {
        runCatching { auth.signOut() }
        _isLoggedIn.value = false
    }

    suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        auth.resetPasswordForEmail(email)
    }

    suspend fun changePassword(newPassword: String): Result<Unit> = runCatching {
        auth.updateUser { password = newPassword }
        Unit
    }

    fun currentEmail(): String? = runCatching { auth.currentUserOrNull()?.email }.getOrNull()
}

class ProductRepository {
    private val client get() = SupabaseProvider.client
    private val mutex = Mutex()

    private val _products = MutableStateFlow<UiState<List<Product>>>(UiState.Loading)
    val products: StateFlow<UiState<List<Product>>> = _products.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _sizes = MutableStateFlow<Map<Int, List<ProductSize>>>(emptyMap())
    val sizes: StateFlow<Map<Int, List<ProductSize>>> = _sizes.asStateFlow()

    suspend fun load(force: Boolean = false) {
        mutex.withLock { loadLocked(force) }
    }

    private suspend fun loadLocked(force: Boolean) {
        if (!force && _products.value is UiState.Success) return
        if (_products.value !is UiState.Success) _products.value = UiState.Loading
        runCatching {
            client.from("product_sizes")
                .select { order(column = "price", order = Order.ASCENDING) }
                .decodeList<ProductSize>()
        }.onSuccess { list -> _sizes.value = list.groupBy { it.productId } }
        runCatching {
            client.from("products")
                .select { order(column = "id", order = Order.ASCENDING) }
                .decodeList<Product>()
        }.onSuccess { list ->
            val sizes = _sizes.value
            _products.value = UiState.Success(
                list.map { p -> p.copy(minSizePrice = sizes[p.id]?.minOfOrNull { it.price }) }
            )
        }.onFailure { e ->
            if (_products.value !is UiState.Success) _products.value = UiState.Error(e)
        }
        runCatching {
            client.from("categories")
                .select { order(column = "id", order = Order.ASCENDING) }
                .decodeList<Category>()
        }.onSuccess { _categories.value = it }
    }
}

class FavoritesRepository {
    private val client get() = SupabaseProvider.client

    private val _ids = MutableStateFlow<Set<Int>>(emptySet())
    val ids: StateFlow<Set<Int>> = _ids.asStateFlow()

    private fun userId(): String? = runCatching { client.auth.currentUserOrNull()?.id }.getOrNull()

    suspend fun load() {
        val uid = userId() ?: return
        runCatching {
            client.from("favorites")
                .select { filter { eq("user_id", uid) } }
                .decodeList<FavoriteRow>()
        }.onSuccess { rows -> _ids.value = rows.map { it.productId }.toSet() }
    }

    fun clear() {
        _ids.value = emptySet()
    }

    suspend fun toggle(productId: Int): Result<Unit> = runCatching {
        val uid = userId() ?: error("not logged in")
        if (productId in _ids.value) {
            _ids.update { it - productId }
            try {
                client.from("favorites").delete {
                    filter {
                        eq("user_id", uid)
                        eq("product_id", productId)
                    }
                }
            } catch (e: Exception) {
                _ids.update { it + productId }
                throw e
            }
        } else {
            _ids.update { it + productId }
            try {
                client.from("favorites").insert(FavoriteInsert(userId = uid, productId = productId))
            } catch (e: Exception) {
                _ids.update { it - productId }
                throw e
            }
        }
    }
}

class PromoRepository {
    private val client get() = SupabaseProvider.client

    suspend fun find(code: String): PromoCode? = runCatching {
        client.from("promo_codes")
            .select { filter { eq("code", code.trim().uppercase()) } }
            .decodeSingleOrNull<PromoCode>()
    }.getOrNull()?.takeIf { it.active && it.discountPercent > 0 }
}

class OrdersRepository {
    private val client get() = SupabaseProvider.client

    private fun userId(): String? = runCatching { client.auth.currentUserOrNull()?.id }.getOrNull()

    suspend fun load(): Result<List<OrderWithItems>> = runCatching {
        val uid = userId() ?: error("not logged in")
        val orders = client.from("orders")
            .select {
                filter { eq("user_id", uid) }
                order(column = "id", order = Order.DESCENDING)
            }
            .decodeList<OrderRow>()
        orders.map { order ->
            val items = client.from("order_items")
                .select { filter { eq("order_id", order.id) } }
                .decodeList<OrderItemRow>()
            OrderWithItems(order, items)
        }
    }

    suspend fun createOrder(
        recipientName: String?,
        comment: String?,
        delivery: DeliveryType,
        address: String?,
        payment: PaymentType,
        lines: List<CartLine>,
        total: Double,
    ): Result<Int> = runCatching {
        val uid = userId() ?: error("not logged in")
        val order = client.from("orders")
            .insert(
                OrderInsert(
                    userId = uid,
                    totalPrice = total,
                    status = "paid",
                    recipientName = recipientName,
                    giftMessage = comment,
                    deliveryType = delivery.code,
                    address = address,
                    paymentMethod = payment.code,
                )
            ) { select() }
            .decodeSingle<OrderRow>()
        client.from("order_items").insert(
            lines.map {
                OrderItemInsert(
                    orderId = order.id,
                    productId = it.product.id,
                    quantity = it.quantity,
                    price = it.unitPrice,
                    sizeName = it.size?.name,
                )
            }
        )
        order.id
    }
}

class AddressRepository {
    private val client get() = SupabaseProvider.client

    private val _items = MutableStateFlow<List<AddressRow>>(emptyList())
    val items: StateFlow<List<AddressRow>> = _items.asStateFlow()

    private fun userId(): String? = runCatching { client.auth.currentUserOrNull()?.id }.getOrNull()

    suspend fun load() {
        val uid = userId() ?: return
        runCatching {
            client.from("addresses")
                .select {
                    filter { eq("user_id", uid) }
                    order(column = "id", order = Order.ASCENDING)
                }
                .decodeList<AddressRow>()
        }.onSuccess { _items.value = it }
    }

    suspend fun add(title: String?, address: String): Result<Unit> = runCatching {
        val uid = userId() ?: error("not logged in")
        client.from("addresses").insert(AddressInsert(userId = uid, title = title, address = address.trim()))
        load()
    }

    suspend fun addIfMissing(address: String) {
        val value = address.trim()
        if (value.isEmpty() || _items.value.any { it.address.equals(value, ignoreCase = true) }) return
        add(null, value)
    }

    suspend fun remove(id: Int): Result<Unit> = runCatching {
        client.from("addresses").delete { filter { eq("id", id) } }
        _items.update { list -> list.filter { it.id != id } }
    }

    fun clear() {
        _items.value = emptyList()
    }
}

class NotificationsRepository {
    private val client get() = SupabaseProvider.client

    suspend fun load(): Result<List<NotificationRow>> = runCatching {
        client.from("notifications")
            .select { order(column = "id", order = Order.DESCENDING) }
            .decodeList<NotificationRow>()
    }

    suspend fun add(title: String, body: String) {
        runCatching {
            val uid = client.auth.currentUserOrNull()?.id ?: return
            client.from("notifications").insert(NotificationInsert(userId = uid, title = title, body = body))
        }
    }
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val prefs: AppPrefs by lazy { AppPrefs(appContext) }
    val profileRepository: ProfileRepository by lazy { ProfileRepository() }
    val authRepository: AuthRepository by lazy { AuthRepository(profileRepository) }
    val productRepository: ProductRepository by lazy { ProductRepository() }
    val favoritesRepository: FavoritesRepository by lazy { FavoritesRepository() }
    val cartRepository: CartRepository by lazy { CartRepository(appContext) }
    val cardsRepository: CardsRepository by lazy { CardsRepository(appContext) }
    val promoRepository: PromoRepository by lazy { PromoRepository() }
    val ordersRepository: OrdersRepository by lazy { OrdersRepository() }
    val addressRepository: AddressRepository by lazy { AddressRepository() }
    val notificationsRepository: NotificationsRepository by lazy { NotificationsRepository() }

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    favoritesRepository.load()
                    addressRepository.load()
                } else {
                    favoritesRepository.clear()
                    addressRepository.clear()
                }
            }
        }
    }
}
