package com.quran.labs.androidquran.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.LinearLayout

/**
 * Custom LinearLayout yang menggambar background dengan lekukan (notch) di tengah atas,
 * mirip efek tombol QRIS di aplikasi Livin' Mandiri.
 */
class CurvedBottomNavContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val bgPath = Path()
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val topShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#1C000000")
    }

    // Dimensi notch dalam dp
    private val notchHalfWidthDp = 38f   // setengah lebar lekukan dari center
    private val notchDepthDp    = 20f    // kedalaman lekukan
    private val smoothingDp     = 14f    // bezier control offset untuk kelembutan kurva

    init {
        setWillNotDraw(false)
        setBackgroundColor(Color.TRANSPARENT)
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        val resolvedColor = androidx.core.content.ContextCompat.getColor(
            context,
            com.quran.labs.androidquran.R.color.secondary_dark_background
        )
        bgPaint.color = resolvedColor

        bgPaint.setShadowLayer(dp(8f), 0f, dp(-3f), Color.parseColor("#1A000000"))
        topShadowPaint.strokeWidth = dp(1f)
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildPath(w.toFloat(), h.toFloat())
    }

    private fun buildPath(w: Float, h: Float) {
        val cx          = w / 2f
        val halfW       = dp(notchHalfWidthDp)
        val depth       = dp(notchDepthDp)
        val smooth      = dp(smoothingDp)

        bgPath.reset()
        bgPath.moveTo(0f, 0f)

        // Garis kiri sampai tepi kiri lekukan
        bgPath.lineTo(cx - halfW - smooth, 0f)

        // Kurva Bezier kiri — turun ke bawah menuju titik terdalam lekukan
        bgPath.cubicTo(
            cx - halfW, 0f,
            cx - halfW * 0.45f, depth,
            cx, depth
        )

        // Kurva Bezier kanan — naik kembali ke atas
        bgPath.cubicTo(
            cx + halfW * 0.45f, depth,
            cx + halfW, 0f,
            cx + halfW + smooth, 0f
        )

        // Garis kanan sampai ujung
        bgPath.lineTo(w, 0f)
        bgPath.lineTo(w, h)
        bgPath.lineTo(0f, h)
        bgPath.close()
    }

    override fun onDraw(canvas: Canvas) {
        // Gambar background putih dengan lekukan
        canvas.drawPath(bgPath, bgPaint)
        // Gambar garis shadow tipis mengikuti path (bukan hanya garis lurus)
        canvas.drawPath(bgPath, topShadowPaint)
    }
}
