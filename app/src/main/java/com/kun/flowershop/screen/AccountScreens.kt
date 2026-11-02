package com.kun.flowershop.screen

import com.kun.flowershop.*
import com.kun.flowershop.R
import com.kun.flowershop.data.*
import com.kun.flowershop.ui.*
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonalUiState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val passwordRepeat: String = "",
    val busy: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val errorDetail: String? = null,
    @StringRes val infoRes: Int? = null,
)

class PersonalViewModel(
    private val profiles: ProfileRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(
        PersonalUiState(
            email = auth.currentEmail().orEmpty(),
            phone = profiles.currentPhone().orEmpty(),
        )
    )
    val state: StateFlow<PersonalUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val name = runCatching { profiles.currentDisplayName() }.getOrNull().orEmpty()
            _state.update { it.copy(name = name) }
        }
    }

    private fun edit(block: PersonalUiState.() -> PersonalUiState) =
        _state.update { it.block().copy(errorRes = null, errorDetail = null, infoRes = null) }

    fun onName(value: String) = edit { copy(name = value) }
    fun onPhone(value: String) = edit { copy(phone = value) }
    fun onPassword(value: String) = edit { copy(password = value) }
    fun onPasswordRepeat(value: String) = edit { copy(passwordRepeat = value) }

    fun saveProfile() {
        val s = _state.value
        if (s.busy) return
        if (s.name.isBlank()) {
            _state.update { it.copy(errorRes = R.string.err_empty_name, infoRes = null) }
            return
        }
        _state.update { it.copy(busy = true, errorRes = null, errorDetail = null, infoRes = null) }
        viewModelScope.launch {
            profiles.saveProfile(s.name.trim(), s.phone.trim()).fold(
                onSuccess = { _state.update { it.copy(busy = false, infoRes = R.string.info_saved) } },
                onFailure = { e ->
                    _state.update {
                        it.copy(busy = false, errorRes = R.string.err_save, errorDetail = e.message?.take(200))
                    }
                },
            )
        }
    }

    fun changePassword() {
        val s = _state.value
        if (s.busy) return
        if (s.password.length < MIN_PASSWORD) {
            _state.update { it.copy(errorRes = R.string.err_short_password, infoRes = null) }
            return
        }
        if (s.password != s.passwordRepeat) {
            _state.update { it.copy(errorRes = R.string.err_password_mismatch, infoRes = null) }
            return
        }
        _state.update { it.copy(busy = true, errorRes = null, errorDetail = null, infoRes = null) }
        viewModelScope.launch {
            auth.changePassword(s.password).fold(
                onSuccess = {
                    _state.update {
                        it.copy(busy = false, password = "", passwordRepeat = "", infoRes = R.string.info_password_changed)
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(busy = false, errorRes = R.string.err_save, errorDetail = e.message?.take(200))
                    }
                },
            )
        }
    }

    private companion object {
        const val MIN_PASSWORD = 6
    }
}

