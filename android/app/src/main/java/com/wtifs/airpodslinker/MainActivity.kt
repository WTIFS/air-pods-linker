package com.wtifs.airpodslinker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

// ── Activity ────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    private var permissionGranted by mutableStateOf(false)
    private var resumeCount by mutableIntStateOf(0)

    private fun hasPermission() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionGranted = hasPermission()
        setContent {
            if (permissionGranted) {
                AirPodsLinkerApp(resumeCount)
            } else {
                MaterialTheme {
                    Column(
                        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Allow Nearby devices access to find and control your paired AirPods.")
                        Button(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            }
                        }) { Text("Allow Bluetooth access") }
                        TextButton(onClick = {
                            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.parse("package:$packageName")))
                        }) { Text("App Settings") }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionGranted = hasPermission()
        resumeCount++
    }
}

// ── Composable UI ───────────────────────────────────────────────────────

@Composable
private fun AirPodsLinkerApp(resumeCount: Int) {
    val context = LocalContext.current
    val manager = remember { A2dpManager(context.applicationContext) }
    DisposableEffect(manager) { onDispose { manager.close() } }
    LaunchedEffect(resumeCount) { manager.refresh() }
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF007AFF),
            surface = Color(0xFFF2F2F7),
            background = Color.White,
            onBackground = Color(0xFF1C1C1E),
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(24.dp))

                // ── Icon ──
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            if (manager.isConnected) Color(0xFFE8F5E9)
                            else Color(0xFFF5F5F5)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (!manager.bluetoothOn) Icons.Outlined.BluetoothDisabled
                        else if (manager.isConnected) Icons.Outlined.BluetoothConnected
                        else Icons.Outlined.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = when {
                            !manager.bluetoothOn -> Color(0xFFC7C7CC)
                            manager.isConnected -> Color(0xFF34C759)
                            else -> Color(0xFF007AFF)
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ── Device name ──
                Text(
                    text = manager.deviceName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E)
                )

                Spacer(Modifier.height(6.dp))

                // ── Status ──
                Text(
                    text = when {
                        !manager.bluetoothOn -> "Bluetooth is off"
                        manager.isBusy -> "Updating connection…"
                        manager.isConnected -> "Connected"
                        else -> "Disconnected"
                    },
                    fontSize = 15.sp,
                    color = when {
                        !manager.bluetoothOn -> Color(0xFF8E8E93)
                        manager.isConnected -> Color(0xFF34C759)
                        else -> Color(0xFF8E8E93)
                    }
                )

                Spacer(Modifier.height(48.dp))

                // ── Toggle button ──
                Button(
                    onClick = { manager.toggleConnection() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (manager.isConnected)
                            Color(0xFFFF3B30) else Color(0xFF007AFF)
                    ),
                    enabled = manager.canToggle
                ) {
                    Icon(
                        imageVector = if (manager.isConnected)
                            Icons.Outlined.BluetoothDisabled
                        else Icons.Outlined.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (manager.isBusy) "Working…" else if (manager.isConnected) "Disconnect" else "Connect",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(12.dp))

                manager.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = {
                    context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                }) { Text("Bluetooth Settings") }

                // ── Refresh button ──
                TextButton(onClick = {
                    manager.refresh()
                }) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF8E8E93)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Refresh", color = Color(0xFF8E8E93), fontSize = 13.sp)
                }

                Spacer(Modifier.height(24.dp))

                // ── Footer ──
                Text(
                    text = "AirPodsLinker",
                    fontSize = 12.sp,
                    color = Color(0xFFC7C7CC),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        }
    }
}
