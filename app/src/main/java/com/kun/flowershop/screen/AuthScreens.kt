package com.kun.flowershop.screen

import com.kun.flowershop.*
import com.kun.flowershop.R
import com.kun.flowershop.data.*
import com.kun.flowershop.ui.*
import android.annotation.SuppressLint
import android.util.Patterns
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.IOException
import kotlin.math.abs
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SplashViewModel(
    private val authRepository: AuthRepository,
    private val prefs: AppPrefs,
) : ViewModel() {
    private val _showOnboarding = MutableStateFlow<Boolean?>(null)
    val showOnboarding: StateFlow<Boolean?> = _showOnboarding.asStateFlow()

    init {
        viewModelScope.launch {
            val session = async { authRepository.restoreSession() }
            delay(SPLASH_MIN_MS)
            val hasSession = session.await()
            _showOnboarding.value = !(prefs.onboardingSeen || hasSession)
        }
    }

    private companion object {
        const val SPLASH_MIN_MS = 1500L
    }
}

@Composable
fun SplashScreen(
    onFinished: (showOnboarding: Boolean) -> Unit,
    viewModel: SplashViewModel = viewModel(
        factory = AppViewModelProvider.Factory
    ),
) {
    val showOnboarding by viewModel.showOnboarding.collectAsStateWithLifecycle()

    LaunchedEffect(showOnboarding) {
        showOnboarding?.let(onFinished)
    }

    SplashContent()
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun SplashContent() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        val topOffset = maxHeight * (276f / 697f)

        ScreenBackground(R.drawable.bg_splash)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = topOffset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            KunLogoMark(width = 200.dp)

            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(146.dp)
                    .height(2.dp)
                    .background(KunColors.Orange),
            )

            Column(
                modifier = Modifier
                    .width(146.dp)
                    .padding(top = 18.dp),
            ) {
                Text(
                    text = stringResource(R.string.brand_name),
                    style = kunText(
                        50f,
                        FontWeight.Bold,
                        KunColors.Orange,
                        lineHeight = 34f,
                    ),
                )

                Text(
                    text = stringResource(R.string.brand_tagline),
                    style = kunText(
                        9f,
                        color = KunColors.Brown,
                    ),
                )
            }
        }
    }
}

@Preview(
    name = "Splash Screen",
    showBackground = true,
    showSystemUi = true,
    widthDp = 360,
    heightDp = 697,
)
@Composable
private fun SplashScreenPreview() {
    SplashContent()
}

private data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    @DrawableRes val background: Int,
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = remember {
        listOf(
            OnboardingPage(R.string.onb_title_1, R.string.onb_subtitle_1, R.drawable.bg_onboarding_1),
            OnboardingPage(R.string.onb_title_2, R.string.onb_subtitle_2, R.drawable.bg_onboarding_2),
            OnboardingPage(R.string.onb_title_3, R.string.onb_subtitle_3, R.drawable.bg_onboarding_3),
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            val page = pages[index]
            Box(Modifier.fillMaxSize()) {
                ScreenBackground(page.background)
                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(500.dp))
                    Text(
                        text = stringResource(page.title),
                        style = kunText(28f, color = KunColors.TextGray, lineHeight = 36f),
                        modifier = Modifier.padding(start = 36.dp, end = 33.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(page.subtitle),
                        style = kunText(15f, color = KunColors.TextMuted, lineHeight = 18f),
                        modifier = Modifier.padding(start = 36.dp, end = 60.dp),
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.onb_skip),
            style = kunText(13f, color = KunColors.TextGray),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 26.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFinish,
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PageIndicator(count = pages.size, current = pagerState.currentPage)
            Spacer(Modifier.height(39.dp))
            KunButton(
                text = stringResource(R.string.onb_next),
                onClick = {
                    if (pagerState.currentPage < pages.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                },
                height = 44.dp,
                modifier = Modifier.width(242.dp),
            )
            Spacer(Modifier.height(64.dp))
        }
    }
}

@Composable
private fun PageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val distance = abs(index - current)
            val width: Dp by animateDpAsState(
                targetValue = when (distance) {
                    0 -> 45.dp
                    1 -> 25.dp
                    else -> 9.dp
                },
                label = "indicatorWidth",
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(9.dp)
                    .clip(CircleShape)
                    .background(if (distance == 0) KunColors.Orange else KunColors.Dot),
            )
        }
    }
}

