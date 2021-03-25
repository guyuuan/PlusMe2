package cn.chitanda.plusme2.hook

import android.app.Activity
import android.app.AndroidAppHelper
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Keep
import androidx.core.view.get
import cn.chitanda.plusme2.utile.BroadcastUtil
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Keep
class LauncherHook : IXposedHookLoadPackage {
    private lateinit var launcherContext: Context
    private lateinit var launcherActivity: Activity
    private lateinit var launcherClass: Class<*>
    private lateinit var header: ViewGroup
    private var width = 0
    private var height = 0
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            XposedBridge.log("Launcher onReceive:")
            when (intent?.action) {
                BroadcastUtil.CHANGE.action -> {
                    XposedBridge.log("Change")
                    val uri = Uri.parse(intent!!.getStringExtra("Uri"))
                    if (uri != null) MainScope().launch {
                        saveAndSetBackground(
                            uri
                        )
                    }
                }
                BroadcastUtil.DELETE.action -> {
                    XposedBridge.log("Delete")
                    MainScope().launch { removeBg() }
                }
                BroadcastUtil.GET_SIZE.action -> {
                    XposedBridge.log("getSize")
                    launcherContext.sendBroadcast(BroadcastUtil.GET_SIZE.apply {
                        setPackage("cn.chitanda.plusme2")
                        putExtra("Width", width)
                        putExtra("Height", height)
                    })
                }
            }
        }
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if ("net.oneplus.launcher" != lpparam?.processName) return
        try {
            launcherClass =
                XposedHelpers.findClass("net.oneplus.launcher.Launcher", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(
                launcherClass,
                "onCreate",
                Bundle::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam?) {
                        super.afterHookedMethod(param)
                        launcherActivity = param?.thisObject as Activity
                        launcherContext = AndroidAppHelper.currentApplication().applicationContext
                        XposedBridge.log("Context获取成功：$launcherContext")
                        launcherContext.registerReceiver(receiver, IntentFilter().apply {
                            addAction(BroadcastUtil.CHANGE.action)
                            addAction(BroadcastUtil.DELETE.action)
                            addAction(BroadcastUtil.GET_SIZE.action)
                        })
                    }
                }
            )
            XposedHelpers.findAndHookMethod(
                launcherClass,
                "onDestroy",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        super.beforeHookedMethod(param)
                        val context = AndroidAppHelper.currentApplication().applicationContext
                        XposedBridge.log("Context获取成功：$launcherContext")
                        context.unregisterReceiver(receiver)
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("Context获取错误")
            XposedBridge.log(t)
        }
        try {
            val clazz = XposedHelpers.findClass(
                "net.oneplus.launcher.quickpage.QuickPageCoordinatorLayout",
                lpparam.classLoader
            )
            XposedHelpers.findAndHookConstructor(clazz,
                Context::class.java, AttributeSet::class.java, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam?) {
                        super.afterHookedMethod(param)
                        val view = param?.thisObject as ViewGroup
                        view.post {
                            if (view.get(0) is ImageView) return@post
                            XposedBridge.log("add view ")
                            view.addView(ImageView(view.context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setImageDrawable(ColorDrawable(Color.GRAY))
                            }, 0)
                        }
                    }
                })
//            XposedHelpers.findAndHookMethod(
//                "net.oneplus.launcher.quickpage.QuickPageCoordinatorLayout",
//                lpparam.classLoader,
//                "dispatchTouchEvent",
//                MotionEvent::class.java,
//                object : XC_MethodHook() {
//                    override fun afterHookedMethod(param: MethodHookParam?) {
//                        super.afterHookedMethod(param)
//                        val view = param?.thisObject as ViewGroup
//                        view.post {
//                            if (view.get(0) is ImageView) return@post
//                            XposedBridge.log("add view ")
//                            view.addView(ImageView(view.context).apply {
//                                layoutParams = ViewGroup.LayoutParams(
//                                    ViewGroup.LayoutParams.MATCH_PARENT,
//                                    ViewGroup.LayoutParams.MATCH_PARENT
//                                )
//                                setImageDrawable(ColorDrawable(Color.GRAY))
//                            }, 0)
//                        }
//                    }
//                }
//            )
            XposedHelpers.findAndHookMethod(
                "net.oneplus.launcher.quickpage.view.WelcomePanel",
                lpparam.classLoader,
                "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam?) {
                        super.afterHookedMethod(param)
                        header = param?.thisObject as ViewGroup
                        val title =
                            XposedHelpers.getObjectField(header, "mWelcomeTitle") as TextView
//                        title.post {
//                            title.maxLines=-1
//                            title.updatePadding(top =1000)
//                        }
                        header.post {
                            (header.parent as ViewGroup).minimumHeight = 3120

                            if (width == 0 || height == 0) {
                                width = header.width
                                height = header.height
                                XposedBridge.log("welcome panel: $width $height ")
                                launcherContext.sendBroadcast(BroadcastUtil.GET_SIZE.apply {
                                    setPackage("cn.chitanda.plusme2")
                                    putExtra("Width", width)
                                    putExtra("Height", height)
                                })
                            }
                        }
//                        MainScope().launch { setBackground(getSavedBg()) }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("WelcomeHeader错误:")
            XposedBridge.log(t)
        }

    }

    private suspend fun removeBg() {
        withContext(Dispatchers.IO) {
            try {
                val file = File(launcherContext.filesDir, "bg")
                if (file.exists() && file.delete()) {
                    Toast.makeText(launcherContext, "删除成功", Toast.LENGTH_SHORT).show()
                }
//                setBackground(getSavedBg())
                header.backgroundTintList = null
            } catch (t: Throwable) {
                XposedBridge.log(t)
            }
        }
    }

    suspend fun saveAndSetBackground(uri: Uri?) {
        withContext(Dispatchers.IO) {
            try {
                if (saveBg(
                        BitmapFactory.decodeStream(
                            launcherContext.contentResolver.openInputStream(
                                uri!!
                            )
                        )
                    )
                ) {
                    deleteCrop(uri)
                    setBackground(getSavedBg())
                } else {
                    false
                }
            } catch (t: Throwable) {
                XposedBridge.log(t)
                false
            }
        }
    }

    private suspend fun deleteCrop(uri: Uri) = withContext(Dispatchers.IO) {
        launcherContext.contentResolver.delete(uri, null, null)
    }

    private fun setBackground(savedBg: Bitmap?) = if (savedBg != null) {
        header.background = BitmapDrawable(launcherContext.resources, savedBg)
        true
    } else {
        XposedBridge.log("Saved BitMap is null")
//        header.background = ColorDrawable(Color.parseColor("#22FFFFFF"))
        false
    }.also {
        launcherContext.sendBroadcast(BroadcastUtil.CHANGE_RESULT.apply {
            putExtra(BroadcastUtil.CHANGE_RESULT.action, it)
        })
    }


    private suspend fun saveBg(bitmap: Bitmap?) = withContext(Dispatchers.IO) {
        try {
            bitmap?.compress(
                Bitmap.CompressFormat.PNG,
                100,
                FileOutputStream(File(launcherContext.filesDir, "bg"))
            ) ?: false
        } catch (t: Throwable) {
            XposedBridge.log(t)
            false
        }
    }

    private suspend fun getSavedBg() = withContext(Dispatchers.IO) {
        val file = File(launcherContext.filesDir, "bg")
        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    }
}

