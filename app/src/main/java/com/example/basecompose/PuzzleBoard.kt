package com.example.basecompose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/** Ô dọc tỉ lệ 2:3 giống video. */
private const val CELL_RATIO = 1.5f
/** Chiều cao vùng xấp bài & khoảng cách tới bàn (theo chiều cao ô). */
private const val DECK_H = 0.5f
private const val DECK_GAP = 0.14f

private val SlotColor = Color(0xFF2D5AAE)

/** easeOutBounce: rơi chạm đáy rồi nảy 2 nhịp nhỏ. */
private val BounceEasing = Easing { x ->
    val n = 7.5625f; val d = 2.75f
    when {
        x < 1f / d -> n * x * x
        x < 2f / d -> { val t = x - 1.5f / d; n * t * t + 0.75f }
        x < 2.5f / d -> { val t = x - 2.25f / d; n * t * t + 0.9375f }
        else -> { val t = x - 2.625f / d; n * t * t + 0.984375f }
    }
}
private val SlotEdge = Color(0x33FFFFFF)

class PieceAnim(start: Offset, scale: Float = 1f, flip: Float = 1f) {
    val pos = Animatable(start, Offset.VectorConverter)
    val scale = Animatable(scale)
    /** 0 = úp (mặt sau), 1 = ngửa. Lật quanh trục dọc. */
    val flip = Animatable(flip)
    /** Lớp sáng trắng phủ lên khi vừa ghép dính / gợi ý. */
    val flash = Animatable(0f)
    var z = 0
}

class Celebration(val pic: Int, val topLeft: Offset) {
    val pop = Animatable(1f)
    val shine = Animatable(0f)
    val fly = Animatable(0f)
    val rot = Animatable(0f)
    var sparkles by mutableStateOf(true)
}

class FloatingWord(val text: String, val center: Offset) {
    val scale = Animatable(0f)
    val alpha = Animatable(1f)
    val rise = Animatable(0f)
}

/**
 * Điều phối toàn bộ animation của bàn chơi. Mọi vị trí tính theo ĐƠN VỊ Ô
 * (x: cột, y: hàng; y < 0 là vùng xấp bài). Canvas chỉ đọc giá trị để vẽ
 * => animate chỉ redraw, không recompose.
 */
