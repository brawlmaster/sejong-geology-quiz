package com.notibeam.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.notibeam.data.AppPreferences
import com.notibeam.data.FirebaseRepository
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

@Composable
fun PairingScreen(navController: NavController) {
    val channelIdState = remember { mutableStateOf("") }
    val qrBitmapState = remember { mutableStateOf<Bitmap?>(null) }
    val manualCode = remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val channelId = UUID.randomUUID().toString()
        channelIdState.value = channelId
        qrBitmapState.value = generateQrBitmap(
            JSONObject(mapOf("channelId" to channelId)).toString()
        )
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(title = { Text("Pair devices") })
        Text("Receiver: Scan this QR on the sender device to pair")
        qrBitmapState.value?.let { bmp ->
            Image(bitmap = bmp.asImageBitmap(), contentDescription = "Pair QR")
        }
        Button(onClick = { navController.navigate("scan") }) { Text("Sender: Scan QR") }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Or paste a pairing code:")
        OutlinedTextField(value = manualCode.value, onValueChange = { manualCode.value = it })
        Button(onClick = {
            val channelId = manualCode.value.ifBlank { channelIdState.value }
            if (channelId.isNotBlank()) {
                scope.launch {
                    AppPreferences.setChannelId(navController.context, channelId)
                    FirebaseRepository.registerReceiverDevice(channelId)
                }
            }
        }) { Text("Confirm pairing") }
    }
}

private fun generateQrBitmap(content: String, size: Int = 512): Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        bmp
    } catch (e: Exception) {
        null
    }
}