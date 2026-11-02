package com.kun.flowershop.ui

import com.kun.flowershop.*
import com.kun.flowershop.R
import com.kun.flowershop.data.*
import com.kun.flowershop.screen.*
import android.content.Context
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun KunLogoMark(width: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.kun_logo),
        contentDescription = null,
        modifier = modifier.width(width),
        contentScale = ContentScale.FillWidth,
    )
}

@Composable
fun KunBrandHeader(logoWidth: Dp, modifier: Modifier = Modifier) {
    Column(modifier) {
        KunLogoMark(width = logoWidth)
        Text(
            text = stringResource(R.string.brand_tagline),
            style = kunText(9f, color = KunColors.Brown),
        )
    }
}

@Composable
fun KunButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true,
    height: androidx.compose.ui.unit.Dp = 45.dp,
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(CircleShape)
            .background(KunColors.Orange)
            .alpha(if (enabled) 1f else 0.6f)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = KunColors.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = text, style = kunText(15f, FontWeight.SemiBold, KunColors.White))
        }
    }
}

@Composable
fun KunTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    enabled: Boolean = true,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val textStyle = kunText(11f)

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(KunColors.Field)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = KunColors.Black,
        )
        Spacer(Modifier.width(14.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(text = placeholder, style = textStyle)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(KunColors.Black),
                visualTransformation = if (isPassword && !passwordVisible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                    imeAction = imeAction,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onImeAction() },
                    onDone = { onImeAction() },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (isPassword) {
            Icon(
                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = stringResource(
                    if (passwordVisible) R.string.cd_hide_password else R.string.cd_show_password
                ),
                modifier = Modifier
                    .size(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { passwordVisible = !passwordVisible },
                tint = KunColors.Black,
            )
        }
    }
}

enum class KunTab(
    val route: String,
    @DrawableRes val icon: Int,
    @DrawableRes val iconSelected: Int,
    val labelRes: Int,
) {
    Home(Routes.HOME, R.drawable.ic_nav_home, R.drawable.ic_nav_home_filled, R.string.tab_home),
    Catalog(Routes.CATALOG, R.drawable.ic_nav_grid, R.drawable.ic_nav_grid_filled, R.string.tab_catalog),
    Favorites(Routes.FAVORITES, R.drawable.ic_nav_heart, R.drawable.ic_nav_heart_filled, R.string.tab_favorites),
    Cart(Routes.CART, R.drawable.ic_nav_bag, R.drawable.ic_nav_bag_filled, R.string.tab_cart);

    companion object {
        fun fromRoute(route: String?): KunTab? = entries.firstOrNull { it.route == route }
    }
}

@Composable
fun KunBottomBar(
    selected: KunTab?,
    onSelect: (KunTab) -> Unit,
    modifier: Modifier = Modifier,
    cartCount: Int = 0,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(KunColors.Background)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            thickness = 1.dp,
            color = KunColors.Divider,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 38.dp, end = 34.dp, top = 17.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KunTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Box {
                    Image(
                        painter = painterResource(if (isSelected) tab.iconSelected else tab.icon),
                        contentDescription = stringResource(tab.labelRes),
                        colorFilter = ColorFilter.tint(KunColors.Black),
                        modifier = Modifier
                            .height(31.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSelect(tab) },
                    )
                    if (tab == KunTab.Cart && cartCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                .clip(CircleShape)
                                .background(KunColors.Orange)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (cartCount > 99) "99+" else cartCount.toString(),
                                style = kunText(10f, FontWeight.Bold, KunColors.White),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = KunColors.Orange)
    }
}

@Composable
fun EmptyView(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = kunText(14f, color = KunColors.TextGray))
    }
}

@Composable
fun ErrorView(
    title: String,
    detail: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = kunText(15f, color = KunColors.Green),
            textAlign = TextAlign.Center,
        )
        if (!detail.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = detail,
                style = kunText(10f, color = KunColors.TextMuted),
                textAlign = TextAlign.Center,
                maxLines = 4,
            )
        }
        Spacer(Modifier.height(16.dp))
        KunButton(
            text = stringResource(R.string.retry),
            onClick = onRetry,
            modifier = Modifier.width(160.dp),
        )
    }
}

