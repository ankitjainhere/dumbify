package com.dumbify.app.block

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbify.app.policy.BypassController
import com.dumbify.app.policy.BypassState
import com.dumbify.app.policy.RefuseReason
import com.dumbify.app.policy.RuleStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class BlockScreenActivity : ComponentActivity() {

    @Inject lateinit var bypassController: BypassController
    @Inject lateinit var ruleStore: RuleStore

    private var pkg by mutableStateOf("")

    companion object {
        const val EXTRA_PKG = "pkg"

        fun intent(context: Context, pkg: String): Intent =
            Intent(context, BlockScreenActivity::class.java)
                .putExtra(EXTRA_PKG, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pkg = intent.getStringExtra(EXTRA_PKG)?.takeIf { it.isNotBlank() } ?: run { finish(); return }
        bypassController.cancelRequest()

        setContent {
            val state by bypassController.state.collectAsStateWithLifecycle()
            var customMessage by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                customMessage = ruleStore.getConfig()?.customMessage ?: ""
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BlockScreenContent(
                        pkg = pkg,
                        state = state,
                        customMessage = customMessage,
                        bypassController = bypassController,
                        onGoHome = { goHome() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_PKG)?.takeIf { it.isNotBlank() }?.let { pkg = it }
        bypassController.cancelRequest()
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
        finish()
    }
}

@Composable
private fun BlockScreenContent(
    pkg: String,
    state: BypassState,
    customMessage: String,
    bypassController: BypassController,
    onGoHome: () -> Unit,
) {
    when (state) {
        is BypassState.Idle -> IdleScreen(
            pkg = pkg,
            customMessage = customMessage,
            bypassController = bypassController,
            onGoHome = onGoHome,
        )
        is BypassState.CountingDown -> CountingDownScreen(
            state = state,
            bypassController = bypassController,
        )
        is BypassState.AwaitingPin -> AwaitingPinScreen(
            bypassController = bypassController,
        )
        is BypassState.Granted -> GrantedScreen(onGoHome = onGoHome)
        is BypassState.PinError -> PinErrorScreen(
            state = state,
            bypassController = bypassController,
        )
        is BypassState.Refused -> RefusedScreen(
            state = state,
            onGoHome = onGoHome,
        )
    }
}

@Composable
private fun IdleScreen(
    pkg: String,
    customMessage: String,
    bypassController: BypassController,
    onGoHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = pkg,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (customMessage.isNotEmpty()) customMessage else "App blocked.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { bypassController.requestUnblock(pkg, 5) }) {
                Text("5 min")
            }
            Button(onClick = { bypassController.requestUnblock(pkg, 15) }) {
                Text("15 min")
            }
            Button(onClick = { bypassController.requestUnblock(pkg, 30) }) {
                Text("30 min")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onGoHome) {
            Text("Go back")
        }
    }
}

@Composable
private fun CountingDownScreen(
    state: BypassState.CountingDown,
    bypassController: BypassController,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${state.secondsRemaining}s",
            style = MaterialTheme.typography.displayLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please wait before accessing this app",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { bypassController.cancelRequest() }) {
            Text("Cancel")
        }
    }
}

@Composable
private fun AwaitingPinScreen(
    bypassController: BypassController,
) {
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Enter bypass PIN",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            label = { Text("PIN") },
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                bypassController.submitPin(pin)
                pin = ""
            }) {
                Text("Submit")
            }
            Button(onClick = { bypassController.cancelRequest() }) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun GrantedScreen(onGoHome: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500L)
        onGoHome()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Access granted",
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun PinErrorScreen(
    state: BypassState.PinError,
    bypassController: BypassController,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (state.isCooldown) "Too many attempts. Wait 5 minutes." else "Wrong PIN. Try again.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { bypassController.cancelRequest() }) {
            Text("OK")
        }
    }
}

@Composable
private fun RefusedScreen(
    state: BypassState.Refused,
    onGoHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = when (state.reason) {
                RefuseReason.NO_RULE -> "No bypass configured for this app."
                RefuseReason.GRANT_FAILED -> "Could not grant access. Try again."
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onGoHome) {
            Text("Go back")
        }
    }
}
