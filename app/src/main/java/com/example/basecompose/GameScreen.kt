package com.example.basecompose

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

private val White = TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)

/** Mọi thứ 1 ván cần – tạo ở background thread vì vẽ ảnh tốn ~100ms. */
private class GameAssets(
    val game: GameState,
    val pictures: List<ImageBitmap>,
    val cardBack: ImageBitmap,
    val deckCard: ImageBitmap,
)

@Composable
fun GameScreen() {
    val context = LocalContext.current
    var level by remember { mutableIntStateOf(1) }
    var seed by remember { mutableLongStateOf(Random.nextLong()) }
    var custom by remember { mutableStateOf<Bitmap?>(null) }
    var settings by remember { mutableStateOf(GameSettings.load(context)) }
    var showSettings by remember { mutableStateOf(false) }

    val assets by produceState<GameAssets?>(null, level, seed, custom, settings) {
        value = null
        value = withContext(Dispatchers.Default) {
            val picCount = settings.pictureCount(level, PictureFactory.themeCount)
            val themes = (0 until PictureFactory.themeCount).shuffled(Random(seed)).take(picCount)
            val pics = themes.mapIndexed { i, t ->
                (if (i == 0 && custom != null) custom!! else PictureFactory.picture(t)).asImageBitmap()
            }
            GameAssets(
                GameState(settings.rows, settings.cols, Cut(settings.cut), picCount, seed),
                pics,
                PictureFactory.cardBack().asImageBitmap(),
                PictureFactory.deckCard().asImageBitmap(),
            )
        }
    }

    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val ctrl = remember(assets) { assets?.let { BoardController(it.game, scope, haptics) } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) PictureFactory.load(context, uri)?.let { custom = it; seed = Random.nextLong() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF5B9BF0), Color(0xFF3A74DC), Color(0xFF2F63C9))))
    ) {
        SnowBackground(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize()) {
            // ---- Top bar: Undo | Level | Hint (giống video) ----
            Box(
                Modifier
                    .shadow(8.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF7DB8FF), Color(0xFF4A8FF0))))
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
            ) {
                Row(Modifier.align(Alignment.CenterStart), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoundButton("↶", enabled = ctrl != null && ctrl.game.canUndo && !ctrl.busy) { ctrl?.undo() }
                    RoundButton("⚙") { showSettings = true }
                }
                BasicText(
                    "Level $level",
                    Modifier.align(Alignment.Center),
                    style = White.copy(fontSize = 24.sp, fontWeight = FontWeight.Black,
                        shadow = Shadow(Color(0x66003080), Offset(0f, 3f), 4f)),
                )
                Box(Modifier.align(Alignment.CenterEnd)) {
                    val hints = ctrl?.game?.hints ?: 0
                    RoundButton("💡", enabled = hints > 0 && ctrl?.busy == false) { ctrl?.hint() }
                    Box(
                        Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).size(20.dp)
                            .clip(CircleShape).background(Color(0xFFE53935)),
                        contentAlignment = Alignment.Center,
                    ) { BasicText("$hints", style = White.copy(fontSize = 11.sp)) }
                }
            }

            // ---- Board ----
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                val a = assets
                if (a != null && ctrl != null) {
                    // key(ctrl) => ván mới tạo lại toàn bộ animation (chia bài lại)
                    key(ctrl) { PuzzleBoard(ctrl, a.pictures, a.cardBack, a.deckCard) }
                    if (showSettings) {
                        SettingsDialog(
                            current = settings,
                            onDismiss = { showSettings = false },
                            onApply = {
                                showSettings = false
                                if (it != settings) {
                                    it.save(context)
                                    settings = it
                                    level = 1
                                    seed = Random.nextLong()
                                }
                            },
                        )
                    }
                    if (ctrl.showWin) {
                        Box(Modifier.align(Alignment.Center)) {
                            WinOverlay(ctrl.game.picCount) { level++; seed = Random.nextLong() }
                        }
                    }
                }
            }

            // ---- Bottom actions ----
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PillButton("🖼  Chọn ảnh", Modifier.weight(1f)) {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                PillButton("🔀  Chơi lại", Modifier.weight(1f)) { seed = Random.nextLong() }
            }
        }
    }
}

@Composable
private fun RoundButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier.size(44.dp).shadow(4.dp, CircleShape).background(Color.White, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            label,
            Modifier.alpha(if (enabled) 1f else 0.35f),
            style = TextStyle(fontSize = 22.sp, color = Color(0xFF1E5BC6), fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun PillButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(46.dp).clip(RoundedCornerShape(23.dp))
            .background(Color(0x33FFFFFF)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { BasicText(label, style = White.copy(fontSize = 16.sp)) }
}

@Composable
private fun WinOverlay(pictures: Int, onNext: () -> Unit) {
    val s = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(350)
        s.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f))
    }
    Column(
        Modifier.graphicsLayer { scaleX = s.value; scaleY = s.value; alpha = s.value.coerceIn(0f, 1f) }
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF6FAEFF), Color(0xFF1E5BC6))), RoundedCornerShape(24.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BasicText(
            "🎉 Hoàn thành!",
            style = White.copy(fontSize = 30.sp, fontWeight = FontWeight.Black, shadow = Shadow(Color(0x88002060), Offset(0f, 4f), 6f)),
        )
        BasicText("Đã ghép $pictures bức tranh", style = White.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium))
        Box(
            Modifier.size(220.dp, 50.dp).shadow(6.dp, RoundedCornerShape(25.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF9800))), RoundedCornerShape(25.dp))
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center,
        ) { BasicText("Level tiếp theo ▶", style = White.copy(fontSize = 18.sp)) }
    }
}
