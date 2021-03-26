package cn.chitanda.plusme2.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import cn.chitanda.compose.networkimage.core.NetworkImage
import cn.chitanda.plusme2.R
import cn.chitanda.plusme2.utile.closeOPLauncher
import cn.chitanda.plusme2.utile.rootCommand

/**
 * @Author:       Chen
 * @Date:         2021/3/26 13:44
 * @Description:
 */

@Composable
fun MyApp() {
    val context = LocalContext.current
    Scaffold(modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = {
                rootCommand(context)
                closeOPLauncher()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ok),
                    contentDescription = "Confirm"
                )
            }
        }) {
        var uri by remember { mutableStateOf("") }
        var uriBackup = uri
        val chooseImageLauncher = getChooseImageLauncher { result ->
            uri = result?.toString() ?: uriBackup
        }
        Center(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (uri.isEmpty()) {
                IconButton(
                    modifier = Modifier.fillMaxSize(0.5f),
                    onClick = { chooseImageLauncher.launch("image/*") }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_circle_outline_black_24dp),
                        contentDescription = "Click To Choose Image"
                    )
                }
            } else {
                Log.d("MyApp", ": $uri")
                NetworkImage(
                    url = uri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            chooseImageLauncher.launch("image/*")
                            uri = ""
                        }, onLoading = {
                        CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
                    },
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}


@Composable
private fun getChooseImageLauncher(callback: ActivityResultCallback<Uri>): ActivityResultLauncher<String> {
    val registryOwner = LocalActivityResultRegistryOwner.current
    return registryOwner.activityResultRegistry.register(
        "plusme2",
        ActivityResultContracts.GetContent(),
        callback
    )
}

@Composable
fun Center(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        content()
    }
}