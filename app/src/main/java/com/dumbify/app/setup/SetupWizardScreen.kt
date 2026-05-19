package com.dumbify.app.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dumbify.app.data.entities.BlockMode
import com.dumbify.app.data.entities.BypassMode
import com.dumbify.app.data.entities.UserRole

@Composable
fun SetupWizardScreen(
    viewModel: SetupViewModel = hiltViewModel(),
) {
    BackHandler(enabled = viewModel.step > SetupViewModel.STEP_WELCOME) {
        viewModel.prevStep()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (viewModel.step) {
                SetupViewModel.STEP_WELCOME -> WelcomeScreen(viewModel)
                SetupViewModel.STEP_MODE -> ModeScreen(viewModel)
                SetupViewModel.STEP_PINS -> PinsScreen(viewModel)
                SetupViewModel.STEP_MESSAGE -> MessageScreen(viewModel)
                SetupViewModel.STEP_APP_PICKER -> AppPickerScreen(viewModel)
                SetupViewModel.STEP_BYPASS -> BypassScreen(viewModel)
                SetupViewModel.STEP_LAUNCHER -> LauncherScreen(viewModel)
                SetupViewModel.STEP_DONE -> DoneScreen(viewModel)
            }
        }
    }
}

// ── Screen 1: Welcome + Role ──────────────────────────────────────────────────

@Composable
private fun WelcomeScreen(vm: SetupViewModel) {
    WizardScaffold(
        title = "Welcome to Dumbify",
        step = 1,
        onNext = { vm.nextStep() },
        nextEnabled = true,
    ) {
        Text(
            text = "Let's set up your distraction-free phone in a few steps.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        Text("Who is this for?", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        RadioOption(
            label = "Myself — I'm setting this up voluntarily",
            selected = vm.role == UserRole.SELF,
            onClick = { vm.role = UserRole.SELF },
        )
        RadioOption(
            label = "Someone else — I'm configuring this for another person",
            selected = vm.role == UserRole.MANAGED,
            onClick = { vm.role = UserRole.MANAGED },
        )
    }
}

// ── Screen 2: Mode ───────────────────────────────────────────────────────────

@Composable
private fun ModeScreen(vm: SetupViewModel) {
    WizardScaffold(
        title = "Blocking Mode",
        step = 2,
        onNext = { vm.nextStep() },
        nextEnabled = true,
    ) {
        Text(
            text = "Choose how Dumbify decides which apps to block.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        RadioOption(
            label = "Allowlist — block everything except apps I approve",
            description = "Strictest. Recommended for most people.",
            selected = vm.mode == BlockMode.ALLOWLIST,
            onClick = { vm.mode = BlockMode.ALLOWLIST },
        )
        Spacer(Modifier.height(8.dp))
        RadioOption(
            label = "Denylist — allow everything except apps I block",
            description = "Lighter touch. Block only specific distracting apps.",
            selected = vm.mode == BlockMode.DENYLIST,
            onClick = { vm.mode = BlockMode.DENYLIST },
        )
    }
}

// ── Screen 3: PINs ───────────────────────────────────────────────────────────

