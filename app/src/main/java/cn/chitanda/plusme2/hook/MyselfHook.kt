package cn.chitanda.plusme2.hook

import android.app.Activity
import android.os.Bundle
import androidx.annotation.Keep
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

@Keep
class MyselfHook : IXposedHookLoadPackage {
    companion object {
        lateinit var mainActivity: Activity
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam?.packageName != "cn.chitanda.plusme2") return
        XposedHelpers.findAndHookMethod(
            "cn.chitanda.plusme2.MainActivity",
            lpparam.classLoader,
            "CheckXP",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    param?.result = true
                }
            }
        )

        XposedHelpers.findAndHookMethod(
            "cn.chitanda.plusme2.MainActivity",
            lpparam.classLoader,
            "onCreate",
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    mainActivity = param?.thisObject as Activity
                    XposedBridge.log("MyselfHook:${mainActivity.applicationContext}")
                }
            }
        )
    }
}