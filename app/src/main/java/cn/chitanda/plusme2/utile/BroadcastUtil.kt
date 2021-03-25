package cn.chitanda.plusme2.utile

import android.content.Intent

object BroadcastUtil {
    val CHANGE_RESULT get() = Intent("ChangeResult")
    val CHANGE get() = Intent("Change")
    val DELETE get() = Intent("DELETE")
    val GET_SIZE
        get() = Intent("GetSize")
}