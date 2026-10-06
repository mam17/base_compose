package com.example.basecompose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ảnh demo vẽ bằng code để project không cần asset nào (APK cực nhẹ).
 * Mỗi ảnh tỉ lệ 2:3, cắt 2x2 -> mỗi mảnh 2:3 giống ô trong video.
 * App thật: dùng ảnh WebP ~900px mỗi ảnh, tải theo level.
 */
object PictureFactory {
    const val W = 600
    const val H = 900

    private class Theme(val emoji: String, val top: Int, val bottom: Int, val glow: Int, val deco: String)

    private val themes = listOf(
        Theme("🎅", 0xFFB71C1C.toInt(), 0xFF4A0A0A.toInt(), 0xFFFFD54F.toInt(), "⭐"),
        Theme("🦌", 0xFF64B5F6.toInt(), 0xFF0D47A1.toInt(), 0xFFFFFFFF.toInt(), "❄️"),
        Theme("⛄", 0xFF81D4FA.toInt(), 0xFF1565C0.toInt(), 0xFFE1F5FE.toInt(), "❄️"),
        Theme("🎄", 0xFF1B5E20.toInt(), 0xFF071F0A.toInt(), 0xFFFFEB3B.toInt(), "✨"),
        Theme("🎁", 0xFF8E24AA.toInt(), 0xFF2A0738.toInt(), 0xFFFF80AB.toInt(), "🎀"),
        Theme("🔔", 0xFFC62828.toInt(), 0xFF3E0707.toInt(), 0xFFFFC107.toInt(), "✨"),
        Theme("🍭", 0xFFF48FB1.toInt(), 0xFF880E4F.toInt(), 0xFFFFFFFF.toInt(), "🍬"),
        Theme("🧦", 0xFF6D4C41.toInt(), 0xFF210F0A.toInt(), 0xFFFFAB40.toInt(), "🔥"),
        Theme("🍪", 0xFFFFB74D.toInt(), 0xFF6D3A00.toInt(), 0xFFFFF3E0.toInt(), "🥛"),
        Theme("🧸", 0xFF4DB6AC.toInt(), 0xFF00352F.toInt(), 0xFFFFF59D.toInt(), "🎈"),
        Theme("🐧", 0xFF4FC3F7.toInt(), 0xFF01304F.toInt(), 0xFFFFFFFF.toInt(), "❄️"),
        Theme("🦉", 0xFF283593.toInt(), 0xFF07092E.toInt(), 0xFFFFF176.toInt(), "🌙"),
        Theme("🛷", 0xFF26C6DA.toInt(), 0xFF004D57.toInt(), 0xFFFFFFFF.toInt(), "⛷️"),
        Theme("🌟", 0xFF311B92.toInt(), 0xFF0A0524.toInt(), 0xFFFFD740.toInt(), "✨"),
        Theme("🕯️", 0xFF5D4037.toInt(), 0xFF1B0F0B.toInt(), 0xFFFFCC80.toInt(), "✨"),
        Theme("🏠", 0xFF1A237E.toInt(), 0xFF050826.toInt(), 0xFFFFE082.toInt(), "🌨️"),
    )

    val themeCount get() = themes.size

    fun picture(themeIndex: Int): Bitmap {
        val t = themes[themeIndex % themes.size]
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val rnd = Random(themeIndex * 31 + 7)
        val cx = W / 2f
        val cy = H * 0.5f

        // Nền gradient
        p.shader = LinearGradient(0f, 0f, 0f, H.toFloat(), t.top, t.bottom, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)

        // Tia sáng toả từ tâm
        p.shader = null
        p.color = Color.argb(28, 255, 255, 255)
        val rays = 14
        for (i in 0 until rays step 2) {
            val a0 = Math.PI * 2 * i / rays
            val a1 = Math.PI * 2 * (i + 1) / rays
            c.drawPath(Path().apply {
                moveTo(cx, cy)
                lineTo(cx + (1200 * cos(a0)).toFloat(), cy + (1200 * sin(a0)).toFloat())
                lineTo(cx + (1200 * cos(a1)).toFloat(), cy + (1200 * sin(a1)).toFloat())
                close()
            }, p)
        }

        // Quầng sáng
        p.shader = RadialGradient(cx, cy, W * 0.62f,
            intArrayOf(withAlpha(t.glow, 170), withAlpha(t.glow, 40), Color.TRANSPARENT), floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, W * 0.62f, p)
        p.shader = null

        // Bokeh
        repeat(16) {
            p.color = withAlpha(if (it % 3 == 0) t.glow else Color.WHITE, 30 + rnd.nextInt(50))
            c.drawCircle(rnd.nextFloat() * W, rnd.nextFloat() * H, 18f + rnd.nextFloat() * 46f, p)
        }

        // Đồi tuyết phía dưới
        p.color = Color.argb(235, 240, 246, 255)
        c.drawPath(Path().apply {
            moveTo(0f, H * 0.83f); quadTo(W * 0.3f, H * 0.74f, W * 0.62f, H * 0.82f)
            quadTo(W * 0.85f, H * 0.87f, W.toFloat(), H * 0.79f)
            lineTo(W.toFloat(), H.toFloat()); lineTo(0f, H.toFloat()); close()
        }, p)

        // Emoji trang trí 4 góc (giúp nhận ra từng mảnh)
        val deco = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
        val spots = listOf(0.2f to 0.13f, 0.82f to 0.18f, 0.16f to 0.7f, 0.85f to 0.68f)
        spots.forEachIndexed { i, (x, y) ->
            deco.textSize = 70f + (i % 2) * 24f
            drawCentered(c, t.deco, W * x, H * y, deco)
        }

        // Nhân vật chính cỡ lớn ở giữa – trải đều ra cả 4 mảnh
        val hero = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = W * 0.78f
            setShadowLayer(28f, 0f, 14f, Color.argb(130, 0, 0, 0))
        }
        drawCentered(c, t.emoji, cx, cy, hero)