@Stable
class BoardController(
    val game: GameState,
    private val scope: CoroutineScope,
    private val haptics: HapticFeedback,
) {
    val rows = game.rows
    val cols = game.cols

    val anims = mutableStateMapOf<Int, PieceAnim>()
    val particles = ParticleSystem()
    var busy by mutableStateOf(true)
        private set
    var celebration by mutableStateOf<Celebration?>(null)
        private set
    var word by mutableStateOf<FloatingWord?>(null)
        private set
    var showWin by mutableStateOf(false)
        private set

    /** Đồng hồ hiệu ứng (giây) – chỉ chạy khi có hiệu ứng để không vẽ lại thừa. */
    var clock by mutableFloatStateOf(0f)
        private set
    var density = 1f

    var dragPieces by mutableStateOf(emptySet<Int>())
        private set
    var dragDelta by mutableStateOf(Offset.Zero)
        private set
    private var dragCells = emptyList<Int>()
    val lift = Animatable(0f)

    private var zTop = 0
    private var lastNanos = 0L
    private var combo = 0
    private var lastCompleteAt = 0L

    fun cellPos(cell: Int) = Offset((cell % cols).toFloat(), (cell / cols).toFloat())

    /** Góc trên-trái (đơn vị ô) của lá bài khi còn nằm ở xấp phía trên cột [col]. */
    private fun deckSlot(col: Int) = Offset(col.toFloat(), -(DECK_GAP + DECK_H * 0.5f) - 0.5f)

    // ------------------------------------------------------------ vòng lặp khung hình

    fun onFrame(nanos: Long) {
        val dt = if (lastNanos == 0L) 0.016f else ((nanos - lastNanos) / 1e9f).coerceAtMost(0.05f)
        lastNanos = nanos
        val c = celebration
        if (c != null && c.fly.value > 0.05f && c.fly.value < 0.95f) {
            val ctr = celebrationCenter(c)
            particles.trail(ctr.x, ctr.y, density)
        }
        if (particles.isActive || c != null || word != null) {
            particles.update(dt)
            clock += dt
        }
    }

    fun celebrationCenter(c: Celebration): Offset {
        val half = game.cut.k / 2f
        val from = c.topLeft + Offset(half, half)
        val to = Offset(cols / 2f, -(DECK_GAP + DECK_H * 0.5f))
        val f = c.fly.value
        return lerp(from, to, f) - Offset(0f, sin(f * PI.toFloat()) * 0.8f)
    }

    // ------------------------------------------------------------ mở màn: chia bài

    suspend fun start() {
        busy = true
        val dealt = game.board.mapIndexedNotNull { cell, p -> if (p == EMPTY) null else cell to p }
        flyIn(dealt, stagger = 38L) { cell -> ((cell / cols) + (cell % cols)) * 70L }
        busy = false
        resolve()
    }

    // ------------------------------------------------------------ kéo thả

    fun startDrag(cell: Int) {
        if (busy || dragPieces.isNotEmpty() || cell !in 0 until game.n) return
        val cells = game.groupCellsAt(cell)
        if (cells.isEmpty()) return
        dragCells = cells
        dragPieces = cells.map { game.board[it] }.toSet()
        dragDelta = Offset.Zero
        dragPieces.forEach { anims[it]?.z = ++zTop }
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch { lift.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 700f)) }
    }

    fun drag(dx: Float, dy: Float) {
        if (dragPieces.isNotEmpty()) dragDelta += Offset(dx, dy)
    }

    fun endDrag() {
        val pieces = dragPieces
        if (pieces.isEmpty()) return
        val cells = dragCells
        val d = dragDelta
        // UNDISPATCHED: "đặt" mảnh tại chỗ thả NGAY trong frame này rồi mới animate -> không giật
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            pieces.forEach { p -> anims[p]?.let { it.pos.snapTo(it.pos.value + d) } }
            dragPieces = emptySet()
            dragDelta = Offset.Zero
            launch { lift.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 500f)) }

            val dr = d.y.roundToInt()
            val dc = d.x.roundToInt()
            val sizesBefore = groupSizes()
            when (val merged = game.tryMove(cells, dr, dc)) {
                null -> {
                    // Thả ra ngoài bàn -> bật về và lắc nhẹ; thả tại chỗ -> về êm
                    val wobble = dr != 0 || dc != 0
                    if (wobble) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    pieces.forEach { p ->
                        val a = anims[p] ?: return@forEach
                        launch {
                            a.pos.animateTo(
                                cellPos(game.board.indexOf(p)),
                                if (wobble) spring(dampingRatio = 0.32f, stiffness = 520f) else spring(0.7f, 600f),
                            )
                        }
                    }
                }
                else -> {
                    animateToBoard()
                    if (merged) mergeFx(sizesBefore)
                    resolve()
                }
            }
        }
    }

    // ------------------------------------------------------------ nút trên thanh tiêu đề

    fun undo() {
        if (busy || dragPieces.isNotEmpty() || !game.canUndo) return
        game.undo()
        animateToBoard()
    }

    fun hint() {
        if (busy || dragPieces.isNotEmpty()) return
        val sizesBefore = groupSizes()
        val moved = game.hint() ?: return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        moved.forEach { p ->
            val a = anims[p] ?: return@forEach
            a.z = ++zTop
            scope.launch { a.flash.snapTo(0.8f); a.flash.animateTo(0f, tween(700)) }
            scope.launch {
                a.scale.animateTo(1.15f, tween(160))
                a.scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 400f))
            }
        }
        animateToBoard()
        mergeFx(sizesBefore)
        resolve()
    }

    // ------------------------------------------------------------ nội bộ

    private fun animateToBoard() {
        game.board.forEachIndexed { cell, p ->
            if (p == EMPTY) return@forEachIndexed
            val a = anims[p] ?: return@forEachIndexed
            val t = cellPos(cell)
            if (a.pos.targetValue != t) {
                scope.launch { a.pos.animateTo(t, spring(dampingRatio = 0.72f, stiffness = 420f)) }
            }
        }
    }

    private fun groupSizes(): Map<Int, Int> {
        val g = game.cellGroup
        val count = g.toList().groupingBy { it }.eachCount()
        return game.board.indices.filter { game.board[it] != EMPTY }.associate { game.board[it] to count.getValue(g[it]) }
    }

    /** Mảnh vừa dính thêm: nháy sáng + nảy nhẹ + lấp lánh ở giữa nhóm. */
    private fun mergeFx(before: Map<Int, Int>) {
        val after = groupSizes()
        val grown = after.filter { (p, s) -> s > (before[p] ?: 1) }.keys
        if (grown.isEmpty()) return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        var cx = 0f; var cy = 0f
        grown.forEach { p ->
            val cell = game.board.indexOf(p)
            cx += cell % cols + 0.5f; cy += cell / cols + 0.5f
            val a = anims[p] ?: return@forEach
            scope.launch {
                delay(140)
                launch { a.flash.snapTo(0.55f); a.flash.animateTo(0f, tween(450)) }
                a.scale.animateTo(1.06f, tween(110))
                a.scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
            }
        }
        val n = grown.size
        scope.launch {
            delay(160)
            particles.twinkle(cx / n, cy / n, 1.2f, 1.2f, 7, density, Gold)
        }
    }

    /** Kiểm tra ảnh hoàn thành -> ăn mừng -> gỡ -> chia bài mới, lặp đến khi hết. */
    private fun resolve() {
        if (game.completed().isEmpty()) return
        busy = true
        scope.launch {
            while (true) {
                val (pic, tl) = game.completed().firstOrNull() ?: break
                celebrate(pic, tl)
                game.removePicture(pic)
                anims.keys.filter { game.picOf(it) == pic }.forEach { anims.remove(it) }
                if (game.won) break
                fall()
                // Bài mới vào các ô trống còn lại – sau khi rơi, chúng nằm ở trên cùng
                val dealt = game.deal(game.emptyCells())
                val maxRow = dealt.maxOfOrNull { it.first / cols } ?: 0
                // Giống video: hàng dưới lật trước, lệch nhẹ theo cột
                flyIn(dealt, stagger = 85L) { cell -> (maxRow - cell / cols) * 110L + (cell % cols) * 45L }
            }
            if (game.won) win()
            busy = false
        }
    }

    /** Mảnh phía trên rơi xuống đáy: rơi nhanh dần rồi nảy nhẹ, cột lệch nhau 1 chút. */
    private suspend fun fall() = coroutineScope {
        val moves = game.collapse()
        if (moves.isEmpty()) return@coroutineScope
        delay(60)
        moves.map { (from, to) ->
            val p = game.board[to]
            val a = anims.getValue(p)
            val dist = (to - from) / cols
            launch {
                delay((to % cols) * 35L + (rows - 1 - to / cols) * 25L)
                a.pos.animateTo(cellPos(to), tween(200 + 70 * dist, easing = BounceEasing))
            }
        }.joinAll()
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        // Bụi tuyết bắn lên dưới đáy mỗi cột vừa có mảnh rơi
        moves.map { it.second % cols }.distinct().forEach { c ->
            val bottom = moves.filter { it.second % cols == c }.maxOf { it.second / cols }
            particles.twinkle(c + 0.5f, bottom + 1f, 0.9f, 0.2f, 4, density)
        }
    }

    private suspend fun celebrate(pic: Int, tl: Int) = coroutineScope {
        delay(200) // đợi mảnh cuối trượt vào chỗ
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        val now = System.currentTimeMillis()
        combo = if (now - lastCompleteAt < 6000) combo + 1 else 0
        lastCompleteAt = now

        val c = Celebration(pic, cellPos(tl))
        celebration = c
        val half = game.cut.k / 2f
        val center = c.topLeft + Offset(half, half)
        particles.burst(center.x, center.y, density = density)

        val w = FloatingWord(Words[combo.coerceAtMost(Words.lastIndex)],
            Offset((center.x - 0.4f).coerceIn(1.1f, cols - 1.1f), center.y))
        word = w
        scope.launch {
            w.scale.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 380f))
            delay(900)
            launch { w.rise.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
            w.alpha.animateTo(0f, tween(650))
            if (word === w) word = null
        }

        launch { c.shine.animateTo(1f, tween(800, delayMillis = 120, easing = FastOutSlowInEasing)) }
        c.pop.animateTo(1.07f, spring(dampingRatio = 0.42f, stiffness = 520f))
        delay(650)
        c.sparkles = false
        // Thu nhỏ thành lá bài rồi bay vụt lên trên
        c.pop.animateTo(0.2f, tween(260, easing = FastOutSlowInEasing))
        particles.twinkle(center.x, center.y, 0.8f, 0.8f, 6, density, Gold)
        launch { c.rot.animateTo(-16f, tween(380)) }
        c.fly.animateTo(1f, tween(400, easing = FastOutLinearInEasing))
        celebration = null
    }

    /** Lá bài úp bay từ xấp xuống ô, rồi lật lộ mặt. */
    private suspend fun flyIn(dealt: List<Pair<Int, Int>>, stagger: Long, flipDelay: (Int) -> Long) = coroutineScope {
        if (dealt.isEmpty()) return@coroutineScope
        dealt.forEach { (cell, p) ->
            anims[p] = PieceAnim(deckSlot(cell % cols), scale = 0.42f, flip = 0f).also { it.z = ++zTop }
        }
        dealt.mapIndexed { i, (cell, p) ->
            launch {
                val a = anims.getValue(p)
                delay(i * stagger)
                launch { a.scale.animateTo(1.08f, tween(340, easing = FastOutSlowInEasing)) }
                a.pos.animateTo(cellPos(cell), tween(360, easing = FastOutSlowInEasing))
                a.scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f))
            }
        }.joinAll()
        delay(60)
        dealt.map { (cell, p) ->
            launch {
                val a = anims.getValue(p)
                delay(flipDelay(cell))
                launch {
                    a.scale.animateTo(1.1f, tween(170))
                    a.scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
                }
                a.flip.animateTo(1f, tween(340, easing = FastOutSlowInEasing))
                val ctr = cellPos(cell) + Offset(0.5f, 0.5f)
                particles.twinkle(ctr.x, ctr.y, 0.9f, 1f, 4, density)
            }
        }.joinAll()
    }

    private suspend fun win() {
        delay(250)
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        particles.rain(cols, density)
        showWin = true
    }

    private companion object {
        val Words = listOf("Nice", "Great", "Awesome", "Amazing", "Perfect")
    }
}