@Preview(
    name = "Onboarding Screen",
    showBackground = true,
    showSystemUi = true,
    widthDp = 360,
    heightDp = 800,
)
@Composable
private fun OnboardingScreenPreview() {
    OnboardingScreen(
        onFinish = {}
    )
}

data class AuthUiState(

    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val passwordRepeat: String = "",
    val termsAccepted: Boolean = false,

    val loading: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val errorDetail: String? = null,
    @StringRes val infoRes: Int? = null,

    val authenticated: Boolean = false,
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onName(v: String) = edit { copy(name = v) }
    fun onEmail(v: String) = edit { copy(email = v) }
    fun onPhone(v: String) = edit { copy(phone = v) }
    fun onPassword(v: String) = edit { copy(password = v) }
    fun onPasswordRepeat(v: String) = edit { copy(passwordRepeat = v) }
    fun onTerms(v: Boolean) = edit { copy(termsAccepted = v) }

    fun onAuthenticatedHandled() = _state.update { it.copy(authenticated = false) }

    private fun edit(block: AuthUiState.() -> AuthUiState) =
        _state.update { it.block().copy(errorRes = null, errorDetail = null, infoRes = null) }

    fun login() {
        val s = _state.value
        val email = s.email.trim()
        when {
            email.isEmpty() || s.password.isEmpty() -> return fail(R.string.err_empty_fields)
            !isEmail(email) -> return fail(R.string.err_invalid_email)
        }
        launchRequest {
            authRepository.signIn(email, s.password).fold(
                onSuccess = { _state.update { it.copy(loading = false, authenticated = true) } },
                onFailure = ::handleError,
            )
        }
    }

    fun register() {
        val s = _state.value
        val email = s.email.trim()
        when {
            s.name.isBlank() -> return fail(R.string.err_empty_name)
            !isEmail(email) -> return fail(R.string.err_invalid_email)
            s.password.length < MIN_PASSWORD -> return fail(R.string.err_short_password)
            s.password != s.passwordRepeat -> return fail(R.string.err_password_mismatch)
            !s.termsAccepted -> return fail(R.string.err_terms)
        }
        launchRequest {
            authRepository.signUp(s.name.trim(), email, s.phone.trim(), s.password).fold(
                onSuccess = { outcome ->
                    _state.update {
                        when (outcome) {
                            SignUpOutcome.SignedIn -> it.copy(loading = false, authenticated = true)
                            SignUpOutcome.ConfirmEmail -> it.copy(loading = false, infoRes = R.string.info_confirm_email)
                        }
                    }
                },
                onFailure = ::handleError,
            )
        }
    }

    fun forgotPassword() {
        val email = _state.value.email.trim()
        if (!isEmail(email)) return fail(R.string.err_invalid_email)
        launchRequest {
            authRepository.resetPassword(email).fold(
                onSuccess = { _state.update { it.copy(loading = false, infoRes = R.string.info_reset_sent) } },
                onFailure = ::handleError,
            )
        }
    }

    private fun launchRequest(block: suspend () -> Unit) {
        _state.update { it.copy(loading = true, errorRes = null, errorDetail = null, infoRes = null) }
        viewModelScope.launch { block() }
    }

    private fun fail(@StringRes res: Int) = _state.update { it.copy(errorRes = res) }

    private fun handleError(t: Throwable) {
        val message = t.message.orEmpty()
        val res = when {
            t is SupabaseNotConfiguredException -> R.string.err_not_configured
            message.contains("Invalid login credentials", ignoreCase = true) -> R.string.err_invalid_credentials
            message.contains("already registered", ignoreCase = true) ||
                message.contains("already been registered", ignoreCase = true) -> R.string.err_email_taken
            t is IOException || message.contains("Unable to resolve host", ignoreCase = true) -> R.string.err_network
            else -> R.string.err_unknown
        }
        _state.update {
            it.copy(
                loading = false,
                errorRes = res,
                errorDetail = if (res == R.string.err_unknown) message.take(200) else null,
            )
        }
    }

    private fun isEmail(v: String) = Patterns.EMAIL_ADDRESS.matcher(v).matches()

    private companion object {
        const val MIN_PASSWORD = 6
    }
}

