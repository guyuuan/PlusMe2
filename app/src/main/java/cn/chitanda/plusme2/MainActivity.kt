package cn.chitanda.plusme2

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.IOException

const val REQUEST_IMG_GET = 1
const val REQUEST_IMG_CROP = 2
const val REQUEST_PERMISSION = 3
const val REQUEST_PERMISSION_AGAIN = 4

class MainActivity : AppCompatActivity() {
    private var process: Process? = null
    private var os: DataOutputStream? = null
    private var uri: Uri? = null
    private var bgUri: Uri? = null
    private val sharedPref by lazy {
        applicationContext.getSharedPreferences(
            "Setting",
            Context.MODE_PRIVATE
        )
    }
    private val width by lazy { sharedPref.getInt("Width", 0) }
    private val height by lazy { sharedPref.getInt("Height", 0) }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        registerReceiver(MyReceiver(), IntentFilter("HeaderSize"))
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_PERMISSION
            )
        }
        imageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            if (intent.resolveActivity(packageManager) != null) startActivityForResult(
                intent,
                REQUEST_IMG_GET
            )
        }
        deleteButton.setOnClickListener {
            sendBroadcast(Intent("Delete"))
            RootCommand()
            closeOPLauncher()
            Toast.makeText(applicationContext, "已移除图片", Toast.LENGTH_SHORT).show()
        }
        changeButton.setOnClickListener {
            Log.v("Xposed-Bridge", "$width  $height")
            if (imageView.drawable == null || uri == null) {
                Toast.makeText(applicationContext, "请选择图片", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendBroadcast(Intent("Change").apply {
                putExtra("Uri", uri.toString())
            })
        }
        closeLauncher.setOnClickListener {
            RootCommand()
            closeOPLauncher()
            Toast.makeText(applicationContext, "已重启桌面", Toast.LENGTH_SHORT).show()
        }
        if (CheckXP()) {
            Toast.makeText(applicationContext, "Hooked", Toast.LENGTH_SHORT).show()
            sendBroadcast(Intent("getSize"))
        } else {
            Toast.makeText(applicationContext, "Hook failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMG_GET && resultCode == Activity.RESULT_OK) {
            data?.data?.let { cropIMG(it) }
        }
        if (requestCode == REQUEST_IMG_CROP && resultCode == Activity.RESULT_OK) {
            uri = data?.data
//            Toast.makeText(applicationContext, data?.data.toString(), Toast.LENGTH_SHORT).show()
            imageView.setImageURI(uri!!)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        applicationContext,
                        "呐，没有存储权限，是没有办法使用的呢 (*^_^*)",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    Handler().postDelayed({
                        requestPermissions(
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ),
                            REQUEST_PERMISSION_AGAIN
                        )
                    }, 2000)
                }
            }
            REQUEST_PERMISSION_AGAIN -> {
                Toast.makeText(applicationContext, "?????????????????????", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun saveImage() = withContext(Dispatchers.IO) {
        val bitmap = imageView.drawable.toBitmap()
        val saveUri = applicationContext.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues().also {
                it.put(
                    MediaStore.Images.ImageColumns.RELATIVE_PATH,
                    "Pictures/" + "PlusMe2"
                )
                it.put(
                    MediaStore.Images.ImageColumns.DISPLAY_NAME,
                    System.currentTimeMillis().toString()
                )
            }
        ) ?: kotlin.run {
            MainScope().launch {
                Toast.makeText(
                    applicationContext,
                    "Save Failed:URI is Null",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return@withContext false
        }
        applicationContext.contentResolver.openOutputStream(saveUri).use {
            if (bitmap.compress(Bitmap.CompressFormat.WEBP, 100, it)) {
                bgUri = saveUri
                MainScope().launch {
                    Toast.makeText(applicationContext, "Save Succeeded", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                MainScope().launch {
                    Toast.makeText(applicationContext, "Save Failed", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }
        }
        true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun deleteCrop(uri: Uri?) = withContext(Dispatchers.IO) {
        try {
            contentResolver.delete(uri!!, null, null)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e1: RecoverableSecurityException) {
            e1.printStackTrace()
            try {
                startIntentSenderForResult(
                    e1.userAction.actionIntent.intentSender,
                    111,
                    null, 0, 0, 0
                )
            } catch (e2: IntentSender.SendIntentException) {
                e2.printStackTrace()
            }
        }
    }


    private fun cropIMG(uri: Uri) {
        val intent = Intent("com.android.camera.action.CROP").apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra("aspectX", width)
            putExtra("aspectY", height)
            putExtra("outputX", width)
            putExtra("outputY", height)
            putExtra("crop", true)
            putExtra("return-data", false)
            putExtra("noFaceDetection", true)
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMG_CROP)
        }
    }

    private fun RootCommand(): Boolean {
        val command = "chmod 777 $packageCodePath"

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

    private fun closeOPLauncher() {
        val command = "am force-stop net.oneplus.launcher \n"
        try {
            process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process?.outputStream)
            os?.apply {
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

    @Keep
    private fun CheckXP() = false
}
