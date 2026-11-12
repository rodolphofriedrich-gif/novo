package com.robsonmartins.androidmidisynth

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.floor

/**
 * Teclado virtual de duas oitavas com suporte a acordes por multitoque.
 */
class PianoKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onNoteOn: ((note: Int, velocity: Int) -> Unit)? = null
    var onNoteOff: ((note: Int) -> Unit)? = null
    var velocity: Int = 100

    private var baseNote = 48 // C3
    private val pointerNotes = mutableMapOf<Int, Int>()
    private val notePressCount = mutableMapOf<Int, Int>()

    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val blackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF172033.toInt() }
    private val pressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6366F1.toInt() }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF64748B.toInt()
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF475569.toInt()
        textSize = dp(10f)
        textAlign = Paint.Align.CENTER
    }

    fun clearPressedKeys() {
        releaseAllNotes()
    }

    fun setBaseNote(note: Int) {
        releaseAllNotes()
        baseNote = note.coerceIn(0, 96)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val whiteWidth = width / 14f
        if (whiteWidth <= 0f) return
        val blackWidth = whiteWidth * 0.62f
        val blackHeight = height * 0.62f
        val whiteSemitones = intArrayOf(0, 2, 4, 5, 7, 9, 11)

        for (whiteIndex in 0 until 14) {
            val octave = whiteIndex / 7
            val degree = whiteIndex % 7
            val note = baseNote + octave * 12 + whiteSemitones[degree]
            val rect = RectF(
                whiteIndex * whiteWidth,
                0f,
                (whiteIndex + 1) * whiteWidth,
                height.toFloat()
            )
            canvas.drawRect(rect, if (notePressCount.containsKey(note)) pressedPaint else whitePaint)
            canvas.drawRect(rect, borderPaint)
            if (degree == 0) {
                val octaveNumber = (note / 12) - 1
                canvas.drawText("C$octaveNumber", rect.centerX(), height - dp(9f), labelPaint)
            }
        }

        val blackDegrees = intArrayOf(1, 3, 6, 8, 10)
        val whiteBefore = intArrayOf(0, 1, 3, 4, 5)
        for (semitone in 0 until 24) {
            val degree = semitone % 12
            val blackIndex = blackDegrees.indexOf(degree)
            if (blackIndex < 0) continue
            val octave = semitone / 12
            val note = baseNote + semitone
            val center = (octave * 7 + whiteBefore[blackIndex] + 1) * whiteWidth
            val rect = RectF(
                center - blackWidth / 2f,
                0f,
                center + blackWidth / 2f,
                blackHeight
            )
            canvas.drawRoundRect(
                rect,
                dp(2f),
                dp(2f),
                if (notePressCount.containsKey(note)) pressedPaint else blackPaint
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                pressPointer(event.getPointerId(index), event.xForIndex(index), event.yForIndex(index))
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                for (index in 0 until event.pointerCount) {
                    movePointer(event.getPointerId(index), event.xForIndex(index), event.yForIndex(index))
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                releasePointer(event.getPointerId(event.actionIndex))
                if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                releaseAllNotes()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun pressPointer(pointerId: Int, x: Float, y: Float) {
        val note = hitTest(x, y)
        if (note >= 0) {
            pointerNotes[pointerId] = note
            val count = notePressCount[note] ?: 0
            notePressCount[note] = count + 1
            if (count == 0) onNoteOn?.invoke(note, velocity.coerceIn(1, 127))
            invalidate()
        }
    }

    private fun movePointer(pointerId: Int, x: Float, y: Float) {
        val nextNote = hitTest(x, y)
        val previousNote = pointerNotes[pointerId] ?: -1
        if (nextNote == previousNote) return
        if (previousNote >= 0) releaseNote(previousNote)
        if (nextNote >= 0) {
            pointerNotes[pointerId] = nextNote
            val count = notePressCount[nextNote] ?: 0
            notePressCount[nextNote] = count + 1
            if (count == 0) onNoteOn?.invoke(nextNote, velocity.coerceIn(1, 127))
        } else {
            pointerNotes.remove(pointerId)
        }
        invalidate()
    }

    private fun releasePointer(pointerId: Int) {
        val note = pointerNotes.remove(pointerId) ?: return
        releaseNote(note)
        invalidate()
    }

    private fun releaseNote(note: Int) {
        val count = (notePressCount[note] ?: 1) - 1
        if (count <= 0) {
            notePressCount.remove(note)
            onNoteOff?.invoke(note)
        } else {
            notePressCount[note] = count
        }
    }

    private fun releaseAllNotes() {
        notePressCount.keys.toList().forEach { onNoteOff?.invoke(it) }
        notePressCount.clear()
        pointerNotes.clear()
        invalidate()
    }

    private fun hitTest(x: Float, y: Float): Int {
        val whiteWidth = width / 14f
        if (whiteWidth <= 0f || x < 0f || x > width || y < 0f || y > height) return -1
        val blackWidth = whiteWidth * 0.62f
        if (y <= height * 0.62f) {
            val blackDegrees = intArrayOf(1, 3, 6, 8, 10)
            val whiteBefore = intArrayOf(0, 1, 3, 4, 5)
            for (semitone in 0 until 24) {
                val degree = semitone % 12
                val blackIndex = blackDegrees.indexOf(degree)
                if (blackIndex < 0) continue
                val octave = semitone / 12
                val center = (octave * 7 + whiteBefore[blackIndex] + 1) * whiteWidth
                if (x >= center - blackWidth / 2f && x <= center + blackWidth / 2f) {
                    return baseNote + semitone
                }
            }
        }
        val whiteIndex = floor((x / whiteWidth).toDouble()).toInt().coerceIn(0, 13)
        val whiteSemitones = intArrayOf(0, 2, 4, 5, 7, 9, 11)
        return baseNote + (whiteIndex / 7) * 12 + whiteSemitones[whiteIndex % 7]
    }

    private fun MotionEvent.xForIndex(index: Int): Float = getX(index)
    private fun MotionEvent.yForIndex(index: Int): Float = getY(index)
    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
