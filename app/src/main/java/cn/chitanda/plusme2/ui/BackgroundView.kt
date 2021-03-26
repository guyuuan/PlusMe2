package cn.chitanda.plusme2.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import de.robv.android.xposed.XposedBridge
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * @Author:       Chen
 * @Date:         2021/3/26 9:32
 * @Description:
 */
private const val TAG = "BackgroundView"

class BackgroundView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val IOScope = CoroutineScope(CoroutineName("PlusMe2-BackgroundView") + Dispatchers.IO)

    init {
        log("create")
    }

    var uri: Uri? = null
        set(value) {
            if (value == null) return
            field = value
            log("url = $uri")
//            this.post {
            loadBitmapFromUri(value)
//            }
        }
    private var bitmap: Bitmap? = null

    private fun log(s: String) {
        XposedBridge.log("$TAG: $s")
    }

    private fun loadBitmapFromUri(uri: Uri) {
        MainScope().launch {
            BitmapFactory.Options().run {
                try {
                    inJustDecodeBounds = true
                    BitmapFactory.decodeStream(
                        context.contentResolver.openInputStream(uri), Rect(-1, -1, -1, -1), this
                    )
                    inSampleSize = calculateInSampleSize(this, width, height)
                    inJustDecodeBounds = false
                    bitmap = BitmapFactory.decodeStream(
                        context.contentResolver.openInputStream(uri)
                    )
                    log("bitmap = $bitmap")
                    postInvalidate()
                } catch (e: Exception) {
                    XposedBridge.log(e.toString())
                }
            }
        }
    }

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(bitmap ?: return, 0f, 0f, paint)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    override fun onDetachedFromWindow() {
//        IOScope.cancel()
        super.onDetachedFromWindow()
    }
}