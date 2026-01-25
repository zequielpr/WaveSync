package com.kunano.wavesynch.ui.main_screen.drawer.screens

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.view.ContextThemeWrapper
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.kunano.wavesynch.CrashReporter
import com.kunano.wavesynch.R
import java.util.Locale



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPoliciesScreen(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.privacy_policies),
    url: String = stringResource(R.string.privacy_policies_url),
    onBack: (() -> Unit),
) {
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val webView = remember {
        WebView(ContextThemeWrapper(context, R.style.Theme_Wavesynch))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Image(
                            painter = painterResource(R.drawable.arrow_back_ios_48px),
                            contentDescription = null
                        )
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
                factory = {
                    webView.apply {
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
                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?,
                            ) {
                                isLoading = true
                                canGoBack = view?.canGoBack() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                // Keep navigation inside this WebView
                                return false
                            }
                        }

                        loadUrl(url)
                    }
                },
                update = {
                    canGoBack = it.canGoBack()
                }
            )

            // Handle back inside WebView first
            BackHandler(enabled = canGoBack) {
                webView.goBack()
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }
}


@Composable
fun PrivacyPolicyDialog(
    show: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    if (!show) return

    val title = stringResource(R.string.privacy_policies)
    val body = stringResource(R.string.privacy_policies_body)
    val reviewPrivacyPolicy = stringResource(R.string.review_privacy_policy)
    val policyAgreement = stringResource(R.string.policy_agreement)
    val mediumSize = MaterialTheme.typography.bodyMedium
    val POLICY_URL = stringResource(R.string.privacy_policies_url)

    val context = LocalContext.current
    var accepted by remember { mutableStateOf(false) }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        onDismissRequest = { /* force explicit action */ },
        title = {
            Text(title)
        },
        text = {
            Column {
                Text(
                    style = mediumSize,
                    text = body
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                POLICY_URL.toUri()
                            )
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            CrashReporter.set("url", POLICY_URL)
                            CrashReporter.log("Error opening $POLICY_URL")
                            CrashReporter.record(e)

                        }catch (e: Exception){
                            CrashReporter.set("url", POLICY_URL)
                            CrashReporter.log("Error opening $POLICY_URL")
                            CrashReporter.record(e)
                        }
                    }
                ) {
                    Text(
                        reviewPrivacyPolicy,
                        style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurface
                        ),
                        checked = accepted,
                        onCheckedChange = { accepted = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        style = mediumSize,
                        text = policyAgreement
                    )
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = accepted,
                onClick = onAccept
            ) {
                Text(stringResource(R.string.accept), style = MaterialTheme.typography.bodyMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(
                    stringResource(R.string.decline),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                )
            }
        }
    )
}

