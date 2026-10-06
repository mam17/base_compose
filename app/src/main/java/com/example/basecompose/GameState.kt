package com.example.basecompose

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

const val EMPTY = -1

/**
 * Mỗi ảnh được cắt k x k mảnh. pieceId = pic * k² + idx, idx = hàng * k + cột trong ảnh.
 * (k = 2: idx 0 TL, 1 TR, 2 BL, 3 BR.)
 */
class Cut(val k: Int) {
    val per = k * k
    fun picOf(piece: Int) = piece / per
    fun idxOf(piece: Int) = piece % per
}

/**
 * Logic thuần Kotlin, không phụ thuộc UI.
 * Bàn chơi trộn mảnh của NHIỀU ảnh. Hai mảnh cùng ảnh nằm cạnh nhau đúng thứ tự
 * sẽ tự "dính" thành 1 nhóm. Đủ k x k mảnh thành khối (ở bất kỳ đâu) => ảnh hoàn thành.
 */
object PuzzleLogic {

    private fun joinsRight(cut: Cut, p: Int, q: Int) = p != EMPTY && q == p + 1 && cut.idxOf(p) % cut.k < cut.k - 1
    private fun joinsDown(cut: Cut, p: Int, q: Int) = p != EMPTY && q == p + cut.k && cut.idxOf(p) / cut.k < cut.k - 1

    /** Trả về id nhóm cho từng ô (union-find). Ô trống có nhóm riêng. */
    fun groupOfCells(board: List<Int>, rows: Int, cols: Int, cut: Cut): IntArray {
        val n = rows * cols
        val parent = IntArray(n) { it }
        fun find(x: Int): Int {
            var a = x
            while (parent[a] != a) { parent[a] = parent[parent[a]]; a = parent[a] }
            return a
        }
        fun union(a: Int, b: Int) { parent[find(a)] = find(b) }

        for (i in 0 until n) {
            val r = i / cols; val c = i % cols
            if (c < cols - 1 && joinsRight(cut, board[i], board[i + 1])) union(i, i + 1)
            if (r < rows - 1 && joinsDown(cut, board[i], board[i + cols])) union(i, i + cols)
        }
        return IntArray(n) { find(it) }
    }

    /** Số cặp cạnh đang dính – tăng lên nghĩa là vừa ghép thêm được mảnh. */
    fun joinCount(board: List<Int>, rows: Int, cols: Int, cut: Cut): Int {
        var k = 0
        for (i in board.indices) {
            val r = i / cols; val c = i % cols
            if (c < cols - 1 && joinsRight(cut, board[i], board[i + 1])) k++
            if (r < rows - 1 && joinsDown(cut, board[i], board[i + cols])) k++
        }
        return k
    }

    /** Các ảnh đã đủ khối k x k: (pic, ô góc trên-trái). */
    fun completed(board: List<Int>, rows: Int, cols: Int, cut: Cut): List<Pair<Int, Int>> {
        val k = cut.k
        val out = ArrayList<Pair<Int, Int>>()
        for (i in board.indices) {
            val p = board[i]
            if (p == EMPTY || cut.idxOf(p) != 0) continue
            val r = i / cols; val c = i % cols
            if (r > rows - k || c > cols - k) continue
            val ok = (0 until cut.per).all { j -> board[i + (j / k) * cols + j % k] == p + j }
            if (ok) out += cut.picOf(p) to i
        }
        return out
    }

    /**
     * Dịch cả nhóm [groupCells] đi (dr, dc) ô. Các mảnh (hoặc ô trống) bị chiếm chỗ
     * được đẩy vào các ô mà nhóm vừa bỏ trống. Trả null nếu không hợp lệ.
     */
    fun move(board: List<Int>, rows: Int, cols: Int, groupCells: List<Int>, dr: Int, dc: Int): List<Int>? {
        if (dr == 0 && dc == 0) return null
        val targets = groupCells.map {
            val r = it / cols + dr
            val c = it % cols + dc
            if (r !in 0 until rows || c !in 0 until cols) return null
            r * cols + c
        }
        val gs = groupCells.toSet()
        val ts = targets.toSet()
        val vacated = groupCells.filter { it !in ts }.sorted()
        val displaced = targets.filter { it !in gs }.sorted()
        val out = board.toMutableList()
        groupCells.forEachIndexed { k, cell -> out[targets[k]] = board[cell] }
        displaced.forEachIndexed { k, cell -> out[vacated[k]] = board[cell] }
        return out
    }

