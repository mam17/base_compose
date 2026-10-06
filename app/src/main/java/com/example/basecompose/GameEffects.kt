package com.example.basecompose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/* ------------------------------------------------------------------ */
/*  Hệ hạt đơn giản. Toạ độ & vận tốc tính theo ĐƠN VỊ Ô để không phụ  */
/*  thuộc kích thước màn hình; size tính theo px.                      */
/* ------------------------------------------------------------------ */

enum class ParticleKind { CONFETTI, STAR, GLOW }

class Particle(
    var x: Float, var y: Float,
    var vx: Float, var vy: Float,
    val life: Float,
    val size: Float,
    val color: Color,
    val kind: ParticleKind,
    val gravity: Float,
    var rot: Float = 0f,
    val vr: Float = 0f,
) {
    var age = 0f
}

class ParticleSystem {
    val items = ArrayList<Particle>()
    private val rnd = Random.Default

    val isActive get() = items.isNotEmpty()

    fun update(dt: Float) {
        val it = items.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.age += dt
            if (p.age >= p.life) { it.remove(); continue }
            p.vy += p.gravity * dt
            p.vx *= 1f - 1.2f * dt // lực cản không khí
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rot += p.vr * dt
        }
    }

    /** Nổ pháo hoa giấy + sao từ tâm ảnh vừa hoàn thành. */
    fun burst(cx: Float, cy: Float, count: Int = 44, density: Float) {
        repeat(count) { i ->
            val a = rnd.nextFloat() * 2 * PI.toFloat()
            val sp = 1.6f + rnd.nextFloat() * 3.6f
            val star = i % 3 == 0
            items += Particle(
                cx, cy, cos(a) * sp, sin(a) * sp - 1.5f,
                life = 0.9f + rnd.nextFloat() * 0.7f,
                size = (if (star) 7f else 5f + rnd.nextFloat() * 4f) * density,
                color = if (star) Gold else ConfettiColors.random(rnd),
                kind = if (star) ParticleKind.STAR else ParticleKind.CONFETTI,
                gravity = if (star) 2f else 7f,
                rot = rnd.nextFloat() * 360f,
                vr = (rnd.nextFloat() - 0.5f) * 900f,
            )
        }
    }

    /** Vài ngôi sao nhỏ lấp lánh quanh 1 điểm (lật bài, ghép dính). */
    fun twinkle(cx: Float, cy: Float, spreadX: Float, spreadY: Float, count: Int, density: Float, color: Color = Color.White) {
        repeat(count) {
            items += Particle(
                cx + (rnd.nextFloat() - 0.5f) * spreadX, cy + (rnd.nextFloat() - 0.5f) * spreadY,
                (rnd.nextFloat() - 0.5f) * 0.8f, -0.4f - rnd.nextFloat() * 0.8f,
                life = 0.45f + rnd.nextFloat() * 0.4f,
                size = (4f + rnd.nextFloat() * 5f) * density,
                color = color,
                kind = if (it % 2 == 0) ParticleKind.STAR else ParticleKind.GLOW,
                gravity = 0f,
            )
        }
    }

    /** Vệt sáng bám theo lá bài đang bay. */
    fun trail(x: Float, y: Float, density: Float) {
        items += Particle(
            x + (rnd.nextFloat() - 0.5f) * 0.3f, y + (rnd.nextFloat() - 0.5f) * 0.3f, 0f, 0.3f,
            life = 0.4f, size = (3f + rnd.nextFloat() * 4f) * density,
            color = Gold, kind = ParticleKind.GLOW, gravity = 0f,
        )
    }

    /** Mưa confetti khi thắng. */
    fun rain(cols: Int, density: Float) {
        repeat(140) {
            items += Particle(
                rnd.nextFloat() * cols, -2.5f - rnd.nextFloat() * 3f,
                (rnd.nextFloat() - 0.5f) * 1.5f, 1f + rnd.nextFloat() * 2f,
                life = 3.5f + rnd.nextFloat() * 1.5f,
                size = (5f + rnd.nextFloat() * 5f) * density,
                color = ConfettiColors.random(rnd), kind = ParticleKind.CONFETTI,
                gravity = 1.6f, rot = rnd.nextFloat() * 360f, vr = (rnd.nextFloat() - 0.5f) * 720f,
            )
        }
    }

    fun clear() = items.clear()
}

val Gold = Color(0xFFFFD54F)
private val ConfettiColors = listOf(
    Color(0xFFE53935), Color(0xFFFFD54F), Color(0xFF43A047), Color(0xFFFFFFFF),
    Color(0xFF29B6F6), Color(0xFFEC407A), Color(0xFFAB47BC),
)

/** Ngôi sao 4 cánh kiểu "lấp lánh". */
fun DrawScope.sparkle(center: Offset, radius: Float, color: Color, alpha: Float = 1f) {
    if (radius <= 0.5f || alpha <= 0.01f) return
    val inner = radius * 0.22f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x + inner, center.y - inner, center.x + radius, center.y)
        quadraticTo(center.x + inner, center.y + inner, center.x, center.y + radius)
        quadraticTo(center.x - inner, center.y + inner, center.x - radius, center.y)
        quadraticTo(center.x - inner, center.y - inner, center.x, center.y - radius)
        close()
    }
    drawCircle(color.copy(alpha = 0.35f * alpha), radius * 0.45f, center)
    drawPath(path, color, alpha = alpha)
}

/** Vẽ hạt; [toPx] đổi toạ độ ô -> px. */
fun DrawScope.drawParticles(ps: ParticleSystem, toPx: (Float, Float) -> Offset) {
    for (p in ps.items) {
        val t = p.age / p.life
        val a = if (t < 0.7f) 1f else 1f - (t - 0.7f) / 0.3f
        val c = toPx(p.x, p.y)
        when (p.kind) {
            ParticleKind.CONFETTI -> rotate(p.rot, c) {
                // Lật lật như giấy: chiều rộng dao động theo góc quay
                val w = p.size * (0.35f + 0.65f * kotlin.math.abs(cos(p.rot * PI.toFloat() / 180f)))
                drawRect(p.color, Offset(c.x - w / 2, c.y - p.size * 0.35f), Size(w, p.size * 0.7f), alpha = a)
            }
            ParticleKind.STAR -> sparkle(c, p.size * (1f - 0.4f * t), p.color, a)
            ParticleKind.GLOW -> {
                drawCircle(p.color.copy(alpha = 0.25f * a), p.size * 1.8f, c)
                drawCircle(p.color.copy(alpha = a), p.size * 0.6f, c)
            }
        }
    }
}

/** Nền tuyết rơi chậm phía sau bàn chơi. */
@Composable
fun SnowBackground(modifier: Modifier = Modifier) {
    val flakes = remember {
        val r = Random(42)
        List(55) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.4f + r.nextFloat(), r.nextFloat() * 6.28f) }
    }
    val t by rememberInfiniteTransition(label = "snow").animateFloat(
        0f, 1f, infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart), label = "t",
    )
    Canvas(modifier) {
        for (f in flakes) {
            val speed = f[2]
            val y = ((f[1] + t * speed * 3f) % 1f) * size.height
            val x = f[0] * size.width + sin(t * 2 * PI.toFloat() * 4f * speed + f[3]) * 18.dp2px(density)
            val r = (1.5f + speed * 2.5f) * density
            drawCircle(Color.White.copy(alpha = 0.18f + 0.25f * speed / 1.4f), r, Offset(x, y))
        }
    }
}

private fun Int.dp2px(density: Float) = this * density