@Composable
fun BackArrow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_back),
        contentDescription = stringResource(R.string.cd_back),
        colorFilter = ColorFilter.tint(KunColors.Black),
        modifier = modifier
            .size(width = 32.dp, height = 28.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}

@Composable
fun KunCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(shape)
            .background(if (checked) KunColors.Green else KunColors.Background)
            .border(1.dp, if (checked) KunColors.Green else KunColors.CheckboxBorder, shape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = KunColors.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
fun FormMessage(errorRes: Int?, detail: String?, infoRes: Int?, modifier: Modifier = Modifier) {
    val text = when {
        errorRes != null -> stringResource(errorRes) + (detail?.takeIf { it.isNotBlank() }?.let { "\n$it" } ?: "")
        infoRes != null -> stringResource(infoRes)
        else -> return
    }
    Text(
        text = text,
        style = kunText(10.5f, color = if (errorRes != null) KunColors.Error else KunColors.Green),
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onGoToLogin: () -> Unit,
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.authenticated) {
        if (state.authenticated) {
            viewModel.onAuthenticatedHandled()
            onAuthenticated()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        ScreenBackground(R.drawable.bg_register)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(53.dp))
            BackArrow(onClick = onBack, modifier = Modifier.padding(start = 22.dp))

            Spacer(Modifier.height(18.dp))
            KunBrandHeader(logoWidth = 85.dp, modifier = Modifier.padding(start = 21.dp))

            Spacer(Modifier.height(26.dp))
            Text(
                text = stringResource(R.string.reg_title),
                style = kunText(28f, lineHeight = 34f),
                modifier = Modifier.padding(start = 22.dp),
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = stringResource(R.string.reg_subtitle),
                style = kunText(10f, color = KunColors.TextGray.copy(alpha = 0.95f), lineHeight = 11.5f),
                modifier = Modifier.padding(start = 24.dp),
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.reg_have_account))
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(stringResource(R.string.reg_login_link))
                    }
                },
                style = kunText(11.5f, color = KunColors.TextGray),
                modifier = Modifier
                    .padding(start = 240.dp)
                    .clickable(onClick = onGoToLogin),
            )

            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .padding(horizontal = 29.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                KunTextField(
                    value = state.name,
                    onValueChange = viewModel::onName,
                    placeholder = stringResource(R.string.field_name),
                    leadingIcon = Icons.Outlined.Person,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
                )
                KunTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmail,
                    placeholder = stringResource(R.string.field_email),
                    leadingIcon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
                )
                KunTextField(
                    value = state.phone,
                    onValueChange = viewModel::onPhone,
                    placeholder = stringResource(R.string.field_phone),
                    leadingIcon = Icons.Outlined.Phone,
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
                )
                KunTextField(
                    value = state.password,
                    onValueChange = viewModel::onPassword,
                    placeholder = stringResource(R.string.field_password),
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
                )
                KunTextField(
                    value = state.passwordRepeat,
                    onValueChange = viewModel::onPasswordRepeat,
                    placeholder = stringResource(R.string.field_password_repeat),
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    onImeAction = viewModel::register,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
                )
            }

            FormMessage(
                errorRes = state.errorRes,
                detail = state.errorDetail,
                infoRes = state.infoRes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 29.dp, vertical = 6.dp),
            )

            Spacer(Modifier.height(10.dp))
            KunButton(
                text = stringResource(R.string.reg_button),
                onClick = viewModel::register,
                loading = state.loading,
                height = 44.dp,
                modifier = Modifier
                    .padding(horizontal = 29.dp)
                    .fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.padding(start = 29.dp, end = 29.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KunCheckbox(checked = state.termsAccepted, onCheckedChange = viewModel::onTerms)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.reg_terms),
                    style = kunText(9f, lineHeight = 11f),
                )
            }
        }
    }
}

