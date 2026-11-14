package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import kotlin.math.cos
import kotlin.math.sin

// Dedicated Dark Liquid Glass Login Palette
private val DeepVoidBg = Color(0xFF0F111A)
private val GlassCardSurface = Color(0xFF171B2A)
private val GlassFieldSurface = Color(0xFF1E2438)
private val RoyalBlue = Color(0xFF2563EB)
private val ElectricIndigo = Color(0xFF4F46E5)
private val NeonViolet = Color(0xFF9333EA)
private val NeonMagenta = Color(0xFFC026D3)
private val LuminousCyan = Color(0xFF38BDF8)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF94A3B8)

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

    // Ambient floating orbs behind the deep #0F111A glassmorphism canvas
    val infiniteTransition = rememberInfiniteTransition(label = "dark_glass_aura")
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
            .background(DeepVoidBg)
    ) {
        // Luminous Nebula Orbs refracted behind the glass panels
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val blueCenter = Offset(
                x = w * 0.18f + cos(phase1) * (w * 0.10f),
                y = h * 0.16f + sin(phase1) * (h * 0.06f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        RoyalBlue.copy(alpha = 0.42f),
                        ElectricIndigo.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = blueCenter,
                    radius = w * 0.72f * pulse
                ),
                center = blueCenter,
                radius = w * 0.72f * pulse
            )

            val violetCenter = Offset(
                x = w * 0.85f - sin(phase1) * (w * 0.10f),
                y = h * 0.34f + cos(phase1) * (h * 0.08f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonViolet.copy(alpha = 0.36f),
                        NeonMagenta.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = violetCenter,
                    radius = w * 0.65f
                ),
                center = violetCenter,
                radius = w * 0.65f
            )

            val cyanCenter = Offset(
                x = w * 0.42f + cos(phase1 * 0.8f) * (w * 0.12f),
                y = h * 0.88f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LuminousCyan.copy(alpha = 0.16f),
                        RoyalBlue.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = cyanCenter,
                    radius = w * 0.62f * (2f - pulse)
                ),
                center = cyanCenter,
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
            // Subtle Top Language Pill
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
                        .background(GlassCardSurface.copy(alpha = 0.72f))
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    NeonViolet.copy(alpha = 0.45f)
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
                        tint = LuminousCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isArabic) "EN" else "عربي",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Translucent Liquid Glass Container Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .testTag("auth_welcome_card")
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                GlassCardSurface.copy(alpha = 0.74f),
                                GlassCardSurface.copy(alpha = 0.58f)
                            )
                        )
                    )
                    .border(
                        width = 1.4.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.28f),
                                RoyalBlue.copy(alpha = 0.55f),
                                NeonViolet.copy(alpha = 0.65f),
                                Color.White.copy(alpha = 0.14f)
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
                    // 1. HEADER: Royal Blue -> Neon Purple Gradient Banner
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
                                        RoyalBlue,
                                        ElectricIndigo,
                                        NeonViolet
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(1000f, 520f)
                                )
                            )
                            .drawBehind {
                                // Subtle specular glass sheen across the top gradient header
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
                                        Color.White.copy(alpha = 0.38f),
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
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .border(
                                        width = 1.2.dp,
                                        color = Color.White.copy(alpha = 0.40f),
                                        shape = RoundedCornerShape(18.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudSync,
                                    contentDescription = "Cloud Sync",
                                    tint = Color.White,
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
                                            color = Color.White,
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
                                            color = Color.White.copy(alpha = 0.88f),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 19.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Form Body inside the Dark Liquid Glass Card
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

                        // Optional Confirm Password in Sign-Up Mode
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

                        // 6a. Quick Link: "نسيت كلمة المرور؟"
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
                                    color = LuminousCyan,
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

                        // Inline Error / Info Glass Banner
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
                                    color = if (isErr) Color(0xFFFDA4AF) else Color(0xFF6EE7B7),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3. PRIMARY LOGIN BUTTON: Capsule Pill + Gradient + Loading State
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("welcome_primary_login_button")
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            RoyalBlue,
                                            ElectricIndigo,
                                            NeonViolet
                                        )
                                    )
                                )
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.45f),
                                            NeonMagenta.copy(alpha = 0.65f),
                                            Color.White.copy(alpha = 0.30f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clickable(
                                    enabled = !isLoading,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = Color.White)
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
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isArabic) "جاري المصادقة..." else "Authenticating...",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
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
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
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
                                color = Color.White.copy(alpha = 0.12f)
                            )
                            Text(
                                text = if (isArabic) "  أو المتابعة عبر  " else "  OR CONTINUE WITH  ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.12f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. GOOGLE BUTTON: Sleek translucent border + authentic multicolored Google icon
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("welcome_google_sign_in_button")
                                .clip(CircleShape)
                                .background(GlassFieldSurface.copy(alpha = 0.65f))
                                .border(
                                    width = 1.2.dp,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.34f),
                                            LuminousCyan.copy(alpha = 0.38f),
                                            NeonViolet.copy(alpha = 0.42f),
                                            Color.White.copy(alpha = 0.22f)
                                        )
                                    ),
                                    shape = CircleShape
                                )
                                .clickable(
                                    enabled = !isLoading,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true, color = LuminousCyan)
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
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6b. FOOTER QUICK LINK: "أنشئ حساباً الآن"
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
                    color = TextSecondary
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
                    color = LuminousCyan,
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

        // Password Reset Glass Dialog triggered from "نسيت كلمة المرور؟"
        if (showForgotPasswordDialog) {
            var resetEmail by remember { mutableStateOf(email) }
            AlertDialog(
                onDismissRequest = { showForgotPasswordDialog = false },
                containerColor = GlassCardSurface,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                shape = RoundedCornerShape(26.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = LuminousCyan
                        )
                        Text(
                            text = if (isArabic) "استعادة كلمة المرور" else "Reset Password",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
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
                            color = TextSecondary,
                            lineHeight = 19.sp
                        )
                        GlassRoundedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            placeholder = if (isArabic) "البريد الإلكتروني" else "Email Address",
                            leadingIcon = Icons.Rounded.Email,
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
                            containerColor = RoyalBlue,
                            contentColor = Color.White
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
                            color = TextSecondary
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun GlassRoundedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
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
            listOf(RoyalBlue, NeonViolet, LuminousCyan)
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                NeonViolet.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.14f)
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
                if (isFocused) GlassFieldSurface.copy(alpha = 0.92f)
                else GlassFieldSurface.copy(alpha = 0.68f)
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
            tint = if (isFocused) LuminousCyan else TextSecondary,
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
                    color = TextSecondary.copy(alpha = 0.78f),
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
                    color = TextPrimary
                ),
                cursorBrush = SolidColor(LuminousCyan),
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
                    tint = if (passwordVisible) LuminousCyan else TextSecondary,
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

        // Google Blue (Right-bottom segment + crossbar)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -15f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Google Green (Bottom segment)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 50f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Google Yellow (Left segment)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 140f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Google Red (Top segment)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 210f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        )
        // Horizontal Blue bar of the "G"
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.50f, size.height * 0.50f),
            end = Offset(size.width * 0.94f, size.height * 0.50f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
