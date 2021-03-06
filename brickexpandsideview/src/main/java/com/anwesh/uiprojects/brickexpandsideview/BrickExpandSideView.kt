package com.anwesh.uiprojects.brickexpandsideview

/**
 * Created by anweshmishra on 29/07/20.
 */

import android.view.View
import android.view.MotionEvent
import android.app.Activity
import android.content.Context
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF

val colors : Array<String> = arrayOf("#3F51B5", "#4CAF50", "#2196F3", "#F44336", "#009688")
val bricks : Int = 5
val scGap : Float = 0.02f / bricks
val sizeFactor : Float = 2.4f
val delay : Long = 20
val backColor : Int = Color.parseColor("#BDBDBD")

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.sinify() : Float = Math.sin(this * Math.PI).toFloat()

fun Canvas.drawSideExpanderBrick(i : Int, scale : Float, w : Float, h : Float, paint : Paint) {
    val sf : Float = scale.sinify()
    val sf1 : Float = sf.divideScale(0, 2)
    val sf2 : Float = sf.divideScale(1, 2)
    val sf1i : Float = sf1.divideScale(i, bricks)
    val sf2i : Float = sf2.divideScale(i, bricks)
    val gap : Float = h / (bricks + 1)
    val size : Float = gap / sizeFactor
    val y : Float = h - gap * (i + 1)
    val upSize : Float = size * sf1i
    save()
    translate(-(w / 2 - size) * sf2i, y)
    drawRect(RectF(-upSize, -size / 2, 0f, size / 2), paint)
    restore()
}

fun Canvas.drawSideExpanderBricks(scale : Float, w : Float, h : Float, paint : Paint) {
    for (j in 0..1) {
        save()
        translate(w / 2, 0f)
        scale(1f - 2 * j, 1f)
        for (i in 0..(bricks - 1)) {
            drawSideExpanderBrick(i, scale, w, h, paint)
        }
        restore()
    }
}

fun Canvas.drawSEBNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    paint.color = Color.parseColor(colors[i])
    drawSideExpanderBricks(scale, w, h, paint)
}

class BrickExpandSideView(ctx : Context) : View(ctx) {

    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scGap * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class BESNode(var i : Int, val state : State = State()) {

        private var next : BESNode? = null
        private var prev : BESNode? = null

        init {
           addNeighbor()
        }

        fun addNeighbor() {
            if (i < colors.size - 1) {
                next = BESNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawSEBNode(i, state.scale, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : BESNode {
            var curr : BESNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class BrickExpandSide(var i : Int) {

        private var curr : BESNode = BESNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : BrickExpandSideView) {

        private val animator : Animator = Animator(view)
        private val bes : BrickExpandSide = BrickExpandSide(0)
        private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun render(canvas : Canvas) {
            canvas.drawColor(backColor)
            bes.draw(canvas, paint)
            animator.animate {
                bes.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            bes.startUpdating {
                animator.start()
            }
        }
    }

    companion object {

        fun create(activity : Activity) : BrickExpandSideView {
            val view : BrickExpandSideView = BrickExpandSideView(activity)
            activity.setContentView(view)
            return view
        }
    }
}