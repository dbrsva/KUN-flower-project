package com.kun.flowershop.screen

import com.kun.flowershop.*
import com.kun.flowershop.R
import com.kun.flowershop.data.*
import com.kun.flowershop.ui.*
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val load: UiState<List<Product>> = UiState.Loading,
    val type: ProductType = ProductType.BOUQUET,
    val query: String = "",
) {
    fun visibleProducts(): List<Product> {
        val all = (load as? UiState.Success<List<Product>>)?.data.orEmpty()
        val q = query.trim()
        return all.filter { type.matches(it.type) }
            .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
    }
}

class CatalogViewModel(
    private val products: ProductRepository,
    favorites: FavoritesRepository,
    cart: CartRepository,
    auth: AuthRepository,
) : ShopViewModel(favorites, cart, auth) {
    private val type = MutableStateFlow(ProductType.BOUQUET)
    private val query = MutableStateFlow("")

    val state: StateFlow<CatalogUiState> =
        combine(products.products, type, query) { load, t, q -> CatalogUiState(load, t, q) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, CatalogUiState())

    init {
        viewModelScope.launch { products.load() }
    }

    fun load() {
        viewModelScope.launch { products.load(force = true) }
    }

    fun onTypeSelected(value: ProductType) = type.update { value }
    fun onQueryChange(value: String) = query.update { value }
}

@Composable
fun CatalogScreen(
    onProductClick: (Product) -> Unit = {},
    onRequireLogin: () -> Unit = {},
    viewModel: CatalogViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    CatalogContent(
        state = state,
        favoriteIds = favoriteIds,
        onQueryChange = viewModel::onQueryChange,
        onTypeSelected = viewModel::onTypeSelected,
        onRetry = viewModel::load,
        onProductClick = onProductClick,
        onFavorite = { viewModel.toggleFavorite(it.id, onRequireLogin) },
        onAddToCart = {
            viewModel.addToCart(it.id)
            context.toast(context.getString(R.string.added_to_cart))
        },
    )
}

@Composable
fun CatalogContent(
    state: CatalogUiState,
    favoriteIds: Set<Int>,
    onQueryChange: (String) -> Unit = {},
    onTypeSelected: (ProductType) -> Unit = {},
    onRetry: () -> Unit = {},
    onProductClick: (Product) -> Unit = {},
    onFavorite: (Product) -> Unit = {},
    onAddToCart: (Product) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        Spacer(Modifier.height(50.dp))
        ScreenTitle(stringResource(R.string.catalog_title))
        Spacer(Modifier.height(11.dp))
        KunSearchField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = stringResource(R.string.catalog_search_placeholder),
            iconTint = KunColors.Green,
            placeholderColor = KunColors.CatalogPlaceholder,
            placeholderSize = 10f,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(18.dp))
        TypeTabs(
            selected = state.type,
            onSelect = onTypeSelected,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(16.dp))

        when (val load = state.load) {
            UiState.Loading -> LoadingView()
            is UiState.Error -> ErrorView(
                title = stringResource(R.string.catalog_error),
                detail = load.throwable.message,
                onRetry = onRetry,
            )
            is UiState.Success<*> -> {
                val items = state.visibleProducts()
                if (items.isEmpty()) {
                    EmptyView(stringResource(R.string.catalog_empty))
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 25.dp, end = 25.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        items(items, key = { it.id }) { product ->
                            ProductListCard(
                                product = product,
                                isFavorite = product.id in favoriteIds,
                                onClick = { onProductClick(product) },
                                onFavorite = { onFavorite(product) },
                                onAddToCart = { onAddToCart(product) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductListCard(
    product: Product,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onAddToCart: () -> Unit,
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
        ProductImage(
            product = product,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                Text(
                    text = product.name,
                    style = kunText(16f, color = KunColors.Green),
                    maxLines = 2,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = buildAnnotatedString {
                        append("от ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                            append(formatPrice(product.fromPrice))
                        }
                    },
                    style = kunText(12f),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FavoriteButton(isFavorite = isFavorite, onClick = onFavorite, size = 28.dp)
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = stringResource(R.string.cd_open),
                tint = KunColors.Arrow,
                modifier = Modifier.size(20.dp),
            )
            AddToCartButton(onClick = onAddToCart, size = 32.dp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CatalogScreenPreview() {
    CatalogContent(
        state = CatalogUiState(load = UiState.Success(previewProducts)),
        favoriteIds = setOf(1),
    )
}
