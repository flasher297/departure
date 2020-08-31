package uk.departure.dashboard

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt


class SpeedometerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.RED
    }
    private val paintArch = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    val textPaint = Paint().apply {
        color = Color.GREEN
        textSize = 80f
        textAlign = Paint.Align.CENTER

    }


    val mTextPaths = mutableListOf<Path>()

    var mph: Float = 0.0f

    init {

        GlobalScope.launch(Dispatchers.Main) {
            var startTime = 0L

            while (true) {
                delay(100)
                val radian = Math.toRadians(startTime.toDouble())
                val coef = abs(
                    (
                            sin(8 * radian) + sin(0.4 * sqrt(radian))) / 2
                )
                updateRelatively(coef.toFloat().coerceIn(0f, 1f))
                startTime++
            }


        }

    }


    private val path = Path()

    var arcPath: Path? = null
    val oval = RectF()


    private fun drawTickMarks(canvas: Canvas?) {
        val radius = width * 0.45f
        val cy = height * 0.5f
        val cx = width * 0.5f

        var iTickRad: Double

        for (iTick in 0..180 step 10) {
            iTickRad = Math.toRadians(iTick.toDouble())

            val onArchX = radius - (Math.cos(iTickRad) * radius * 1.05)
            val onArchY = radius - (Math.sin(iTickRad) * radius * 1.05)
            val innerTickX = radius - (Math.cos(iTickRad) * radius * 0.95)
            val innerTickY = radius - (Math.sin(iTickRad) * radius * 0.95)

            val fromX = (cx - radius) + onArchX
            val fromY = (cy - radius) + onArchY

            val toX = (cx - radius) + innerTickX
            val toY = (cy - radius) + innerTickY

            canvas?.drawLine(
                fromX.toFloat(),
                fromY.toFloat(),
                toX.toFloat(),
                toY.toFloat(),
                paintArch
            )


        }
    }

    private fun drawTexts(canvas: Canvas?) {

        val radius = width * 0.45f
        val cy = height * 0.5f
        val cx = width * 0.5f

        var iTickRad:Double

        // Tick every 20 degrees (small ticks)
        if (mTextPaths.isEmpty()) {

            for (iTick in 0..180 step 10) {
                iTickRad = Math.toRadians(iTick.toDouble())

                val onArchX = radius - (Math.cos(iTickRad) * radius * 0.9)
                val onArchY = radius - (Math.sin(iTickRad) * radius * 0.9)

                var fromX = (cx - radius) + onArchX
                var fromY = (cy - radius) + onArchY


                val textBound = Rect()
                textPaint.getTextBounds(iTick.toString(), 0, iTick.toString().length, textBound)
                when {
                    iTick < 50 -> {
                        fromX += textBound.width() * iTick / 90
                        fromY += textBound.height() * 0.7
                    }
                    iTick < 70 -> {
                        fromX += textBound.width() * 0.3
                        fromY += textBound.height()
                    }
                    iTick <= 90 -> {
                        fromY += textBound.height() * 1.2
                    }
                    iTick <= 110 -> {
                        fromY += textBound.height() * 1f
                    }
                    iTick <= 130 -> {
                        fromX -= textBound.width() * 0.1
                        fromY += textBound.height() * 1.1f
                    }
                    else -> {
                        fromX -= textBound.width() * 0.5 * iTick / 180
                        fromY += textBound.height() * 0.7
                    }
                }



                ///canvas?.drawLine(fromX.toFloat(), fromY.toFloat(), toX.toFloat(), toY.toFloat(), paintArch);
                if (iTick % 20 != 0) {
                    //canvas?.drawText(iTick.toString(), fromX.toFloat(), fromY.toFloat(), textPaint)
                    val path = Path()
                    textPaint.getTextPath(
                        iTick.toString(),
                        0,
                        iTick.toString().length,
                        fromX.toFloat(),
                        fromY.toFloat(),
                        path
                    )
                    mTextPaths.add(path)
                }


            }
        }
        mTextPaths.forEach {
            canvas?.drawPath(it, textPaint)
        }
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val radius = width * 0.45f
        val cy = height * 0.5f
        val cx = width * 0.5f

        arcPath = Path()
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius)

        //val startAngle =  (180 / Math.PI * Math.atan2(point.y - point1.y, point.x - point1.x));
        arcPath?.arcTo(oval, -180f, 180f, true)
    }

    override fun onDraw(canvas: Canvas?) {

        canvas?.drawColor(Color.CYAN)


        drawTickMarks(canvas)
        drawTexts(canvas)

        // TODO: onAttach or on size change all object allocations


        canvas?.drawPath(arcPath!!, paintArch)

        val barh = height * 0.5f

        canvas?.save()
        canvas?.rotate(mph * 180.0f, width * 0.5f, height * 0.5050f)


        canvas?.drawRect(
            width * 0.15f,
            barh,
            width * 0.5f,
            height * 0.51f,
            paint
        )
        canvas?.restore()
    }

    private var _val: Float = 0f
    val value: Float
        get() = _val

    private var animator: Animator? = null

    var doOnValueUpdateAction: (Float) -> Unit = {
        mph = it
        invalidate()
    }

    fun setValue(n: Float) {
        _val = n
        doOnValueUpdateAction(n)
    }

    fun updateRelatively(newValue: Float) {
        if (animator?.isRunning == true) {
            return
        }
        val delta = value - newValue
        animator = ValueAnimator.ofFloat(value, newValue).apply {
            duration = (abs(delta) * 300L).toLong()
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                setValue(it.animatedValue as Float)
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.end()
    }
}