/**
 * Toàn bộ bàn chơi (xấp bài + lưới) là MỘT Canvas. Mỗi mảnh là 1 lệnh drawImage
 * cắt 1/4 ảnh gốc.
 */
@Composable
fun PuzzleBoard(
    ctrl: BoardController,
    pictures: List<ImageBitmap>,
    cardBack: ImageBitmap,
    deckCard: ImageBitmap,
    modifier: Modifier = Modifier,
) {
    val game = ctrl.game
    val rows = game.rows
    val cols = game.cols
    val textMeasurer = rememberTextMeasurer()
    val wordCache = remember { HashMap<String, TextLayoutResult>() }
    val density = LocalDensity.current.density
    ctrl.density = density

    LaunchedEffect(ctrl) { ctrl.start() }
    LaunchedEffect(ctrl) { while (true) withFrameNanos(ctrl::onFrame) }

    BoxWithConstraints(modifier) {
        val unitH = CELL_RATIO * (rows + DECK_H + DECK_GAP) / cols // chiều cao / chiều rộng
        val w = minOf(maxWidth, maxHeight / unitH)
        val h = w * unitH

        Canvas(
            Modifier
                .size(w, h)
                .pointerInput(ctrl) {
                    fun cw() = size.width.toFloat() / cols
                    fun ch() = cw() * CELL_RATIO
                    detectDragGestures(
                        onDragStart = { p ->
                            val top = (DECK_H + DECK_GAP) * ch()
                            val c = floor(p.x / cw()).toInt()
                            val r = floor((p.y - top) / ch()).toInt()
                            if (r in 0 until rows && c in 0 until cols) ctrl.startDrag(r * cols + c)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            ctrl.drag(amount.x / cw(), amount.y / ch())
                        },
                        onDragEnd = { ctrl.endDrag() },
                        onDragCancel = { ctrl.endDrag() },
                    )
                }
        ) {
            ctrl.clock // đăng ký vẽ lại theo đồng hồ hiệu ứng
            val cw = size.width / cols
            val ch = cw * CELL_RATIO
            val top = (DECK_H + DECK_GAP) * ch
            val gap = 1.5f * density
            fun px(x: Float, y: Float) = Offset(x * cw, top + y * ch)

            drawDeck(game.deckSize, cols, cw, DECK_H * ch, deckCard, density)

            // Ô trống
            for (i in 0 until game.n) {
                val o = px((i % cols).toFloat(), (i / cols).toFloat())
                drawRect(SlotColor, o + Offset(gap, gap), Size(cw - 2 * gap, ch - 2 * gap))
                drawRect(SlotEdge, o + Offset(gap, gap), Size(cw - 2 * gap, ch - 2 * gap), style = Stroke(density))
            }

            val board = game.board
            val groups = game.cellGroup
            val cel = ctrl.celebration
            val dragging = ctrl.dragPieces
            val items = board.indices
                .filter { board[it] != EMPTY && ctrl.anims[board[it]] != null && (cel == null || game.picOf(board[it]) != cel.pic) }
                .sortedBy { ctrl.anims.getValue(board[it]).z }

            fun pieceRect(cell: Int, pos: Offset): Pair<Offset, Size> {
                val r = cell / cols; val c = cell % cols; val g = groups[cell]
                // Cạnh nào dính với mảnh cùng nhóm thì không có khe hở -> trông như 1 khối
                val gl = if (c > 0 && groups[cell - 1] == g) 0f else gap
                val gr = if (c < cols - 1 && groups[cell + 1] == g) 0f else gap
                val gt = if (r > 0 && groups[cell - cols] == g) 0f else gap
                val gb = if (r < rows - 1 && groups[cell + cols] == g) 0f else gap
                val o = px(pos.x, pos.y)
                return Offset(o.x + gl, o.y + gt) to Size(cw - gl - gr, ch - gt - gb)
            }

            fun drawPiece(cell: Int, extra: Offset, liftScale: Float) {
                val p = board[cell]
                val a = ctrl.anims.getValue(p)
                val (tl, sz) = pieceRect(cell, a.pos.value + extra)
                val center = Offset(tl.x + sz.width / 2, tl.y + sz.height / 2)
                val f = a.flip.value
                val sx = abs(cos(f * PI.toFloat()))
                val s = a.scale.value * liftScale
                withTransform({ scale(sx * s, s, center) }) {
                    if (f >= 0.5f) {
                        val k = game.cut.k
                        val img = pictures[game.picOf(p)]
                        val sw = img.width / k
                        val sh = img.height / k
                        val idx = game.idxOf(p)
                        drawImage(
                            img,
                            srcOffset = IntOffset((idx % k) * sw, (idx / k) * sh),
                            srcSize = IntSize(sw, sh),
                            dstOffset = IntOffset(tl.x.roundToInt(), tl.y.roundToInt()),
                            dstSize = IntSize(sz.width.roundToInt(), sz.height.roundToInt()),
                            filterQuality = FilterQuality.Medium,
                        )
                    } else {
                        drawImage(
                            cardBack,
                            dstOffset = IntOffset(tl.x.roundToInt(), tl.y.roundToInt()),
                            dstSize = IntSize(sz.width.roundToInt(), sz.height.roundToInt()),
                            filterQuality = FilterQuality.Medium,
                        )
                    }
                    if (sx < 0.98f) drawRect(Color.Black.copy(alpha = (1f - sx) * 0.35f), tl, sz)
                    val fl = a.flash.value
                    if (fl > 0.01f) drawRect(Color.White.copy(alpha = fl), tl, sz)
                }
            }

            // Mảnh nằm yên
            items.filter { board[it] !in dragging }.forEach { drawPiece(it, Offset.Zero, 1f) }

            // Mảnh đang kéo: bóng đổ + phóng to nhẹ, vẽ sau cùng (nằm trên)
            val lift = ctrl.lift.value
            val dragItems = items.filter { board[it] in dragging }
            val shadowDy = 8f * density * lift
            dragItems.forEach { cell ->
                val (tl, sz) = pieceRect(cell, ctrl.anims.getValue(board[cell]).pos.value + ctrl.dragDelta)
                drawRect(Color.Black.copy(alpha = 0.28f * lift), tl + Offset(shadowDy * 0.4f, shadowDy), sz)
            }
            dragItems.forEach { drawPiece(it, ctrl.dragDelta, 1f + 0.06f * lift) }

            cel?.let { drawCelebration(ctrl, it, pictures[it.pic], cw, ch, ::px) }

            drawParticles(ctrl.particles, ::px)

            ctrl.word?.let { wd ->
                val layout = wordCache.getOrPut(wd.text) { textMeasurer.measure(wd.text, WordStyle) }
                drawWord(wd, layout, px(wd.center.x, wd.center.y - wd.rise.value * 0.6f), ctrl.clock)
            }
        }
    }
}

