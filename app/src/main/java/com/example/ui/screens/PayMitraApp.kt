package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.ui.NetworkQuality
import com.example.ui.PayMitraViewModel
import com.example.ui.PaymentResult
import com.example.ui.AiInsightsState
import com.example.data.DbTransaction
import com.example.data.DbUserProfile
import com.example.data.DbFavoriteContact
import com.example.ui.theme.*
import androidx.compose.ui.res.painterResource
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Navigation route definitions
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val SCAN_PAY = "scan_pay"
    const val PAYMENT = "payment/{name}/{upiId}/{amount}"
    const val AI_INSIGHTS = "ai_insights"
    const val TRANSACTIONS = "transactions"
    
    fun makePaymentRoute(name: String, upiId: String, amount: String = "0") =
        "payment/${name}/${upiId}/${amount}"
}

@Composable
fun PayMitraNavigation(viewModel: PayMitraViewModel) {
    val navController = rememberNavController()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(onSplashFinished = {
                if (isLoggedIn) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                } else {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            })
        }
        
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            MainLayout(navController = navController, viewModel = viewModel, currentTab = 0) {
                HomeScreen(navController = navController, viewModel = viewModel)
            }
        }

        composable(Routes.SCAN_PAY) {
            MainLayout(navController = navController, viewModel = viewModel, currentTab = 1) {
                ScanPayScreen(navController = navController)
            }
        }

        composable(
            route = Routes.PAYMENT,
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("upiId") { type = NavType.StringType },
                navArgument("amount") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val upiId = backStackEntry.arguments?.getString("upiId") ?: ""
            val amount = backStackEntry.arguments?.getString("amount") ?: "0"

            PaymentScreen(
                recipientName = name,
                recipientUpiId = upiId,
                initialAmount = amount,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onPaymentComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }

        composable(Routes.AI_INSIGHTS) {
            MainLayout(navController = navController, viewModel = viewModel, currentTab = 2) {
                AiInsightsScreen(viewModel = viewModel)
            }
        }

        composable(Routes.TRANSACTIONS) {
            MainLayout(navController = navController, viewModel = viewModel, currentTab = 3) {
                TransactionsScreen(viewModel = viewModel)
            }
        }
    }
}

// Custom Glassmorphic Card Container with customizable alpha and border glow
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White.copy(alpha = 0.05f),
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(borderColor, Color.Transparent, borderColor.copy(alpha = 0.02f)),
                    start = Offset(0f, 0f),
                    end = Offset(300f, 600f)
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            content()
        }
    }
}

