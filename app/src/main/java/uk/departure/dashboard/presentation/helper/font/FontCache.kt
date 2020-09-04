package uk.departure.dashboard.presentation.helper.font

import android.content.Context
import android.graphics.Typeface
import java.util.*

/**
 * Cache fonts to decrease memory footprint of multiple creation of same Typefaces
 */
object FontCache {

    private const val FONTS_DIR = "fonts/"
    private val fontCache: MutableMap<String, Typeface?> = HashMap()

    // TODO: Support other types of fonts, not only Regular
    fun selectTypeface(context: Context, fontName: String): Typeface? {
        return getTypeface(
            "$fontName-Regular.ttf",
            context
        )
    }

    private fun getTypeface(fontName: String, context: Context): Typeface? {
        var typeface = fontCache[fontName]
        if (typeface == null) {
            typeface = try {
                Typeface.createFromAsset(context.assets, FONTS_DIR + fontName)
            } // Device specific workaround for
            catch (e: Exception) {
                return null
            }
            fontCache[fontName] = typeface
        }
        return typeface
    }
}