        // Tuyết rơi
        p.color = Color.argb(210, 255, 255, 255)
        repeat(70) { c.drawCircle(rnd.nextFloat() * W, rnd.nextFloat() * H, rnd.nextFloat() * 4f + 1.5f, p) }
        return bmp
    }

    /** Mặt sau lá bài: xanh lá, hoa văn kim cương, cây thông ở giữa (giống video). */
    fun cardBack(): Bitmap {
        val w = 300; val h = 450
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val border = 16f
        p.color = Color.WHITE
        c.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 18f, 18f, p)
        val inner = RectF(border, border, w - border, h - border)
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), Color.rgb(52, 178, 82), Color.rgb(24, 128, 52), Shader.TileMode.CLAMP)
        c.drawRoundRect(inner, 10f, 10f, p)
        p.shader = null
        c.save()
        c.clipRect(inner)
        drawArgyle(c, p, inner, 60f, 80f)
        c.restore()

        // Quầng sáng + cây thông
        val cx = w / 2f; val cy = h / 2f
        p.shader = RadialGradient(cx, cy, 120f, Color.argb(120, 200, 255, 200), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, 120f, p)
        p.shader = null
        p.color = Color.rgb(170, 240, 170)
        for (k in 0 until 3) {
            val top = cy - 70f + k * 34f
            val half = 30f + k * 14f
            c.drawPath(Path().apply { moveTo(cx, top); lineTo(cx - half, top + 52f); lineTo(cx + half, top + 52f); close() }, p)
        }
        c.drawRect(cx - 10f, cy + 50f, cx + 10f, cy + 72f, p)
        return bmp
    }

    /** Lá bài nằm trong xấp (dải ngang) – chỉ thấy cạnh trên. */
    fun deckCard(): Bitmap {
        val w = 300; val h = 120
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.WHITE
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        val inner = RectF(10f, 10f, w - 10f, h - 10f)
        p.color = Color.rgb(36, 150, 64)
        c.drawRect(inner, p)
        c.save(); c.clipRect(inner)
        drawArgyle(c, p, inner, 60f, 70f)
        c.restore()
        return bmp
    }

    private fun drawArgyle(c: Canvas, p: Paint, r: RectF, dw: Float, dh: Float) {
        p.color = Color.argb(45, 255, 255, 255)
        var y = r.top
        var row = 0
        while (y < r.bottom + dh) {
            var x = r.left + if (row % 2 == 0) 0f else dw / 2
            while (x < r.right + dw) {
                c.drawPath(Path().apply {
                    moveTo(x, y - dh / 2); lineTo(x + dw / 2, y); lineTo(x, y + dh / 2); lineTo(x - dw / 2, y); close()
                }, p)
                x += dw
            }
            y += dh / 2
            row++
        }
    }

    private fun drawCentered(c: Canvas, text: String, x: Float, y: Float, p: Paint) {
        val fm = p.fontMetrics
        c.drawText(text, x, y - (fm.ascent + fm.descent) / 2, p)
    }

    private fun withAlpha(color: Int, a: Int) = Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))

    /** Đọc ảnh người dùng chọn, giảm kích thước và cắt giữa theo tỉ lệ 2:3. */
    fun load(context: Context, uri: Uri): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1400) sample *= 2
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val ratio = W.toFloat() / H
        val (w, h) = if (bmp.width.toFloat() / bmp.height > ratio)
            (bmp.height * ratio).toInt() to bmp.height
        else bmp.width to (bmp.width / ratio).toInt()
        Bitmap.createScaledBitmap(Bitmap.createBitmap(bmp, (bmp.width - w) / 2, (bmp.height - h) / 2, w, h), W, H, true)
    }.getOrNull()
}
