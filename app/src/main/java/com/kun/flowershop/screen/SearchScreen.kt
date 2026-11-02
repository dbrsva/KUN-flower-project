package com.kun.flowershop.screen

import com.kun.flowershop.*
import com.kun.flowershop.R
import com.kun.flowershop.data.*
import com.kun.flowershop.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val products: ProductRepository,
    favorites: FavoritesRepository,
    cart: CartRepository,
    auth: AuthRepository,
) : ShopViewModel(favorites, cart, auth) {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    val productsState: StateFlow<UiState<List<Product>>> = products.products

    init {
        viewModelScope.launch { products.load() }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun reload() {
        viewModelScope.launch { products.load(force = true) }
    }
}

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onProductClick: (Product) -> Unit,
    onRequireLogin: () -> Unit,
    viewModel: SearchViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    val query by viewModel.query.collectAsStateWithLifecycle()
    val load by viewModel.productsState.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, start = 22.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackArrow(onClick = onBack)
            Spacer(Modifier.width(14.dp))
            KunSearchField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.home_search_placeholder),
                iconTint = KunColors.Green,
                placeholderColor = KunColors.CatalogPlaceholder,
                placeholderSize = 10f,
                focusRequester = focusRequester,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(18.dp))

        when (val state = load) {
            UiState.Loading -> LoadingView()
            is UiState.Error -> ErrorView(
                title = stringResource(R.string.catalog_error),
                detail = state.throwable.message,
                onRetry = viewModel::reload,
            )
            is UiState.Success<*> -> {
                val q = query.trim()
                if (q.isEmpty()) {
                    EmptyView(stringResource(R.string.search_hint_start))
                } else {
                    val found = (state as UiState.Success<List<Product>>).data.filter {
                        it.name.contains(q, ignoreCase = true) ||
                            (it.description?.contains(q, ignoreCase = true) == true)
                    }
                    if (found.isEmpty()) {
                        EmptyView(stringResource(R.string.catalog_empty))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 25.dp, end = 25.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            items(found, key = { it.id }) { product ->
                                ProductListCard(
                                    product = product,
                                    isFavorite = product.id in favoriteIds,
                                    onClick = { onProductClick(product) },
                                    onFavorite = { viewModel.toggleFavorite(product.id, onRequireLogin) },
                                    onAddToCart = {
                                        viewModel.addToCart(product.id)
                                        context.toast(context.getString(R.string.added_to_cart))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