private val WordStyle = TextStyle(fontSize = 46.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic)
private val WordFill = Brush.verticalGradient(listOf(Color(0xFFFFA6E8), Color(0xFFE040FB), Color(0xFF7B1FA2)))
private val GoldStroke = listOf(Color(0xFFFFF59D), Color(0xFFFFB300), Color(0xFFFFF8E1), Color(0xFFFF8F00), Color(0xFFFFF59D))

/** Xấp bài phía trên: mỗi cột 1 chồng, lá dưới cùng hiện rõ nhất (giống video). */
private fun DrawScope.drawDeck(remaining: Int, cols: Int, cw: Float, deckH: Float, card: ImageBitmap, density: Float) {
    val stripH = deckH * 0.42f
    val step = (deckH - stripH) / 3f
    for (col in 0 until cols) {
        // Chồng bên trái hết trước
        val count = remaining / cols + if (col >= cols - remaining % cols) 1 else 0
        val x = col * cw + cw * 0.05f
        val wd = cw * 0.9f
        if (count == 0) {
            drawRoundRect(Color.White.copy(alpha = 0.10f), Offset(x, 0f), Size(wd, deckH), CornerRadius(4f * density),
                style = Stroke(1.2f * density))
            continue
        }
        val visible = count.coerceAtMost(4)
        for (k in 0 until visible) {
            val y = k * step
            drawRect(Color.Black.copy(alpha = 0.18f), Offset(x, y + 2f * density), Size(wd, stripH))
            drawImage(
                card,
                dstOffset = IntOffset(x.roundToInt(), y.roundToInt()),
                dstSize = IntSize(wd.roundToInt(), stripH.roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}

/** Ảnh hoàn thành: phóng to, viền vàng phát sáng, vệt sáng quét, sao lấp lánh, rồi thu nhỏ bay đi. */
private fun DrawScope.drawCelebration(
    ctrl: BoardController,
    c: Celebration,
    img: ImageBitmap,
    cw: Float,
    ch: Float,
    px: (Float, Float) -> Offset,
) {
    val d = ctrl.density
    val ctr = ctrl.celebrationCenter(c)
    val cp = px(ctr.x, ctr.y)
    val w = cw * ctrl.game.cut.k
    val h = ch * ctrl.game.cut.k
    val s = c.pop.value
    val alpha = (1f - (c.fly.value - 0.6f).coerceAtLeast(0f) / 0.4f).coerceIn(0f, 1f)
    val tl = Offset(cp.x - w / 2, cp.y - h / 2)
    val t = ctrl.clock

    withTransform({ rotate(c.rot.value, cp); scale(s, s, cp) }) {
        // Quầng sáng vàng phía sau
        for (k in 4 downTo 1) {
            val e = k * 5f * d
            drawRoundRect(
                Gold.copy(alpha = 0.10f * alpha * (5 - k)), Offset(tl.x - e, tl.y - e), Size(w + 2 * e, h + 2 * e),
                CornerRadius(e), style = Stroke(5f * d),
            )
        }
        drawImage(
            img,
            dstOffset = IntOffset(tl.x.roundToInt(), tl.y.roundToInt()),
            dstSize = IntSize(w.roundToInt(), h.roundToInt()),
            alpha = alpha,
            filterQuality = FilterQuality.Medium,
        )
        // Vệt sáng quét chéo
        val sh = c.shine.value
        if (sh in 0.001f..0.999f) clipRect(tl.x, tl.y, tl.x + w, tl.y + h) {
            val x0 = tl.x - w + sh * 2.6f * w
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                    start = Offset(x0, tl.y), end = Offset(x0 + w * 0.45f, tl.y + h * 0.3f),
                ),
                tl, Size(w, h),
            )
        }
        drawRect(
            Brush.linearGradient(GoldStroke, start = tl, end = Offset(tl.x + w, tl.y + h)),
            tl, Size(w, h), alpha = alpha, style = Stroke(3.5f * d),
        )

        if (c.sparkles) {
            // Sao chạy quanh viền
            val per = 2 * (w + h)
            for (i in 0 until 14) {
                val tw = sin(t * 7f + i * 1.9f)
                if (tw <= 0f) continue
                var u = ((i / 14f + t * 0.06f) % 1f) * per
                val p = when {
                    u < w -> Offset(tl.x + u, tl.y)
                    u < w + h -> { u -= w; Offset(tl.x + w, tl.y + u) }
                    u < 2 * w + h -> { u -= w + h; Offset(tl.x + w - u, tl.y + h) }
                    else -> { u -= 2 * w + h; Offset(tl.x, tl.y + h - u) }
                }
                sparkle(p, (5f + 6f * tw) * d, if (i % 3 == 0) Gold else Color.White, tw)
            }
            // Vài sao lấp lánh bên trong ảnh
            for (i in 0 until 6) {
                val tw = sin(t * 5f + i * 2.3f)
                if (tw <= 0f) continue
                val p = Offset(tl.x + w * Inner[i].first, tl.y + h * Inner[i].second)
                sparkle(p, (4f + 7f * tw) * d, Color.White, tw)
            }
        }
    }
}

private val Inner = listOf(0.2f to 0.18f, 0.78f to 0.27f, 0.35f to 0.62f, 0.7f to 0.8f, 0.15f to 0.85f, 0.55f to 0.1f)

/** Chữ "Nice" gradient hồng-tím, viền trắng + quầng vàng, nảy vào rồi bay lên mờ dần. */
private fun DrawScope.drawWord(w: FloatingWord, layout: TextLayoutResult, c: Offset, t: Float) {
    val a = w.alpha.value
    val s = w.scale.value
    if (a <= 0.01f || s <= 0.01f) return
    val tl = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f)
    withTransform({ rotate(-6f, c); scale(s, s, c) }) {
        drawText(layout, color = Color(0xFFFFE082), topLeft = tl, alpha = a * 0.75f,
            drawStyle = Stroke(width = 12f * density, join = StrokeJoin.Round))
        drawText(layout, color = Color.White, topLeft = tl, alpha = a,
            drawStyle = Stroke(width = 6f * density, join = StrokeJoin.Round))
        drawText(layout, brush = WordFill, topLeft = tl, alpha = a)
        // Lấp lánh 2 bên chữ
        for (i in 0 until 3) {
            val tw = sin(t * 6f + i * 2.1f)
            if (tw <= 0f) continue
            val p = Offset(
                tl.x + layout.size.width * listOf(1.02f, -0.04f, 0.6f)[i],
                tl.y + layout.size.height * listOf(0.15f, 0.75f, 0.0f)[i],
            )
            sparkle(p, (6f + 6f * tw) * density, Color.White, tw * a)
        }
    }
}
