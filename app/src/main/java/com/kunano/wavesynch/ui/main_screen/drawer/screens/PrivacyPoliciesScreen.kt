package com.kunano.wavesynch.ui.main_screen.drawer.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import com.kunano.wavesynch.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPoliciesScreen(
    modifier: Modifier = Modifier,
    title: String = "Privacy & Policies",
    url: String = "https://sites.google.com/view/wavesync",
    onBack: (() -> Unit)
) {
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Image(painter = painterResource(R.drawable.arrow_back_ios_48px), contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        @SuppressLint("SetJavaScriptEnabled")
                        settings.apply {
                            javaScriptEnabled = true // Google Sites often needs this
                            domStorageEnabled = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            builtInZoomControls = false
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                canGoBack = view?.canGoBack() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                // Keep navigation inside this WebView
                                return false
                            }
                        }

                        loadUrl(url)
                    }
                },
                update = { webView ->
                    canGoBack = webView.canGoBack()
                }
            )

            // Handle back inside WebView first
            BackHandler(enabled = canGoBack) {
                // go back in web history
                // (we need a reference; easiest is to use WebView state holder if you want more control)
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(androidx.compose.ui.Alignment.TopCenter)
                )
            }
        }
    }
}