@Composable
fun KunSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    editable: Boolean = true,
    iconTint: Color = KunColors.Black,
    placeholderColor: Color = KunColors.Placeholder,
    placeholderSize: Float = 9f,
    focusRequester: FocusRequester? = null,
) {
    val textStyle = kunText(placeholderSize + 1f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(CircleShape)
            .background(KunColors.Field)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = stringResource(R.string.cd_search),
            modifier = Modifier.size(24.dp),
            tint = iconTint,
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = kunText(placeholderSize, color = placeholderColor),
                    maxLines = 1,
                )
            }
            if (editable) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(KunColors.Black),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                )
            }
        }
    }
}

// Фоны экранов: замените файлы bg_*.png в res/drawable-nodpi/ на свои
@Composable
fun ScreenBackground(@DrawableRes image: Int, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(image),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = kunText(28f, FontWeight.SemiBold, KunColors.Green, lineHeight = 34f),
        modifier = modifier.padding(start = 26.dp),
    )
}

@Composable
fun TypeTabs(
    selected: ProductType,
    onSelect: (ProductType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val widths = mapOf(
        ProductType.BOUQUET to 90.dp,
        ProductType.FLOWER to 89.dp,
        ProductType.COMPOSITION to 118.dp,
    )
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ProductType.entries.forEach { type ->
            val isSelected = type == selected
            Box(
                modifier = Modifier
                    .width(widths.getValue(type))
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) KunColors.Green else Color.Transparent)
                    .then(
                        if (isSelected) Modifier
                        else Modifier.border(BorderStroke(1.dp, KunColors.Green), CircleShape)
                    )
                    .clickable { onSelect(type) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = type.title,
                    style = kunText(
                        12f,
                        FontWeight.SemiBold,
                        if (isSelected) KunColors.White else KunColors.Green,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun ProductImage(product: Product, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(ImageUrl.resolve(product.image))
            .crossfade(true)
            .build(),
        contentDescription = product.name,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(KunColors.Field),
    )
}

@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    onImage: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(if (onImage) Modifier.background(KunColors.White.copy(alpha = 0.85f)) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(R.string.tab_favorites),
            tint = if (isFavorite) KunColors.Orange else KunColors.Green,
            modifier = Modifier.size(size * 0.66f),
        )
    }
}

@Composable
fun AddToCartButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 34.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(KunColors.Orange)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.AddShoppingCart,
            contentDescription = stringResource(R.string.cd_add_to_cart),
            tint = KunColors.White,
            modifier = Modifier.size(size * 0.58f),
        )
    }
}

@Composable
fun GuestGate(text: String, onLogin: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = kunText(15f, color = KunColors.TextGray, lineHeight = 21f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        KunButton(
            text = stringResource(R.string.login_short),
            onClick = onLogin,
            modifier = Modifier.width(200.dp),
        )
    }
}

@Composable
fun KunTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 50.dp, start = 22.dp, end = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackArrow(onClick = onBack)
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = kunText(22f, FontWeight.Medium, KunColors.Green),
            maxLines = 1,
        )
    }
}

@Composable
fun KunOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .border(1.dp, KunColors.Line, shape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = kunText(12f, color = KunColors.Placeholder),
                maxLines = 1,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = kunText(12f),
            cursorBrush = SolidColor(KunColors.Black),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction() },
                onNext = { onImeAction() },
                onSearch = { onImeAction() },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun EmptyState(text: String, buttonText: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = kunText(15f, color = KunColors.TextGray, lineHeight = 21f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        KunButton(
            text = buttonText,
            onClick = onClick,
            modifier = Modifier.width(200.dp),
        )
    }
}

@Composable
fun RadioMark(selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(KunColors.Green)
                else Modifier.border(1.dp, KunColors.Green, CircleShape)
            ),
    )
}

/** Диалог подтверждения удаления: запись удаляется только после нажатия «Удалить». */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KunColors.Background,
        title = { Text(title, style = kunText(18f, FontWeight.SemiBold, KunColors.Green)) },
        text = { Text(text, style = kunText(14f, color = KunColors.TextGray, lineHeight = 19f)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.cd_remove), color = Color(0xFFC0392B))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = KunColors.Green)
            }
        },
    )
}
