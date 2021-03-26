package cn.chitanda.plusme2.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import cn.chitanda.plusme2.utile.BroadcastUtil

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        when (intent.action) {
            BroadcastUtil.GET_SIZE.action -> {
                val sharedPref = context.getSharedPreferences("Setting", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putInt("Width", intent.getIntExtra("Width", 0))
                    putInt("Height", intent.getIntExtra("Height", 0))
                    if (sharedPref.getInt("BackupHeight", 0) == 0) {
                        putInt("BackupHeight", intent.getIntExtra("Height", 0))
                    }
                    apply()
                }
            }
            BroadcastUtil.CHANGE_RESULT.action -> {
                if (intent.getBooleanExtra(BroadcastUtil.CHANGE_RESULT.action, false)) {
                    Toast.makeText(context, "设置成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "设置失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
