package com.kun.flowershop.screen

import com.kun.flowershop.*
import com.kun.flowershop.R
import com.kun.flowershop.data.*
import com.kun.flowershop.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

class NotificationsViewModel(private val repository: NotificationsRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<NotificationRow>>>(UiState.Loading)
    val state: StateFlow<UiState<List<NotificationRow>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            repository.load().fold(
                onSuccess = { _state.value = UiState.Success(it) },
                onFailure = { _state.value = UiState.Error(it) },
            )
        }
    }
}

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        KunTopBar(title = stringResource(R.string.notifications_title), onBack = onBack)
        Spacer(Modifier.height(18.dp))

        when (val load = state) {
            UiState.Loading -> LoadingView()
            is UiState.Error -> ErrorView(
                title = stringResource(R.string.notifications_error),
                detail = load.throwable.message,
                onRetry = viewModel::load,
            )
            is UiState.Success<*> -> {
                val list = (load as UiState.Success<List<NotificationRow>>).data
                if (list.isEmpty()) {
                    EmptyView(stringResource(R.string.notifications_empty))
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(list, key = { it.id }) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(KunColors.White)
                                    .padding(14.dp),
                            ) {
                                Text(
                                    text = item.title,
                                    style = kunText(15f, FontWeight.Medium, KunColors.Green),
                                )
                                if (!item.body.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = item.body,
                                        style = kunText(12f, color = KunColors.TextGray, lineHeight = 16f),
                                    )
                                }
                                if (!item.createdAt.isNullOrBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = formatDate(item.createdAt),
                                        style = kunText(10f, color = KunColors.TextMuted),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
