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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.auth.rememberGoogleSignInController
import com.example.ui.glass.BlurryScreenEdges
import com.example.ui.glass.LocalColoredContainerBorders
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppThemePreset
import com.example.ui.theme.tr
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
    val coloredBorders = LocalColoredContainerBorders.current
    val currentLanguage = LocalAppLanguage.current

    // Dynamic palette derived strictly from the active theme preset, replacing pink hues with crisp aqua/indigo
    val bgBase by animateColorAsState(activePreset.background, tween(320), label = "auth_bg")
    val cardSurface by animateColorAsState(activePreset.surface, tween(320), label = "auth_card")
    val fieldSurface by animateColorAsState(activePreset.surfaceVariant, tween(320), label = "auth_field")
    val rawPrimary = replacePinkInAuth(activePreset.primary, if (effectiveDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
    val rawSecondary = replacePinkInAuth(activePreset.secondary, if (effectiveDark) Color(0xFF6366F1) else Color(0xFF4F46E5))
    val rawTertiary = replacePinkInAuth(activePreset.tertiary, if (effectiveDark) Color(0xFF2DD4BF) else Color(0xFF0D9488))
    val primaryAccent by animateColorAsState(rawPrimary, tween(320), label = "auth_primary")
    val secondaryAccent by animateColorAsState(rawSecondary, tween(320), label = "auth_secondary")
    val tertiaryAccent by animateColorAsState(rawTertiary, tween(320), label = "auth_tertiary")
    val textPrimary by animateColorAsState(activePreset.onSurface, tween(280), label = "auth_text")
    val textSecondary by animateColorAsState(activePreset.onSurfaceVariant, tween(280), label = "auth_subtext")

    val orb1Color = replacePinkInAuth(activePreset.orb1, rawPrimary)
    val orb2Color = replacePinkInAuth(activePreset.orb2, rawSecondary)
    val orb3Color = replacePinkInAuth(activePreset.orb3, rawTertiary)

    val accentAvgLum = listOf(primaryAccent, secondaryAccent, tertiaryAccent).map { it.luminance() }.average()
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

    val usernameRequiredError = tr(
        "يرجى كتابة اسم المستخدم (حقل إجباري لإنشاء الحساب)",
        "Le nom d'utilisateur est requis pour créer un compte.",
        "Username is required. Please enter your name to create an account.",
        "Isem n useqdac ilaq i tmerna n umiḍan."
    )

    fun attemptSignUp() {
        focusManager.clearFocus()
        if (username.trim().isEmpty()) {
            localValidationError = usernameRequiredError
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

    val neutralEdge = textPrimary.copy(alpha = if (effectiveDark) 0.16f else 0.11f)
    val cardBorderBrush = if (coloredBorders) {
        Brush.linearGradient(
            colors = listOf(
                textPrimary.copy(alpha = if (effectiveDark) 0.22f else 0.12f),
                primaryAccent.copy(alpha = 0.45f),
                secondaryAccent.copy(alpha = 0.48f),
                textPrimary.copy(alpha = 0.10f)
            ),
            start = Offset.Zero,
            end = Offset(900f, 1400f)
        )
    } else {
        SolidColor(neutralEdge)
    }

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
                        orb1Color.copy(alpha = 0.40f * orbStrength),
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
                        orb2Color.copy(alpha = 0.34f * orbStrength),
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
                        orb3Color.copy(alpha = 0.20f * orbStrength),
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Subtle Top Action Row: Theme mode toggle + Language toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                        .clip(CircleShape)
                        .background(cardSurface.copy(alpha = if (effectiveDark) 0.74f else 0.88f))
                        .border(
                            width = 1.dp,
                            brush = if (coloredBorders) {
                                Brush.horizontalGradient(
                                    listOf(
                                        textPrimary.copy(alpha = 0.16f),
                                        secondaryAccent.copy(alpha = 0.36f)
                                    )
                                )
                            } else {
                                SolidColor(neutralEdge)
                            },
                            shape = CircleShape
                        )
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

                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .testTag("welcome_language_toggle")
                        .clip(CircleShape)
                        .background(cardSurface.copy(alpha = if (effectiveDark) 0.76f else 0.88f))
                        .border(
                            width = 1.dp,
                            brush = if (coloredBorders) {
                                Brush.horizontalGradient(
                                    listOf(
                                        textPrimary.copy(alpha = 0.18f),
                                        primaryAccent.copy(alpha = 0.45f)
                                    )
                                )
                            } else {
                                SolidColor(neutralEdge)
                            },
                            shape = CircleShape
                        )
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

            Spacer(modifier = Modifier.height(12.dp))

            // Main Translucent Theme-Matched Glass Container Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .testTag("auth_welcome_card")
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                cardSurface.copy(alpha = if (activePreset.isGlassStyle) 0.80f else 0.95f),
                                cardSurface.copy(alpha = if (activePreset.isGlassStyle) 0.66f else 0.91f)
                            )
                        )
                    )
                    .border(
                        width = 1.3.dp,
                        brush = cardBorderBrush,
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. COMPACT TOP HEADER: Branded App Icon + Concise Title & Tagline
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    topStart = 28.dp,
                                    topEnd = 28.dp,
                                    bottomStart = 22.dp,
                                    bottomEnd = 22.dp
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
                                    end = Offset(920f, 360f)
                                )
                            )
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.20f),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width * 0.14f, size.height * 0.20f),
                                        radius = size.width * 0.48f
                                    ),
                                    center = Offset(size.width * 0.14f, size.height * 0.20f),
                                    radius = size.width * 0.48f
                                )
                            }
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = if (coloredBorders) 0.34f else 0.18f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 28.dp,
                                    topEnd = 28.dp,
                                    bottomStart = 22.dp,
                                    bottomEnd = 22.dp
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Fluffy App Icon Badge
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .border(
                                        width = 1.4.dp,
                                        color = Color.White.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(15.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_background),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                    contentDescription = "Fluffy App Icon",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = 1.24f
                                            scaleY = 1.24f
                                        }
                                )
                            }

                            AnimatedContent(
                                targetState = isSignUpMode,
                                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(180)) },
                                modifier = Modifier.weight(1f),
                                label = "header_title_anim"
                            ) { signUp ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (signUp) {
                                            tr(
                                                "إنشاء حساب Fluffy",
                                                "Créer un compte Fluffy",
                                                "Create Fluffy Account",
                                                "Rnu amiḍan Fluffy"
                                            )
                                        } else {
                                            tr(
                                                "مرحباً بك في Fluffy",
                                                "Bienvenue sur Fluffy",
                                                "Sign In to Fluffy",
                                                "Ansuf ɣer Fluffy"
                                            )
                                        },
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 19.sp,
                                            color = accentForeground
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (signUp) {
                                            tr(
                                                "احفظ وزامن جميع ملاحظاتك وخزنتك بأمان",
                                                "Sauvegardez et synchronisez vos notes",
                                                "Back up & sync your notes securely",
                                                "Sekles udiɣ smezdi tira-k s tɣellist"
                                            )
                                        } else {
                                            tr(
                                                "مزامنة فورية لملاحظاتك وخزنتك السحابية",
                                                "Synchronisation instantanée de vos notes",
                                                "Instant cloud sync for notes & vault",
                                                "Amtawi usrid n tira d uɣella"
                                            )
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            color = accentForeground.copy(alpha = 0.86f)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Form Body inside the Theme-Matched Glass Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
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
                                    label = tr("اسم المستخدم *", "Nom d'utilisateur *", "Username *", "Isem n useqdac *"),
                                    placeholder = tr("أدخل اسمك", "Votre nom", "Enter your name", "Sekcem isem-ik"),
                                    leadingIcon = Icons.Rounded.PersonOutline,
                                    fieldSurface = fieldSurface,
                                    primaryAccent = primaryAccent,
                                    secondaryAccent = secondaryAccent,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    isDark = effectiveDark,
                                    coloredBorders = coloredBorders,
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next,
                                    testTag = "auth_username_input"
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // 2. NEATLY ALIGNED INPUT FIELDS: Email & Password
                        GlassRoundedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                localValidationError = null
                                onClearAuthMessages()
                                googleController.clearError()
                            },
                            label = tr("البريد الإلكتروني", "Adresse e-mail", "Email Address", "Imayl"),
                            placeholder = "name@example.com",
                            leadingIcon = Icons.Rounded.Email,
                            fieldSurface = fieldSurface,
                            primaryAccent = primaryAccent,
                            secondaryAccent = secondaryAccent,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = effectiveDark,
                            coloredBorders = coloredBorders,
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                            testTag = "auth_email_input"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        GlassRoundedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                onClearAuthMessages()
                                googleController.clearError()
                            },
                            label = tr("كلمة المرور", "Mot de passe", "Password", "Awal uffir"),
                            placeholder = "••••••••",
                            leadingIcon = Icons.Rounded.Lock,
                            fieldSurface = fieldSurface,
                            primaryAccent = primaryAccent,
                            secondaryAccent = secondaryAccent,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            isDark = effectiveDark,
                            coloredBorders = coloredBorders,
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
                                Spacer(modifier = Modifier.height(12.dp))
                                GlassRoundedTextField(
                                    value = confirmPassword,
                                    onValueChange = {
                                        confirmPassword = it
                                        onClearAuthMessages()
                                    },
                                    label = tr("تأكيد كلمة المرور", "Confirmer le mot de passe", "Confirm Password", "Sentem awal uffir"),
                                    placeholder = "••••••••",
                                    leadingIcon = Icons.Rounded.Lock,
                                    fieldSurface = fieldSurface,
                                    primaryAccent = primaryAccent,
                                    secondaryAccent = secondaryAccent,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    isDark = effectiveDark,
                                    coloredBorders = coloredBorders,
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
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = tr(
                                        "نسيت كلمة المرور؟",
                                        "Mot de passe oublié ?",
                                        "Forgot password?",
                                        "Tettud awal uffir?"
                                    ),
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
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bannerColor.copy(alpha = 0.14f))
                                    .border(
                                        width = 1.dp,
                                        color = bannerColor.copy(alpha = 0.42f),
                                        shape = RoundedCornerShape(14.dp)
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. PRIMARY LOGIN BUTTON: Sleek Theme Gradient Pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("welcome_primary_login_button")
                                .clip(RoundedCornerShape(16.dp))
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
                                    width = 1.1.dp,
                                    brush = if (coloredBorders) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.38f),
                                                tertiaryAccent.copy(alpha = 0.50f),
                                                Color.White.copy(alpha = 0.24f)
                                            )
                                        )
                                    } else {
                                        SolidColor(Color.White.copy(alpha = 0.22f))
                                    },
                                    shape = RoundedCornerShape(16.dp)
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
                                        text = tr(
                                            "جاري المصادقة...",
                                            "Authentification...",
                                            "Authenticating...",
                                            "Asentem yetteddu..."
                                        ),
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
                                        text = if (isSignUpMode) {
                                            tr("إنشاء حساب", "Créer un compte", "Create Account", "Rnu amiḍan")
                                        } else {
                                            tr("تسجيل الدخول", "Se connecter", "Sign In", "Kcem")
                                        },
                                        fontSize = 15.sp,
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

                        Spacer(modifier = Modifier.height(16.dp))

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
                                text = tr(
                                    "  أو المتابعة عبر  ",
                                    "  OU CONTINUER AVEC  ",
                                    "  OR CONTINUE WITH  ",
                                    "  NEƔ KEMMEL S  "
                                ),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textSecondary
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = textPrimary.copy(alpha = 0.12f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. GOOGLE BUTTON: Clean Aligned Surface + Authentic Google Mark
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("welcome_google_sign_in_button")
                                .clip(RoundedCornerShape(16.dp))
                                .background(fieldSurface.copy(alpha = if (effectiveDark) 0.74f else 0.92f))
                                .border(
                                    width = 1.1.dp,
                                    brush = if (coloredBorders) {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                textPrimary.copy(alpha = 0.20f),
                                                primaryAccent.copy(alpha = 0.38f),
                                                secondaryAccent.copy(alpha = 0.42f),
                                                textPrimary.copy(alpha = 0.14f)
                                            )
                                        )
                                    } else {
                                        SolidColor(neutralEdge)
                                    },
                                    shape = RoundedCornerShape(16.dp)
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
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.96f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MultiColorGoogleGIcon(modifier = Modifier.size(17.dp))
                                }
                                Text(
                                    text = tr(
                                        "المتابعة باستخدام Google",
                                        "Continuer avec Google",
                                        "Continue with Google",
                                        "Kemmel s Google"
                                    ),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                    text = if (isSignUpMode) {
                        tr("لديك حساب بالفعل؟", "Déjà inscrit ?", "Already have an account?", "Tesɛiḍ yakan amiḍan?")
                    } else {
                        tr("ليس لديك حساب؟", "Pas de compte ?", "Don't have an account?", "Ur tesɛiḍ ara amiḍan?")
                    },
                    fontSize = 13.sp,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSignUpMode) {
                        tr("تسجيل الدخول", "Se connecter", "Sign In", "Kcem")
                    } else {
                        tr("أنشئ حساباً الآن", "Créer un compte", "Create an account now", "Rnu amiḍan tura")
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
                        GlassRoundedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = tr("البريد الإلكتروني", "Adresse e-mail", "Email Address", "Imayl"),
                            placeholder = "name@example.com",
                            leadingIcon = Icons.Rounded.Email,
                            fieldSurface = fieldSurface,
                            primaryAccent = primaryAccent,
                            secondaryAccent = secondaryAccent,
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

private fun replacePinkInAuth(color: Color, fallback: Color): Color {
    val isPinkish = color.red > 0.65f &&
        color.red > color.green + 0.22f &&
        color.blue > 0.25f &&
        color.red >= color.blue * 0.88f
    return if (isPinkish) fallback else color
}

@Composable
private fun GlassRoundedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    fieldSurface: Color,
    primaryAccent: Color,
    secondaryAccent: Color,
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

    val borderBrush = when {
        isFocused && coloredBorders -> Brush.horizontalGradient(
            listOf(primaryAccent, secondaryAccent, primaryAccent)
        )
        isFocused -> SolidColor(textPrimary.copy(alpha = 0.45f))
        coloredBorders -> Brush.horizontalGradient(
            listOf(
                textPrimary.copy(alpha = if (isDark) 0.14f else 0.09f),
                secondaryAccent.copy(alpha = 0.24f),
                textPrimary.copy(alpha = 0.10f)
            )
        )
        else -> SolidColor(textPrimary.copy(alpha = if (isDark) 0.16f else 0.11f))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isFocused) primaryAccent else textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isFocused) fieldSurface.copy(alpha = 0.96f)
                    else fieldSurface.copy(alpha = if (isDark) 0.72f else 0.88f)
                )
                .border(
                    width = if (isFocused) 1.5.dp else 1.dp,
                    brush = borderBrush,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(start = 10.dp, end = if (isPassword || value.isNotEmpty()) 4.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isFocused) primaryAccent.copy(alpha = 0.16f)
                        else textPrimary.copy(alpha = if (isDark) 0.07f else 0.05f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = label,
                    tint = if (isFocused) primaryAccent else textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 13.5.sp,
                        color = textSecondary.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        modifier = Modifier.size(19.dp)
                    )
                }
            } else if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = textSecondary.copy(alpha = 0.72f),
                        modifier = Modifier.size(17.dp)
                    )
                }
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
