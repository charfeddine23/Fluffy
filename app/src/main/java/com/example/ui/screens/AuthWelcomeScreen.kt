package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.rememberGoogleSignInController
import com.example.ui.glass.BlurryScreenEdges
import com.example.ui.glass.LocalColoredContainerBorders
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppThemePreset
import com.example.ui.theme.tr
import kotlin.math.min

private enum class AuthCardStage {
    WELCOME,
    LOGIN,
    REGISTER
}

@Composable
fun AuthWelcomeScreen(
    isDark: Boolean,
    isArabic: Boolean,
    isAuthBusy: Boolean = false,
    authErrorMessage: String? = null,
    authInfoMessage: String? = null,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onGoogleIdToken: (String) -> Unit,
    onEmailSignIn: (email: String, password: String) -> Unit = { _, _ -> },
    onEmailSignUp: (username: String, email: String, password: String, confirmPassword: String) -> Unit = { _, _, _, _ -> },
    onResetPassword: (email: String) -> Unit = {},
    onClearAuthMessages: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val googleController = rememberGoogleSignInController(
        isArabic = isArabic,
        onGoogleIdToken = onGoogleIdToken
    )
    val isLoading = googleController.isLoading || isAuthBusy
    val focusManager = LocalFocusManager.current

    val activePreset = LocalAppThemePreset.current
    val effectiveDark = activePreset.isDark || isDark
    val coloredBorders = LocalColoredContainerBorders.current
    val currentLanguage = LocalAppLanguage.current

    // Automatically apply active theme colors (replacing pink defaults with crisp theme accent)
    val bgBase by animateColorAsState(activePreset.background, tween(320), label = "auth_bg")
    val cardSurface by animateColorAsState(activePreset.surface, tween(320), label = "auth_card")
    val fieldSurface by animateColorAsState(
        targetValue = if (effectiveDark) activePreset.surfaceVariant else Color.White,
        animationSpec = tween(320),
        label = "auth_field"
    )
    val rawPrimary = replacePinkInAuth(
        activePreset.primary,
        if (effectiveDark) Color(0xFF38BDF8) else Color(0xFF2196F3)
    )
    val rawSecondary = replacePinkInAuth(
        activePreset.secondary,
        if (effectiveDark) Color(0xFF6366F1) else Color(0xFF1E88E5)
    )
    val primaryAccent by animateColorAsState(rawPrimary, tween(320), label = "auth_primary")
    val secondaryAccent by animateColorAsState(rawSecondary, tween(320), label = "auth_secondary")
    val textPrimary by animateColorAsState(activePreset.onSurface, tween(280), label = "auth_text")
    val textSecondary by animateColorAsState(activePreset.onSurfaceVariant, tween(280), label = "auth_subtext")

    val accentForeground = if (primaryAccent.luminance() > 0.58f) Color(0xFF0F172A) else Color.White

    var authStage by remember { mutableStateOf(AuthCardStage.WELCOME) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    val displayedError = localValidationError ?: authErrorMessage ?: googleController.errorMessage

    BackHandler(enabled = authStage != AuthCardStage.WELCOME) {
        localValidationError = null
        onClearAuthMessages()
        googleController.clearError()
        authStage = AuthCardStage.WELCOME
    }

    val nameRequiredError = tr(
        "يرجى كتابة الاسم الأول واسم العائلة",
        "Veuillez saisir votre prénom et votre nom.",
        "Please enter your first and last name.",
        "Ttxil-k sekcem isem-ik d twacult-ik."
    )

    fun switchStage(target: AuthCardStage) {
        focusManager.clearFocus()
        localValidationError = null
        onClearAuthMessages()
        googleController.clearError()
        authStage = target
    }

    fun attemptSignUp() {
        focusManager.clearFocus()
        val combinedName = listOf(firstName.trim(), lastName.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        if (combinedName.isEmpty()) {
            localValidationError = nameRequiredError
            return
        }
        localValidationError = null
        onEmailSignUp(combinedName, email, password, confirmPassword)
    }

    // Animated backdrop circle position matching the reference mockup across the 3 stages
    val circleXFraction by animateFloatAsState(
        targetValue = when (authStage) {
            AuthCardStage.LOGIN -> 0.14f
            AuthCardStage.WELCOME -> 0.50f
            AuthCardStage.REGISTER -> 0.86f
        },
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "backdrop_circle_x"
    )
    val circleYFraction by animateFloatAsState(
        targetValue = when (authStage) {
            AuthCardStage.LOGIN -> 0.34f
            AuthCardStage.WELCOME -> 0.52f
            AuthCardStage.REGISTER -> 0.56f
        },
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "backdrop_circle_y"
    )
    val circleScale by animateFloatAsState(
        targetValue = when (authStage) {
            AuthCardStage.WELCOME -> 0.72f
            AuthCardStage.LOGIN -> 0.88f
            AuthCardStage.REGISTER -> 0.94f
        },
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "backdrop_circle_scale"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(bgBase)
    ) {
        val outerGlowTint = if (effectiveDark) primaryAccent.copy(alpha = 0.30f) else primaryAccent.copy(alpha = 0.22f)

        // Signature large solid/themed decorative circle behind the floating card
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val baseRadius = min(w, h) * 0.44f * circleScale
            val center = Offset(w * circleXFraction, h * circleYFraction)

            // Soft theme aura halo around the circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryAccent.copy(alpha = if (effectiveDark) 0.24f else 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.38f
                ),
                center = center,
                radius = baseRadius * 1.38f
            )

            // Bold signature circle tinted with active theme primary color
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryAccent,
                        secondaryAccent.copy(alpha = 0.92f)
                    ),
                    start = Offset(center.x - baseRadius, center.y - baseRadius),
                    end = Offset(center.x + baseRadius, center.y + baseRadius)
                ),
                center = center,
                radius = baseRadius,
                alpha = if (effectiveDark) 0.82f else 0.94f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Compact Top Controls Bar (Back / Theme Toggle / Language Pill)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedVisibility(visible = authStage != AuthCardStage.WELCOME) {
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 44.dp, minHeight = 40.dp)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = CircleShape,
                                    ambientColor = outerGlowTint,
                                    spotColor = outerGlowTint
                                )
                                .clip(CircleShape)
                                .background(cardSurface.copy(alpha = 0.95f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true)
                                ) {
                                    switchStage(AuthCardStage.WELCOME)
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = tr("رجوع", "Retour", "Back", "Uɣal"),
                                tint = textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 44.dp, minHeight = 40.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                ambientColor = outerGlowTint,
                                spotColor = outerGlowTint
                            )
                            .clip(CircleShape)
                            .background(cardSurface.copy(alpha = 0.95f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onToggleTheme
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (effectiveDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = tr("تبديل المظهر", "Changer le thème", "Switch theme", "Senfel asentel"),
                            tint = primaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("welcome_language_toggle")
                        .shadow(
                            elevation = 4.dp,
                            shape = CircleShape,
                            ambientColor = outerGlowTint,
                            spotColor = outerGlowTint
                        )
                        .clip(CircleShape)
                        .background(cardSurface.copy(alpha = 0.95f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onToggleLanguage
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = tr("تبديل اللغة", "Changer la langue", "Switch Language", "Senfel tutlayt"),
                        tint = primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (currentLanguage.isRtl) "EN" else "عربي",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Main Floating Card matching the reference design proportions
            val cardBorderModifier = if (coloredBorders) {
                Modifier.border(
                    width = 1.dp,
                    color = primaryAccent.copy(alpha = if (effectiveDark) 0.28f else 0.14f),
                    shape = RoundedCornerShape(26.dp)
                )
            } else {
                Modifier.border(
                    width = 1.dp,
                    color = textPrimary.copy(alpha = if (effectiveDark) 0.14f else 0.08f),
                    shape = RoundedCornerShape(26.dp)
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
                    .heightIn(min = 520.dp)
                    .testTag("auth_welcome_card")
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = primaryAccent.copy(alpha = 0.28f),
                        spotColor = primaryAccent.copy(alpha = 0.30f)
                    )
                    .then(cardBorderModifier),
                shape = RoundedCornerShape(26.dp),
                color = cardSurface
            ) {
                AnimatedContent(
                    targetState = authStage,
                    transitionSpec = {
                        val forward = targetState.ordinal >= initialState.ordinal
                        val dir = if (forward) 1 else -1
                        (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { w -> dir * w / 4 } +
                            fadeIn(tween(240))) togetherWith
                            (slideOutHorizontally(tween(260)) { w -> -dir * w / 5 } +
                                fadeOut(tween(180)))
                    },
                    label = "auth_stage_content"
                ) { stage ->
                    when (stage) {
                        AuthCardStage.WELCOME -> {
                            WelcomeLandingStage(
                                primaryAccent = primaryAccent,
                                accentForeground = accentForeground,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                fieldSurface = fieldSurface,
                                isDark = effectiveDark,
                                isLoading = isLoading,
                                displayedError = displayedError,
                                onRegisterNow = { switchStage(AuthCardStage.REGISTER) },
                                onHaveAccount = { switchStage(AuthCardStage.LOGIN) },
                                onGoogleSignIn = {
                                    onClearAuthMessages()
                                    googleController.startSignIn(isArabic = isArabic)
                                }
                            )
                        }

                        AuthCardStage.LOGIN -> {
                            LoginCardStage(
                                email = email,
                                onEmailChange = {
                                    email = it
                                    localValidationError = null
                                    onClearAuthMessages()
                                    googleController.clearError()
                                },
                                password = password,
                                onPasswordChange = {
                                    password = it
                                    localValidationError = null
                                    onClearAuthMessages()
                                    googleController.clearError()
                                },
                                passwordVisible = passwordVisible,
                                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                                isLoading = isLoading,
                                displayedError = displayedError,
                                authInfoMessage = authInfoMessage,
                                primaryAccent = primaryAccent,
                                accentForeground = accentForeground,
                                fieldSurface = fieldSurface,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                isDark = effectiveDark,
                                coloredBorders = coloredBorders,
                                onForgotPassword = {
                                    onClearAuthMessages()
                                    showForgotPasswordDialog = true
                                },
                                onSignIn = {
                                    focusManager.clearFocus()
                                    localValidationError = null
                                    onEmailSignIn(email, password)
                                },
                                onSwitchToRegister = { switchStage(AuthCardStage.REGISTER) },
                                onGoogleSignIn = {
                                    onClearAuthMessages()
                                    googleController.startSignIn(isArabic = isArabic)
                                }
                            )
                        }

                        AuthCardStage.REGISTER -> {
                            RegisterCardStage(
                                firstName = firstName,
                                onFirstNameChange = {
                                    firstName = it
                                    localValidationError = null
                                    onClearAuthMessages()
                                    googleController.clearError()
                                },
                                lastName = lastName,
                                onLastNameChange = {
                                    lastName = it
                                    localValidationError = null
                                    onClearAuthMessages()
                                    googleController.clearError()
                                },
                                email = email,
                                onEmailChange = {
                                    email = it
                                    localValidationError = null
                                    onClearAuthMessages()
                                    googleController.clearError()
                                },
                                password = password,
                                onPasswordChange = {
                                    password = it
                                    localValidationError = null
                                    onClearAuthMessages()
                                },
                                confirmPassword = confirmPassword,
                                onConfirmPasswordChange = {
                                    confirmPassword = it
                                    localValidationError = null
                                    onClearAuthMessages()
                                },
                                passwordVisible = passwordVisible,
                                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                                confirmPasswordVisible = confirmPasswordVisible,
                                onToggleConfirmPasswordVisibility = {
                                    confirmPasswordVisible = !confirmPasswordVisible
                                },
                                isLoading = isLoading,
                                displayedError = displayedError,
                                authInfoMessage = authInfoMessage,
                                primaryAccent = primaryAccent,
                                accentForeground = accentForeground,
                                fieldSurface = fieldSurface,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                isDark = effectiveDark,
                                coloredBorders = coloredBorders,
                                onRegister = { attemptSignUp() },
                                onSwitchToLogin = { switchStage(AuthCardStage.LOGIN) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Password Reset Dialog styled to match the active theme
        if (showForgotPasswordDialog) {
            var resetEmail by remember { mutableStateOf(email) }
            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                containerColor = cardSurface,
                titleContentColor = textPrimary,
                textContentColor = textSecondary,
                shape = RoundedCornerShape(26.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = primaryAccent
                        )
                        Text(
                            text = tr(
                                "استعادة كلمة المرور",
                                "Réinitialiser le mot de passe",
                                "Reset Password",
                                "Ales awennez n wawal uffir"
                            ),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = tr(
                                "أدخل بريدك الإلكتروني وسنرسل إليك رابطاً آمناً لإعادة تعيين كلمة المرور.",
                                "Entrez votre adresse e-mail pour recevoir un lien de réinitialisation sécurisé.",
                                "Enter your email address and we will send you a password reset link.",
                                "Sekcem imayl-ik akken ad ak-d-nazen aseɣwen n uwennez."
                            ),
                            fontSize = 13.sp,
                            color = textSecondary,
                            lineHeight = 19.sp
                        )
                        MockupPillTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            placeholder = tr("البريد الإلكتروني", "Adresse e-mail", "Email Address", "Imayl"),
                            leadingIcon = Icons.Rounded.Email,
                            fieldSurface = fieldSurface,
                            primaryAccent = primaryAccent,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = effectiveDark,
                            coloredBorders = coloredBorders,
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                            testTag = "reset_password_email_input"
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onResetPassword(resetEmail)
                            showForgotPasswordDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryAccent,
                            contentColor = accentForeground
                        ),
                        shape = CircleShape
                    ) {
                        Text(
                            text = tr("إرسال الرابط", "Envoyer", "Send Link", "Azen aseɣwen"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text(
                            text = tr("إلغاء", "Annuler", "Cancel", "Sefsex"),
                            color = textSecondary
                        )
                    }
                }
            )
        }

        BlurryScreenEdges(
            isDark = effectiveDark,
            edgeColorOverride = bgBase
        )
    }
}

@Composable
private fun WelcomeLandingStage(
    primaryAccent: Color,
    accentForeground: Color,
    textPrimary: Color,
    textSecondary: Color,
    fieldSurface: Color,
    isDark: Boolean,
    isLoading: Boolean,
    displayedError: String?,
    onRegisterNow: () -> Unit,
    onHaveAccount: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 520.dp)
    ) {
        // Signature Top-Start Organic Cloud Blob (exact silhouette from the middle mockup card)
        Canvas(
            modifier = Modifier
                .size(164.dp)
                .align(Alignment.TopStart)
        ) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = primaryAccent,
                radius = w * 0.48f,
                center = Offset(w * 0.14f, h * 0.08f)
            )
            drawCircle(
                color = primaryAccent,
                radius = w * 0.30f,
                center = Offset(w * 0.44f, h * 0.18f)
            )
            drawCircle(
                color = primaryAccent,
                radius = w * 0.32f,
                center = Offset(w * 0.14f, h * 0.42f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 520.dp)
                .padding(horizontal = 28.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = tr("مرحباً بك", "Bienvenue", "Welcome", "Ansuf"),
                fontSize = 27.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = textPrimary,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(tr("في ", "sur ", "To ", "ɣer "))
                    }
                    withStyle(
                        SpanStyle(
                            color = primaryAccent,
                            fontSize = 31.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontStyle = FontStyle.Italic,
                            fontFamily = FontFamily.Cursive
                        )
                    ) {
                        append("Fluffy !")
                    }
                },
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Primary Centered Pill: "Register now !"
            ThemePillButton(
                text = tr("سجّل الآن !", "S'inscrire !", "Register now !", "Jerred tura !"),
                primaryAccent = primaryAccent,
                accentForeground = accentForeground,
                enabled = !isLoading,
                onClick = onRegisterNow,
                modifier = Modifier
                    .widthIn(min = 196.dp)
                    .testTag("welcome_register_now_button")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Centered Pill: "Have an account"
            ThemePillButton(
                text = tr("لديّ حساب", "J'ai un compte", "Have an account", "Sɛiɣ amiḍan"),
                primaryAccent = primaryAccent,
                accentForeground = accentForeground,
                enabled = !isLoading,
                onClick = onHaveAccount,
                modifier = Modifier
                    .widthIn(min = 196.dp)
                    .testTag("welcome_have_account_button")
            )

            if (!displayedError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(18.dp))
                InlineAuthBanner(
                    message = displayedError,
                    isError = true,
                    isDark = isDark
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            // Subtle Google option at bottom of landing card
            GooglePillButton(
                label = tr(
                    "المتابعة باستخدام Google",
                    "Continuer avec Google",
                    "Continue with Google",
                    "Kemmel s Google"
                ),
                fieldSurface = fieldSurface,
                primaryAccent = primaryAccent,
                textPrimary = textPrimary,
                isDark = isDark,
                isLoading = isLoading,
                onClick = onGoogleSignIn
            )
        }
    }
}

@Composable
private fun LoginCardStage(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    displayedError: String?,
    authInfoMessage: String?,
    primaryAccent: Color,
    accentForeground: Color,
    fieldSurface: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    coloredBorders: Boolean,
    onForgotPassword: () -> Unit,
    onSignIn: () -> Unit,
    onSwitchToRegister: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 520.dp)
            .padding(horizontal = 26.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Centered Brand Wordmark ("Fluffy") in script style like "BenApp"
        Text(
            text = "Fluffy",
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Cursive,
            color = primaryAccent,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Left/Start-aligned Login Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = tr("تسجيل الدخول", "Connexion", "Login", "Anekcum"),
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tr(
                    "سجّل دخولك للمتابعة",
                    "Connectez-vous pour continuer",
                    "Sign in to continue",
                    "Kcem akken ad tkemmleḍ"
                ),
                fontSize = 13.sp,
                color = textSecondary
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Pill Field 1: Email
        MockupPillTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = tr("البريد الإلكتروني", "Adresse e-mail", "Your Email Address", "Imayl-ik"),
            leadingIcon = Icons.Rounded.Email,
            fieldSurface = fieldSurface,
            primaryAccent = primaryAccent,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            isDark = isDark,
            coloredBorders = coloredBorders,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            testTag = "auth_email_input"
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Pill Field 2: Password
        MockupPillTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = tr("كلمة المرور", "Mot de passe", "Password", "Awal uffir"),
            leadingIcon = Icons.Rounded.Lock,
            fieldSurface = fieldSurface,
            primaryAccent = primaryAccent,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            isDark = isDark,
            coloredBorders = coloredBorders,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onDone = {
                if (!isLoading) onSignIn()
            },
            testTag = "auth_password_input"
        )

        // Forgot password link neatly aligned at end
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = tr(
                    "نسيت كلمة المرور؟",
                    "Mot de passe oublié ?",
                    "Forgot password?",
                    "Tettud awal uffir?"
                ),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary,
                modifier = Modifier
                    .testTag("auth_forgot_password_link")
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onForgotPassword
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        if (!displayedError.isNullOrBlank() || !authInfoMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            InlineAuthBanner(
                message = displayedError ?: authInfoMessage.orEmpty(),
                isError = !displayedError.isNullOrBlank(),
                isDark = isDark
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Bottom Row matching the Login mockup: "Register" link on start + "Sign in" pill on end
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("إنشاء حساب", "S'inscrire", "Register", "Jerred"),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryAccent,
                modifier = Modifier
                    .testTag("auth_footer_toggle_mode")
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onSwitchToRegister
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            )

            ThemePillButton(
                text = if (isLoading) {
                    tr("جاري...", "Chargement...", "Signing in...", "Yetteddu...")
                } else {
                    tr("تسجيل الدخول", "Connexion", "Sign in", "Kcem")
                },
                primaryAccent = primaryAccent,
                accentForeground = accentForeground,
                enabled = !isLoading,
                isLoading = isLoading,
                onClick = onSignIn,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                modifier = Modifier.testTag("welcome_primary_login_button")
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        GooglePillButton(
            label = tr(
                "المتابعة باستخدام Google",
                "Continuer avec Google",
                "Continue with Google",
                "Kemmel s Google"
            ),
            fieldSurface = fieldSurface,
            primaryAccent = primaryAccent,
            textPrimary = textPrimary,
            isDark = isDark,
            isLoading = isLoading,
            onClick = onGoogleSignIn
        )
    }
}

@Composable
private fun RegisterCardStage(
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    confirmPasswordVisible: Boolean,
    onToggleConfirmPasswordVisibility: () -> Unit,
    isLoading: Boolean,
    displayedError: String?,
    authInfoMessage: String?,
    primaryAccent: Color,
    accentForeground: Color,
    fieldSurface: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    coloredBorders: Boolean,
    onRegister: () -> Unit,
    onSwitchToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 520.dp)
            .padding(horizontal = 26.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Start-aligned "Register" heading matching the right card in the reference image
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = tr("إنشاء حساب", "Inscription", "Register", "Ajerred"),
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Pill Field 1: First Name
        MockupPillTextField(
            value = firstName,
            onValueChange = onFirstNameChange,
            placeholder = tr("الاسم الأول", "Prénom", "First name", "Isem amezwaru"),
            leadingIcon = null,
            fieldSurface = fieldSurface,
            primaryAccent = primaryAccent,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            isDark = isDark,
            coloredBorders = coloredBorders,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            testTag = "auth_username_input"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Pill Field 2: Last Name
        MockupPillTextField(
            value = lastName,
            onValueChange = onLastNameChange,
            placeholder = tr("اسم العائلة", "Nom", "Last name", "Isem n twacult"),
            leadingIcon = null,
            fieldSurface = fieldSurface,
            primaryAccent = primaryAccent,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            isDark = isDark,
            coloredBorders = coloredBorders,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            testTag = "auth_last_name_input"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Pill Field 3: Email Address
        MockupPillTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = tr("البريد الإلكتروني", "Adresse e-mail", "Email address", "Imayl"),
            leadingIcon = null,
            fieldSurface = fieldSurface,
            primaryAccent = primaryAccent,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            isDark = isDark,
            coloredBorders = coloredBorders,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            testTag = "auth_email_input"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Pill Field 4: Password
        MockupPillTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = tr("كلمة المرور", "Mot de passe", "Password", "Awal uffir"),
            leadingIcon = null,
            fieldSurface = fieldSurface,
            primaryAccent = primaryAccent,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            isDark = isDark,
            coloredBorders = coloredBorders,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            testTag = "auth_password_input"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Pill Field 5: Confirm Password
        MockupPillTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = tr("تأكيد كلمة المرور", "Confirmer le mot de passe", "Confirm password", "Sentem awal uffir"),
            leadingIcon = null,
            fieldSurface = fieldSurface,
            primaryAccent = primaryAccent,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            isDark = isDark,
            coloredBorders = coloredBorders,
            isPassword = true,
            passwordVisible = confirmPasswordVisible,
            onTogglePasswordVisibility = onToggleConfirmPasswordVisibility,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onDone = {
                if (!isLoading) onRegister()
            },
            testTag = "auth_confirm_password_input"
        )

        if (!displayedError.isNullOrBlank() || !authInfoMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            InlineAuthBanner(
                message = displayedError ?: authInfoMessage.orEmpty(),
                isError = !displayedError.isNullOrBlank(),
                isDark = isDark
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Bottom Row matching the Register mockup: "Login" link on start + "Register" pill on end
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tr("تسجيل الدخول", "Connexion", "Login", "Anekcum"),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryAccent,
                modifier = Modifier
                    .testTag("auth_footer_toggle_mode")
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onSwitchToLogin
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            )

            ThemePillButton(
                text = if (isLoading) {
                    tr("جاري...", "Création...", "Creating...", "Yetteddu...")
                } else {
                    tr("تسجيل", "S'inscrire", "Register", "Jerred")
                },
                primaryAccent = primaryAccent,
                accentForeground = accentForeground,
                enabled = !isLoading,
                isLoading = isLoading,
                onClick = onRegister,
                contentPadding = PaddingValues(horizontal = 26.dp, vertical = 10.dp),
                modifier = Modifier.testTag("welcome_primary_login_button")
            )
        }
    }
}

@Composable
private fun ThemePillButton(
    text: String,
    primaryAccent: Color,
    accentForeground: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 11.dp)
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                ambientColor = primaryAccent.copy(alpha = 0.45f),
                spotColor = primaryAccent.copy(alpha = 0.50f)
            )
            .clip(CircleShape)
            .background(primaryAccent)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accentForeground),
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = accentForeground
                )
            }
            Text(
                text = text,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GooglePillButton(
    label: String,
    fieldSurface: Color,
    primaryAccent: Color,
    textPrimary: Color,
    isDark: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag("welcome_google_sign_in_button")
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.08f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.10f)
            )
            .clip(CircleShape)
            .background(fieldSurface)
            .border(
                width = 1.dp,
                color = textPrimary.copy(alpha = if (isDark) 0.14f else 0.07f),
                shape = CircleShape
            )
            .clickable(
                enabled = !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = primaryAccent),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        MultiColorGoogleGIcon(modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = textPrimary
        )
    }
}

