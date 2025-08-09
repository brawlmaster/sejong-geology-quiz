package com.notibeam.ui.screens

import android.Manifest
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.notibeam.data.AppPreferences
import com.notibeam.data.FirebaseRepository
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScanScreen(navController: NavController) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val scope = rememberCoroutineScope()
    val errorText = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    if (!cameraPermission.status.isGranted) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission required for scanning")
        }
        return
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            DecoratedBarcodeView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
                decodeContinuous { result ->
                    val text = result.text ?: return@decodeContinuous
                    try {
                        val obj = runCatching { JSONObject(text) }.getOrNull()
                        val channelId = obj?.optString("channelId").takeUnless { it.isNullOrBlank() }
                            ?: text // allow raw code
                        scope.launch {
                            AppPreferences.setChannelId(context, channelId)
                            FirebaseRepository.registerReceiverDevice(channelId)
                            this@apply.pause()
                            navController.popBackStack()
                        }
                    } catch (e: Exception) {
                        errorText.value = "Invalid QR"
                    }
                }
                resume()
            }
        },
        update = { view ->
            view.resume()
        }
    )
}