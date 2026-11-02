package com.kun.flowershop.screen

import com.kun.flowershop.*
import com.kun.flowershop.R
import com.kun.flowershop.data.*
import com.kun.flowershop.ui.*
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrdersViewModel(
    private val orders: OrdersRepository,
    private val products: ProductRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<OrderWithItems>>>(UiState.Loading)
    val state: StateFlow<UiState<List<OrderWithItems>>> = _state.asStateFlow()
    val productsState: StateFlow<UiState<List<Product>>> = products.products

    init {
        viewModelScope.launch { products.load() }
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            orders.load().fold(
                onSuccess = { _state.value = UiState.Success(it) },
                onFailure = { _state.value = UiState.Error(it) },
            )
        }
    }
}

@Composable
fun OrdersScreen(
    onBack: () -> Unit,
    onOpenCatalog: () -> Unit,
    viewModel: OrdersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val productsLoad by viewModel.productsState.collectAsStateWithLifecycle()
    val products = (productsLoad as? UiState.Success<List<Product>>)?.data.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        KunTopBar(title = stringResource(R.string.orders_title), onBack = onBack)
        Spacer(Modifier.height(18.dp))

        when (val load = state) {
            UiState.Loading -> LoadingView()
            is UiState.Error -> ErrorView(
                title = stringResource(R.string.orders_error),
                detail = load.throwable.message,
                onRetry = viewModel::load,
            )
            is UiState.Success<*> -> {
                val list = (load as UiState.Success<List<OrderWithItems>>).data
                if (list.isEmpty()) {
                    EmptyState(
                        text = stringResource(R.string.orders_empty),
                        buttonText = stringResource(R.string.cart_to_catalog),
                        onClick = onOpenCatalog,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(list, key = { it.order.id }) { entry ->
                            OrderCard(entry = entry, products = products)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(entry: OrderWithItems, products: List<Product>) {
    val order = entry.order
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KunColors.White)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.order_number, order.id),
                style = kunText(16f, FontWeight.SemiBold, KunColors.Green),
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(KunColors.Field)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = orderStatusLabel(order.status),
                    style = kunText(11f, FontWeight.SemiBold, KunColors.Green),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        val delivery = if (order.deliveryType == DeliveryType.DELIVERY.code) {
            stringResource(R.string.checkout_delivery_option)
        } else {
            stringResource(R.string.checkout_pickup)
        }
        Text(
            text = formatDate(order.createdAt) + " · " + delivery,
            style = kunText(11f, color = KunColors.TextGray),
        )
        if (!order.address.isNullOrBlank()) {
            Text(
                text = order.address,
                style = kunText(11f, color = KunColors.TextGray),
                maxLines = 2,
            )
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(thickness = 1.dp, color = KunColors.Line)

        entry.items.forEach { item ->
            val product = products.firstOrNull { it.id == item.productId }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KunColors.Field),
                ) {
                    if (product != null) ProductImage(product, Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = product?.name ?: stringResource(R.string.order_item_unknown, item.productId),
                        style = kunText(13f, FontWeight.Medium, KunColors.Green),
                        maxLines = 1,
                    )
                    val sizePart = item.sizeName?.takeIf { it.isNotBlank() }?.let { "$it · " }.orEmpty()
                    Text(
                        text = sizePart + item.quantity + " × " + formatMoney(item.price),
                        style = kunText(11f, color = KunColors.TextGray),
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(thickness = 1.dp, color = KunColors.Line)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.cart_total), style = kunText(14f, FontWeight.SemiBold))
            Spacer(Modifier.weight(1f))
            Text(formatMoney(order.totalPrice), style = kunText(16f, FontWeight.SemiBold))
        }
    }
}
