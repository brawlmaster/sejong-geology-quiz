package com.notibeam.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.notibeam.data.AppPreferences
import com.notibeam.data.model.PairedDevice
import kotlinx.coroutines.launch

@Composable
fun DevicesScreen(navController: NavController) {
    val pairedDevices = remember { mutableStateOf(listOf<PairedDevice>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val ctx = navController.context
        val channelId = AppPreferences.getChannelId(ctx) ?: return@LaunchedEffect
        FirebaseFirestore.getInstance()
            .collection("channels").document(channelId)
            .collection("devices")
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map {
                    PairedDevice(
                        deviceId = it.getString("deviceId") ?: it.id,
                        channelId = channelId,
                        fcmToken = it.getString("token"),
                        displayName = it.getString("displayName")
                    )
                } ?: emptyList()
                pairedDevices.value = list
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Paired devices") })
        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(pairedDevices.value) { device ->
                ListItem(
                    headlineContent = { Text(device.displayName ?: device.deviceId) },
                    supportingContent = { Text(device.deviceId) },
                    trailingContent = {
                        Button(onClick = {
                            scope.launch {
                                val ctx = navController.context
                                val channelId = AppPreferences.getChannelId(ctx) ?: return@launch
                                com.notibeam.data.FirebaseRepository.removeDevice(channelId, device.deviceId)
                            }
                        }) { Text("Remove") }
                    }
                )
            }
        }
    }
}