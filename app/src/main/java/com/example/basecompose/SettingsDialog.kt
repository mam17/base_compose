package com.example.basecompose

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.ceil

/** Kiểu cắt ảnh (k x k mảnh) + kích thước bàn. Lưu trong SharedPreferences. */
data class GameSettings(val cut: Int = 2, val cols: Int = 4, val rows: Int = 4) {

    /** Số ảnh mỗi ván: đủ lấp bàn + tăng dần theo level. */
    fun pictureCount(level: Int, max: Int): Int {
        val fill = ceil(cols * rows / (cut * cut).toFloat()).toInt()
        return (fill + 2 + level * 2).coerceAtMost(max)
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("cut", cut).putInt("cols", cols).putInt("rows", rows).apply()
    }

    companion object {
        private const val PREFS = "game_settings"
        val Cuts = listOf(2, 3, 4)
        /** (cột, hàng) */
        val Boards = listOf(4 to 4, 4 to 5, 5 to 5, 6 to 6)

        fun load(context: Context): GameSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val s = GameSettings(p.getInt("cut", 2), p.getInt("cols", 4), p.getInt("rows", 4))
            return if (s.cut <= minOf(s.cols, s.rows)) s else GameSettings()
        }
    }
}

private val Title = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
private val Label = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
private val Note = TextStyle(color = Color(0xDDFFFFFF), fontSize = 13.sp)

@Composable
fun SettingsDialog(current: GameSettings, onDismiss: () -> Unit, onApply: (GameSettings) -> Unit) {
    var cut by remember { mutableStateOf(current.cut) }
    var board by remember { mutableStateOf(current.cols to current.rows) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF6FAEFF), Color(0xFF1E5BC6))), RoundedCornerShape(24.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BasicText("⚙  Cài đặt", style = Title)

            BasicText("Cắt mỗi ảnh thành", style = Label)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GameSettings.Cuts.forEach { k ->
                    OptionChip(selected = cut == k, onClick = {
                        cut = k
                        // Bàn phải chứa vừa 1 ảnh -> tự nâng bàn nếu đang nhỏ hơn
                        if (minOf(board.first, board.second) < k) board = GameSettings.Boards.first { minOf(it.first, it.second) >= k }
                    }) {
                        GridIcon(k, k)
                        BasicText("$k×$k", style = Label)
                    }
                }
            }

            BasicText("Kích thước bàn (cột × hàng)", style = Label)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameSettings.Boards.forEach { b ->
                    val ok = minOf(b.first, b.second) >= cut
                    OptionChip(selected = board == b, enabled = ok, onClick = { board = b }) {
                        BasicText("${b.first}×${b.second}", style = Label)
                    }
                }
            }

            val preview = GameSettings(cut, board.first, board.second)
            BasicText(
                "Mỗi ảnh ${cut * cut} mảnh · Level 1 có ${preview.pictureCount(1, PictureFactory.themeCount)} ảnh\n" +
                        "Đổi cài đặt sẽ bắt đầu lại từ Level 1.",
                style = Note,
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogButton("Huỷ", Modifier.weight(1f), Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x26FFFFFF))), onDismiss)
                DialogButton(
                    "Áp dụng", Modifier.weight(1f),
                    Brush.verticalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF9800))),
                ) { onApply(preview) }
            }
        }
    }
}

@Composable
private fun OptionChip(
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .alpha(if (enabled) 1f else 0.35f)
            .background(if (selected) Color(0xFFFFB300) else Color(0x33FFFFFF), shape)
            .border(2.dp, if (selected) Color(0xFFFFF59D) else Color(0x55FFFFFF), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

/** Biểu tượng lưới nhỏ minh hoạ kiểu cắt. */
@Composable
private fun GridIcon(cols: Int, rows: Int) {
    Column(Modifier.width(30.dp).aspectRatio(2f / 3f), verticalArrangement = Arrangement.spacedBy(1.5.dp)) {
        repeat(rows) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                repeat(cols) {
                    Box(Modifier.weight(1f).fillMaxHeight().background(Color.White, RoundedCornerShape(1.dp)))
                }
            }
        }
    }
}

@Composable
private fun DialogButton(label: String, modifier: Modifier, brush: Brush, onClick: () -> Unit) {
    Box(
        modifier.height(46.dp).background(brush, RoundedCornerShape(23.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { BasicText(label, style = Label.copy(fontSize = 17.sp)) }
}