// Background wave gradient decoration
@Composable
fun PremiumBackgroundDecoration(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "BgAnimation")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Shift"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LuxuryBgDark)
            .drawBehind {
                // Futuristic radial glow in the corners
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PremiumEmeraldGreen.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width - 200f + gradientShift * 0.1f, 100f),
                        radius = size.width * 0.8f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(RoyalBlue.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(-100f + gradientShift * 0.05f, size.height - 300f),
                        radius = size.width * 0.9f
                    )
                )
            }
    ) {
        content()
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(0.5f) }
    val glowAnim = remember { Animatable(0f) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "SplashLiquid")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Offset"
    )

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1.1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(500)
        )
    }

    LaunchedEffect(Unit) {
        glowAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(1800, easing = FastOutSlowInEasing)
        )
        delay(600)
        onSplashFinished()
    }

    PremiumBackgroundDecoration {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Liquid glow background wave drawing
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path()
                val midY = size.height * 0.5f
                path.moveTo(0f, midY)
                for (x in 0..size.width.toInt() step 5) {
                    val angle = (x / size.width) * 2 * PI.toFloat() + waveOffset
                    val y = midY + sin(angle) * 35f * glowAnim.value
                    path.lineTo(x.toFloat(), y)
                }
                path.lineTo(size.width, size.height)
                path.lineTo(0f, size.height)
                path.close()

                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            PremiumEmeraldGreen.copy(alpha = 0.03f * glowAnim.value),
                            RoyalBlue.copy(alpha = 0.05f * glowAnim.value)
                        )
                    )
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.scale(scaleAnim.value)
            ) {
                // Glassmorphic App Icon Holder
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(PremiumEmeraldGreen.copy(alpha = 0.3f), Color.Transparent),
                                    radius = size.width * 0.7f
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "PayMitra Security",
                        modifier = Modifier
                            .size(64.dp)
                            .drawBehind {
                                drawCircle(
                                    color = RoyalBlue.copy(alpha = 0.4f),
                                    radius = size.width * 0.45f
                                )
                            },
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(PremiumEmeraldGreen)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "PayMitra",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "UPI SUPER FAST PAYMENT",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = PremiumEmeraldGreen,
                        letterSpacing = 4.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "HyperOS • OxygenOS Inspired",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

// ==========================================
// 2. LOGIN SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: PayMitraViewModel, onLoginSuccess: () -> Unit) {
    var name by remember { mutableStateOf("Amit Kumar") }
    var mobile by remember { mutableStateOf("9876543210") }
    var selectedBank by remember { mutableStateOf("State Bank of India") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val banks = listOf(
        "State Bank of India",
        "HDFC Bank",
        "ICICI Bank",
        "Axis Bank",
        "Punjab National Bank",
        "Paytm Payments Bank"
    )

    PremiumBackgroundDecoration {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // High-Fidelity Bento Onboarding Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_login_hero),
                        contentDescription = "Futuristic UPI Payments Onboarding",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Semi-transparent vignette dark overlay to match the flagship dark UI
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Welcome to PayMitra",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Your next-gen flagship payment experience",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondaryDark
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Secure Onboarding",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    // Name input
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PremiumEmeraldGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = PremiumEmeraldGreen,
                            unfocusedLabelColor = TextSecondaryDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input"),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Name", tint = PremiumEmeraldGreen)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mobile number
                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { if (it.length <= 10) mobile = it },
                        label = { Text("Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PremiumEmeraldGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedLabelColor = PremiumEmeraldGreen,
                            unfocusedLabelColor = TextSecondaryDark,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = "Phone", tint = PremiumEmeraldGreen)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bank Selection
                    Text(
                        text = "Primary Bank Connection",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondaryDark)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        banks.forEach { bank ->
                            val isSelected = selectedBank == bank
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PremiumEmeraldGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                    .border(
                                        1.dp,
                                        if (isSelected) PremiumEmeraldGreen else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedBank = bank }
                                    .padding(vertical = 10.dp, horizontal = 16.dp)
                            ) {
                                Text(
                                    text = bank,
                                    color = if (isSelected) PremiumEmeraldGreen else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    Button(
                        onClick = {
                            if (name.isBlank() || mobile.length < 10) {
                                Toast.makeText(context, "Please fill in all details with a valid mobile", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.loginUser(name, mobile, selectedBank)
                                Toast.makeText(context, "UPI Automatic Device Binding Successful!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumEmeraldGreen),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("login_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = "Lock")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bind Device & Create UPI ID",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Biometrics Shortcut Icon
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable {
                            viewModel.loginUser(name, mobile, selectedBank)
                            Toast.makeText(context, "Biometric Fingerprint Authentication Verified", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        }
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(30.dp))
                        .padding(vertical = 12.dp, horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Fingerprint", tint = PremiumEmeraldGreen, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Instant Biometric Login", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// ==========================================
// CENTRAL LAYOUT & BOTTOM NAVIGATION
// ==========================================
@Composable
fun MainLayout(
    navController: NavHostController,
    viewModel: PayMitraViewModel,
    currentTab: Int,
    content: @Composable () -> Unit
) {
    val connection by viewModel.networkQuality.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = LuxuryBgDark.copy(alpha = 0.9f),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { if (currentTab != 0) navController.navigate(Routes.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PremiumEmeraldGreen,
                        selectedTextColor = PremiumEmeraldGreen,
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark,
                        indicatorColor = PremiumEmeraldGreen.copy(alpha = 0.12f)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { if (currentTab != 1) navController.navigate(Routes.SCAN_PAY) },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan & Pay") },
                    label = { Text("Scan", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PremiumEmeraldGreen,
                        selectedTextColor = PremiumEmeraldGreen,
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark,
                        indicatorColor = PremiumEmeraldGreen.copy(alpha = 0.12f)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { if (currentTab != 2) navController.navigate(Routes.AI_INSIGHTS) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "MitraAI Insights") },
                    label = { Text("MitraAI", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PremiumEmeraldGreen,
                        selectedTextColor = PremiumEmeraldGreen,
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark,
                        indicatorColor = PremiumEmeraldGreen.copy(alpha = 0.12f)
                    )
                )

                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { if (currentTab != 3) navController.navigate(Routes.TRANSACTIONS) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Transactions") },
                    label = { Text("Ledger", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PremiumEmeraldGreen,
                        selectedTextColor = PremiumEmeraldGreen,
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark,
                        indicatorColor = PremiumEmeraldGreen.copy(alpha = 0.12f)
                    )
                )
            }
        }
    ) { innerPadding ->
        PremiumBackgroundDecoration {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                content()

                // Connection indicator overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            if (connection == NetworkQuality.EXCELLENT) PremiumEmeraldGreen.copy(alpha = 0.15f)
                            else GoldenGlow.copy(alpha = 0.2f)
                        )
                        .clickable {
                            viewModel.toggleNetworkMode()
                            val msg = if (connection == NetworkQuality.EXCELLENT)
                                "Network changed: EXCELLENT (Sub-second settle active)"
                            else
                                "Network changed: WEAK (Adaptive Offline Queues enabled)"
                            Toast
                                .makeText(context, msg, Toast.LENGTH_SHORT)
                                .show()
                        }
                        .border(
                            1.dp,
                            if (connection == NetworkQuality.EXCELLENT) PremiumEmeraldGreen.copy(alpha = 0.4f)
                            else GoldenGlow.copy(alpha = 0.5f),
                            RoundedCornerShape(30.dp)
                        )
                        .padding(vertical = 6.dp, horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (connection == NetworkQuality.EXCELLENT) PremiumEmeraldGreen
                                else GoldenGlow
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (connection == NetworkQuality.EXCELLENT) "Excellent Network" else "Weak Connection",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (connection == NetworkQuality.EXCELLENT) PremiumEmeraldGreen else GoldenGlow
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Connection Indicator",
                        modifier = Modifier.size(12.dp),
                        tint = if (connection == NetworkQuality.EXCELLENT) PremiumEmeraldGreen else GoldenGlow
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. HOME SCREEN
// ==========================================
@Composable
fun HomeScreen(navController: NavHostController, viewModel: PayMitraViewModel) {
    val profile by viewModel.userProfile.collectAsState()
    val favorites by viewModel.favoriteContacts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val pendingQueue by viewModel.pendingQueue.collectAsState()
    var balanceVisible by remember { mutableStateOf(false) }
    var activeScratchCardIndex by remember { mutableStateOf<Int?>(null) }
    var scratchCardRevealed by remember { mutableStateOf(false) }
    var scratchCashbackAmount by remember { mutableStateOf(0) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Welcome Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val greeting = remember {
                        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                        when {
                            hour < 12 -> "Good Morning"
                            hour < 17 -> "Good Afternoon"
                            else -> "Good Evening"
                        }
                    }
                    Text(
                        text = "$greeting, 👋",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryDark)
                    )
                    Text(
                        text = profile?.name ?: "PayMitra User",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                }

                // Profile Image Placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(RoyalBlue.copy(alpha = 0.2f))
                        .border(1.dp, PremiumEmeraldGreen, CircleShape)
                        .clickable {
                            Toast
                                .makeText(context, "UPI Profile Settings active", Toast.LENGTH_SHORT)
                                .show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile?.name?.firstOrNull()?.toString()?.uppercase() ?: "P",
                        color = PremiumEmeraldGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Available Balance / Account Card (Bento Design with radial glows)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF0A0C16)) // Sleek dark slate body
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.02f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .drawBehind {
                        // Emerald neon top-right radial glow (HTML style)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(PremiumEmeraldGreen.copy(alpha = 0.18f), Color.Transparent),
                                center = Offset(size.width, 0f),
                                radius = size.width * 0.45f
                            )
                        )
                        // Royal Blue bottom-left radial glow (HTML style)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(RoyalBlue.copy(alpha = 0.18f), Color.Transparent),
                                center = Offset(0f, size.height),
                                radius = size.width * 0.4f
                            )
                        )
                    }
                    .clickable { balanceVisible = !balanceVisible }
                    .padding(24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = profile?.bankName ?: "Connecting Bank...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = profile?.bankAccountNumber ?: "Acct Discovering...",
                                fontSize = 11.sp,
                                color = TextSecondaryDark,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(vertical = 4.dp, horizontal = 10.dp)
                        ) {
                            Text(
                                text = "PAYMITRA",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Available Balance",
                                fontSize = 11.sp,
                                color = TextSecondaryDark,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            AnimatedVisibility(
                                visible = balanceVisible,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Text(
                                    text = "₹${profile?.balance ?: "15,000"}.00",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                )
                            }
                            if (!balanceVisible) {
                                Text(
                                    text = "••••••",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = { balanceVisible = !balanceVisible },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = if (balanceVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Balance visibility",
                                tint = PremiumEmeraldGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // UPI Details Banner (Bento style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "VPA ID",
                                tint = PremiumEmeraldGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = profile?.upiId ?: "generating@paymitra",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondaryDark
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    Toast
                                        .makeText(context, "UPI ID copied to clipboard!", Toast.LENGTH_SHORT)
                                        .show()
                                }
                        )
                    }
                }
            }
        }

        // Quick Actions Bento Grid Row (HTML styling style)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Quick Settle Actions",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Action 1: SCAN QR
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { navController.navigate(Routes.SCAN_PAY) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = PremiumEmeraldGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "SCAN QR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryDark,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Action 2: CONTACTS
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (favorites.isNotEmpty()) {
                                    navController.navigate(Routes.makePaymentRoute(favorites.last().name, favorites.last().upiId))
                                } else {
                                    Toast.makeText(context, "Recent contacts list is empty.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = "Contacts",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "CONTACTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryDark,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Action 3: TRANSFER
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (favorites.isNotEmpty()) {
                                    navController.navigate(Routes.makePaymentRoute(favorites.first().name, favorites.first().upiId))
                                } else {
                                    Toast.makeText(context, "No recent contacts. Use Scan QR.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Transfer",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "TRANSFER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryDark,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Action 4: BANK
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                navController.navigate(Routes.makePaymentRoute("State Bank Merchant", "sbi@paymitra", "150"))
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Bank",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "BANK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryDark,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Favorite Contacts Horizontal Slider
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Contacts",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )

                    Text(
                        text = "+ Add Contact",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumEmeraldGreen,
                        modifier = Modifier.clickable {
                            viewModel.addFavorite("Vikash Jha", "vikash@paymitra")
                            Toast.makeText(context, "Vikash Jha added to favorites", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(favorites) { contact ->
                        val avatarColor = remember(contact.avatarColorHex) {
                            try {
                                Color(android.graphics.Color.parseColor(contact.avatarColorHex.replace("0x", "#")))
                            } catch (e: Exception) {
                                PremiumEmeraldGreen
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    navController.navigate(Routes.makePaymentRoute(contact.name, contact.upiId))
                                }
                                .width(64.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor.copy(alpha = 0.15f))
                                    .border(1.5.dp, avatarColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "",
                                    color = avatarColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = contact.name.split(" ").firstOrNull() ?: "",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Weak Network pending transactions alert
        if (pendingQueue.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GoldenGlow.copy(alpha = 0.15f))
                        .border(1.dp, GoldenGlow, RoundedCornerShape(16.dp))
                        .clickable {
                            viewModel.syncQueue()
                            Toast.makeText(context, "Syncing pending requests with UPI network...", Toast.LENGTH_SHORT).show()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = "Queue Alert", tint = GoldenGlow)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pending Offline Sync (${pendingQueue.size})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Waiting for network to confirm.",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.syncQueue()
                            Toast.makeText(context, "Settle synchronization dispatched!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldenGlow),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Sync Now", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Rewards Center (Gamified Scratch Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Scratch & Settle Rewards",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (i in 0..1) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(100.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(RoyalBlue, PremiumEmeraldGreen)
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .clickable {
                                    activeScratchCardIndex = i
                                    scratchCardRevealed = false
                                    scratchCashbackAmount = (10..150).random()
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = "Reward", tint = GoldenGlow, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap to Scratch",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Service Cards Category (Asymmetrical Bento Grid Layout)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Bento Services Grid",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Column (comprises "Mobile Recharge" card at top, and "Electricity" & "Rewards" cards side-by-side at bottom)
                    Column(
                        modifier = Modifier.weight(4f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Mobile Recharge Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                .clickable {
                                    navController.navigate(Routes.makePaymentRoute("Airtel PrePaid", "airtel@upi", "349"))
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text(
                                        text = "Mobile Recharge",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Instant activation",
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFFECE0)), // Light orange-red background like HTML
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = "Mobile Recharge",
                                        tint = Color(0xFFFF5722),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Bottom row containing Electricity and Rewards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Electricity Utility Card
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                    .clickable {
                                        navController.navigate(Routes.makePaymentRoute("BSES Yamuna", "electricity@upi", "1250"))
                                    }
                                    .padding(12.dp),
                                contentAlignment = Alignment.TopStart
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFFFF9C4)), // light yellow
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = "Electricity",
                                            tint = Color(0xFFFBC02D),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "Electricity",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Rewards Card (Gold/Amber)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(GoldenGlow) // Gold background like HTML
                                    .clickable {
                                        activeScratchCardIndex = 0
                                        scratchCardRevealed = false
                                        scratchCashbackAmount = (10..150).random()
                                    }
                                    .padding(12.dp),
                                contentAlignment = Alignment.TopStart
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CardGiftcard,
                                            contentDescription = "Rewards",
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "Rewards",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    // Right Column: Bill Pay (Tall blue card, weight 2.5f)
                    Box(
                        modifier = Modifier
                            .weight(2.5f)
                            .height(197.dp) // Perfect matching height (90 + 12 + 95)
                            .clip(RoundedCornerShape(24.dp))
                            .background(RoyalBlue) // Royal Blue color
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                            .clickable {
                                navController.navigate(Routes.makePaymentRoute("Delhi Jal Board", "djb@upi", "450"))
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn, // Bolt/Flash icon for instant bill pay
                                    contentDescription = "Bill Pay",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Bill",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 18.sp
                                )
                                Text(
                                    text = "Pay",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Payments Header & Mini List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Payments Ledger",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Text(
                    text = "View Ledger",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PremiumEmeraldGreen,
                    modifier = Modifier.clickable { navController.navigate(Routes.TRANSACTIONS) }
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions recorded yet.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(transactions.take(4)) { tx ->
                TransactionRow(tx = tx)
            }
        }
    }

    // Reward Dialog overlay
    if (activeScratchCardIndex != null) {
        AlertDialog(
            onDismissRequest = { activeScratchCardIndex = null },
            confirmButton = {
                TextButton(onClick = { activeScratchCardIndex = null }) {
                    Text("Awesome!", color = PremiumEmeraldGreen)
                }
            },
            containerColor = LuxuryBgDark,
            title = {
                Text(
                    text = "PayMitra Scratch Card",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (scratchCardRevealed) Color.Black else Color.DarkGray)
                        .clickable { scratchCardRevealed = true }
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (scratchCardRevealed) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Won", tint = PremiumEmeraldGreen, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Flat Cash Settle Success!", color = TextSecondaryDark, fontSize = 12.sp)
                            Text(
                                text = "₹$scratchCashbackAmount Cashback",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = GoldenGlow,
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scratch", tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Swipe/Tap here to scratch!", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ServiceItem(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .width(74.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = TextSecondaryDark,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TransactionRow(tx: DbTransaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val isSuccess = tx.status == "SUCCESS"
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSuccess) PremiumEmeraldGreen.copy(alpha = 0.12f)
                        else GoldenGlow.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tx.recipientName.firstOrNull()?.toString()?.uppercase() ?: "T",
                    color = if (isSuccess) PremiumEmeraldGreen else GoldenGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = tx.recipientName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = tx.recipientUpiId,
                    color = TextSecondaryDark,
                    fontSize = 10.sp
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "₹${tx.amount}",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isSuccess = tx.status == "SUCCESS"
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isSuccess) PremiumEmeraldGreen else GoldenGlow)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tx.status,
                    color = if (isSuccess) PremiumEmeraldGreen else GoldenGlow,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==========================================
// 4. SCAN & PAY SCREEN
// ==========================================
@Composable
fun ScanPayScreen(navController: NavHostController) {
    val context = LocalContext.current
    var isFlashOn by remember { mutableStateOf(false) }

    val scanSuggestions = listOf(
        Triple("Arjun Sharma", "arjun@ybl", "500"),
        Triple("Sharma Grocery Store", "sharma.grocery@okhdfc", "120"),
        Triple("Chai Tapri Corner", "chaitapri@paytm", "20"),
        Triple("Priya Patel", "priya@okhdfc", "1250")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Instant QR Scanner",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )

            IconButton(onClick = { isFlashOn = !isFlashOn }) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flash",
                    tint = if (isFlashOn) GoldenGlow else Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AI Viewfinder Camera Box Simulator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(2.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Simulated camera scanner drawing with moving beam line
            val infiniteTransition = rememberInfiniteTransition(label = "Laser")
            val laserY by infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "LaserY"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Moving green neon scan line
                drawLine(
                    color = PremiumEmeraldGreen,
                    start = Offset(20.dp.toPx(), size.height * laserY),
                    end = Offset(size.width - 20.dp.toPx(), size.height * laserY),
                    strokeWidth = 3.dp.toPx()
                )

                // Corner bracket layouts to represent a QR scanner
                val cornerLength = 24.dp.toPx()
                val thickness = 4.dp.toPx()
                val offset = 30.dp.toPx()

                // Top Left
                drawRect(PremiumEmeraldGreen, Offset(offset, offset), Size(cornerLength, thickness))
                drawRect(PremiumEmeraldGreen, Offset(offset, offset), Size(thickness, cornerLength))

                // Top Right
                drawRect(PremiumEmeraldGreen, Offset(size.width - offset - cornerLength, offset), Size(cornerLength, thickness))
                drawRect(PremiumEmeraldGreen, Offset(size.width - offset - thickness, offset), Size(thickness, cornerLength))

                // Bottom Left
                drawRect(PremiumEmeraldGreen, Offset(offset, size.height - offset - thickness), Size(cornerLength, thickness))
                drawRect(PremiumEmeraldGreen, Offset(offset, size.height - offset - cornerLength), Size(thickness, cornerLength))

                // Bottom Right
                drawRect(PremiumEmeraldGreen, Offset(size.width - offset - cornerLength, size.height - offset - thickness), Size(cornerLength, thickness))
                drawRect(PremiumEmeraldGreen, Offset(size.width - offset - thickness, size.height - offset - cornerLength), Size(thickness, cornerLength))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = "Scan target",
                    tint = PremiumEmeraldGreen.copy(alpha = 0.4f),
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Center any QR inside the frame",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Simulated QR decoder selections
        Text(
            text = "AI QR Sandbox Simulation",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Text(
            text = "Select a simulated QR to instantly scan & settle",
            fontSize = 11.sp,
            color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            scanSuggestions.forEach { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .clickable {
                            Toast.makeText(context, "AI Code Match: ${suggestion.first} detected!", Toast.LENGTH_SHORT).show()
                            navController.navigate(
                                Routes.makePaymentRoute(
                                    suggestion.first,
                                    suggestion.second,
                                    suggestion.third
                                )
                            )
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "QR", tint = PremiumEmeraldGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(suggestion.first, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(suggestion.second, color = TextSecondaryDark, fontSize = 10.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("₹${suggestion.third}", color = GoldenGlow, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = "Proceed", tint = PremiumEmeraldGreen, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. PAYMENT SCREEN & OXYGENOS ANIMATION
// ==========================================
enum class PaymentState {
    INPUT, PROCESS, SUCCESS, FAIL
}

@Composable
fun PaymentScreen(
    recipientName: String,
    recipientUpiId: String,
    initialAmount: String,
    viewModel: PayMitraViewModel,
    onBack: () -> Unit,
    onPaymentComplete: () -> Unit
) {
    var amountText by remember { mutableStateOf(if (initialAmount == "0") "" else initialAmount) }
    var noteText by remember { mutableStateOf("") }
    var paymentState by remember { mutableStateOf(PaymentState.INPUT) }
    var errorMessage by remember { mutableStateOf("") }
    var latestTransaction by remember { mutableStateOf<DbTransaction?>(null) }
    
    val profile by viewModel.userProfile.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Keyboard action callback
    val onKeyClick: (String) -> Unit = { key ->
        if (paymentState == PaymentState.INPUT) {
            when (key) {
                "BACK" -> {
                    if (amountText.isNotEmpty()) {
                        amountText = amountText.dropLast(1)
                    }
                }
                "." -> {
                    if (!amountText.contains(".")) {
                        amountText = if (amountText.isEmpty()) "0." else "$amountText."
                    }
                }
                else -> {
                    // Prevent giant unrealistic numbers
                    if (amountText.length < 7) {
                        amountText = if (amountText == "0") key else amountText + key
                    }
                }
            }
        }
    }

    PremiumBackgroundDecoration {
        Box(modifier = Modifier.fillMaxSize()) {
            when (paymentState) {
                PaymentState.INPUT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("UPI Transfer", style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Recipient Profile Card
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(PremiumEmeraldGreen.copy(alpha = 0.15f))
                                        .border(1.5.dp, PremiumEmeraldGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        recipientName.firstOrNull()?.toString()?.uppercase() ?: "",
                                        color = PremiumEmeraldGreen,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 20.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(recipientName, style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.Bold))
                                    Text(recipientUpiId, fontSize = 11.sp, color = TextSecondaryDark)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Amount Sizing Indicator
                        Text(
                            text = "Enter settlement amount",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Text("₹", style = MaterialTheme.typography.displayMedium.copy(color = PremiumEmeraldGreen, fontWeight = FontWeight.Black))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = amountText.ifEmpty { "0" },
                                style = MaterialTheme.typography.displayLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                ),
                                modifier = Modifier.testTag("amount_indicator")
                            )
                        }

                        // Note Input
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = { Text("Add payment note (e.g. rent, dinner)", color = TextSecondaryDark, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PremiumEmeraldGreen.copy(alpha = 0.3f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Glassmorphic Custom Numeric Keyboard
                        NumericKeyboard(onKeyClick = onKeyClick)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Slide/Submit Pay Button
                        Button(
                            onClick = {
                                val amountVal = amountText.toDoubleOrNull() ?: 0.0
                                if (amountVal <= 0.0) {
                                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                } else {
                                    scope.launch {
                                        paymentState = PaymentState.PROCESS
                                        delay(2200) // Beautiful OxygenOS Ring progress wait
                                        
                                        val res = viewModel.pay(recipientName, recipientUpiId, amountVal, noteText)
                                        when (res) {
                                            is PaymentResult.Success -> {
                                                latestTransaction = res.transaction
                                                paymentState = PaymentState.SUCCESS
                                            }
                                            is PaymentResult.Queued -> {
                                                latestTransaction = res.transaction
                                                paymentState = PaymentState.SUCCESS
                                            }
                                            is PaymentResult.Failure -> {
                                                errorMessage = res.message
                                                paymentState = PaymentState.FAIL
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumEmeraldGreen),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("submit_payment_button")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = "Secure pay")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Securely Settle ₹${amountText.ifEmpty { "0" }}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.Black
                            )
                        }
                    }
                }

                PaymentState.PROCESS -> {
                    // OxygenOS Premium Animation with custom drawing
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier.size(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "CircleWave")
                            val progressAngle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "Spin"
                            )

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Background outer dim circle
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.05f),
                                    style = Stroke(width = 6.dp.toPx())
                                )

                                // Active emerald spinning progress ring
                                drawArc(
                                    color = PremiumEmeraldGreen,
                                    startAngle = progressAngle,
                                    sweepAngle = 110f,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Inner secondary pulsing blue glow
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(RoyalBlue.copy(alpha = 0.15f), Color.Transparent),
                                        radius = size.width * 0.45f
                                    )
                                )
                            }

                            Icon(
                                Icons.Default.Security,
                                contentDescription = "Securing",
                                tint = PremiumEmeraldGreen,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        Text(
                            text = "Securing UPI Gateway Settle...",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Direct bank-to-bank instant validation active",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                PaymentState.SUCCESS -> {
                    // Confetti successes & Receipts
                    val tx = latestTransaction
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))

                        // Glow Icon Success
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(PremiumEmeraldGreen.copy(alpha = 0.12f))
                                .border(2.dp, PremiumEmeraldGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Success check",
                                tint = PremiumEmeraldGreen,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = if (tx?.status == "PENDING") "Settle Queued Offline!" else "UPI Settlement Success!",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = if (tx?.status == "PENDING") GoldenGlow else PremiumEmeraldGreen,
                                fontWeight = FontWeight.Black
                            )
                        )

                        Text(
                            text = if (tx?.status == "PENDING") "Weak Network Detected. Transaction logged locally." else "Settle completed instantly with 100% security.",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        // Receipt Card Slide-up Simulator
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "PayMitra Secure Receipt",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Amount Settle", color = TextSecondaryDark, fontSize = 12.sp)
                                Text("₹${tx?.amount}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.05f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Recipient", color = TextSecondaryDark, fontSize = 12.sp)
                                Text(tx?.recipientName ?: "", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.05f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Recipient UPI ID", color = TextSecondaryDark, fontSize = 12.sp)
                                Text(tx?.recipientUpiId ?: "", color = Color.White, fontSize = 12.sp)
                            }
                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.05f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Payment Gateway", color = TextSecondaryDark, fontSize = 12.sp)
                                Text("BHIM UPI 2.0", color = PremiumEmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Button(
                            onClick = onPaymentComplete,
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumEmeraldGreen),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text("Done & Settle Dashboard", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                PaymentState.FAIL -> {
                    // Retry visual display
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = 0.12f))
                                .border(1.5.dp, Color.Red, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Error, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(48.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Validation Failed",
                            style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = errorMessage,
                            color = TextSecondaryDark,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                        )

                        Button(
                            onClick = { paymentState = PaymentState.INPUT },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text("Retry Transaction", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(onClick = onBack) {
                            Text("Cancel payment", color = TextSecondaryDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NumericKeyboard(onKeyClick: (String) -> Unit) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "BACK")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { onKeyClick(key) }
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "BACK") {
                            Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = Color.White)
                        } else {
                            Text(
                                text = key,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. AI INSIGHTS & BUDGET DASHBOARD
// ==========================================
@Composable
fun AiInsightsScreen(viewModel: PayMitraViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val budget by viewModel.budget.collectAsState()
    val aiInsightsState by viewModel.aiInsights.collectAsState()
    
    val totalSpent = remember(transactions) {
        transactions.filter { it.status == "SUCCESS" }.sumOf { it.amount }
    }

    // Spend breakdown by categories for custom chart rendering
    val categoryStats = remember(transactions) {
        transactions.filter { it.status == "SUCCESS" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
    }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var userQuestion by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var customBudgetInput by remember { mutableStateOf(budget.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = "MitraAI Spending Co-pilot",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
            Text(
                text = "Track your budget, analyze categories, & ask MitraAI",
                color = TextSecondaryDark,
                fontSize = 12.sp
            )
        }

        // Budget overview progress bar
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Monthly Limit Budget", color = TextSecondaryDark, fontSize = 12.sp)
                        Text("₹$budget", style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Black))
                    }

                    Button(
                        onClick = { showBudgetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumEmeraldGreen),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Modify Limit", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Progress Indicator bar
                val ratio = if (budget > 0) (totalSpent / budget).toFloat().coerceIn(0f, 1f) else 0f
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Spent: ₹$totalSpent", color = TextSecondaryDark, fontSize = 11.sp)
                        val remaining = budget - totalSpent
                        Text(
                            text = if (remaining >= 0) "Rem: ₹$remaining" else "Over Limit by ₹${-remaining}",
                            color = if (remaining >= 0) PremiumEmeraldGreen else Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(RoyalBlue, if (ratio > 0.9f) Color.Red else PremiumEmeraldGreen)
                                    )
                                )
                        )
                    }
                }
            }
        }

        // Expenditure Categories Custom Donut/Bar Chart (Custom drawing)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Spending Categories Breakdown",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (categoryStats.isEmpty()) {
                    Text(
                        text = "Make a transaction to populate category stats.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Custom Bar charts with category names
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        categoryStats.forEach { (cat, spent) ->
                            val maxSpend = categoryStats.values.maxOrNull() ?: 1.0
                            val share = (spent / maxSpend).toFloat().coerceIn(0.1f, 1f)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("₹$spent", color = TextSecondaryDark, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(share)
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(PremiumEmeraldGreen)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Conversational AI Dialog / Chatbot Section
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = PremiumEmeraldGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "MitraAI Live Copilot Advisor",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (val state = aiInsightsState) {
                    is AiInsightsState.Idle -> {
                        Text(
                            text = "Need budget optimization advice? Settle insights with the click of a button.",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.generateInsights() },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumEmeraldGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Query MitraAI Insights", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    is AiInsightsState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(color = PremiumEmeraldGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("MitraAI analyzing transaction categories...", color = TextSecondaryDark, fontSize = 12.sp)
                        }
                    }
                    is AiInsightsState.Success -> {
                        Text(
                            text = state.insights,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.generateInsights() },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumEmeraldGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Refresh Analysis Insights", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    is AiInsightsState.Error -> {
                        Text(state.message, color = Color.Red, fontSize = 12.sp)
                    }
                }
            }
        }

        // Question input field
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userQuestion,
                    onValueChange = { userQuestion = it },
                    placeholder = { Text("Ask MitraAI: 'Am I on budget?'", color = TextSecondaryDark, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PremiumEmeraldGreen,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (userQuestion.isNotBlank()) {
                            viewModel.generateInsights() // Triggers analysis
                            Toast.makeText(context, "Query sent to MitraAI!", Toast.LENGTH_SHORT).show()
                            userQuestion = ""
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(PremiumEmeraldGreen)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Query", tint = Color.Black)
                }
            }
        }
    }

    // Budget Dialog
    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amt = customBudgetInput.toDoubleOrNull() ?: budget
                        viewModel.updateBudget(amt)
                        showBudgetDialog = false
                    }
                ) {
                    Text("Confirm", color = PremiumEmeraldGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = LuxuryBgDark,
            title = { Text("Update Limit Budget", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = customBudgetInput,
                    onValueChange = { customBudgetInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PremiumEmeraldGreen
                    )
                )
            }
        )
    }
}

// ==========================================
// 7. TRANSACTIONS SCREEN
// ==========================================
@Composable
fun TransactionsScreen(viewModel: PayMitraViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    var searchPrompt by remember { mutableStateOf("") }

    val filteredList = remember(transactions, searchPrompt) {
        if (searchPrompt.isBlank()) transactions
        else transactions.filter {
            it.recipientName.contains(searchPrompt, ignoreCase = true) ||
                    it.recipientUpiId.contains(searchPrompt, ignoreCase = true) ||
                    it.category.contains(searchPrompt, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Payments Ledger Database",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        )

        // Search Ledger
        OutlinedTextField(
            value = searchPrompt,
            onValueChange = { searchPrompt = it },
            placeholder = { Text("Search by recipient, UPI, or category", color = TextSecondaryDark, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PremiumEmeraldGreen) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PremiumEmeraldGreen,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = "Empty", tint = TextSecondaryDark, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No transactions found matching query.", color = TextSecondaryDark, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList) { tx ->
                    TransactionRow(tx = tx)
                }
            }
        }
    }
}