@Preview(
    name = "Register Screen",
    showBackground = true,
    showSystemUi = true,
    widthDp = 360,
    heightDp = 800,
)
@Composable
private fun RegisterScreenPreview() {
    RegisterScreen(
        onBack = {},
        onGoToLogin = {},
        onAuthenticated = {},
    )
}

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onGoToRegister: () -> Unit,
    onAuthenticated: () -> Unit,
    onForgot: () -> Unit = {},
    viewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.authenticated) {
        if (state.authenticated) {
            viewModel.onAuthenticatedHandled()
            onAuthenticated()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        ScreenBackground(R.drawable.bg_login)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(53.dp))
            BackArrow(onClick = onBack, modifier = Modifier.padding(start = 22.dp))

            Spacer(Modifier.height(18.dp))
            KunBrandHeader(logoWidth = 85.dp, modifier = Modifier.padding(start = 21.dp))

            Spacer(Modifier.height(26.dp))
            Text(
                text = stringResource(R.string.login_title),
                style = kunText(28f, lineHeight = 34f),
                modifier = Modifier.padding(start = 22.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.login_subtitle),
                style = kunText(9.5f, lineHeight = 12f),
                modifier = Modifier.padding(start = 23.dp),
            )

            Spacer(Modifier.height(48.dp))
            Column(
                modifier = Modifier
                    .padding(horizontal = 29.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(21.dp),
            ) {
                KunTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmail,
                    placeholder = stringResource(R.string.field_email_or_phone),
                    leadingIcon = Icons.Outlined.Person,
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
                )
                KunTextField(
                    value = state.password,
                    onValueChange = viewModel::onPassword,
                    placeholder = stringResource(R.string.field_password),
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    onImeAction = viewModel::login,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.loading,
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.login_forgot),
                style = kunText(10f, FontWeight.Medium),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 31.dp)
                    .clickable(onClick = onForgot)
                    .padding(vertical = 4.dp),
            )

            FormMessage(
                errorRes = state.errorRes,
                detail = state.errorDetail,
                infoRes = state.infoRes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 29.dp, vertical = 6.dp),
            )

            Spacer(Modifier.height(26.dp))
            KunButton(
                text = stringResource(R.string.login_button),
                onClick = viewModel::login,
                loading = state.loading,
                height = 38.dp,
                modifier = Modifier
                    .padding(horizontal = 29.dp)
                    .fillMaxWidth(),
            )

            Spacer(Modifier.height(41.dp))
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.login_no_account))
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(stringResource(R.string.login_register_link))
                    }
                },
                style = kunText(12f, color = KunColors.TextGray),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(onClick = onGoToRegister)
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KunColors.Background),
    ) {
        ScreenBackground(R.drawable.bg_login)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(53.dp))
            BackArrow(onClick = onBack, modifier = Modifier.padding(start = 22.dp))

            Spacer(Modifier.height(18.dp))
            KunBrandHeader(logoWidth = 85.dp, modifier = Modifier.padding(start = 21.dp))

            Spacer(Modifier.height(26.dp))
            Text(
                text = stringResource(R.string.forgot_title),
                style = kunText(26f, lineHeight = 32f),
                modifier = Modifier.padding(start = 22.dp, end = 120.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.forgot_subtitle),
                style = kunText(10f, lineHeight = 12f),
                modifier = Modifier.padding(start = 23.dp, end = 140.dp),
            )

            Spacer(Modifier.height(48.dp))
            KunTextField(
                value = state.email,
                onValueChange = viewModel::onEmail,
                placeholder = stringResource(R.string.field_email),
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                onImeAction = viewModel::forgotPassword,
                modifier = Modifier
                    .padding(horizontal = 29.dp)
                    .fillMaxWidth(),
                enabled = !state.loading,
            )

            FormMessage(
                errorRes = state.errorRes,
                detail = state.errorDetail,
                infoRes = state.infoRes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 29.dp, vertical = 10.dp),
            )

            Spacer(Modifier.height(20.dp))
            KunButton(
                text = stringResource(R.string.forgot_button),
                onClick = viewModel::forgotPassword,
                loading = state.loading,
                height = 38.dp,
                modifier = Modifier
                    .padding(horizontal = 29.dp)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
