package cn.chitanda.plusme2.hook

import android.app.ActivityThread
import android.util.Log
import cn.chitanda.plusme2.service.IPlusMeStub
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers


/**
 * @Author:       Chen
 * @Date:         2021/3/26 17:32
 * @Description:
 */
class ApplicationSocket : IXposedHookZygoteInit {
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        XposedBridge.hookAllMethods(ActivityThread::class.java, "systemMain", SystemServiceHook())

    }
}

class SystemServiceHook : XC_MethodHook() {
    companion object {
        var plusMeBinder: IPlusMeStub? = null
        var systemHooked = false
    }

    override fun afterHookedMethod(param: MethodHookParam?) {
        if (systemHooked) return
        val classLoader = Thread.currentThread().contextClassLoader
        var activityManagerServiceClazz: Class<*>? = null
        try {
            activityManagerServiceClazz =
                XposedHelpers.findClass("com.android.server.am.ActivityManagerService", classLoader)
        } catch (e: RuntimeException) {
            errorLog(e)
        }

        if (!systemHooked && activityManagerServiceClazz != null) {
            XposedBridge.hookAllMethods(
                activityManagerServiceClazz,
                "systemReady",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam?) {
                        super.afterHookedMethod(param)
                        Log.i(TAG, "systemReady: ")
                    }
                })

        }
    }
}

fun errorLog(s: Throwable) {
    XposedBridge.log("$TAG error: $s")
}