package cn.chitanda.plusme2.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import cn.chitanda.plusme2.IPlusMeService

/**
 * @Author:       Chen
 * @Date:         2021/3/26 16:46
 * @Description:
 */
class IPlusMeStub : IPlusMeService.Stub() {
    private var welcomePanelWidth = 0
    private var welcomePanelHeight = 0
    override fun setWelcomePanelSize(width: Int, height: Int) {
        welcomePanelWidth = width
        welcomePanelHeight = height
    }

    override fun getWelcomePanelWidth(): Int {
        return welcomePanelWidth
    }

    override fun getWelcomePanelHeight(): Int {
        return welcomePanelHeight
    }
}

class PlusMeService : Service() {
    private val binder = IPlusMeStub()
    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }
}