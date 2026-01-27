package com.sukitier.ui.dashboard

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Dashboard Activity for SukiTier AI Neural Kernel Management
 * Hosts WebView with dashboard UI and bridges to backend services
 */
class DashboardActivity : ComponentActivity() {
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var webViewService: DashboardWebViewService? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SukiTierDashboardTheme {
                DashboardScreen(
                    onWebViewCreated = { webView ->
                        webViewService = DashboardWebViewService(
                            context = this@DashboardActivity,
                            webView = webView,
                            scope = scope
                        )
                    }
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        webViewService = null
    }
}

@Composable
fun DashboardScreen(
    onWebViewCreated: (WebView) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    onWebViewCreated(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SukiTierDashboardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme()
    ) {
        content()
    }
}