    /** Trọng lực: mỗi cột dồn mảnh xuống đáy. Trả về bàn mới + danh sách (từ ô, tới ô). */
    fun collapse(board: List<Int>, rows: Int, cols: Int): Pair<List<Int>, List<Pair<Int, Int>>> {
        val out = MutableList(board.size) { EMPTY }
        val moves = ArrayList<Pair<Int, Int>>()
        for (c in 0 until cols) {
            var write = rows - 1
            for (r in rows - 1 downTo 0) {
                val from = r * cols + c
                if (board[from] == EMPTY) continue
                val to = write * cols + c
                out[to] = board[from]
                if (to != from) moves += from to to
                write--
            }
        }
        return out to moves
    }

    /**
     * Sinh bàn đầu + xấp bài sao cho KHÔNG BAO GIỜ bế tắc: sau mỗi lần chia,
     * trên bàn luôn còn ít nhất 1 ảnh đủ mảnh.
     * (Ảnh nào bị người chơi xếp xong trước, hay mảnh rơi thế nào, đều không ảnh hưởng –
     * số mảnh còn lại của các ảnh dở dang là như nhau, nên mô phỏng 1 lần là đủ.)
     */
    fun generate(picCount: Int, cells: Int, cut: Cut, rnd: Random): Pair<List<Int>, List<Int>> {
        val per = cut.per
        val pool = (0 until picCount * per).toMutableList()
        val onBoard = IntArray(picCount)
        val removed = BooleanArray(picCount)

        fun draw(k: Int): List<Int> {
            val res = ArrayList<Int>()
            fun take(p: Int) { pool.remove(p); res += p; onBoard[cut.picOf(p)]++ }
            val hasComplete = (0 until picCount).any { !removed[it] && onBoard[it] == per }
            if (!hasComplete && pool.isNotEmpty()) {
                // Ưu tiên hoàn thiện ảnh đang có nhiều mảnh nhất trên bàn
                val pic = pool.map(cut::picOf).distinct().maxBy { onBoard[it] * 10 + rnd.nextInt(10) }
                pool.filter { cut.picOf(it) == pic }.forEach(::take)
            }
            while (res.size < k && pool.isNotEmpty()) {
                val active = pool.filter { onBoard[cut.picOf(it)] > 0 }
                val src = if (active.isNotEmpty() && rnd.nextFloat() < 0.55f) active else pool
                take(src.random(rnd))
            }
            return res.shuffled(rnd)
        }

        val board = draw(cells)
        val queue = ArrayList<Int>()
        while (pool.isNotEmpty()) {
            val done = (0 until picCount).first { !removed[it] && onBoard[it] == per }
            removed[done] = true
            onBoard[done] = 0
            queue += draw(per)
        }
        return board + List(cells - board.size) { EMPTY } to queue
    }

    /** Xếp bàn đầu sao cho chưa có ảnh nào hoàn thành sẵn và ít mảnh dính sẵn. */
    fun arrange(pieces: List<Int>, rows: Int, cols: Int, cut: Cut, rnd: Random): List<Int> {
        var best = pieces
        var bestScore = Int.MAX_VALUE
        repeat(200) {
            val b = pieces.shuffled(rnd)
            if (completed(b, rows, cols, cut).isNotEmpty()) return@repeat
            val s = joinCount(b, rows, cols, cut)
            if (s < bestScore) { best = b; bestScore = s }
            if (s <= 1) return b
        }
        return best
    }
}

