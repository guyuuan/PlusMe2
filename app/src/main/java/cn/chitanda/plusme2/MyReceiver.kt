package cn.chitanda.plusme2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        val sharedPref = context.getSharedPreferences("Setting", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("Width", intent.getIntExtra("Width", -1))
            putInt("Height", intent.getIntExtra("Height", -1))
            apply()
        }
    }
}
