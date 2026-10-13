package dev.x360.emu

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Intro de arranque original (no usa material de Microsoft): una esfera verde que
 * se enciende, cintas que ondulan a su alrededor y un fundido final. Un toque la omite.
 */
class IntroView(context: Context, private val onFinished: () -> Unit) : View(context) {
    private val green = Color.parseColor("#9BF00B")
    private val darkGreen = Color.parseColor("#2E7D0F")
    private val ribbon = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val orb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var t = 0f  // progreso 0..1
    private var done = false

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = DURATION_MS
        interpolator = LinearInterpolator()
        addUpdateListener { t = it.animatedValue as Float; invalidate(); if (t >= 1f) finish() }
    }

    init { setBackgroundColor(Color.BLACK) }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
        IntroSound.play()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        IntroSound.stop()
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) finish()
        return true
    }

    private fun finish() {
        if (done) return
        done = true
        animate().alpha(0f).setDuration(350).withEndAction(onFinished).start()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2; val cy = h / 2
        val r = min(w, h) * 0.16f
        val glow = smooth((t - 0.05f) / 0.35f)             // la esfera se enciende
        val fade = 1f - smooth((t - 0.88f) / 0.12f)        // se apaga al final
        val k = glow * fade

        // Cintas
        for (i in 0 until 6) {
            val phase = i * 0.9f + t * 2f * PI.toFloat() * (1.2f + i * 0.08f)
            val amp = h * (0.05f + 0.035f * i) * smooth((t - 0.15f) / 0.4f)
            ribbon.strokeWidth = h * (0.012f - 0.0012f * i)
            ribbon.color = Color.argb((120 * k * (1f - i * 0.1f)).toInt(), 155, 240, 11)
            path.rewind()
            var x = 0f
            while (x <= w) {
                val env = sin(PI * x / w).toFloat()  // las cintas convergen en los bordes
                val y = cy + sin(x / w * 2f * PI.toFloat() * 1.5f + phase) * amp * env
                if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                x += 12f
            }
            c.drawPath(path, ribbon)
        }

        // Halo y esfera
        val pulse = 1f + 0.04f * sin(t * 14f)
        orb.shader = RadialGradient(cx, cy, r * 3.2f * pulse,
            intArrayOf(Color.argb((170 * k).toInt(), 155, 240, 11), Color.argb(0, 155, 240, 11)),
            null, Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r * 3.2f * pulse, orb)
        orb.shader = RadialGradient(cx - r * 0.3f, cy - r * 0.35f, r * 1.2f,
            intArrayOf(Color.argb((255 * k).toInt(), 230, 255, 160), Color.argb((255 * k).toInt(), 155, 240, 11),
                Color.argb((255 * k).toInt(), 46, 125, 15)),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r * pulse, orb)
    }

    private fun smooth(x: Float): Float { val v = x.coerceIn(0f, 1f); return v * v * (3 - 2 * v) }

    companion object { const val DURATION_MS = 4200L }
}
