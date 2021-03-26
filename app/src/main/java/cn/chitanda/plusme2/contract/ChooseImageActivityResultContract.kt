package cn.chitanda.plusme2.contract

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

/**
 * @Author:       Chen
 * @Date:         2021/3/26 15:12
 * @Description:
 */
class ChooseImageActivityResultContract: ActivityResultContract<Nothing, String>() {
    override fun createIntent(context: Context, input: Nothing?): Intent {
        return Intent()
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String {
        TODO("Not yet implemented")
    }
}