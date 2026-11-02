package com.kun.flowershop.screen

import com.kun.flowershop.*
import com.kun.flowershop.R
import com.kun.flowershop.data.*
import com.kun.flowershop.ui.*
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val delivery: DeliveryType = DeliveryType.PICKUP,
    val payment: PaymentType = PaymentType.CARD,
    val address: String = "",
    val comment: String = "",
    val processing: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val errorDetail: String? = null,
    val orderId: Int? = null,
)

class CheckoutViewModel(
    private val products: ProductRepository,
    private val cart: CartRepository,
    private val orders: OrdersRepository,
    private val addresses: AddressRepository,
    cards: CardsRepository,
    private val notifications: NotificationsRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    val productsState: StateFlow<UiState<List<Product>>> = products.products
    val sizes: StateFlow<Map<Int, List<ProductSize>>> = products.sizes
    val cartItems: StateFlow<Map<CartKey, Int>> = cart.items
    val promo: StateFlow<PromoCode?> = cart.promo
    val savedAddresses: StateFlow<List<AddressRow>> = addresses.items
    val savedCards: StateFlow<List<SavedCard>> = cards.cards

    init {
        viewModelScope.launch {
            products.load()
            addresses.load()
            val last = addresses.items.value.lastOrNull()?.address
            if (last != null) {
                _state.update { if (it.address.isBlank()) it.copy(address = last) else it }
            }
        }
    }

    fun onDelivery(value: DeliveryType) = _state.update { it.copy(delivery = value, errorRes = null, errorDetail = null) }
    fun onPayment(value: PaymentType) = _state.update { it.copy(payment = value) }
    fun onAddress(value: String) = _state.update { it.copy(address = value, errorRes = null, errorDetail = null) }
    fun onComment(value: String) = _state.update { it.copy(comment = value) }
    fun onOrderHandled() = _state.update { it.copy(orderId = null) }

    fun pay(lines: List<CartLine>, total: Double) {
        val s = _state.value
        if (s.processing) return
        if (lines.isEmpty()) {
            _state.update { it.copy(errorRes = R.string.err_cart_empty) }
            return
        }
        if (s.delivery == DeliveryType.DELIVERY && s.address.isBlank()) {
            _state.update { it.copy(errorRes = R.string.err_address) }
            return
        }
        _state.update { it.copy(processing = true, errorRes = null, errorDetail = null) }
        viewModelScope.launch {
            delay(PAYMENT_STUB_MS)
            val name = runCatching { profiles.currentDisplayName() }.getOrNull()
            val address = if (s.delivery == DeliveryType.DELIVERY) s.address.trim() else null
            orders.createOrder(
                recipientName = name,
                comment = s.comment.trim().ifEmpty { null },
                delivery = s.delivery,
                address = address,
                payment = s.payment,
                lines = lines,
                total = total,
            ).fold(
                onSuccess = { id ->
                    if (address != null) addresses.addIfMissing(address)
                    notifications.add(
                        "Заказ №$id оформлен",
                        "Мы уже собираем ваш букет и скоро свяжемся с вами",
                    )
                    cart.clear()
                    _state.update { it.copy(processing = false, orderId = id) }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            processing = false,
                            errorRes = R.string.err_order,
                            errorDetail = e.message?.take(200),
                        )
                    }
                },
            )
        }
    }

    private companion object {
        const val PAYMENT_STUB_MS = 1500L
    }
}