@Composable
fun PersonalScreen(
    onBack: () -> Unit,
    viewModel: PersonalViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background)
            .imePadding(),
    ) {
        KunTopBar(title = stringResource(R.string.personal_title), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 29.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                KunTextField(
                    value = state.name,
                    onValueChange = viewModel::onName,
                    placeholder = stringResource(R.string.field_name),
                    leadingIcon = Icons.Outlined.Person,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy,
                )
                KunTextField(
                    value = state.phone,
                    onValueChange = viewModel::onPhone,
                    placeholder = stringResource(R.string.field_phone),
                    leadingIcon = Icons.Outlined.Phone,
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy,
                )
                KunTextField(
                    value = state.email,
                    onValueChange = {},
                    placeholder = stringResource(R.string.field_email),
                    leadingIcon = Icons.Outlined.Email,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                )
            }
            Spacer(Modifier.height(18.dp))
            KunButton(
                text = stringResource(R.string.personal_save),
                onClick = viewModel::saveProfile,
                loading = state.busy,
                height = 44.dp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.personal_password_title),
                style = kunText(18f, FontWeight.SemiBold, KunColors.Green),
            )
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                KunTextField(
                    value = state.password,
                    onValueChange = viewModel::onPassword,
                    placeholder = stringResource(R.string.personal_new_password),
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy,
                )
                KunTextField(
                    value = state.passwordRepeat,
                    onValueChange = viewModel::onPasswordRepeat,
                    placeholder = stringResource(R.string.field_password_repeat),
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    onImeAction = viewModel::changePassword,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.busy,
                )
            }
            Spacer(Modifier.height(18.dp))
            KunButton(
                text = stringResource(R.string.personal_change_password),
                onClick = viewModel::changePassword,
                loading = state.busy,
                height = 44.dp,
                modifier = Modifier.fillMaxWidth(),
            )

            FormMessage(
                errorRes = state.errorRes,
                detail = state.errorDetail,
                infoRes = state.infoRes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

class AddressesViewModel(private val repository: AddressRepository) : ViewModel() {
    val items: StateFlow<List<AddressRow>> = repository.items

    init {
        viewModelScope.launch { repository.load() }
    }

    fun add(title: String, address: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(repository.add(title.trim().ifEmpty { null }, address).isSuccess)
        }
    }

    fun remove(id: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.remove(id).isSuccess) }
    }
}

@Composable
fun AddressesScreen(
    onBack: () -> Unit,
    viewModel: AddressesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var addressToDelete by remember { mutableStateOf<AddressRow?>(null) }
    var title by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        KunTopBar(title = stringResource(R.string.addresses_title), onBack = onBack)
        Spacer(Modifier.height(18.dp))

        Box(Modifier.weight(1f)) {
            if (items.isEmpty()) {
                EmptyView(stringResource(R.string.addresses_empty))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(KunColors.White)
                                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = KunColors.Green,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                if (!item.title.isNullOrBlank()) {
                                    Text(
                                        text = item.title,
                                        style = kunText(14f, FontWeight.Medium, KunColors.Green),
                                        maxLines = 1,
                                    )
                                }
                                Text(
                                    text = item.address,
                                    style = kunText(12f, color = KunColors.TextGray),
                                    maxLines = 2,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { addressToDelete = item },
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
            }
        }

        KunButton(
            text = stringResource(R.string.address_add),
            onClick = { showDialog = true },
            height = 46.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
        )
    }

    addressToDelete?.let { item ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.confirm_delete_address_title),
            text = stringResource(R.string.confirm_delete_address_text, item.title?.takeIf { it.isNotBlank() } ?: item.address),
            onConfirm = {
                addressToDelete = null
                viewModel.remove(item.id) { ok ->
                    if (!ok) context.toast(context.getString(R.string.err_save))
                }
            },
            onDismiss = { addressToDelete = null },
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = KunColors.Background,
            title = {
                Text(
                    text = stringResource(R.string.address_add),
                    style = kunText(18f, FontWeight.SemiBold, KunColors.Green),
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KunOutlinedField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = stringResource(R.string.address_title_hint),
                        imeAction = ImeAction.Next,
                    )
                    KunOutlinedField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = stringResource(R.string.address_value_hint),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (address.isNotBlank()) {
                            viewModel.add(title, address) { ok ->
                                if (!ok) context.toast(context.getString(R.string.err_save))
                            }
                            title = ""
                            address = ""
                            showDialog = false
                        }
                    },
                ) { Text(stringResource(R.string.personal_save), color = KunColors.Green) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel), color = KunColors.TextGray)
                }
            },
        )
    }
}

class PaymentsViewModel(private val repository: CardsRepository) : ViewModel() {
    val cards: StateFlow<List<SavedCard>> = repository.cards

    fun add(number: String) = repository.add(number)
    fun remove(card: SavedCard) = repository.remove(card)
}

