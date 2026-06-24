package com.aviateclone.launcher.widget

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

class SideAlphabetView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        val LETTERS = (listOf("#") + ('A'..'Z').map { it.toString() })
    }

    var onLetterSelected: ((String) -> Unit)? = null

    private val paintNormal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4D5BBF")
        textSize = 11f * resources.displayMetrics.density
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }
    private val paintActive = Paint(paintNormal).apply {
        color = Color.WHITE
        textSize = 13f * resources.displayMetrics.density
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC4D5BBF")
    }
    private val bgRect = RectF()
    private var activeIndex = -1
    private var isPressed = false

    override fun onDraw(canvas: Canvas) {
        if (isPressed) {
            bgRect.set(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(bgRect, width / 2f, width / 2f, bgPaint)
        }
        val cellH = height.toFloat() / LETTERS.size
        LETTERS.forEachIndexed { i, letter ->
            val y = cellH * i + cellH / 2f
            val paint = if (i == activeIndex) paintActive else paintNormal
            val metrics = paint.fontMetrics
            canvas.drawText(letter, width / 2f,
                y - (metrics.ascent + metrics.descent) / 2f, paint)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                isPressed = true
                val idx = (e.y / height * LETTERS.size).toInt().coerceIn(0, LETTERS.size - 1)
                if (idx != activeIndex) {
                    activeIndex = idx
                    // FIX 2: TEXT_HANDLE_MOVE richiede API 27; fallback su API 26
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                    } else {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                    onLetterSelected?.invoke(LETTERS[idx])
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false; activeIndex = -1; invalidate()
            }
        }
        return true
    }

    override fun onMeasure(w: Int, h: Int) {
        val desired = (18 * resources.displayMetrics.density).roundToInt()
        setMeasuredDimension(desired, resolveSize(
            (LETTERS.size * 14 * resources.displayMetrics.density).roundToInt(), h))
    }
}
