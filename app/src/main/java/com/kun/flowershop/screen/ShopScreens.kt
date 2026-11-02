package com.kun.flowershop.screen

import com.kun.flowershop.*
import com.kun.flowershop.R
import com.kun.flowershop.data.*
import com.kun.flowershop.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val products: ProductRepository,
    favorites: FavoritesRepository,
    cart: CartRepository,
    auth: AuthRepository,
) : ShopViewModel(favorites, cart, auth) {
    private val _type = MutableStateFlow(ProductType.BOUQUET)
    val selectedType: StateFlow<ProductType> = _type.asStateFlow()
    val productsState: StateFlow<UiState<List<Product>>> = products.products

    init {
        viewModelScope.launch { products.load() }
    }

    fun onTypeSelected(type: ProductType) {
        _type.value = type
    }

    fun reload() {
        viewModelScope.launch { products.load(force = true) }
    }
}

@Composable
fun FavoritesScreen(
    onRequireLogin: () -> Unit = {},
    onProductClick: (Product) -> Unit = {},
    viewModel: FavoritesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val loggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val type by viewModel.selectedType.collectAsStateWithLifecycle()
    val load by viewModel.productsState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    FavoritesContent(
        loggedIn = loggedIn,
        load = load,
        favoriteIds = favoriteIds,
        type = type,
        onTypeSelected = viewModel::onTypeSelected,
        onRetry = viewModel::reload,
        onLogin = onRequireLogin,
        onRemove = { viewModel.toggleFavorite(it.id, onRequireLogin) },
        onProductClick = onProductClick,
    )
}

@Composable
fun FavoritesContent(
    loggedIn: Boolean,
    load: UiState<List<Product>>,
    favoriteIds: Set<Int>,
    type: ProductType,
    onTypeSelected: (ProductType) -> Unit = {},
    onRetry: () -> Unit = {},
    onLogin: () -> Unit = {},
    onRemove: (Product) -> Unit = {},
    onProductClick: (Product) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        Spacer(Modifier.height(50.dp))
        ScreenTitle(stringResource(R.string.favorites_title))

        if (!loggedIn) {
            GuestGate(text = stringResource(R.string.favorites_login_prompt), onLogin = onLogin)
        } else {
            Spacer(Modifier.height(14.dp))
            TypeTabs(
                selected = type,
                onSelect = onTypeSelected,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(16.dp))

            when (load) {
                UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(
                    title = stringResource(R.string.catalog_error),
                    detail = load.throwable.message,
                    onRetry = onRetry,
                )
                is UiState.Success<*> -> {
                    val items = (load as UiState.Success<List<Product>>).data
                        .filter { it.id in favoriteIds && type.matches(it.type) }
                    if (items.isEmpty()) {
                        EmptyView(stringResource(R.string.favorites_empty))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 25.dp, end = 25.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            items(items, key = { it.id }) { product ->
                                FavoriteCard(
                                    product = product,
                                    onClick = { onProductClick(product) },
                                    onRemove = { onRemove(product) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(product: Product, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(KunColors.White)
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductImage(product, Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)))
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                Text(product.name, style = kunText(16f, color = KunColors.Green), maxLines = 2)
                Spacer(Modifier.height(6.dp))
                Text(priceFromStyled(product.fromPrice), style = kunText(12f))
            }
        }
        FavoriteButton(isFavorite = true, onClick = onRemove, size = 36.dp)
        Spacer(Modifier.width(4.dp))
    }
}

private fun priceFromStyled(price: Double) = buildAnnotatedString {
    append("от ")
    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(formatPrice(price)) }
}

data class CartLine(val product: Product, val quantity: Int, val size: ProductSize? = null) {
    val key: CartKey get() = CartKey(product.id, size?.id)
    val unitPrice: Double get() = size?.price ?: product.price
}

enum class PromoStatus { Idle, Loading, NotFound }

class CartViewModel(
    private val products: ProductRepository,
    favorites: FavoritesRepository,
    private val cartRepository: CartRepository,
    auth: AuthRepository,
    private val promos: PromoRepository,
) : ShopViewModel(favorites, cartRepository, auth) {
    val productsState: StateFlow<UiState<List<Product>>> = products.products
    val sizes: StateFlow<Map<Int, List<ProductSize>>> = products.sizes
    val promo: StateFlow<PromoCode?> = cartRepository.promo

    private val _promoStatus = MutableStateFlow(PromoStatus.Idle)
    val promoStatus: StateFlow<PromoStatus> = _promoStatus.asStateFlow()

    init {
        viewModelScope.launch { products.load() }
    }

    fun increase(line: CartLine) = cartRepository.setQuantity(line.key, line.quantity + 1)
    fun decrease(line: CartLine) = cartRepository.setQuantity(line.key, maxOf(1, line.quantity - 1))
    fun remove(line: CartLine) = cartRepository.remove(line.key)

    fun applyPromo(code: String) {
        if (code.isBlank()) return
        _promoStatus.value = PromoStatus.Loading
        viewModelScope.launch {
            val found = promos.find(code)
            cartRepository.setPromo(found)
            _promoStatus.value = if (found != null) PromoStatus.Idle else PromoStatus.NotFound
        }
    }

    fun clearPromo() {
        cartRepository.setPromo(null)
        _promoStatus.value = PromoStatus.Idle
    }

    fun reload() {
        viewModelScope.launch { products.load(force = true) }
    }
}

@Composable
fun CartScreen(
    onCheckout: () -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    onRequireLogin: () -> Unit = {},
    onProductClick: (Product) -> Unit = {},
    viewModel: CartViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val load by viewModel.productsState.collectAsStateWithLifecycle()
    val sizes by viewModel.sizes.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val promo by viewModel.promo.collectAsStateWithLifecycle()
    val promoStatus by viewModel.promoStatus.collectAsStateWithLifecycle()
    var lineToDelete by remember { mutableStateOf<CartLine?>(null) }

    lineToDelete?.let { line ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.confirm_delete_cart_title),
            text = stringResource(R.string.confirm_delete_cart_text, line.product.name),
            onConfirm = {
                lineToDelete = null
                viewModel.remove(line)
            },
            onDismiss = { lineToDelete = null },
        )
    }

    CartContent(
        load = load,
        cartItems = cartItems,
        sizes = sizes,
        favoriteIds = favoriteIds,
        promo = promo,
        promoStatus = promoStatus,
        onIncrease = viewModel::increase,
        onDecrease = viewModel::decrease,
        onRemove = { lineToDelete = it },
        onFavorite = { viewModel.toggleFavorite(it.id, onRequireLogin) },
        onApplyPromo = viewModel::applyPromo,
        onClearPromo = viewModel::clearPromo,
        onProductClick = onProductClick,
        onCheckout = onCheckout,
        onOpenCatalog = onOpenCatalog,
        onRetry = viewModel::reload,
    )
}