@Composable
fun PaymentsScreen(
    onBack: () -> Unit,
    viewModel: PaymentsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var number by remember { mutableStateOf("") }
    var cardToDelete by remember { mutableStateOf<SavedCard?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        KunTopBar(title = stringResource(R.string.payments_title), onBack = onBack)
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.payments_note),
            style = kunText(11f, color = KunColors.TextGray, lineHeight = 15f),
            modifier = Modifier.padding(horizontal = 26.dp),
        )
        Spacer(Modifier.height(16.dp))

        Box(Modifier.weight(1f)) {
            if (cards.isEmpty()) {
                EmptyView(stringResource(R.string.payments_empty))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(cards, key = { it.brand + it.last4 }) { card ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(KunColors.White)
                                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CreditCard,
                                contentDescription = null,
                                tint = KunColors.Green,
                                modifier = Modifier.size(26.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = card.brand + " ****" + card.last4,
                                style = kunText(15f, FontWeight.Medium, KunColors.Green),
                                modifier = Modifier.weight(1f),
                            )
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { cardToDelete = card },
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
            }
        }

        KunButton(
            text = stringResource(R.string.card_add),
            onClick = { showDialog = true },
            height = 46.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
        )
    }

    cardToDelete?.let { card ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.confirm_delete_card_title),
            text = stringResource(R.string.confirm_delete_card_text, card.brand + " ****" + card.last4),
            onConfirm = {
                cardToDelete = null
                viewModel.remove(card)
            },
            onDismiss = { cardToDelete = null },
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = KunColors.Background,
            title = {
                Text(
                    text = stringResource(R.string.card_add),
                    style = kunText(18f, FontWeight.SemiBold, KunColors.Green),
                )
            },
            text = {
                KunOutlinedField(
                    value = number,
                    onValueChange = { number = it.filter { c -> c.isDigit() || c == ' ' }.take(23) },
                    placeholder = stringResource(R.string.card_number_hint),
                    keyboardType = KeyboardType.Number,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val digits = number.count { it.isDigit() }
                        if (digits in 13..19) {
                            viewModel.add(number)
                            number = ""
                            showDialog = false
                        } else {
                            context.toast(context.getString(R.string.card_number_error))
                        }
                    },
                ) { Text(stringResource(R.string.personal_save), color = KunColors.Green) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel), color = KunColors.TextGray)
                }
            },
        )
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val phone = stringResource(R.string.help_phone_value)
    val email = stringResource(R.string.help_email_value)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        KunTopBar(title = stringResource(R.string.help_title), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(KunColors.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HelpLine(stringResource(R.string.help_label_address), stringResource(R.string.checkout_shop_address))
                HelpLine(stringResource(R.string.help_label_phone), phone)
                HelpLine(stringResource(R.string.help_label_email), email)
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KunButton(
                    text = stringResource(R.string.help_call),
                    onClick = {
                        val number = phone.filter { it.isDigit() || it == '+' }
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                        }
                    },
                    height = 44.dp,
                    modifier = Modifier.weight(1f),
                )
                KunButton(
                    text = stringResource(R.string.help_write),
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")))
                        }
                    },
                    height = 44.dp,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.help_faq),
                style = kunText(18f, FontWeight.SemiBold, KunColors.Green),
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FaqItem(stringResource(R.string.faq_q1), stringResource(R.string.faq_a1))
                FaqItem(stringResource(R.string.faq_q2), stringResource(R.string.faq_a2))
                FaqItem(stringResource(R.string.faq_q3), stringResource(R.string.faq_a3))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HelpLine(label: String, value: String) {
    Column {
        Text(text = label, style = kunText(10f, color = KunColors.TextMuted))
        Text(text = value, style = kunText(14f, FontWeight.Medium, KunColors.Green))
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KunColors.White)
            .clickable { expanded = !expanded }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = question,
                style = kunText(14f, FontWeight.Medium, KunColors.Green),
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = KunColors.Arrow,
                modifier = Modifier.size(22.dp),
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = answer,
                style = kunText(12f, color = KunColors.TextGray, lineHeight = 17f),
            )
        }
    }
}