@Composable
private fun PinsScreen(vm: SetupViewModel) {
    var showRemovalPin by remember { mutableStateOf(false) }
    val removalPinOk = vm.removalPin.length >= 4

    WizardScaffold(
        title = "Set PINs",
        step = 3,
        onNext = { vm.nextStep() },
        nextEnabled = removalPinOk,
    ) {
        Text(
            text = "Removal PIN protects against uninstalling Dumbify. " +
                "Ideally set by a trusted person you don't share it with.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = vm.removalPin,
            onValueChange = { vm.removalPin = it },
            label = { Text("Removal PIN (min 4 digits)") },
            visualTransformation = if (showRemovalPin) androidx.compose.ui.text.input.VisualTransformation.None
                else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = { showRemovalPin = !showRemovalPin }) {
            Text(if (showRemovalPin) "Hide" else "Show")
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Bypass PIN (optional) — required when a rule uses PIN-based bypass.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = vm.bypassPin,
            onValueChange = { vm.bypassPin = it },
            label = { Text("Bypass PIN (optional)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Screen 4: Custom Message ─────────────────────────────────────────────────

@Composable
private fun MessageScreen(vm: SetupViewModel) {
    WizardScaffold(
        title = "Motivational Message",
        step = 4,
        onNext = { vm.nextStep() },
        nextEnabled = true,
    ) {
        Text(
            text = "This message is shown on the block screen when you try to open a blocked app.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = vm.customMessage,
            onValueChange = { vm.customMessage = it },
            label = { Text("Message (leave blank for default)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
    }
}

// ── Screen 5: App Picker ─────────────────────────────────────────────────────

@Composable
private fun AppPickerScreen(vm: SetupViewModel) {
    val actionLabel = if (vm.mode == BlockMode.ALLOWLIST) "Select apps to keep" else "Select apps to block"

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            WizardStepIndicator(step = 5)
            Spacer(Modifier.height(4.dp))
            Text(actionLabel, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (vm.mode == BlockMode.ALLOWLIST)
                    "Checked apps stay accessible. Pre-selected essentials cannot be removed."
                else
                    "Checked apps will be blocked. Pre-selected essentials cannot be blocked.",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (vm.appsLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(vm.installedApps, key = { it.packageName }) { app ->
                    AppPickerRow(
                        app = app,
                        checked = app.packageName in vm.selectedPkgs,
                        onToggle = { vm.toggleApp(app.packageName) },
                    )
                    HorizontalDivider()
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(onClick = { vm.prevStep() }, modifier = Modifier.padding(end = 8.dp)) {
                Text("Back")
            }
            Button(onClick = { vm.nextStep() }) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun AppPickerRow(
    app: AppInfo,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = checked,
                role = Role.Checkbox,
                onClick = onToggle,
                enabled = !app.isAlwaysAllowed,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = !app.isAlwaysAllowed,
        )
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyMedium)
            if (app.isAlwaysAllowed) {
                Text("Always allowed", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ── Screen 6: Bypass Defaults ────────────────────────────────────────────────

@Composable
private fun BypassScreen(vm: SetupViewModel) {
    val blockedApps = vm.installedApps.filter { app ->
        when (vm.mode) {
            com.dumbify.app.data.entities.BlockMode.ALLOWLIST -> app.packageName !in vm.selectedPkgs
            com.dumbify.app.data.entities.BlockMode.DENYLIST -> app.packageName in vm.selectedPkgs
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            WizardStepIndicator(step = 6)
            Spacer(Modifier.height(4.dp))
            Text("Bypass Defaults", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("How should users request access to blocked apps?", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))

            BypassModeSelector(
                selected = vm.defaultBypassMode,
                onSelect = { vm.defaultBypassMode = it },
            )

            if (vm.defaultBypassMode != BypassMode.PIN) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Default delay: ${vm.defaultDelaySeconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = vm.defaultDelaySeconds.toFloat(),
                    onValueChange = { vm.defaultDelaySeconds = it.toInt() },
                    valueRange = 5f..300f,
                    steps = 58,
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { vm.applyDefaultBypassToAll() }, modifier = Modifier.fillMaxWidth()) {
                Text("Apply defaults to all apps")
            }
        }

        if (blockedApps.isNotEmpty()) {
            Text(
                "Per-app overrides",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(blockedApps, key = { it.packageName }) { app ->
                    val override = vm.perAppBypassOverrides[app.packageName] ?: vm.defaultBypassMode
                    BypassOverrideRow(
                        app = app,
                        currentMode = override,
                        onCycle = {
                            val next = when (override) {
                                BypassMode.DELAY -> BypassMode.PIN
                                BypassMode.PIN -> BypassMode.DELAY_AND_PIN
                                BypassMode.DELAY_AND_PIN -> BypassMode.DELAY
                            }
                            vm.setPerAppBypass(app.packageName, next)
                        },
                    )
                    HorizontalDivider()
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(onClick = { vm.prevStep() }, modifier = Modifier.padding(end = 8.dp)) {
                Text("Back")
            }
            Button(onClick = { vm.nextStep() }) {
                Text("Next")
            }
        }
    }
}

@Composable
private fun BypassModeSelector(selected: BypassMode, onSelect: (BypassMode) -> Unit) {
    Column {
        BypassMode.entries.forEach { mode ->
            RadioOption(
                label = when (mode) {
                    BypassMode.DELAY -> "Delay — wait N seconds before access"
                    BypassMode.PIN -> "PIN — enter bypass PIN"
                    BypassMode.DELAY_AND_PIN -> "Delay + PIN — wait, then enter PIN"
                },
                selected = selected == mode,
                onClick = { onSelect(mode) },
            )
        }
    }
}

@Composable
private fun BypassOverrideRow(
    app: AppInfo,
    currentMode: BypassMode,
    onCycle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onCycle) {
            Text(
                when (currentMode) {
                    BypassMode.DELAY -> "Delay"
                    BypassMode.PIN -> "PIN"
                    BypassMode.DELAY_AND_PIN -> "Delay+PIN"
                }
            )
        }
    }
}

// ── Screen 7: Launcher ───────────────────────────────────────────────────────

@Composable
private fun LauncherScreen(vm: SetupViewModel) {
    WizardScaffold(
        title = "Minimal Launcher",
        step = 7,
        onNext = { vm.nextStep() },
        nextEnabled = true,
    ) {
        Text(
            text = "Dumbify can replace your home screen with a minimal launcher that only shows allowed apps.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Replace home screen launcher", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = vm.launcherEnabled,
                onCheckedChange = { vm.launcherEnabled = it },
            )
        }
    }
}

// ── Screen 8: Done ───────────────────────────────────────────────────────────

@Composable
private fun DoneScreen(vm: SetupViewModel) {
    WizardScaffold(
        title = "Ready to Go",
        step = 8,
        onNext = { vm.finishWizard() },
        nextLabel = if (vm.saving) "Applying…" else "Apply & Finish",
        nextEnabled = !vm.saving,
    ) {
        Text(
            text = "Dumbify will apply your settings and start protecting your phone.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(24.dp))
        SummaryRow("Mode", vm.mode.name.lowercase().replaceFirstChar { it.uppercase() })
        SummaryRow("Role", vm.role.name.lowercase().replaceFirstChar { it.uppercase() })
        SummaryRow("Allowed apps", "${vm.selectedPkgs.size}")
        SummaryRow("Default bypass", vm.defaultBypassMode.name.replace('_', ' ').lowercase()
            .replaceFirstChar { it.uppercase() })
        SummaryRow("Launcher", if (vm.launcherEnabled) "Enabled" else "Disabled")
        if (vm.saving) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun WizardScaffold(
    title: String,
    step: Int,
    onNext: () -> Unit,
    nextEnabled: Boolean,
    nextLabel: String = "Next",
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        WizardStepIndicator(step = step)
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            content()
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            if (step > 1) {
                OutlinedButton(
                    onClick = { /* back is handled by BackHandler */ },
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Text("Back")
                }
            }
            Button(onClick = onNext, enabled = nextEnabled) {
                Text(nextLabel)
            }
        }
    }
}

@Composable
private fun WizardStepIndicator(step: Int) {
    Text(
        text = "Step $step of 8",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
