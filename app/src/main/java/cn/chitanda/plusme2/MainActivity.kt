package cn.chitanda.plusme2

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import cn.chitanda.compose.networkimage.glide.ProvideGlideLoader
import cn.chitanda.plusme2.receiver.MyReceiver
import cn.chitanda.plusme2.ui.MyApp
import cn.chitanda.plusme2.utile.BroadcastUtil
import java.io.DataOutputStream

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class MainActivity : AppCompatActivity() {
    private var process: Process? = null
    private var os: DataOutputStream? = null
    private val receiver by lazy { MyReceiver() }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(BroadcastUtil.GET_SIZE.action)
                addAction(BroadcastUtil.CHANGE_RESULT.action)
            })
        setContent {
            ProvideGlideLoader {
                MyApp()
            }
        }
    }

    @Keep
    private fun checkXP() = false


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}
