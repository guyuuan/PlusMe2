package cn.chitanda.plusme2.utile

import android.content.Context
import android.util.Log
import java.io.DataOutputStream

/**
 * @Author:       Chen
 * @Date:         2021/3/26 17:03
 * @Description:
 */
fun closeOPLauncher() {
    val command = "am force-stop net.oneplus.launcher \n"
    var process: Process? = null
    var os: DataOutputStream? = null
    try {
        process = Runtime.getRuntime().exec("su")
        os = DataOutputStream(process?.outputStream)
        os.apply {
            writeBytes(command)
            writeBytes("exit\n")
            flush()
        }
        process?.waitFor()
    } catch (e: Exception) {
        Log.d("Head", e.toString())
    } finally {
        os?.close()
        process?.destroy()
    }
}

fun rootCommand(context: Context): Boolean {
    val command = "chmod 777 ${context.packageCodePath}"
    var process: Process? = null
    var os: DataOutputStream? = null
    try {
        process = Runtime.getRuntime().exec("su")
        os = DataOutputStream(process?.outputStream)
        os?.apply {
            writeBytes(command + "\n")
            writeBytes("exit\n")
            flush()
        }
        process?.waitFor()
    } catch (e: Exception) {
        Log.d("Head", e.toString())
    } finally {
        os?.close()
        process?.destroy()
    }
    return true
}