package com.kun.flowershop.screen

import com.kun.flowershop.*
import com.kun.flowershop.R
import com.kun.flowershop.data.*
import com.kun.flowershop.ui.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class ShopViewModel(
    protected val favorites: FavoritesRepository,
    protected val cart: CartRepository,
    protected val auth: AuthRepository,
) : ViewModel() {
    val favoriteIds: StateFlow<Set<Int>> = favorites.ids
    val cartItems: StateFlow<Map<CartKey, Int>> = cart.items
    val isLoggedIn: StateFlow<Boolean> = auth.isLoggedIn

    fun toggleFavorite(productId: Int, onRequireLogin: () -> Unit) {
        if (!auth.isLoggedIn.value) {
            onRequireLogin()
            return
        }
        viewModelScope.launch { favorites.toggle(productId) }
    }

    fun addToCart(productId: Int) = cart.add(productId)
}

class HomeViewModel(
    private val profileRepository: ProfileRepository,
    private val products: ProductRepository,
    favorites: FavoritesRepository,
    cart: CartRepository,
    auth: AuthRepository,
) : ShopViewModel(favorites, cart, auth) {
    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()

    val productsState: StateFlow<UiState<List<Product>>> = products.products
    val categories: StateFlow<List<Category>> = products.categories

    fun refreshName() {
        if (!auth.isLoggedIn.value) return
        viewModelScope.launch {
            _userName.value = runCatching { profileRepository.currentDisplayName() }.getOrNull()
        }
    }

    init {
        viewModelScope.launch { products.load() }

        viewModelScope.launch {
            auth.isLoggedIn.collect { loggedIn ->
                _userName.value = if (loggedIn) {
                    runCatching { profileRepository.currentDisplayName() }.getOrNull()
                } else {
                    null
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    onRequireLogin: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onProductClick: (Product) -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val state by viewModel.productsState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val products = (state as? UiState.Success<List<Product>>)?.data.orEmpty()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshName() }

    HomeContent(
        userName = userName,
        products = products,
        categories = categories,
        favoriteIds = favoriteIds,
        onNotificationsClick = onNotificationsClick,
        onProfileClick = onProfileClick,
        onOpenCatalog = onOpenCatalog,
        onSearchClick = onSearchClick,
        onProductClick = onProductClick,
        onFavorite = { viewModel.toggleFavorite(it.id, onRequireLogin) },
        onAddToCart = {
            viewModel.addToCart(it.id)
            context.toast(context.getString(R.string.added_to_cart))
        },
    )
}

@Composable
fun HomeContent(
    userName: String?,
    products: List<Product>,
    categories: List<Category>,
    favoriteIds: Set<Int>,
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onProductClick: (Product) -> Unit = {},
    onFavorite: (Product) -> Unit = {},
    onAddToCart: (Product) -> Unit = {},
) {
    val popular = products.take(HOME_ROW_SIZE)
    val newest = products.sortedByDescending { it.id }.take(HOME_ROW_SIZE)
    val featured = products.maxByOrNull { it.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 47.dp, start = 28.dp, end = 26.dp),
            verticalAlignment = Alignment.Top,
        ) {
            KunBrandHeader(logoWidth = 100.dp)
            Spacer(Modifier.weight(1f))
            HeaderIcon(R.drawable.ic_bell, R.string.cd_notifications, 32.dp, 33.dp, onNotificationsClick, topPadding = 6.dp)
            Spacer(Modifier.width(24.dp))
            HeaderIcon(R.drawable.ic_user, R.string.cd_profile, 33.dp, 33.dp, onProfileClick, topPadding = 6.dp)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = (userName?.let { stringResource(R.string.home_greeting_name, it) }
                ?: stringResource(R.string.home_greeting)) + "🌷",
            style = kunText(30f, lineHeight = 26f),
            modifier = Modifier.padding(start = 27.dp),
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = kunText(8f, lineHeight = 9.5f),
            modifier = Modifier.padding(start = 30.dp, top = 1.dp),
        )

        Spacer(Modifier.height(24.dp))

        Box(Modifier.clickable(onClick = onSearchClick)) {
            KunSearchField(
                value = "",
                onValueChange = {},
                placeholder = stringResource(R.string.home_search_placeholder),
                editable = false,
                modifier = Modifier.padding(horizontal = 26.dp),
            )
        }

        if (categories.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                items(categories.take(HOME_ROW_SIZE), key = { it.id }) { category ->
                    val preview = products.firstOrNull { it.categoryId == category.id && !it.image.isNullOrBlank() }
                    CategoryTile(name = category.name, product = preview, onClick = onOpenCatalog)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .padding(horizontal = 26.dp)
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(KunColors.White)
                .then(if (featured != null) Modifier.clickable { onProductClick(featured) } else Modifier),
        ) {
            if (featured != null) FeaturedBanner(featured) { onProductClick(featured) }
        }

        Spacer(Modifier.height(18.dp))
        SectionHeader(title = stringResource(R.string.home_popular), onSeeAll = onOpenCatalog)
        Spacer(Modifier.height(14.dp))
        ProductRow(popular, favoriteIds, onProductClick, onFavorite, onAddToCart)

        if (products.size > HOME_ROW_SIZE) {
            Spacer(Modifier.height(24.dp))
            SectionHeader(title = stringResource(R.string.home_new_section), onSeeAll = onOpenCatalog)
            Spacer(Modifier.height(14.dp))
            ProductRow(newest, favoriteIds, onProductClick, onFavorite, onAddToCart)
        }

        Spacer(Modifier.height(24.dp))
        DeliveryInfoCard(onOpenCatalog)
        Spacer(Modifier.height(24.dp))
    }
}

private const val HOME_ROW_SIZE = 8

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = kunText(24f, FontWeight.Medium))
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.home_see_all),
            style = kunText(16f, color = KunColors.TextSoft),
            modifier = Modifier
                .clickable(onClick = onSeeAll)
                .padding(top = 4.dp),
        )
    }
}

