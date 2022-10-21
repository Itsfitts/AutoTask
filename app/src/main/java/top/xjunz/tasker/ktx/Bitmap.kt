package top.xjunz.tasker.ktx

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * @author xjunz 2022/10/15
 */

fun Bitmap.clip(bounds: Rect): Bitmap {
    val clipped =
        Bitmap.createBitmap(this, bounds.left, bounds.top, bounds.width(), bounds.height())
    recycle()
    return clipped
}