class GameState(
    val rows: Int,
    val cols: Int,
    val cut: Cut,
    val picCount: Int,
    seed: Long = Random.nextLong(),
) {
    val n = rows * cols
    private val rnd = Random(seed)

    var board by mutableStateOf(emptyList<Int>())
        private set
    private val deck = ArrayDeque<Int>()
    var deckSize by mutableIntStateOf(0)
        private set
    var collected by mutableIntStateOf(0)
        private set
    var hints by mutableIntStateOf(3)
        private set
    var canUndo by mutableStateOf(false)
        private set

    private val history = ArrayDeque<List<Int>>()

    init {
        require(cut.k <= rows && cut.k <= cols) { "Bàn phải lớn hơn kiểu cắt" }
        val (b, q) = PuzzleLogic.generate(picCount, n, cut, rnd)
        board = PuzzleLogic.arrange(b, rows, cols, cut, rnd)
        deck.addAll(q)
        deckSize = deck.size
    }

    val cellGroup by derivedStateOf { PuzzleLogic.groupOfCells(board, rows, cols, cut) }
    val won by derivedStateOf { collected == picCount }

    fun picOf(piece: Int) = cut.picOf(piece)
    fun idxOf(piece: Int) = cut.idxOf(piece)

    fun groupCellsAt(cell: Int): List<Int> {
        if (board[cell] == EMPTY) return emptyList()
        val g = cellGroup[cell]
        return (0 until n).filter { cellGroup[it] == g }
    }

    fun completed() = PuzzleLogic.completed(board, rows, cols, cut)

    /** null = nước đi không hợp lệ, true = vừa ghép dính thêm mảnh. */
    fun tryMove(groupCells: List<Int>, dr: Int, dc: Int): Boolean? {
        val nb = PuzzleLogic.move(board, rows, cols, groupCells, dr, dc) ?: return null
        val before = PuzzleLogic.joinCount(board, rows, cols, cut)
        commit(nb)
        return PuzzleLogic.joinCount(nb, rows, cols, cut) > before
    }

    fun undo() {
        if (history.isEmpty()) return
        board = history.removeLast()
        canUndo = history.isNotEmpty()
    }

    /** Gợi ý: đưa 1 mảnh của ảnh đang ghép được về đúng chỗ. Trả về các mảnh vừa đổi. */
    fun hint(): List<Int>? {
        if (hints == 0) return null
        val per = cut.per
        val counts = IntArray(picCount)
        board.forEach { if (it != EMPTY) counts[picOf(it)]++ }
        val pics = (0 until picCount).filter { counts[it] == per }
        var best: Triple<Int, Int, Int>? = null // pic, anchor, score
        for (pic in pics) for (r in 0..rows - cut.k) for (c in 0..cols - cut.k) {
            val a = r * cols + c
            val score = (0 until per).count { board[anchorCell(a, it)] == pic * per + it }
            if (score < per && (best == null || score > best.third)) best = Triple(pic, a, score)
        }
        val (pic, anchor, _) = best ?: return null
        val k = (0 until per).first { board[anchorCell(anchor, it)] != pic * per + it }
        val target = anchorCell(anchor, k)
        val piece = pic * per + k
        val source = board.indexOf(piece)
        val nb = board.toMutableList()
        nb[source] = board[target]
        nb[target] = piece
        commit(nb)
        hints--
        return listOfNotNull(piece, board[source].takeIf { it != EMPTY })
    }

    private fun anchorCell(anchor: Int, idx: Int) = anchor + (idx / cut.k) * cols + idx % cut.k

    /** Gỡ ảnh đã hoàn thành khỏi bàn, trả về các ô vừa trống. Không undo qua mốc này. */
    fun removePicture(pic: Int): List<Int> {
        val cells = board.indices.filter { board[it] != EMPTY && picOf(board[it]) == pic }
        board = board.mapIndexed { i, p -> if (i in cells) EMPTY else p }
        collected++
        history.clear()
        canUndo = false
        return cells
    }

    /** Mảnh phía trên rơi xuống lấp chỗ trống. Trả về (từ ô, tới ô) của các mảnh đã rơi. */
    fun collapse(): List<Pair<Int, Int>> {
        val (nb, moves) = PuzzleLogic.collapse(board, rows, cols)
        board = nb
        return moves
    }

    fun emptyCells() = board.indices.filter { board[it] == EMPTY }

    /** Chia bài từ xấp vào các ô trống. Trả về (ô, mảnh). */
    fun deal(cells: List<Int>): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>()
        val nb = board.toMutableList()
        for (cell in cells) {
            val p = deck.removeFirstOrNull() ?: break
            nb[cell] = p
            out += cell to p
        }
        board = nb
        deckSize = deck.size
        return out
    }

    private fun commit(nb: List<Int>) {
        history.addLast(board)
        board = nb
        canUndo = true
    }
}
