package com.example.ui

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

enum class AppTab {
    DASHBOARD, PROGRAMS, CUSTOM_TIMER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutApp(viewModel: WorkoutViewModel) {
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var selectedDetailProgram by remember { mutableStateOf<WorkoutTable?>(null) }
    
    val activeProgram by viewModel.activeProgram.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    
    // Sound Generator Setup
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Throwable) {
            null
        }
    }
    
    DisposableEffect(toneGenerator) {
        onDispose {
            try {
                toneGenerator?.release()
            } catch (e: Throwable) {
                // Ignore gracefully
            }
        }
    }
    
    // Sound triggers from VM
    LaunchedEffect(Unit) {
        viewModel.soundAlertTrigger.collectLatest { type ->
            try {
                when (type) {
                    WorkoutViewModel.SoundAlertType.TICK -> {
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                    }
                    WorkoutViewModel.SoundAlertType.START -> {
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 250)
                    }
                    WorkoutViewModel.SoundAlertType.REST -> {
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
                    }
                    WorkoutViewModel.SoundAlertType.CONGRATS -> {
                        toneGenerator?.startTone(ToneGenerator.TONE_SUP_CONGESTION, 500)
                    }
                }
            } catch (e: Throwable) {
                // Ignore audio errors gracefully
            }
        }
    }
    
    // Enforce Arabic RTL layout globally for consistency in translation and alignment
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            bottomBar = {
                // Hide main navigation if the interactive player is active for a focus mode layout
                if (playerState == WorkoutViewModel.PlayerState.IDLE) {
                    NavigationBar(
                        containerColor = CardSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = currentTab == AppTab.DASHBOARD,
                            onClick = { 
                                currentTab = AppTab.DASHBOARD
                                selectedDetailProgram = null
                            },
                            icon = { Icon(if (currentTab == AppTab.DASHBOARD) Icons.Filled.Dashboard else Icons.Outlined.Dashboard, contentDescription = "الرئيسية") },
                            label = { Text("الرئيسية", fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryOrange,
                                selectedTextColor = PrimaryOrange,
                                indicatorColor = HighlightSurface,
                                unselectedIconColor = TextGray,
                                unselectedTextColor = TextGray
                            ),
                            modifier = Modifier.testTag("nav_tab_dashboard")
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.PROGRAMS,
                            onClick = { 
                                currentTab = AppTab.PROGRAMS 
                                selectedDetailProgram = null
                            },
                            icon = { Icon(if (currentTab == AppTab.PROGRAMS) Icons.Filled.FitnessCenter else Icons.Outlined.FitnessCenter, contentDescription = "الجداول") },
                            label = { Text("الجداول التدريبية", fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryOrange,
                                selectedTextColor = PrimaryOrange,
                                indicatorColor = HighlightSurface,
                                unselectedIconColor = TextGray,
                                unselectedTextColor = TextGray
                            ),
                            modifier = Modifier.testTag("nav_tab_programs")
                        )
                        NavigationBarItem(
                            selected = currentTab == AppTab.CUSTOM_TIMER,
                            onClick = { 
                                currentTab = AppTab.CUSTOM_TIMER
                                selectedDetailProgram = null
                            },
                            icon = { Icon(if (currentTab == AppTab.CUSTOM_TIMER) Icons.Filled.Timer else Icons.Outlined.Timer, contentDescription = "المؤقت") },
                            label = { Text("مؤقت مخصص", fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryOrange,
                                selectedTextColor = PrimaryOrange,
                                indicatorColor = HighlightSurface,
                                unselectedIconColor = TextGray,
                                unselectedTextColor = TextGray
                            ),
                            modifier = Modifier.testTag("nav_tab_timer")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .drawBehindBackgroundAccent()
                    .padding(paddingValues)
            ) {
                // Switch Content based on tab
                if (playerState != WorkoutViewModel.PlayerState.IDLE) {
                    WorkoutPlayerOverlay(
                        viewModel = viewModel,
                        onClose = { viewModel.exitPlayer() }
                    )
                } else {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                        },
                        label = "tab_transitions"
                    ) { tab ->
                        when (tab) {
                            AppTab.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onSelectProgram = { program ->
                                    selectedDetailProgram = program
                                    currentTab = AppTab.PROGRAMS
                                }
                            )
                            AppTab.PROGRAMS -> {
                                if (selectedDetailProgram != null) {
                                    ProgramDetailScreen(
                                        program = selectedDetailProgram!!,
                                        onBack = { selectedDetailProgram = null },
                                        onStart = {
                                            viewModel.selectProgram(selectedDetailProgram!!)
                                            viewModel.startProgram()
                                        }
                                    )
                                } else {
                                    ProgramsScreen(
                                        onProgramClick = { selectedDetailProgram = it }
                                    )
                                }
                            }
                            AppTab.CUSTOM_TIMER -> CustomTimerScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- DASHBOARD TAB ----------------

@Composable
fun DashboardScreen(
    viewModel: WorkoutViewModel,
    onSelectProgram: (WorkoutTable) -> Unit
) {
    val streakDays by viewModel.streakDays.collectAsStateWithLifecycle()
    val totalWorkouts by viewModel.totalWorkouts.collectAsStateWithLifecycle()
    val totalMinutes by viewModel.totalMinutes.collectAsStateWithLifecycle()
    val totalCalories by viewModel.totalCalories.collectAsStateWithLifecycle()
    val historyLog by viewModel.history.collectAsStateWithLifecycle()
    
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcoming Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مرحباً بك، بطل!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "جاهز لتمرين رائع اليوم للمبتدئين؟ ⚡",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextGray
                    )
                }
                IconButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .background(HighlightSurface, CircleShape)
                        .testTag("reset_all_data_btn")
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "مسح كافة البيانات",
                        tint = Color.Red.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Streak & Interactive Motivation Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(BorderSlate, PrimaryOrange.copy(alpha = 0.25f))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehindBackgroundAccent()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سلسلة التحدي اليومي",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (streakDays > 0) 
                                    "أنت تبلي بلاءً رائعاً! حافظ على استمرارك يوماً بعد يوم."
                                    else "ابدأ يومك الأول اليوم وابنِ عادة رياضية مستدامة!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(PrimaryOrange.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$streakDays أيام متتالية",
                                        fontWeight = FontWeight.Black,
                                        color = PrimaryOrange,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                        
                        // Circular Streak Visualizer
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(90.dp)
                        ) {
                            val animatedSweep by animateFloatAsState(
                                targetValue = if (streakDays > 0) (streakDays * 30f).coerceAtMost(360f) else 15f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "streak_progress"
                            )
                            Canvas(modifier = Modifier.size(80.dp)) {
                                drawCircle(
                                    color = HighlightSurface,
                                    radius = size.minDimension / 2,
                                    style = Stroke(width = 8.dp.toPx())
                                )
                                drawArc(
                                    color = PrimaryOrange,
                                    startAngle = -90f,
                                    sweepAngle = animatedSweep,
                                    useCenter = false,
                                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(30.dp)
                                )
                                Text(
                                    text = "$streakDays",
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Stats Metrics grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Workouts
                StatWidget(
                    title = "إجمالي التمارين",
                    value = "$totalWorkouts",
                    unit = "تمارين",
                    icon = Icons.Default.DirectionsRun,
                    tint = SecondaryNeonGreen,
                    modifier = Modifier.weight(1f)
                )
                // Total Minutes
                StatWidget(
                    title = "الوقت النشط",
                    value = "$totalMinutes",
                    unit = "دقيقة",
                    icon = Icons.Default.AccessTime,
                    tint = TertiaryElectricBlue,
                    modifier = Modifier.weight(1f)
                )
                // Total Calories
                StatWidget(
                    title = "السعرات المكتسبة",
                    value = "$totalCalories",
                    unit = "كالو",
                    icon = Icons.Default.Whatshot,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Beginner Health & Training Tips Carousel
        item {
            Text(
                text = "نصيحة اليوم للمبتدئين 💡",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            val tips = listOf(
                "الاستمرار بريتم خفيف أفضل بكثير من مجهود شاق لمرة واحدة وتقطع العادات.",
                "لا تنسَ شرب كميات كافية من المياه أثناء التدريب، ومارس عملية الزفير في ذروة الجهد والشهيق عند العودة بالبداية.",
                "الراحة بين التمارين جزء أساسي من تطور اللياقة البدنية ونمو الكتلة العضلية وتحسين النفس.",
                "تمارين الضغط على الركبتين مخصصة لتبسيط التمرين للمبتدئين وبناء النوى العلوية بطريقة آمنة ومتوسطة الضغط مفاصلك."
            )
            val randomTip = remember { tips.random() }
            Card(
                colors = CardDefaults.cardColors(containerColor = HighlightSurface.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(BorderSlate.copy(alpha = 0.5f), TertiaryElectricBlue.copy(alpha = 0.22f))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(TertiaryElectricBlue.copy(alpha = 0.15f), CircleShape)
                            .padding(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = TertiaryElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = randomTip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextWhite,
                        lineHeight = 22.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Recent Workouts History Header
        item {
            Text(
                text = "سجل النشاط التدريبي المؤخر",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Empty state vs History list
        if (historyLog.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد تمارين مسجلة حالياً.",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "قم ببدء تمرينك الأول من تبويب الجداول لتسجيل نشاطاتك وبناء إحصائياتك هنا!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(historyLog.take(6)) { item ->
                HistoryItemRow(item)
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("تأكيد مسح البيانات") },
            text = { Text("هل أنت متأكد من رغبتك في حذف كافة سجلاتك التدريبية وصفر سلسلة التحديات الحالية؟ لا يمكن التراجع عن هذه الخطوة.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistoryData()
                        showResetDialog = false
                    }
                ) {
                    Text("نعم، احذف الكل", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("إلغاء", color = Color.White)
                }
            },
            containerColor = CardSurface,
            titleContentColor = Color.White,
            textContentColor = TextGray
        )
    }
}

@Composable
fun StatWidget(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(tint.copy(alpha = 0.5f), BorderSlate.copy(alpha = 0.2f))
            ),
            shape = RoundedCornerShape(20.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .background(tint.copy(alpha = 0.12f), CircleShape)
                    .padding(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = TextWhite
            )
            Text(
                text = "$unit",
                fontSize = 11.sp,
                color = TextGray,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                color = TextGray.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HistoryItemRow(item: WorkoutHistoryItem) {
    val formatter = remember { SimpleDateFormat("yyyy/MM/dd | HH:mm", Locale.getDefault()) }
    val formattedDate = remember(item.timestamp) { formatter.format(Date(item.timestamp)) }
    
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSlate.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .background(PrimaryOrange.copy(alpha = 0.15f), CircleShape)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SecondaryNeonGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = item.workoutTitle,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${item.durationSeconds / 60}د : ${item.durationSeconds % 60}ث",
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryOrange,
                    fontSize = 14.sp
                )
                Text(
                    text = "🔥 ${item.caloriesBurned} سعرة",
                    fontSize = 11.sp,
                    color = TextGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ---------------- PROGRAMS TAB ----------------

@Composable
fun ProgramsScreen(
    onProgramClick: (WorkoutTable) -> Unit
) {
    val programs = WorkoutData.beginnerPrograms
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "جداول المبتدئين 🏋️‍♂️",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "جداول تمارين منزلية آمنة وبدون من لوازم معقدة لبناء نمط حياة رياضي جديد وبسيط.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextGray
                )
            }
        }
        
        items(programs) { program ->
            ProgramCard(program = program, onClick = { onProgramClick(program) })
        }
    }
}

@Composable
fun ProgramCard(
    program: WorkoutTable,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(BorderSlate, PrimaryOrange.copy(alpha = 0.22f))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("program_card_${program.id}")
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(PrimaryOrange.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "مستوى كامل للمبتدئين",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "~${program.totalMinutes} دقيقة",
                        fontSize = 12.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = program.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = program.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Divider(color = BorderSlate.copy(alpha = 0.5f))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WorkoutMiniTag(icon = Icons.Default.FitnessCenter, label = "بدون أدوات")
                    WorkoutMiniTag(icon = Icons.Default.FormatListNumberedRtl, label = "${program.exercises.size} تمارين")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "تفاصيل وتدريب",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    Icon(
                        Icons.Default.ArrowBack, // Since RTL, arrow points left (backwards) or right correctly
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutMiniTag(icon: ImageVector, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
        Text(label, fontSize = 11.sp, color = TextGray)
    }
}

// ---------------- PROGRAM DETAIL SCREEN ----------------

@Composable
fun ProgramDetailScreen(
    program: WorkoutTable,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier.background(HighlightSurface, CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowForward, // Since we are in RTL, going back is forward arrow (points right)
                        contentDescription = "الرجوع لخِيار الجداول",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "تفاصيل البرنامج التدريبي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        
        // Massive Info Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSlate, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = program.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = program.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextGray,
                        lineHeight = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailTagWidget(
                            title = "الوقت الكلي",
                            value = "~${program.totalMinutes} دقيقة",
                            icon = Icons.Default.AccessTime,
                            modifier = Modifier.weight(1f)
                        )
                        DetailTagWidget(
                            title = "الأدوات",
                            value = program.equipments,
                            icon = Icons.Default.Home,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = { onStart() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(
                                brush = Brush.horizontalGradient(listOf(PrimaryOrange, Color(0xFFFF9E00))),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clip(RoundedCornerShape(18.dp))
                            .testTag("start_selected_program_btn")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ابدأ التمرين الآن 🚀",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
        
        // Exercises list header
        item {
            Text(
                text = "قائمة التمارين المجدولة (${program.exercises.size} تمارين)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        // Exercises item cards
        items(program.exercises) { exercise ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSlate.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Small visual indicator box with custom graphics representer
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(HighlightSurface, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(45.dp)) {
                            drawCircle(
                                color = PrimaryOrange.copy(alpha = 0.2f),
                                radius = size.minDimension / 2.2f
                            )
                            drawCircle(
                                color = PrimaryOrange,
                                radius = size.minDimension / 5f,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            // Draw dynamic small exercise lines inside representer box
                            drawLine(
                                color = PrimaryOrange,
                                start = Offset(size.width * 0.2f, size.height * 0.5f),
                                end = Offset(size.width * 0.8f, size.height * 0.5f),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = exercise.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "العضلة الرئيسية: ${exercise.targetMuscles}",
                            fontSize = 11.sp,
                            color = TertiaryElectricBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(PrimaryOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${exercise.durationSeconds} ثانية",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryOrange,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailTagWidget(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = HighlightSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
            Column {
                Text(title, fontSize = 10.sp, color = TextGray)
                Text(value, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------- CUSTOM TIMER TAB ----------------

@Composable
fun CustomTimerScreen(viewModel: WorkoutViewModel) {
    val workSec by viewModel.customWorkSeconds.collectAsStateWithLifecycle()
    val restSec by viewModel.customRestSeconds.collectAsStateWithLifecycle()
    val rounds by viewModel.customTotalRounds.collectAsStateWithLifecycle()
    val timerState by viewModel.customTimerState.collectAsStateWithLifecycle()
    val secondsRemaining by viewModel.customSecondsRemaining.collectAsStateWithLifecycle()
    val currentRound by viewModel.customCurrentRound.collectAsStateWithLifecycle()
    val isPaused by viewModel.customIsPaused.collectAsStateWithLifecycle()
    
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "مؤقت الفترات التفاعلي ⏱️",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "صمم فترات مخصصة لتمارينك البيتية وتحكم في أوقات الراحة والتوجيهات.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (timerState == WorkoutViewModel.PlayerState.IDLE || timerState == WorkoutViewModel.PlayerState.SUMMARY) {
            // Configuration mode
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSlate, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "تخصيص المؤقت الخاص بك",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    // Work Duration Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("وقت التمرين (ثانية)", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("$workSec ثانية", color = PrimaryOrange, fontWeight = FontWeight.Black)
                        }
                        Slider(
                            value = workSec.toFloat(),
                            onValueChange = { viewModel.updateCustomConfig(it.toInt(), restSec, rounds) },
                            valueRange = 10f..120f,
                            steps = 11,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryOrange,
                                activeTrackColor = PrimaryOrange,
                                inactiveTrackColor = HighlightSurface
                            ),
                            modifier = Modifier.testTag("custom_timer_work_slider")
                        )
                    }
                    
                    // Rest Duration Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("وقت الراحة (ثانية)", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("$restSec ثانية", color = SecondaryNeonGreen, fontWeight = FontWeight.Black)
                        }
                        Slider(
                            value = restSec.toFloat(),
                            onValueChange = { viewModel.updateCustomConfig(workSec, it.toInt(), rounds) },
                            valueRange = 5f..120f,
                            steps = 23,
                            colors = SliderDefaults.colors(
                                thumbColor = SecondaryNeonGreen,
                                activeTrackColor = SecondaryNeonGreen,
                                inactiveTrackColor = HighlightSurface
                            ),
                            modifier = Modifier.testTag("custom_timer_rest_slider")
                        )
                    }
                    
                    // Rounds Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("عدد الجولات / تكرارات", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("$rounds جولات", color = TertiaryElectricBlue, fontWeight = FontWeight.Black)
                        }
                        Slider(
                            value = rounds.toFloat(),
                            onValueChange = { viewModel.updateCustomConfig(workSec, restSec, it.toInt()) },
                            valueRange = 1f..15f,
                            steps = 14,
                            colors = SliderDefaults.colors(
                                thumbColor = TertiaryElectricBlue,
                                activeTrackColor = TertiaryElectricBlue,
                                inactiveTrackColor = HighlightSurface
                            ),
                            modifier = Modifier.testTag("custom_timer_rounds_slider")
                        )
                    }
                    
                    Button(
                        onClick = { viewModel.startCustomTimer() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(
                                brush = Brush.horizontalGradient(listOf(PrimaryOrange, Color(0xFFFF9E00))),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clip(RoundedCornerShape(18.dp))
                            .testTag("start_custom_timer_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ابدأ المؤقت الآن 🎯", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
            
            // If completed, show summary card
            if (timerState == WorkoutViewModel.PlayerState.SUMMARY) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SecondaryNeonGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SecondaryNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "عمل ممتاز! 🏆",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = SecondaryNeonGreen
                        )
                        Text(
                            "لقد أنجزت تمرين الفترات المخصص المكون من $rounds جولات، تم حفظ التمرين في سجلك!",
                            color = TextWhite,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
        } else {
            // Active timer mode
            val timerColor = when (timerState) {
                WorkoutViewModel.PlayerState.PREPARING -> TertiaryElectricBlue
                WorkoutViewModel.PlayerState.WORKING -> PrimaryOrange
                WorkoutViewModel.PlayerState.RESTING -> SecondaryNeonGreen
                else -> PrimaryOrange
            }
            
            val stateText = when (timerState) {
                WorkoutViewModel.PlayerState.PREPARING -> "احصل على استعداد! ⏱️"
                WorkoutViewModel.PlayerState.WORKING -> "تجرّع الجهد ونشط عضلاتك! 🔥"
                WorkoutViewModel.PlayerState.RESTING -> "خذ نفساً عميقاً وارتح 🧘"
                else -> ""
            }
            
            val maxSecondsForPercent = when (timerState) {
                WorkoutViewModel.PlayerState.PREPARING -> 5
                WorkoutViewModel.PlayerState.WORKING -> workSec
                WorkoutViewModel.PlayerState.RESTING -> restSec
                else -> 30
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, BorderSlate, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Title Active Indicator
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "الجولة $currentRound من $rounds",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = TertiaryElectricBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stateText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = timerColor,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // Circular Timer representation
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(220.dp)
                    ) {
                        val percentage = (secondsRemaining.toFloat() / maxSecondsForPercent).coerceIn(0f, 1f)
                        val animPercent by animateFloatAsState(targetValue = percentage, animationSpec = tween(500), label = "circle_percentage")
                        
                        // Glowing effect pulse
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scaleState by infiniteTransition.animateFloat(
                            initialValue = 0.96f,
                            targetValue = 1.04f,
                            animationSpec = infiniteRepeatable(
                                animation = twinScale(if (timerState == WorkoutViewModel.PlayerState.WORKING) 700 else 1200),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_scale"
                        )
                        
                        Canvas(
                            modifier = Modifier
                                .size(210.dp)
                                .animateMovementPulse(if (!isPaused) scaleState else 1f)
                        ) {
                            // Track Draw
                            drawCircle(
                                color = HighlightSurface,
                                radius = size.minDimension / 2.05f,
                                style = Stroke(width = 14.dp.toPx())
                            )
                            // Progress arc sweep
                            drawArc(
                                color = timerColor,
                                startAngle = -90f,
                                sweepAngle = animPercent * 360f,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                            
                            // Center Glowing ambient radial gradient
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(timerColor.copy(alpha = 0.12f), Color.Transparent),
                                    center = center,
                                    radius = size.minDimension / 1.5f
                                ),
                                radius = size.minDimension / 2.3f
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$secondsRemaining",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 72.sp
                            )
                            Text(
                                text = "ثانية متبقية",
                                fontSize = 14.sp,
                                color = TextGray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Active Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pause / Play
                        Button(
                            onClick = { viewModel.pauseResumeCustomTimer() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isPaused) SecondaryNeonGreen else PrimaryOrange),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("custom_timer_pause_btn")
                        ) {
                            Icon(
                                if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "متابعة" else "إيقاف مؤقت",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(32.dp))
                        
                        // Restart / Cancel
                        Button(
                            onClick = { viewModel.resetCustomTimer() },
                            colors = ButtonDefaults.buttonColors(containerColor = HighlightSurface),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(56.dp)
                                .border(1.dp, BorderSlate, CircleShape)
                                .testTag("custom_timer_reset_btn")
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "إنهاء المؤقت",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ---------------- INTERACTIVE WORKOUT PLAYER OVERLAY ----------------

@Composable
fun WorkoutPlayerOverlay(
    viewModel: WorkoutViewModel,
    onClose: () -> Unit
) {
    val program by viewModel.activeProgram.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val currentIdx by viewModel.currentExerciseIndex.collectAsStateWithLifecycle()
    val secondsRemaining by viewModel.secondsRemaining.collectAsStateWithLifecycle()
    val isPaused by viewModel.timerIsPaused.collectAsStateWithLifecycle()
    
    val targetProgram = program ?: return
    val currentExercise = targetProgram.exercises.getOrNull(currentIdx) ?: return
    val nextExercise = targetProgram.exercises.getOrNull(currentIdx + 1)
    
    // Dynamic styling context
    val currentTimerMax = when (playerState) {
        WorkoutViewModel.PlayerState.PREPARING -> 5
        WorkoutViewModel.PlayerState.WORKING -> currentExercise.durationSeconds
        WorkoutViewModel.PlayerState.RESTING -> 15
        else -> 30
    }
    
    val accentColor = when (playerState) {
        WorkoutViewModel.PlayerState.PREPARING -> TertiaryElectricBlue
        WorkoutViewModel.PlayerState.WORKING -> PrimaryOrange
        WorkoutViewModel.PlayerState.RESTING -> SecondaryNeonGreen
        else -> PrimaryOrange
    }

    if (playerState == WorkoutViewModel.PlayerState.SUMMARY) {
        WorkoutCompleteCelebration(
            programTitle = targetProgram.title,
            exerciseCount = targetProgram.exercises.size,
            onClose = onClose
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Exit Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.background(HighlightSurface, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "خروج وإلغاء التمرين", tint = Color.White)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = targetProgram.title,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "تمارين مبتدئين آمنة 🏠",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
                
                // Static dummy space to balance RTL layout header
                Spacer(modifier = Modifier.width(48.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Linear Progress indicators
            Column(modifier = Modifier.fillMaxWidth()) {
                val overallProgressPercentage = (currentIdx.toFloat() / targetProgram.exercises.size).coerceIn(0f, 1f)
                val animatedLinearProgress by animateFloatAsState(targetValue = overallProgressPercentage, animationSpec = tween(500), label = "linear_prog")
                
                LinearProgressIndicator(
                    progress = animatedLinearProgress,
                    color = PrimaryOrange,
                    trackColor = HighlightSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التمرين ${currentIdx + 1} من ${targetProgram.exercises.size}",
                        fontSize = 12.sp,
                        color = PrimaryOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(overallProgressPercentage * 100).toInt()}% منجز",
                        fontSize = 12.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Body Player Card containing illustrations, titles, and step descriptions
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, BorderSlate, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    
                    // State details
                    Crossfade(targetState = playerState, label = "state_details_crossfade") { state ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (state) {
                                WorkoutViewModel.PlayerState.PREPARING -> {
                                    Text(
                                        text = "احصل على استعداد! ⏱️",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TertiaryElectricBlue
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentExercise.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                WorkoutViewModel.PlayerState.WORKING -> {
                                    Text(
                                        text = "تمرين نشط الآن 💪",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryOrange
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentExercise.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentExercise.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextGray,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                                WorkoutViewModel.PlayerState.RESTING -> {
                                    Text(
                                        text = "وقت الراحة 🧘",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryNeonGreen
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "التمرين التالي: ${nextExercise?.name ?: ' '}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                    
                    // Dynamic Custom Art Canvas
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        AnimatedExerciseVisualizer(
                            imageType = currentExercise.imageType,
                            isActive = playerState == WorkoutViewModel.PlayerState.WORKING && !isPaused,
                            secondsRemaining = secondsRemaining
                        )
                    }
                    
                    // Huge numerical display
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(140.dp)
                    ) {
                        val percentage = (secondsRemaining.toFloat() / currentTimerMax).coerceIn(0f, 1f)
                        val animPercent by animateFloatAsState(targetValue = percentage, animationSpec = tween(500), label = "overlay_percent")
                        
                        Canvas(modifier = Modifier.size(130.dp)) {
                            drawCircle(
                                color = HighlightSurface,
                                radius = size.minDimension / 2,
                                style = Stroke(width = 8.dp.toPx())
                            )
                            drawArc(
                                color = accentColor,
                                startAngle = -90f,
                                sweepAngle = animPercent * 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$secondsRemaining",
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "ثانية",
                                fontSize = 12.sp,
                                color = TextGray
                            )
                        }
                    }
                    
                    // Navigation Player controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Exercise Button
                        IconButton(
                            onClick = { viewModel.skipPrevious() },
                            modifier = Modifier
                                .background(HighlightSurface, CircleShape)
                                .size(48.dp)
                                .border(1.dp, BorderSlate.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious, // In RTL Compose will auto mirror this
                                contentDescription = "التمرين السابق",
                                tint = Color.White
                            )
                        }
                        
                        // Play / Pause Circle
                        IconButton(
                            onClick = { viewModel.pauseResumePlayer() },
                            modifier = Modifier
                                .background(accentColor, CircleShape)
                                .size(60.dp)
                        ) {
                            Icon(
                                if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "استئناف" else "مؤقت مؤقتاً",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        // Next Exercise Button
                        IconButton(
                            onClick = { viewModel.skipNext() },
                            modifier = Modifier
                                .background(HighlightSurface, CircleShape)
                                .size(48.dp)
                                .border(1.dp, BorderSlate.copy(alpha = 0.5f), CircleShape)
                                .testTag("skip_next_exercise_btn")
                        ) {
                            Icon(
                                Icons.Default.SkipNext, // In RTL Compose will auto mirror this
                                contentDescription = "تخطي للآتي",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- CELEBRATION COMPONENT ----------------

@Composable
fun WorkoutCompleteCelebration(
    programTitle: String,
    exerciseCount: Int,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        // Confetti visual representation using a simple animated loop drawing on canvas
        val infiniteTransition = rememberInfiniteTransition(label = "confetti_anim")
        val alphaAnim by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha_confetti"
        )
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw dummy confetti pieces
            val confettiColors = listOf(Color.Yellow, Color.Cyan, Color.Green, PrimaryOrange, Color.Magenta)
            val randomObj = Random(12345L)
            for (i in 0..40) {
                val x = randomObj.nextFloat() * size.width
                val y = randomObj.nextFloat() * size.height
                val radius = randomObj.nextFloat() * 6.dp.toPx() + 4.dp.toPx()
                val color = confettiColors[randomObj.nextInt(confettiColors.size)]
                drawCircle(
                    color = color.copy(alpha = alphaAnim),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(2.dp, SecondaryNeonGreen, RoundedCornerShape(28.dp))
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(SecondaryNeonGreen.copy(alpha = 0.15f), CircleShape)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = SecondaryNeonGreen,
                        modifier = Modifier.size(64.dp)
                    )
                }
                
                Text(
                    text = "عمل بطولي مميز! 🎉",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "لقد أكملت جدول \"$programTitle\" بنجاح تام وبطاقة ممتازة من البداية وحتى المحطة الختامية.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                
                Divider(color = BorderSlate)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "التمارين", fontSize = 12.sp, color = TextGray)
                        Text(text = "$exerciseCount", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "العائد كربوهيدرات", fontSize = 12.sp, color = TextGray)
                        Text(text = "🔥 مُحقق", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryOrange)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "مستوى التدريب", fontSize = 12.sp, color = TextGray)
                        Text(text = "مبتدئ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TertiaryElectricBlue)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            brush = Brush.horizontalGradient(listOf(SecondaryNeonGreen, TertiaryElectricBlue)),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .testTag("celebration_close_btn")
                ) {
                    Text(
                        "العودة للوحة الرئيسية",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// ---------------- ANIMATED SPORT ILLUSTRATIONS ----------------

@Composable
fun AnimatedExerciseVisualizer(
    imageType: String,
    isActive: Boolean,
    secondsRemaining: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "exercise_illustrator")
    
    // Pace bounce multiplier based on workout active state or static
    val animDuration = if (isActive) 1200 else 4000
    val translationFactor by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translation"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h / 2f
        
        when (imageType) {
            "jumping_jacks" -> {
                // Draw a stick figure executing jumping jacks
                val legWidth = if (isActive) (30 + translationFactor * 50f).dp.toPx() else 40f
                val armAngle = if (isActive) translationFactor * 100f - 50f else 20f // degrees
                
                // Head
                drawCircle(color = PrimaryOrange, radius = 18.dp.toPx(), center = Offset(centerX, centerY - 45.dp.toPx()))
                // Spine line
                drawLine(
                    color = Color.White,
                    start = Offset(centerX, centerY - 27.dp.toPx()),
                    end = Offset(centerX, centerY + 18.dp.toPx()),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Legs
                drawLine(
                    color = Color.White,
                    start = Offset(centerX, centerY + 18.dp.toPx()),
                    end = Offset(centerX - legWidth, centerY + 54.dp.toPx()),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(centerX, centerY + 18.dp.toPx()),
                    end = Offset(centerX + legWidth, centerY + 54.dp.toPx()),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                // Arms moving up/down
                val armAngleRadLeft = Math.toRadians((180 + armAngle).toDouble())
                val armAngleRadRight = Math.toRadians((360 - armAngle).toDouble())
                val armLength = 40.dp.toPx()
                
                // Left arm
                drawLine(
                    color = Color.White,
                    start = Offset(centerX, centerY - 15.dp.toPx()),
                    end = Offset(
                        (centerX + armLength * cos(armAngleRadLeft)).toFloat(),
                        (centerY - 15.dp.toPx() + armLength * sin(armAngleRadLeft)).toFloat()
                    ),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Right arm
                drawLine(
                    color = Color.White,
                    start = Offset(centerX, centerY - 15.dp.toPx()),
                    end = Offset(
                        (centerX + armLength * cos(armAngleRadRight)).toFloat(),
                        (centerY - 15.dp.toPx() + armLength * sin(armAngleRadRight)).toFloat()
                    ),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            "squat" -> {
                // Draw bending stick figure performing squats
                val hipOffset = if (isActive) translationFactor * 25.dp.toPx() else 0f
                val headY = centerY - 40.dp.toPx() + hipOffset
                val hipY = centerY + 10.dp.toPx() + hipOffset
                val kneeY = centerY + 30.dp.toPx()
                val feetY = centerY + 55.dp.toPx()
                
                // Head
                drawCircle(color = PrimaryOrange, radius = 18.dp.toPx(), center = Offset(centerX, headY))
                // Back
                drawLine(
                    color = Color.White,
                    start = Offset(centerX, headY + 18.dp.toPx()),
                    end = Offset(centerX - 10.dp.toPx(), hipY),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Thighs bending backwards
                drawLine(
                    color = Color.White,
                    start = Offset(centerX - 10.dp.toPx(), hipY),
                    end = Offset(centerX - 35.dp.toPx(), kneeY),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Calves down to floor
                drawLine(
                    color = Color.White,
                    start = Offset(centerX - 35.dp.toPx(), kneeY),
                    end = Offset(centerX - 30.dp.toPx(), feetY),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Arms extended straight out
                drawLine(
                    color = PrimaryOrange,
                    start = Offset(centerX, headY + 20.dp.toPx()),
                    end = Offset(centerX + 35.dp.toPx() , headY + 20.dp.toPx()),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            "pushup" -> {
                // Slanted pushup bar representation
                val tiltFactor = if (isActive) translationFactor * 18.dp.toPx() else 0f
                val handX = centerX - 40.dp.toPx()
                val feetX = centerX + 40.dp.toPx()
                val shoulderY = centerY + 15.dp.toPx() + tiltFactor
                val hipY = centerY + 25.dp.toPx() + (tiltFactor * 0.7f)
                val feetY = centerY + 35.dp.toPx()
                
                // Ground Line
                drawLine(
                    color = HighlightSurface,
                    start = Offset(handX - 20.dp.toPx(), feetY + 10.dp.toPx()),
                    end = Offset(feetX + 20.dp.toPx(), feetY + 10.dp.toPx()),
                    strokeWidth = 4.dp.toPx()
                )
                
                // Head
                drawCircle(color = PrimaryOrange, radius = 14.dp.toPx(), center = Offset(handX - 5.dp.toPx(), shoulderY - 14.dp.toPx()))
                
                // Straight body line (Shoulder -> Hip -> Feet)
                drawLine(
                    color = Color.White,
                    start = Offset(handX, shoulderY),
                    end = Offset(feetX, feetY),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                // Arm representation bending
                drawLine(
                    color = PrimaryOrange,
                    start = Offset(handX, shoulderY),
                    end = Offset(handX - 10.dp.toPx(), feetY + 8.dp.toPx()),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            "plank" -> {
                // Super steady linear plank animation
                val corePulse = if (isActive) (sin(secondsRemaining.toDouble()) * 4f).toFloat() else 0f
                val hX = centerX - 50.dp.toPx()
                val fX = centerX + 50.dp.toPx()
                val posY = centerY + 25.dp.toPx() + corePulse
                
                // Ground
                drawLine(
                    color = HighlightSurface,
                    start = Offset(hX - 20.dp.toPx(), posY + 16.dp.toPx()),
                    end = Offset(fX + 20.dp.toPx(), posY + 16.dp.toPx()),
                    strokeWidth = 3.dp.toPx()
                )
                
                // Flat body
                drawLine(
                    color = Color.White,
                    start = Offset(hX, posY),
                    end = Offset(fX, posY),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                // Head
                drawCircle(color = PrimaryOrange, radius = 14.dp.toPx(), center = Offset(hX - 18.dp.toPx(), posY - 5.dp.toPx()))
                
                // Glowing core circle in the center representing muscle tension
                drawCircle(
                    color = PrimaryOrange.copy(alpha = 0.35f),
                    radius = (20 + corePulse * 2f).dp.toPx(),
                    center = Offset(centerX, posY)
                )
                drawCircle(
                    color = PrimaryOrange,
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, posY)
                )
            }
            
            "bridge" -> {
                // Glute bridge bending path arc
                val elevationY = if (isActive) translationFactor * 30.dp.toPx() else 0f
                val headX = centerX - 45.dp.toPx()
                val kneeX = centerX + 35.dp.toPx()
                val footX = centerX + 45.dp.toPx()
                
                val groundY = centerY + 40.dp.toPx()
                val hipY = groundY - 10.dp.toPx() - elevationY
                
                // Ground
                drawLine(color = HighlightSurface, start = Offset(headX - 10f, groundY), end = Offset(footX + 10f, groundY), strokeWidth = 3.dp.toPx())
                
                // Elevated Bridge lines connecting head -> Hip -> knees -> feet
                drawLine(color = Color.White, start = Offset(headX, groundY), end = Offset(centerX - 10f, hipY), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color = Color.White, start = Offset(centerX - 10f, hipY), end = Offset(kneeX, hipY + 12.dp.toPx()), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color = Color.White, start = Offset(kneeX, hipY + 12.dp.toPx()), end = Offset(footX, groundY), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                
                // Head on ground
                drawCircle(color = PrimaryOrange, radius = 13.dp.toPx(), center = Offset(headX - 14.dp.toPx(), groundY - 5.dp.toPx()))
                
                // Supporting arms lying flat
                drawLine(color = PrimaryOrange, start = Offset(headX, groundY - 2.dp.toPx()), end = Offset(centerX, groundY - 2.dp.toPx()), strokeWidth = 4.dp.toPx())
            }
            
            else -> {
                // Default Breathing / Focus Expanding Pulsing Rings
                val breathingRadius = 40.dp.toPx() + (translationFactor * 35.dp.toPx())
                drawCircle(
                    color = PrimaryOrange.copy(alpha = 0.15f),
                    radius = breathingRadius,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = PrimaryOrange.copy(alpha = 0.35f),
                    radius = breathingRadius - 15.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = PrimaryOrange,
                    radius = 16.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
            }
        }
    }
}

// ---------------- CUSTOM MODIFIERS & DRAW HELPERS ----------------

fun Modifier.drawBehindBackgroundAccent(): Modifier = this.drawBehind {
    // Draw an elegant gradient orb behind cards
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(PrimaryOrange.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(size.width * 0.15f, size.height * 0.2f),
            radius = size.maxDimension / 2f
        ),
        radius = size.maxDimension / 2f,
        center = Offset(size.width * 0.15f, size.height * 0.2f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SecondaryNeonGreen.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(size.width * 0.85f, size.height * 0.8f),
            radius = size.maxDimension / 2.5f
        ),
        radius = size.maxDimension / 2.5f,
        center = Offset(size.width * 0.85f, size.height * 0.8f)
    )
}

fun Modifier.animateMovementPulse(scale: Float): Modifier = this.graphicsLayer(
    scaleX = scale,
    scaleY = scale
)

private fun twinScale(ms: Int): KeyframesSpec<Float> = keyframes {
    durationMillis = ms
    0.96f at 0
    1.04f at (ms / 2)
    0.96f at ms
}
