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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProductViewModel(
    private val products: ProductRepository,
    favorites: FavoritesRepository,
    private val cartRepository: CartRepository,
    auth: AuthRepository,
) : ShopViewModel(favorites, cartRepository, auth) {
    val productsState: StateFlow<UiState<List<Product>>> = products.products
    val sizes: StateFlow<Map<Int, List<ProductSize>>> = products.sizes

    init {
        viewModelScope.launch { products.load() }
    }

    fun addSized(productId: Int, sizeId: Int?) = cartRepository.add(productId, sizeId)

    fun reload() {
        viewModelScope.launch { products.load(force = true) }
    }
}

@Composable
fun ProductScreen(
    productId: Int,
    onBack: () -> Unit,
    onAddedToCart: () -> Unit,
    onRequireLogin: () -> Unit,
    viewModel: ProductViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val load by viewModel.productsState.collectAsStateWithLifecycle()
    val sizesMap by viewModel.sizes.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    when (val state = load) {
        UiState.Loading -> Box(Modifier.fillMaxSize().background(KunColors.Background)) {
            LoadingView()
            BackCircle(onBack, Modifier.padding(top = 56.dp, start = 28.dp))
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().background(KunColors.Background)) {
            ErrorView(
                title = stringResource(R.string.catalog_error),
                detail = state.throwable.message,
                onRetry = viewModel::reload,
            )
            BackCircle(onBack, Modifier.padding(top = 56.dp, start = 28.dp))
        }
        is UiState.Success<*> -> {
            val product = (state as UiState.Success<List<Product>>).data.firstOrNull { it.id == productId }
            if (product == null) {
                Box(Modifier.fillMaxSize().background(KunColors.Background)) {
                    EmptyView(stringResource(R.string.product_not_found))
                    BackCircle(onBack, Modifier.padding(top = 56.dp, start = 28.dp))
                }
            } else {
                ProductContent(
                    product = product,
                    sizes = sizesMap[product.id].orEmpty(),
                    isFavorite = product.id in favoriteIds,
                    onBack = onBack,
                    onFavorite = { viewModel.toggleFavorite(product.id, onRequireLogin) },
                    onAddToCart = { sizeId ->
                        viewModel.addSized(product.id, sizeId)
                        onAddedToCart()
                    },
                )
            }
        }
    }
}

@Composable
fun ProductContent(
    product: Product,
    sizes: List<ProductSize>,
    isFavorite: Boolean,
    onBack: () -> Unit = {},
    onFavorite: () -> Unit = {},
    onAddToCart: (Int?) -> Unit = {},
) {
    var selected by remember(product.id, sizes.size) { mutableStateOf(0) }
    val currentSize = sizes.getOrNull(selected)
    val price = currentSize?.price ?: product.price
    val available = (product.stock ?: 1) > 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 44.dp, start = 15.dp, end = 15.dp)
                    .fillMaxWidth()
                    .height(290.dp)
                    .clip(RoundedCornerShape(30.dp)),
            ) {
                ProductImage(product, Modifier.fillMaxSize())
                BackCircle(onBack, Modifier.align(Alignment.TopStart).padding(14.dp))
                FavoriteButton(
                    isFavorite = isFavorite,
                    onClick = onFavorite,
                    onImage = true,
                    size = 32.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = product.name,
                style = kunText(32f, FontWeight.Medium, lineHeight = 38f),
                modifier = Modifier.padding(horizontal = 26.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = formatMoney(price), style = kunText(20f, FontWeight.Medium))
                Spacer(Modifier.weight(1f))
                if (product.rating != null) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = KunColors.Orange,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", product.rating),
                        style = kunText(16f, FontWeight.Medium),
                    )
                }
            }

            if (!product.description.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = product.description,
                    style = kunText(14f, color = KunColors.TextSoft, lineHeight = 20f),
                    modifier = Modifier.padding(horizontal = 26.dp),
                )
            }

            if (sizes.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.product_size),
                    style = kunText(16f, FontWeight.Medium),
                    modifier = Modifier.padding(horizontal = 26.dp),
                )
                Spacer(Modifier.height(12.dp))
                if (sizes.size <= 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 26.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        sizes.forEachIndexed { index, size ->
                            SizeCard(
                                size = size,
                                selected = index == selected,
                                onClick = { selected = index },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 26.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(sizes.size) { index ->
                            SizeCard(
                                size = sizes[index],
                                selected = index == selected,
                                onClick = { selected = index },
                                modifier = Modifier.width(100.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        KunButton(
            text = stringResource(if (available) R.string.add_to_cart else R.string.out_of_stock),
            onClick = { onAddToCart(currentSize?.id) },
            enabled = available,
            height = 44.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 50.dp, end = 50.dp, top = 8.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun BackCircle(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(KunColors.White.copy(alpha = 0.85f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            tint = KunColors.Black,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SizeCard(
    size: ProductSize,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .height(84.dp)
            .shadow(3.dp, shape)
            .clip(shape)
            .background(KunColors.Field)
            .then(if (selected) Modifier.border(1.dp, KunColors.Black, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = size.name,
                style = kunText(12f, FontWeight.Medium),
                maxLines = 1,
            )
            if (size.pieces != null) {
                Text(
                    text = stringResource(R.string.pieces_fmt, size.pieces),
                    style = kunText(9f, color = KunColors.TextMuted),
                    maxLines = 1,
                )
            }
        }
        Text(
            text = formatMoney(size.price),
            style = kunText(11f, FontWeight.Bold),
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProductScreenPreview() {
    ProductContent(
        product = Product(1, 1, "bouquet", "Пионы", "Нежный букет из свежих пион.\nИдеальный подарок для любого случая.", 11490.0, null, 5, 5.0),
        sizes = listOf(
            ProductSize(1, 1, "Малый", 15, 7490.0),
            ProductSize(2, 1, "Средний", 25, 11490.0),
            ProductSize(3, 1, "Большой", 35, 15490.0),
        ),
        isFavorite = false,
    )
}