@Composable
private fun InlineAuthBanner(
    message: String,
    isError: Boolean,
    isDark: Boolean
) {
    val bannerColor = if (isError) Color(0xFFF43F5E) else Color(0xFF10B981)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bannerColor.copy(alpha = 0.13f))
            .border(
                width = 1.dp,
                color = bannerColor.copy(alpha = 0.40f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = message,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isError) {
                if (isDark) Color(0xFFFDA4AF) else Color(0xFFBE123C)
            } else {
                if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MockupPillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector?,
    fieldSurface: Color,
    primaryAccent: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    coloredBorders: Boolean = true,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 1.4.dp else 0.8.dp,
        animationSpec = tween(180),
        label = "pill_field_border_w"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused && coloredBorders -> primaryAccent.copy(alpha = 0.80f)
            isFocused -> textPrimary.copy(alpha = 0.45f)
            else -> textPrimary.copy(alpha = if (isDark) 0.12f else 0.06f)
        },
        animationSpec = tween(180),
        label = "pill_field_border_c"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .shadow(
                elevation = if (isFocused) 10.dp else 6.dp,
                shape = CircleShape,
                ambientColor = if (isFocused && coloredBorders) {
                    primaryAccent.copy(alpha = 0.24f)
                } else {
                    Color.Black.copy(alpha = if (isDark) 0.35f else 0.07f)
                },
                spotColor = if (isFocused && coloredBorders) {
                    primaryAccent.copy(alpha = 0.28f)
                } else {
                    Color.Black.copy(alpha = if (isDark) 0.35f else 0.09f)
                }
            )
            .clip(CircleShape)
            .background(fieldSurface)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = CircleShape
            )
            .padding(
                start = if (leadingIcon != null) 16.dp else 20.dp,
                end = if (isPassword) 4.dp else 18.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = placeholder,
                tint = if (isFocused) primaryAccent else textSecondary.copy(alpha = 0.82f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 13.5.sp,
                    color = textSecondary.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary
                ),
                cursorBrush = SolidColor(primaryAccent),
                visualTransformation = if (isPassword && !passwordVisible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onDone() }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused }
            )
        }

        if (isPassword && onTogglePasswordVisibility != null) {
            IconButton(
                onClick = onTogglePasswordVisibility,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("${testTag}_visibility_toggle")
            ) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = if (passwordVisible) primaryAccent else textSecondary.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun replacePinkInAuth(color: Color, fallback: Color): Color {
    val isPinkish = color.red > 0.65f &&
        color.red > color.green + 0.22f &&
        color.blue > 0.25f &&
        color.red >= color.blue * 0.88f
    return if (isPinkish) fallback else color
}

/**
 * Draws the authentic 4-color Google "G" mark cleanly using vector Canvas arcs and bar.
 */
@Composable
private fun MultiColorGoogleGIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.21f
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)

        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -15f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 50f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 140f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 210f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.50f, size.height * 0.50f),
            end = Offset(size.width * 0.94f, size.height * 0.50f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
