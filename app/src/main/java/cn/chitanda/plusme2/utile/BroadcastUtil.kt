package cn.chitanda.plusme2.utile

import android.content.Intent

object BroadcastUtil {
    val CHANGE_RESULT = Intent("ChangeResult")
    val CHANGE = Intent("Change")
    val DELETE = Intent("DELETE")
    val GET_SIZE = Intent("GetSize")
}