package com.notibeam.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val selectedRole = remember { mutableStateOf(0) } // 0: Sender, 1: Receiver

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(title = { Text("NotiBeam") })

        SegmentedButtonRow {
            SegmentedButton(
                selected = selectedRole.value == 0,
                onClick = { selectedRole.value = 0 },
                label = { Text("Sender") }
            )
            SegmentedButton(
                selected = selectedRole.value == 1,
                onClick = { selectedRole.value = 1 },
                label = { Text("Receiver") }
            )
        }

        Button(onClick = { navController.navigate("pair") }, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
            Text("Pair devices")
        }
        Button(onClick = { navController.navigate("apps") }, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
            Text("Select apps")
        }
        Button(onClick = { navController.navigate("devices") }, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
            Text("Manage devices")
        }
        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }) {
            Text("Grant notification access")
        }
        Button(onClick = {
            val pm = context.getSystemService(PowerManager::class.java)
            val pkg = context.packageName
            val ignoring = pm?.isIgnoringBatteryOptimizations(pkg) ?: false
            if (!ignoring && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$pkg")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }) { Text("Allow background running") }
    }
}