package cn.chitanda.plusme2.hook

import android.app.Activity
import android.app.AndroidAppHelper
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Keep
import androidx.core.view.get
import cn.chitanda.plusme2.service.IPlusMeStub
import cn.chitanda.plusme2.ui.BackgroundView
import cn.chitanda.plusme2.utile.BroadcastUtil
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

const val TAG = "PlusMe2"

@Keep
class LauncherHook : IXposedHookLoadPackage {
    private lateinit var launcherContext: Context
    private lateinit var launcherActivity: Activity
    private lateinit var launcherClass: Class<*>
    private lateinit var header: ViewGroup
    private var backgroundView: BackgroundView? = null
    private var width = 0
    private var height = 0
/*    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            XposedBridge.log("Launcher onReceive:")
            when (intent?.action) {
                BroadcastUtil.CHANGE.action -> {
                    XposedBridge.log("Change")
                    var uri: Uri? = null
                    intent?.let {
                        uri = Uri.parse(it.getStringExtra("Uri"))
                        val takeFlags = (it.flags
                                and (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
                        try {
                            launcherContext.contentResolver.takePersistableUriPermission(
                                uri!!, takeFlags
                            )
                        } catch (e: Exception) {
                            Toast.makeText(launcherContext, e.message, Toast.LENGTH_SHORT).show()
                        } finally {
                            backgroundView?.uri = uri
                        }
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
    }*/

    private var binder: IPlusMeStub? = null
    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            XposedBridge.log("onServiceConnected")
            Toast.makeText(launcherActivity,"onServiceConnected",Toast.LENGTH_SHORT).show()
            binder = service as? IPlusMeStub
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
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
                    override fun beforeHookedMethod(param: MethodHookParam?) {
                        super.beforeHookedMethod(param)
                        Log.i(TAG, "beforeHookedMethod: Launcher onCreate")
                    }
                    override fun afterHookedMethod(param: MethodHookParam?) {
                        super.afterHookedMethod(param)
                        launcherActivity = param?.thisObject as Activity
                        launcherContext = AndroidAppHelper.currentApplication().applicationContext
                        Log.i(TAG, "afterHookedMethod: Launcher onCreate")
/*                        launcherContext.registerReceiver(receiver, IntentFilter().apply {
                            addAction(BroadcastUtil.CHANGE.action)
                            addAction(BroadcastUtil.DELETE.action)
                            addAction(BroadcastUtil.GET_SIZE.action)
                        })*/
//                        launcherActivity.bindService(
//                            Intent(
//                                launcherActivity,
//                                PlusMeService::class.java
//                            ), conn, Context.BIND_AUTO_CREATE
//                        )
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
//                        context.unregisterReceiver(receiver)
                        launcherActivity.unbindService(conn)
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
                            if (view[0] is ImageView) return@post
                            XposedBridge.log("add view ")
                            backgroundView = BackgroundView(view.context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                            backgroundView?.let { view.addView(it, 0) }
                            val id = view.context.resources.getIdentifier(
                                "status_background",
                                "id",
                                "net.oneplus.launcher"
                            )
                            val statusBarBG = view.findViewById<View>(id)
                            view.removeView(statusBarBG)
                        }
                    }
                })
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
                            (header.parent.parent as ViewGroup).minimumHeight = 3120

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

