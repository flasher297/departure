package uk.departure.dashboard.presentation.helper

import android.content.res.Resources
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer


fun <T : Any> LifecycleOwner.observe(data: LiveData<T>, onObserve: (T) -> Unit) =
    data.observe(this, Observer { onObserve(it) })

fun LifecycleOwner.observeUnit(data: LiveData<Unit>, onObserve: () -> Unit) =
    data.observe(this, Observer { onObserve() })

// Extension property to convert pixels to DP
val Int.pxToDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

val Int.dpToPx: Int
    get() = this.dpToPxFloat.toInt()

val Int.dpToPxFloat: Float
    get() = (this * Resources.getSystem().displayMetrics.density)

val Float.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

// Extension property to convert SP to pixels
val Int.spToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.scaledDensity).toInt()
val Float.spToPx: Float
    get() = this * Resources.getSystem().displayMetrics.scaledDensity