@Composable
fun CartContent(
    load: UiState<List<Product>>,
    cartItems: Map<CartKey, Int>,
    favoriteIds: Set<Int>,
    sizes: Map<Int, List<ProductSize>> = emptyMap(),
    promo: PromoCode? = null,
    promoStatus: PromoStatus = PromoStatus.Idle,
    onIncrease: (CartLine) -> Unit = {},
    onDecrease: (CartLine) -> Unit = {},
    onRemove: (CartLine) -> Unit = {},
    onFavorite: (Product) -> Unit = {},
    onApplyPromo: (String) -> Unit = {},
    onClearPromo: () -> Unit = {},
    onProductClick: (Product) -> Unit = {},
    onCheckout: () -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        Spacer(Modifier.height(50.dp))
        ScreenTitle(stringResource(R.string.cart_title))

        if (cartItems.isEmpty()) {
            CartEmpty(onOpenCatalog)
        } else {
            when (load) {
                UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(
                    title = stringResource(R.string.catalog_error),
                    detail = load.throwable.message,
                    onRetry = onRetry,
                )
                is UiState.Success<*> -> {
                    val all = (load as UiState.Success<List<Product>>).data
                    val lines = buildCartLines(all, sizes, cartItems)
                    val summary = cartSummary(lines, promo)

                    Text(
                        text = itemsLabel(lines.sumOf { it.quantity }),
                        style = kunText(14f),
                        modifier = Modifier.padding(start = 26.dp),
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 25.dp, end = 25.dp, top = 14.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(lines, key = { "${it.product.id}-${it.size?.id ?: 0}" }) { line ->
                            CartItemCard(
                                line = line,
                                isFavorite = line.product.id in favoriteIds,
                                onClick = { onProductClick(line.product) },
                                onIncrease = { onIncrease(line) },
                                onDecrease = { onDecrease(line) },
                                onRemove = { onRemove(line) },
                                onFavorite = { onFavorite(line.product) },
                            )
                        }
                        item {
                            PromoField(
                                promo = promo,
                                status = promoStatus,
                                onApply = onApplyPromo,
                                onClear = onClearPromo,
                            )
                        }
                    }

                    Column(Modifier.padding(start = 25.dp, end = 25.dp, top = 8.dp, bottom = 12.dp)) {
                        if (summary.discount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.cart_discount),
                                    style = kunText(13f, color = KunColors.TextGray),
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "−" + formatMoney(summary.discount),
                                    style = kunText(13f, color = KunColors.TextGray),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.cart_total), style = kunText(16f, FontWeight.SemiBold))
                            Spacer(Modifier.weight(1f))
                            Text(formatMoney(summary.total), style = kunText(18f, FontWeight.SemiBold))
                        }
                        Spacer(Modifier.height(10.dp))
                        KunButton(
                            text = stringResource(R.string.cart_checkout),
                            onClick = onCheckout,
                            height = 48.dp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartEmpty(onOpenCatalog: () -> Unit) {
    EmptyState(
        text = stringResource(R.string.cart_empty),
        buttonText = stringResource(R.string.cart_to_catalog),
        onClick = onOpenCatalog,
    )
}

@Composable
private fun CartItemCard(
    line: CartLine,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit,
    onFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(KunColors.White)
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductImage(line.product, Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(line.product.name, style = kunText(16f, color = KunColors.Green), maxLines = 2)
            Spacer(Modifier.height(4.dp))
            if (line.size != null) {
                Text(
                    text = line.size.name + " · " + formatPrice(line.unitPrice),
                    style = kunText(12f, FontWeight.SemiBold),
                    maxLines = 1,
                )
            } else {
                Text(priceFromStyled(line.product.price), style = kunText(12f))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuantityButton("−", onDecrease)
                Text(
                    text = line.quantity.toString(),
                    style = kunText(14f, FontWeight.Medium),
                    modifier = Modifier.width(30.dp),
                    textAlign = TextAlign.Center,
                )
                QuantityButton("+", onIncrease)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FavoriteButton(isFavorite = isFavorite, onClick = onFavorite, size = 30.dp)
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.cd_remove),
                    tint = KunColors.Green,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun QuantityButton(symbol: String, onClick: () -> Unit, size: Dp = 26.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(KunColors.Field)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = kunText(15f, FontWeight.SemiBold, KunColors.Green))
    }
}

@Composable
private fun PromoField(
    promo: PromoCode?,
    status: PromoStatus,
    onApply: (String) -> Unit,
    onClear: () -> Unit,
) {
    var code by remember { mutableStateOf("") }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(CircleShape)
                .border(1.dp, KunColors.Dot, CircleShape)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (promo != null) {
                Text(
                    text = stringResource(R.string.promo_applied, promo.code, promo.discountPercent),
                    style = kunText(13f, FontWeight.Medium, KunColors.Green),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.cd_clear),
                    tint = KunColors.TextGray,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onClear),
                )
            } else {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (code.isEmpty()) {
                        Text(stringResource(R.string.cart_promo), style = kunText(14f, color = KunColors.TextGray))
                    }
                    BasicTextField(
                        value = code,
                        onValueChange = { code = it },
                        singleLine = true,
                        textStyle = kunText(14f),
                        cursorBrush = SolidColor(KunColors.Black),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = KunColors.TextGray,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(enabled = status != PromoStatus.Loading) { onApply(code) },
                )
            }
        }
        if (status == PromoStatus.NotFound) {
            Text(
                text = stringResource(R.string.promo_not_found),
                style = kunText(11f, color = KunColors.Error),
                modifier = Modifier.padding(start = 18.dp, top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FavoritesScreenPreview() {
    FavoritesContent(
        loggedIn = true,
        load = UiState.Success(previewProducts),
        favoriteIds = setOf(1, 2),
        type = ProductType.BOUQUET,
    )
}

@Preview(showBackground = true)
@Composable
fun CartScreenPreview() {
    CartContent(
        load = UiState.Success(previewProducts),
        cartItems = mapOf(CartKey(1) to 1, CartKey(2) to 2, CartKey(3) to 1),
        favoriteIds = setOf(1),
    )
}
