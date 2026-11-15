package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.rememberGoogleSignInController
import com.example.ui.glass.BlurryScreenEdges
import com.example.ui.theme.LocalAppThemePreset
import kotlin.math.cos
import kotlin.math.sin

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

    // Dynamic palette derived strictly from the active theme preset
    val bgBase by animateColorAsState(activePreset.background, tween(320), label = "auth_bg")
    val cardSurface by animateColorAsState(activePreset.surface, tween(320), label = "auth_card")
    val fieldSurface by animateColorAsState(activePreset.surfaceVariant, tween(320), label = "auth_field")
    val primaryAccent by animateColorAsState(activePreset.primary, tween(320), label = "auth_primary")
    val secondaryAccent by animateColorAsState(activePreset.secondary, tween(320), label = "auth_secondary")
    val tertiaryAccent by animateColorAsState(activePreset.tertiary, tween(320), label = "auth_tertiary")
    val textPrimary by animateColorAsState(activePreset.onSurface, tween(280), label = "auth_text")
    val textSecondary by animateColorAsState(activePreset.onSurfaceVariant, tween(280), label = "auth_subtext")

    val accentAvgLum = listOf(primaryAccent, secondaryAccent).map { it.luminance() }.average()
    val accentForeground = if (accentAvgLum > 0.56) Color(0xFF0F172A) else Color.White

    var isSignUpMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var localValidationError by remember { mutableStateOf<String?>(null) }

    val displayedError = localValidationError ?: authErrorMessage ?: googleController.errorMessage

    fun attemptSignUp() {
        focusManager.clearFocus()
        if (username.trim().isEmpty()) {
            localValidationError = if (isArabic) {
                "يرجى كتابة اسم المستخدم (حقل إجباري لإنشاء الحساب)"
            } else {
                "Username is required. Please enter your name to create an account."
            }
            return
        }
        localValidationError = null
        onEmailSignUp(username, email, password, confirmPassword)
    }

    // Ambient floating orbs tinted with the active theme's atmospheric orbs
    val infiniteTransition = rememberInfiniteTransition(label = "theme_glass_aura")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_phase_1"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBase)
    ) {
        // Luminous Theme Orbs refracted behind the glass panels
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val orbStrength = if (activePreset.isGlassStyle) 1f else 0.65f

            val orb1Center = Offset(
                x = w * 0.18f + cos(phase1) * (w * 0.10f),
                y = h * 0.16f + sin(phase1) * (h * 0.06f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activePreset.orb1.copy(alpha = 0.40f * orbStrength),
                        primaryAccent.copy(alpha = 0.14f * orbStrength),
                        Color.Transparent
                    ),
                    center = orb1Center,
                    radius = w * 0.72f * pulse
                ),
                center = orb1Center,
                radius = w * 0.72f * pulse
            )

            val orb2Center = Offset(
                x = w * 0.85f - sin(phase1) * (w * 0.10f),
                y = h * 0.34f + cos(phase1) * (h * 0.08f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activePreset.orb2.copy(alpha = 0.34f * orbStrength),
                        secondaryAccent.copy(alpha = 0.10f * orbStrength),
                        Color.Transparent
                    ),
                    center = orb2Center,
                    radius = w * 0.65f
                ),
                center = orb2Center,
                radius = w * 0.65f
            )

            val orb3Center = Offset(
                x = w * 0.42f + cos(phase1 * 0.8f) * (w * 0.12f),
                y = h * 0.88f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activePreset.orb3.copy(alpha = 0.20f * orbStrength),
                        tertiaryAccent.copy(alpha = 0.08f * orbStrength),
                        Color.Transparent
                    ),
                    center = orb3Center,
                    radius = w * 0.62f * (2f - pulse)
                ),
                center = orb3Center,
                radius = w * 0.62f * (2f - pulse)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Subtle Top Language Pill matching active theme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("welcome_language_toggle")
                        .clip(CircleShape)
                        .background(cardSurface.copy(alpha = if (effectiveDark) 0.76f else 0.88f))
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    textPrimary.copy(alpha = 0.18f),
                                    primaryAccent.copy(alpha = 0.45f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onToggleLanguage
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = if (isArabic) "تبديل اللغة" else "Switch Language",
                        tint = primaryAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isArabic) "EN" else "عربي",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Translucent Theme-Matched Glass Container Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .testTag("auth_welcome_card")
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                cardSurface.copy(alpha = if (activePreset.isGlassStyle) 0.78f else 0.94f),
                                cardSurface.copy(alpha = if (activePreset.isGlassStyle) 0.64f else 0.90f)
                            )
                        )
                    )
                    .border(
                        width = 1.4.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                textPrimary.copy(alpha = if (effectiveDark) 0.22f else 0.12f),
                                primaryAccent.copy(alpha = 0.48f),
                                secondaryAccent.copy(alpha = 0.52f),
                                textPrimary.copy(alpha = 0.10f)
                            ),
                            start = Offset.Zero,
                            end = Offset(900f, 1400f)
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. HEADER: Active Theme Primary -> Secondary -> Tertiary Gradient Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    topStart = 32.dp,
                                    topEnd = 32.dp,
                                    bottomStart = 28.dp,
                                    bottomEnd = 28.dp
                                )
                            )
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        primaryAccent,
                                        secondaryAccent,
                                        tertiaryAccent
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(1000f, 520f)
                                )
                            )
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.22f),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width * 0.18f, size.height * 0.15f),
                                        radius = size.width * 0.55f
                                    ),
                                    center = Offset(size.width * 0.18f, size.height * 0.15f),
                                    radius = size.width * 0.55f
                                )
                            }
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.36f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 32.dp,
                                    topEnd = 32.dp,
                                    bottomStart = 28.dp,
                                    bottomEnd = 28.dp
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(accentForeground.copy(alpha = 0.16f))
                                    .border(
                                        width = 1.2.dp,
                                        color = accentForeground.copy(alpha = 0.36f),
                                        shape = RoundedCornerShape(18.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudSync,
                                    contentDescription = "Cloud Sync",
                                    tint = accentForeground,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            AnimatedContent(
                                targetState = isSignUpMode,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "header_title_anim"
                            ) { signUp ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = when {
                                            signUp && isArabic -> "إنشاء حساب جديد"
                                            signUp -> "Create Account"
                                            isArabic -> "تسجيل الدخول"
                                            else -> "Sign In"
                                        },
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 25.sp,
                                            color = accentForeground,
                                            textAlign = TextAlign.Center
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = when {
                                            signUp && isArabic ->
                                                "أنشئ حسابك السحابي لحفظ ومزامنة جميع ملاحظاتك بأمان عبر أجهزتك"
                                            signUp ->
                                                "Create your Cloud account to securely back up & sync all notes across devices"
                                            isArabic ->
                                                "سجّل دخولك لمزامنة ملاحظاتك وخزنتك فوريّاً عبر السحابة وجميع أجهزتك"
                                            else ->
                                                "Sign in to sync your notes & vault instantaneously across all your devices"
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.sp,
                                            color = accentForeground.copy(alpha = 0.88f),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 19.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Form Body inside the Theme-Matched Glass Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Mandatory Username field displayed directly ABOVE Email when creating an account
                        AnimatedVisibility(
                            visible = isSignUpMode,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                GlassRoundedTextField(
                                    value = username,
                                    onValueChange = {
                                        username = it
                                        localValidationError = null
                                        onClearAuthMessages()
                                        googleController.clearError()
                                    },
                                    placeholder = if (isArabic) "اسم المستخدم *" else "Username *",
                                    leadingIcon = Icons.Rounded.PersonOutline,
                                    fieldSurface = fieldSurface,
                                    primaryAccent = primaryAccent,
                                    secondaryAccent = secondaryAccent,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    isDark = effectiveDark,
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next,
                                    testTag = "auth_username_input"
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }

                        // 2. ROUNDED INPUT FIELDS: Email & Password
                        GlassRoundedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                localValidationError = null
                                onClearAuthMessages()
                                googleController.clearError()
                            },
                            placeholder = if (isArabic) "البريد الإلكتروني" else "Email Address",
                            leadingIcon = Icons.Rounded.Email,
                            fieldSurface = fieldSurface,
                            primaryAccent = primaryAccent,
                            secondaryAccent = secondaryAccent,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = effectiveDark,
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                            testTag = "auth_email_input"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        GlassRoundedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                onClearAuthMessages()
                                googleController.clearError()
                            },
                            placeholder = if (isArabic) "كلمة المرور" else "Password",
                            leadingIcon = Icons.Rounded.Lock,
                            fieldSurface = fieldSurface,
                            primaryAccent = primaryAccent,
                            secondaryAccent = secondaryAccent,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = effectiveDark,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isSignUpMode) ImeAction.Next else ImeAction.Done,
                            onDone = {
                                focusManager.clearFocus()
                                if (!isSignUpMode && !isLoading) {
                                    onEmailSignIn(email, password)
                                }
                            },
                            testTag = "auth_password_input"
                        )

                        // Confirm Password in Sign-Up Mode
                        AnimatedVisibility(
                            visible = isSignUpMode,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(14.dp))
                                GlassRoundedTextField(
                                    value = confirmPassword,
                                    onValueChange = {
                                        confirmPassword = it
                                        onClearAuthMessages()
                                    },
                                    placeholder = if (isArabic) "تأكيد كلمة المرور" else "Confirm Password",
                                    leadingIcon = Icons.Rounded.Lock,
                                    fieldSurface = fieldSurface,
                                    primaryAccent = primaryAccent,
                                    secondaryAccent = secondaryAccent,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    isDark = effectiveDark,
                                    isPassword = true,
                                    passwordVisible = confirmPasswordVisible,
                                    onTogglePasswordVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                    onDone = {
                                        if (!isLoading) {
                                            attemptSignUp()
                                        }
                                    },
                                    testTag = "auth_confirm_password_input"
                                )
                            }
                        }

                        // Quick Link: "نسيت كلمة المرور؟"
                        AnimatedVisibility(visible = !isSignUpMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = if (isArabic) "نسيت كلمة المرور؟" else "Forgot password?",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryAccent,
                                    modifier = Modifier
                                        .testTag("auth_forgot_password_link")
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true)
                                        ) {
                                            onClearAuthMessages()
                                            showForgotPasswordDialog = true
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Inline Error / Info Banner
                        AnimatedVisibility(
                            visible = !displayedError.isNullOrBlank() || !authInfoMessage.isNullOrBlank()
                        ) {
                            val isErr = !displayedError.isNullOrBlank()
                            val bannerColor = if (isErr) Color(0xFFF43F5E) else Color(0xFF10B981)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bannerColor.copy(alpha = 0.14f))
                                    .border(
                                        width = 1.dp,
                                        color = bannerColor.copy(alpha = 0.42f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = displayedError ?: authInfoMessage.orEmpty(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isErr) {
                                        if (effectiveDark) Color(0xFFFDA4AF) else Color(0xFFBE123C)
                                    } else {
                                        if (effectiveDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                                    },
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3. PRIMARY LOGIN BUTTON: Theme Gradient Capsule Pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("welcome_primary_login_button")
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            primaryAccent,
                                            secondaryAccent,
                                            tertiaryAccent
                                        )
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.42f),
                                            tertiaryAccent.copy(alpha = 0.55f),
                                            Color.White.copy(alpha = 0.28f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clickable(
                                    enabled = !isLoading,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = accentForeground)
                                ) {
                                    focusManager.clearFocus()
                                    if (isSignUpMode) {
                                        attemptSignUp()
                                    } else {
                                        localValidationError = null
                                        onEmailSignIn(email, password)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.4.dp,
                                        color = accentForeground
                                    )
                                    Text(
                                        text = if (isArabic) "جاري المصادقة..." else "Authenticating...",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentForeground
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isSignUpMode && isArabic -> "إنشاء حساب"
                                            isSignUpMode -> "Create Account"
                                            isArabic -> "تسجيل الدخول"
                                            else -> "Sign In"
                                        },
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accentForeground
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = accentForeground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = textPrimary.copy(alpha = 0.12f)
                            )
                            Text(
                                text = if (isArabic) "  أو المتابعة عبر  " else "  OR CONTINUE WITH  ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = textPrimary.copy(alpha = 0.12f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. GOOGLE BUTTON: Theme-matched surface & border + authentic Google icon
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("welcome_google_sign_in_button")
                                .clip(CircleShape)
                                .background(fieldSurface.copy(alpha = if (effectiveDark) 0.72f else 0.90f))
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            textPrimary.copy(alpha = 0.22f),
                                            primaryAccent.copy(alpha = 0.42f),
                                            secondaryAccent.copy(alpha = 0.45f),
                                            textPrimary.copy(alpha = 0.16f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clickable(
                                    enabled = !isLoading,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = primaryAccent)
                                ) {
                                    onClearAuthMessages()
                                    googleController.startSignIn(isArabic = isArabic)
                                }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.96f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MultiColorGoogleGIcon(modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = if (isArabic) "المتابعة باستخدام Google" else "Continue with Google",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // FOOTER QUICK LINK: "أنشئ حساباً الآن"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isSignUpMode && isArabic -> "لديك حساب بالفعل؟"
                        isSignUpMode -> "Already have an account?"
                        isArabic -> "ليس لديك حساب؟"
                        else -> "Don't have an account?"
                    },
                    fontSize = 13.sp,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        isSignUpMode && isArabic -> "تسجيل الدخول"
                        isSignUpMode -> "Sign In"
                        isArabic -> "أنشئ حساباً الآن"
                        else -> "Create an account now"
                    },
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryAccent,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .testTag("auth_footer_toggle_mode")
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true)
                        ) {
                            localValidationError = null
                            onClearAuthMessages()
                            googleController.clearError()
                            isSignUpMode = !isSignUpMode
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
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
                            text = if (isArabic) "استعادة كلمة المرور" else "Reset Password",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (isArabic)
                                "أدخل بريدك الإلكتروني وسنرسل إليك رابطاً آمناً لإعادة تعيين كلمة المرور."
                            else
                                "Enter your email address and we will send you a password reset link.",
                            fontSize = 13.sp,
                            color = textSecondary,
                            lineHeight = 19.sp
                        )
                        GlassRoundedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            placeholder = if (isArabic) "البريد الإلكتروني" else "Email Address",
                            leadingIcon = Icons.Rounded.Email,
                            fieldSurface = fieldSurface,
                            primaryAccent = primaryAccent,
                            secondaryAccent = secondaryAccent,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = effectiveDark,
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
                            text = if (isArabic) "إرسال الرابط" else "Send Link",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPasswordDialog = false }) {
                        Text(
                            text = if (isArabic) "إلغاء" else "Cancel",
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
private fun GlassRoundedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    fieldSurface: Color,
    primaryAccent: Color,
    secondaryAccent: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
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

    val borderBrush = if (isFocused) {
        Brush.horizontalGradient(
            listOf(primaryAccent, secondaryAccent, primaryAccent)
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                textPrimary.copy(alpha = if (isDark) 0.16f else 0.10f),
                secondaryAccent.copy(alpha = 0.28f),
                textPrimary.copy(alpha = 0.12f)
            )
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .clip(RoundedCornerShape(27.dp))
            .background(
                if (isFocused) fieldSurface.copy(alpha = 0.94f)
                else fieldSurface.copy(alpha = if (isDark) 0.72f else 0.86f)
            )
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(27.dp)
            )
            .padding(start = 16.dp, end = if (isPassword) 6.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = placeholder,
            tint = if (isFocused) primaryAccent else textSecondary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 14.sp,
                    color = textSecondary.copy(alpha = 0.78f),
                    maxLines = 1
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.5.sp,
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
                    tint = if (passwordVisible) primaryAccent else textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
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
