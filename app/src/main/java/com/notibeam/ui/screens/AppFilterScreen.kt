package com.notibeam.ui.screens

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.navigation.NavController
import com.notibeam.data.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppFilterScreen(navController: NavController) {
    val context = LocalContext.current
    val pm = context.packageManager
    var apps by remember { mutableStateOf(listOf<AppEntry>()) }
    var enabledPackages by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        enabledPackages = AppPreferences.getEnabledPackages(context)
        apps = withContext(Dispatchers.Default) {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .map { appInfo ->
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    AppEntry(
                        packageName = appInfo.packageName,
                        displayName = label,
                        isSystemApp = isSystem
                    )
                }
                .sortedBy { it.displayName.lowercase() }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Select apps to forward") })
        LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(apps) { app ->
                val checked = enabledPackages.contains(app.packageName)
                ListItem(
                    headlineContent = { Text(app.displayName) },
                    supportingContent = { Text(app.packageName) },
                    trailingContent = {
                        Switch(checked = checked, onCheckedChange = { newValue ->
                            val newSet = if (newValue) enabledPackages + app.packageName else enabledPackages - app.packageName
                            enabledPackages = newSet
                            scope.launch { AppPreferences.setEnabledPackages(context, newSet) }
                        })
                    }
                )
            }
        }
    }
}

data class AppEntry(
    val packageName: String,
    val displayName: String,
    val isSystemApp: Boolean
)