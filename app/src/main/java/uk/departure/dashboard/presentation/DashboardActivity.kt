package uk.departure.dashboard.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import org.koin.android.viewmodel.ext.android.viewModel
import uk.departure.dashboard.R
import uk.departure.dashboard.presentation.helper.observe

class DashboardActivity : AppCompatActivity() {

    private lateinit var speedometerView: DialDisplayView
    private lateinit var tachometerView: DialDisplayView
    private lateinit var root: MotionLayout
    private val viewModel: DashboardViewModel by viewModel()

    @SuppressLint("InlinedApi")
    private val hideRunnable = Runnable {
        root.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)

        initViews()
        makeFullScreenNonSleep()
        subscribeToEngineData()
    }

    private fun subscribeToEngineData() {
        // view updates not propagated from live data not in active state,
        // so in stopped state animation stops
        this.observe(viewModel.engineOutcome) {
            // TODO: think how to stop animation on hidden view
            speedometerView.updateRelatively(it.speedCoeff)
            tachometerView.updateRelatively(it.powerCoeff)
        }
    }

    private fun initViews() {
        root = findViewById(R.id.motionLayout)
        speedometerView = findViewById(R.id.speedometer_view)
        tachometerView = findViewById(R.id.tachometer_view)
        speedometerView.doOnLeftSwipe = {
            root.transitionToEnd()
        }
        tachometerView.doOnRightSwipe = {
            root.transitionToStart()
            // TODO: separate transitions for left and right animations
        }
    }

    private fun makeFullScreenNonSleep() {
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        speedometerView.post(hideRunnable)
    }

}