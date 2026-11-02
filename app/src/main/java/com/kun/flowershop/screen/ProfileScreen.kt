package com.kun.flowershop.screen

import com.kun.flowershop.*
import com.kun.flowershop.R
import com.kun.flowershop.data.*
import com.kun.flowershop.ui.*
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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

data class ProfileUiState(
    val name: String? = null,
    val email: String? = null,
)

class ProfileViewModel(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState(email = auth.currentEmail()))
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val name = runCatching { profiles.currentDisplayName() }.getOrNull()
            _state.value = ProfileUiState(name = name, email = auth.currentEmail())
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            auth.signOut()
            onDone()
        }
    }
}

private val LogoutColor = Color(0xFF8B1A1A)
private val MenuBorder = Color(0xFFD9D3C7)

private class MenuItem(val icon: ImageVector, @StringRes val title: Int, val hasArrow: Boolean = true)

private val menuItems = listOf(
    MenuItem(Icons.Outlined.ShoppingBag, R.string.menu_orders),
    MenuItem(Icons.Outlined.Notifications, R.string.menu_notifications),
    MenuItem(Icons.Outlined.CreditCard, R.string.menu_payment),
    MenuItem(Icons.Outlined.LocationOn, R.string.menu_addresses),
    MenuItem(Icons.AutoMirrored.Outlined.HelpOutline, R.string.menu_help, hasArrow = false),
)

@Composable
fun ProfileScreen(
    onLoggedOut: () -> Unit,
    onMenu: (Int) -> Unit,
    onEditProfile: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    ProfileContent(
        name = state.name,
        email = state.email,
        onMenuClick = onMenu,
        onEditProfile = onEditProfile,
        onLogout = { viewModel.signOut(onDone = onLoggedOut) },
    )
}

@Composable
fun ProfileContent(
    name: String?,
    email: String?,
    onMenuClick: (Int) -> Unit = {},
    onEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    var confirmLogout by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(50.dp))
        ScreenTitle(stringResource(R.string.profile_title))

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditProfile)
                .padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            // Замените файл res/drawable-nodpi/avatar_default.png на своё фото
            Image(
                painter = painterResource(R.drawable.avatar_default),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = name ?: "",
                    style = kunText(24f, FontWeight.Medium, KunColors.Green),
                    maxLines = 1,
                )
                if (!email.isNullOrBlank()) {
                    Text(
                        text = email,
                        style = kunText(13f, color = KunColors.TextGray),
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.cd_edit),
                tint = KunColors.Green,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MenuBorder, RoundedCornerShape(14.dp)),
        ) {
            menuItems.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(thickness = 1.dp, color = MenuBorder)
                MenuRow(item = item, onClick = { onMenuClick(index) })
            }
        }

        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier
                .clickable { confirmLogout = true }
                .padding(horizontal = 26.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = LogoutColor,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = stringResource(R.string.logout),
                style = kunText(18f, FontWeight.Medium, LogoutColor),
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.logout_confirm_title)) },
            text = { Text(stringResource(R.string.logout_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) { Text(stringResource(R.string.logout), color = LogoutColor) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text(stringResource(R.string.cancel), color = KunColors.Green)
                }
            },
        )
    }
}

@Composable
private fun MenuRow(item: MenuItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = KunColors.Black,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(item.title),
            style = kunText(16f, color = KunColors.Green),
            modifier = Modifier.weight(1f),
        )
        if (item.hasArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = KunColors.Arrow,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    ProfileContent(name = "Нина", email = "nina@gmail.com")
}