@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onOpenPayments: () -> Unit,
    onOrderCreated: () -> Unit,
    viewModel: CheckoutViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val load by viewModel.productsState.collectAsStateWithLifecycle()
    val sizes by viewModel.sizes.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val promo by viewModel.promo.collectAsStateWithLifecycle()
    val addresses by viewModel.savedAddresses.collectAsStateWithLifecycle()
    val cards by viewModel.savedCards.collectAsStateWithLifecycle()

    LaunchedEffect(state.orderId) {
        if (state.orderId != null) {
            viewModel.onOrderHandled()
            onOrderCreated()
        }
    }

    val products = (load as? UiState.Success<List<Product>>)?.data.orEmpty()
    val lines = buildCartLines(products, sizes, cartItems)
    val fee = if (state.delivery == DeliveryType.DELIVERY) DELIVERY_FEE else 0.0
    val summary = cartSummary(lines, promo, fee)

    CheckoutContent(
        state = state,
        savedAddresses = addresses,
        card = cards.lastOrNull(),
        promo = promo,
        summary = summary,
        deliveryFee = fee,
        onBack = onBack,
        onDelivery = viewModel::onDelivery,
        onPayment = viewModel::onPayment,
        onAddress = viewModel::onAddress,
        onComment = viewModel::onComment,
        onOpenPayments = onOpenPayments,
        onPay = { viewModel.pay(lines, summary.total) },
    )
}

@Composable
fun CheckoutContent(
    state: CheckoutUiState,
    savedAddresses: List<AddressRow>,
    card: SavedCard?,
    promo: PromoCode?,
    summary: CartSummary,
    deliveryFee: Double,
    onBack: () -> Unit = {},
    onDelivery: (DeliveryType) -> Unit = {},
    onPayment: (PaymentType) -> Unit = {},
    onAddress: (String) -> Unit = {},
    onComment: (String) -> Unit = {},
    onOpenPayments: () -> Unit = {},
    onPay: () -> Unit = {},
) {
    val groupShape = RoundedCornerShape(10.dp)

    Box(Modifier.fillMaxSize().background(KunColors.Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            KunTopBar(title = stringResource(R.string.checkout_title), onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(18.dp))
                SectionTitle(stringResource(R.string.checkout_delivery))
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(groupShape)
                        .border(1.dp, KunColors.Line, groupShape),
                ) {
                    ChoiceRow(
                        selected = state.delivery == DeliveryType.PICKUP,
                        title = stringResource(R.string.checkout_pickup),
                        subtitle = stringResource(R.string.checkout_pickup_note),
                        onClick = { onDelivery(DeliveryType.PICKUP) },
                    )
                    HorizontalDivider(thickness = 1.dp, color = KunColors.Line)
                    ChoiceRow(
                        selected = state.delivery == DeliveryType.DELIVERY,
                        title = stringResource(R.string.checkout_delivery_option),
                        subtitle = stringResource(R.string.checkout_delivery_note),
                        onClick = { onDelivery(DeliveryType.DELIVERY) },
                    )
                    HorizontalDivider(thickness = 1.dp, color = KunColors.Line)
                    AddressRow(
                        editable = state.delivery == DeliveryType.DELIVERY,
                        value = if (state.delivery == DeliveryType.DELIVERY) {
                            state.address
                        } else {
                            stringResource(R.string.checkout_shop_address)
                        },
                        onValueChange = onAddress,
                    )
                }
                if (state.delivery == DeliveryType.DELIVERY && savedAddresses.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(savedAddresses, key = { it.id }) { saved ->
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(KunColors.Field)
                                    .clickable { onAddress(saved.address) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = saved.title?.takeIf { it.isNotBlank() } ?: saved.address,
                                    style = kunText(11f, FontWeight.Medium, KunColors.Green),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))
                SectionTitle(stringResource(R.string.checkout_payment))
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(groupShape)
                        .border(1.dp, KunColors.Line, groupShape),
                ) {
                    PaymentCardRow(
                        selected = state.payment == PaymentType.CARD,
                        subtitle = card?.let { it.brand + " ****" + it.last4 }
                            ?: stringResource(R.string.pay_card_test),
                        onClick = { onPayment(PaymentType.CARD) },
                        onOpen = onOpenPayments,
                    )
                    HorizontalDivider(thickness = 1.dp, color = KunColors.Line)
                    ChoiceRow(
                        selected = state.payment == PaymentType.SBERPAY,
                        title = stringResource(R.string.pay_sberpay),
                        subtitle = null,
                        onClick = { onPayment(PaymentType.SBERPAY) },
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.checkout_comment),
                    style = kunText(14f, FontWeight.Medium, KunColors.Green),
                )
                Spacer(Modifier.height(10.dp))
                KunOutlinedField(
                    value = state.comment,
                    onValueChange = onComment,
                    placeholder = stringResource(R.string.checkout_comment_hint),
                )

                Spacer(Modifier.height(18.dp))
                if (deliveryFee > 0) {
                    SummaryLine(stringResource(R.string.checkout_delivery_fee), formatMoney(deliveryFee))
                }
                if (summary.discount > 0 && promo != null) {
                    SummaryLine(
                        stringResource(R.string.cart_discount) + " (" + promo.code + ")",
                        "−" + formatMoney(summary.discount),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.cart_total), style = kunText(16f, FontWeight.SemiBold))
                    Spacer(Modifier.weight(1f))
                    Text(formatMoney(summary.total), style = kunText(18f, FontWeight.SemiBold))
                }

                FormMessage(
                    errorRes = state.errorRes,
                    detail = state.errorDetail,
                    infoRes = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                Spacer(Modifier.height(12.dp))
            }

            KunButton(
                text = stringResource(R.string.checkout_pay),
                onClick = onPay,
                enabled = !state.processing,
                height = 48.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
            )
        }

        if (state.processing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KunColors.Scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {},
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(KunColors.Background)
                        .padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = KunColors.Orange)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.checkout_processing),
                        style = kunText(14f, FontWeight.Medium, KunColors.Green),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = kunText(18f, FontWeight.SemiBold, KunColors.Green))
}

