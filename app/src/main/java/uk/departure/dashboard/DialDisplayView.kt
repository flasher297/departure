package uk.departure.dashboard

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.annotation.IntDef
import uk.departure.dashboard.DialDisplayView.DialType.Companion.ROTATOR
import uk.departure.dashboard.DialDisplayView.DialType.Companion.SPEED
import uk.departure.dashboard.DialDisplayView.SwipeState.Companion.NONE
import uk.departure.dashboard.DialDisplayView.SwipeState.Companion.SWIPE
import uk.departure.dashboard.font.FontCache
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val SWIPE_THRESHOLD_PX = 100
private const val EDGE_ANGLE = 180f

class DialDisplayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(SPEED, ROTATOR)
    annotation class DialType {
        companion object {
            const val SPEED = 0
            const val ROTATOR = 1
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(NONE, SWIPE)
    annotation class SwipeState {
        companion object {
            const val NONE = 0
            const val SWIPE = 1
        }
    }

    var doOnLeftSwipe: () -> Unit = {}
    var doOnRightSwipe: () -> Unit = {}

    private val arrowPaint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
    }
    private val patternPaint: Paint

    private val digitsPaint: Paint

    // cached Paths, Primitives, Points to reduce memory allocations and calculations onDraw
    private val textPaths = mutableListOf<Path>()
    private val tickLines = mutableListOf<RectF>()
    private var arcPath: Path? = null
    private var arrow: RectF? = null
    private var xArrowPivot = 0.0f
    private var yArrowPivot = 0.0f

    // Key points for dial drawing
    private var radius = 0.0f
    private var cy = 0.0f
    private var cx = 0.0f

    // Custom attributes values
    private val backgroundColor: Int
    private val patternColor: Int

    @DialType
    private val dialDisplayType: Int
    private val digitsFontSize: Int
    private val digitsFontName: String

    // Always between 0 and 1
    private var dialValue: Float = 0f

    //We use ValueAnimator that with help of choreographer sync animations calls with draw calls
    private var animator: Animator? = null

    // Swipe helper fields
    private var swipeStartX = 0f
    private var swipeStopX = 0f

    @SwipeState
    private var mode = NONE


    //TODO: stop animation on hide of view.
    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.DialDisplay,
            0, 0
        ).apply {

            try {
                backgroundColor = getColor(R.styleable.DialDisplay_dialBackground, 0)
                patternColor = getColor(R.styleable.DialDisplay_patternColor, 0)
                dialDisplayType = getInteger(R.styleable.DialDisplay_dialType, SPEED)
                digitsFontSize = getDimensionPixelSize(R.styleable.DialDisplay_digitsFontSize, 80)
                digitsFontName = getString(R.styleable.DialDisplay_digitsFont) ?: ""
            } finally {
                recycle()
            }

            patternPaint = Paint().apply {
                color = patternColor
                style = Paint.Style.STROKE
                strokeWidth = 4.dpToPxFloat
            }

            val typeface = FontCache.selectTypeface(context, digitsFontName)
            digitsPaint = Paint().apply {
                color = Color.GREEN
                typeface?.let {
                    setTypeface(it)
                }
                isAntiAlias = true
                textSize = digitsFontSize.toFloat()
                textAlign = Paint.Align.CENTER
            }
        }
    }

    // TODO: Magic numbers - room for improvement

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        radius = width * 0.45f
        cy = height * 0.5f
        cx = width * 0.5f

        createArcPath()
        createTickerLines()
        createTextPaths()
        createArrow()
    }

    private fun createArrow() {
        xArrowPivot = width * 0.5f
        yArrowPivot = height * 0.5050f
        arrow = RectF(
            width * 0.2f,
            height * 0.5f,
            width * 0.5f,
            height * 0.51f
        )
    }

    /**
     * Creates path for dial display curve
     */
    private fun createArcPath() {
        arcPath = Path()
        val oval = RectF().apply {
            set(cx - radius, cy - radius, cx + radius, cy + radius)
        }
        arcPath?.arcTo(oval, -EDGE_ANGLE, EDGE_ANGLE, true)
    }

    private fun createTextPaths() {
        if (textPaths.isNotEmpty()) {
            textPaths.clear()
        }

        for (iAngle in 0..EDGE_ANGLE.toInt() step 10) {
            val (digitText, drawCondition) = getTextDrawParamsByDialType(iAngle)
            if (drawCondition(iAngle)) {
                val iTickRadian = Math.toRadians(iAngle.toDouble())

                val onArcX = radius - (cos(iTickRadian) * radius * 0.9)
                val onArcY = radius - (sin(iTickRadian) * radius * 0.9)

                var screenX = (cx - radius) + onArcX
                var screenY = (cy - radius) + onArcY

                val textBound = Rect()

                digitsPaint.getTextBounds(
                    digitText,
                    0,
                    digitText.length,
                    textBound
                )

                // Adjust text paddings depending on angle and text bound size
                when {
                    iAngle < 50 -> {
                        screenX += textBound.width() * iAngle / 90
                        screenY += textBound.height() * 0.7
                    }
                    iAngle < 70 -> {
                        screenX += textBound.width() * 0.3
                        screenY += textBound.height()
                    }
                    iAngle <= 90 -> {
                        screenY += textBound.height() * 1.2
                    }
                    iAngle <= 110 -> {
                        screenY += textBound.height() * 1f
                    }
                    iAngle <= 130 -> {
                        screenX -= textBound.width() * 0.1
                        screenY += textBound.height() * 1.1f
                    }
                    else -> {
                        screenX -= textBound.width() * 0.5 * iAngle / 180
                        screenY += textBound.height() * 0.7
                    }
                }

                val path = Path()
                digitsPaint.getTextPath(
                    digitText,
                    0,
                    digitText.length,
                    screenX.toFloat(),
                    screenY.toFloat(),
                    path
                )
                textPaths.add(path)
            }
        }
    }

    /**
     * Depending on the [angle] function returns text to render and function that decides
     * on which angle draw the text.
     */
    private fun getTextDrawParamsByDialType(angle: Int): Pair<String, (Int) -> Boolean> {
        return when (dialDisplayType) {
            SPEED -> {
                // Treat angle as speed in kmh and allow to draw values that are divided by 20 with remainder
                val drawCondition: (Int) -> Boolean = { it % 20 != 0 }
                angle.toString() to drawCondition
            }
            ROTATOR -> {
                // Treat every 30 degree as 1000 rpm
                val drawCondition: (Int) -> Boolean = { it % 30 == 0 }
                (angle / 30).toString() to drawCondition
            }
            else -> throw UnsupportedOperationException("Type is not part of DialType")
        }
    }

    private fun createTickerLines() {
        if (tickLines.isNotEmpty()) {
            tickLines.clear()
        }
        var iTickRad: Double
        for (iTick in 0..EDGE_ANGLE.toInt() step 10) {
            iTickRad = Math.toRadians(iTick.toDouble())

            val outArcTickX = radius - (cos(iTickRad) * radius * 1.05)
            val outArcTickY = radius - (sin(iTickRad) * radius * 1.05)
            val innerArcTickX = radius - (cos(iTickRad) * radius * 0.95)
            val innerArcTickY = radius - (sin(iTickRad) * radius * 0.95)

            val fromScreenX = (cx - radius) + outArcTickX
            val fromScreenY = (cy - radius) + outArcTickY

            val toScreenX = (cx - radius) + innerArcTickX
            val toScreenY = (cy - radius) + innerArcTickY

            // use RectF to store line representation
            val tickLine = RectF(
                fromScreenX.toFloat(),
                fromScreenY.toFloat(),
                toScreenX.toFloat(),
                toScreenY.toFloat()
            )
            tickLines.add(tickLine)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        // Grouping draw calls by draw primitives' types
        canvas?.drawColor(backgroundColor)

        // Draw tickers
        tickLines.forEach {
            canvas?.drawLine(
                it.left,
                it.top,
                it.right,
                it.bottom,
                patternPaint
            )
        }

        // Draw texts
        textPaths.forEach {
            canvas?.drawPath(it, digitsPaint)
        }
        // Draw dial arc
        arcPath?.let {
            canvas?.drawPath(it, patternPaint)
        }

        // Draw and rotate arrow
        canvas?.save()
        canvas?.rotate(dialValue * EDGE_ANGLE, xArrowPivot, yArrowPivot)
        arrow?.let { canvas?.drawRect(it, arrowPaint) }
        canvas?.restore()

    }

    /**
     * We receive a new value [0,1]
     * Start animation only with delta with current value
     */
    fun updateRelatively(newValue: Float) {
        if (animator?.isRunning == true) {
            return
        }
        val delta = dialValue - newValue
        animator = ValueAnimator.ofFloat(dialValue, newValue).apply {
            duration = (abs(delta) * 300L).toLong()
            interpolator = AccelerateInterpolator()
            addUpdateListener {
                dialValue = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.end()
        doOnLeftSwipe = {}
        doOnRightSwipe = {}
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return false
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN -> { // This happens when you touch the screen with second finger
                mode = SWIPE
                swipeStartX = event.getX(0) // We need a diff so can use any pointerIndex
            }
            MotionEvent.ACTION_POINTER_UP -> { // This happens when you release the second finger
                mode = NONE
                if (abs(swipeStartX - swipeStopX) > SWIPE_THRESHOLD_PX) {
                    if (swipeStartX > swipeStopX) {
                        doOnLeftSwipe()
                    } else {
                        doOnRightSwipe()
                    }
                }
                mode = NONE
            }
            MotionEvent.ACTION_MOVE -> if (mode == SWIPE) {
                swipeStopX = event.getX(0)
            }
        }
        return true
    }

}