@Composable
private fun CategoryTile(name: String, product: Product?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(70.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KunColors.Tile),
        ) {
            if (product != null) ProductImage(product, Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = name,
            style = kunText(11f, FontWeight.Medium, KunColors.Green),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FeaturedBanner(product: Product, onClick: () -> Unit) {
    Row(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.home_new),
                style = kunText(12f, FontWeight.SemiBold, KunColors.Orange),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = product.name,
                style = kunText(20f, FontWeight.Medium, KunColors.Green, lineHeight = 24f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = priceFrom(product.fromPrice),
                style = kunText(16f, FontWeight.SemiBold),
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(KunColors.Orange)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_view),
                    style = kunText(13f, FontWeight.SemiBold, KunColors.White),
                )
            }
        }
        ProductImage(product, Modifier.width(150.dp).fillMaxHeight())
    }
}

@Composable
private fun ProductRow(
    products: List<Product>,
    favoriteIds: Set<Int>,
    onProductClick: (Product) -> Unit,
    onFavorite: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
) {
    if (products.isEmpty()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(List(3) { it }) {
                Box(
                    Modifier
                        .size(width = 150.dp, height = 190.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(KunColors.White),
                )
            }
        }
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 26.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(products, key = { it.id }) { product ->
            ProductMiniCard(
                product = product,
                isFavorite = product.id in favoriteIds,
                onClick = { onProductClick(product) },
                onFavorite = { onFavorite(product) },
                onAddToCart = { onAddToCart(product) },
            )
        }
    }
}

@Composable
private fun ProductMiniCard(
    product: Product,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onAddToCart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(KunColors.White)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(140.dp)) {
            ProductImage(product, Modifier.fillMaxSize())
            FavoriteButton(
                isFavorite = isFavorite,
                onClick = onFavorite,
                onImage = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            )
        }
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = kunText(13f, FontWeight.Medium, KunColors.Green),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = priceFrom(product.fromPrice),
                    style = kunText(12f, FontWeight.SemiBold),
                )
            }
            Spacer(Modifier.width(6.dp))
            AddToCartButton(onClick = onAddToCart, size = 30.dp)
        }
    }
}

@Composable
private fun DeliveryInfoCard(onOpenCatalog: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 26.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KunColors.Field)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.home_info_title),
            style = kunText(18f, FontWeight.SemiBold, KunColors.Green),
        )
        Spacer(Modifier.height(10.dp))
        InfoRow(stringResource(R.string.home_info_pickup), stringResource(R.string.home_info_pickup_value))
        Spacer(Modifier.height(6.dp))
        InfoRow(stringResource(R.string.home_info_delivery), stringResource(R.string.home_info_delivery_value))
        Spacer(Modifier.height(14.dp))
        KunButton(
            text = stringResource(R.string.home_to_catalog),
            onClick = onOpenCatalog,
            height = 40.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = kunText(14f, FontWeight.Medium, KunColors.Green), modifier = Modifier.width(100.dp))
        Text(text = value, style = kunText(14f, color = KunColors.TextGray))
    }
}

@Composable
private fun HeaderIcon(
    drawable: Int,
    contentDescription: Int,
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
    topPadding: Dp,
) {
    Image(
        painter = painterResource(drawable),
        contentDescription = stringResource(contentDescription),
        colorFilter = ColorFilter.tint(KunColors.Black),
        modifier = Modifier
            .padding(top = topPadding)
            .size(width = width, height = height)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}

internal val previewProducts = listOf(
    Product(1, 1, "bouquet", "Весеннее утро", null, 6490.0, null, 10),
    Product(2, 1, "bouquet", "Малиновый закат", null, 9490.0, null, 5),
    Product(3, 2, "flower", "Тюльпаны", null, 1790.0, null, 30),
    Product(4, 3, "composition", "С любовью", null, 7990.0, null, 4),
)

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeContent(
        userName = "Нина",
        products = previewProducts,
        categories = listOf(Category(1, "Букеты"), Category(2, "Цветы"), Category(3, "Композиции")),
        favoriteIds = setOf(2),
    )
}