@Composable
private fun SummaryLine(title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = kunText(13f, color = KunColors.TextGray))
        Spacer(Modifier.weight(1f))
        Text(value, style = kunText(13f, color = KunColors.TextGray))
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ChoiceRow(
    selected: Boolean,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(if (selected) KunColors.Field else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioMark(selected = selected)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = kunText(15f, FontWeight.Medium, KunColors.Green))
            if (subtitle != null) {
                Text(subtitle, style = kunText(10f, color = KunColors.TextGray))
            }
        }
    }
}

@Composable
private fun PaymentCardRow(
    selected: Boolean,
    subtitle: String,
    onClick: () -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(if (selected) KunColors.Field else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.CreditCard,
            contentDescription = null,
            tint = KunColors.Green,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.pay_card),
                style = kunText(15f, FontWeight.Medium, KunColors.Green),
            )
            Text(subtitle, style = kunText(10f, color = KunColors.TextGray), maxLines = 1)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onOpen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = stringResource(R.string.cd_open),
                tint = KunColors.Arrow,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AddressRow(editable: Boolean, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = KunColors.Green,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.checkout_address),
                style = kunText(9f, color = KunColors.TextMuted),
            )
            Box(contentAlignment = Alignment.CenterStart) {
                if (editable && value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.checkout_address_hint),
                        style = kunText(12f, color = KunColors.Placeholder),
                        maxLines = 1,
                    )
                }
                if (editable) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = kunText(12f, color = KunColors.Green),
                        cursorBrush = SolidColor(KunColors.Black),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = value,
                        style = kunText(12f, color = KunColors.Green),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun ThanksScreen(onHome: () -> Unit) {
    BackHandler(onBack = onHome)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background)
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        // Замените файл res/drawable-nodpi/img_thanks.png на своё изображение
        Image(
            painter = painterResource(R.drawable.img_thanks),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.thanks_title),
            style = kunText(24f, FontWeight.SemiBold, KunColors.Green),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.thanks_text),
            style = kunText(13f, color = KunColors.TextGray, lineHeight = 18f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        KunButton(
            text = stringResource(R.string.thanks_home),
            onClick = onHome,
            height = 46.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CheckoutScreenPreview() {
    CheckoutContent(
        state = CheckoutUiState(),
        savedAddresses = emptyList(),
        card = SavedCard("МИР", "1274"),
        promo = null,
        summary = CartSummary(10470.0, 0.0, 10470.0),
        deliveryFee = 0.0,
    )
}

@Preview(showBackground = true)
@Composable
fun ThanksScreenPreview() {
    ThanksScreen(onHome = {})
}
