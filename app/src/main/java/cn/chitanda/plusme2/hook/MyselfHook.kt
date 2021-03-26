package cn.chitanda.plusme2.hook

import androidx.annotation.Keep
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

@Keep
class MyselfHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if (lpparam?.packageName != "cn.chitanda.plusme2") return
        XposedHelpers.findAndHookMethod(
            "cn.chitanda.plusme2.MainActivity",
            lpparam.classLoader,
            "checkXP",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    super.afterHookedMethod(param)
                    param?.result = true
                }
            }
        )
    }
}