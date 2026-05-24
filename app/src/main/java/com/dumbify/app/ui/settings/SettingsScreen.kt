package com.dumbify.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbify.app.data.entities.BlockMode
import com.dumbify.app.data.entities.Event
import com.dumbify.app.data.entities.EventType
import com.dumbify.app.policy.PinManager
import com.dumbify.app.ui.common.PinDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    uiState.pinPrompt?.let { prompt ->
        PinDialog(
            state     = prompt,
            onConfirm = { pin -> viewModel.submitPin(pin) },
            onDismiss = { viewModel.dismissPinDialog() },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title   = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
        ) {
            // ── Block Mode ───────────────────────────────────────────────────
            item {
                SectionHeader("Block Mode")
                RadioRow(
                    selected = uiState.config?.mode == BlockMode.ALLOWLIST,
                    title    = "Allowlist",
                    subtitle = "Only listed apps can run. Everything else is blocked.",
                    onClick  = { viewModel.changeMode(BlockMode.ALLOWLIST) },
                )
                RadioRow(
                    selected = uiState.config?.mode == BlockMode.DENYLIST,
                    title    = "Denylist",
                    subtitle = "Listed apps are blocked. Everything else runs normally.",
                    onClick  = { viewModel.changeMode(BlockMode.DENYLIST) },
                )
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp))
                    Text(
                        "Changing mode requires Removal PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Change PINs ──────────────────────────────────────────────────
            item {
                SectionHeader("Change PINs")
                SettingsListItem(
                    leadingIcon = { Icon(Icons.Filled.Shield, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)) },
                    title       = "Change Removal PIN",
                    subtitle    = "High-friction PIN for protection changes",
                    onClick     = { viewModel.startChangePin(PinManager.Scope.REMOVAL) },
                )
                SettingsListItem(
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)) },
                    title       = "Change Bypass PIN",
                    subtitle    = "Used during temporary app grants",
                    onClick     = { viewModel.startChangePin(PinManager.Scope.BYPASS) },
                )
            }

            // ── Audit Log ────────────────────────────────────────────────────
            item {
                SectionHeader("Audit Log")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleAuditLog() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.History, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${uiState.auditEvents.size} recent events",
                            style = MaterialTheme.typography.bodyLarge)
                        Text("Tap to ${if (uiState.auditExpanded) "collapse" else "expand"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
                if (uiState.auditExpanded && uiState.auditEvents.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        uiState.auditEvents.forEachIndexed { idx, ev ->
                            AuditEventRow(ev)
                            if (idx < uiState.auditEvents.lastIndex) {
                                HorizontalDivider(
                                    modifier  = Modifier.padding(horizontal = 16.dp),
                                    thickness = 1.dp,
                                    color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                )
                            }
                        }
                    }
                }
            }

            // ── Danger Zone ──────────────────────────────────────────────────
            item {
                SectionHeader("Danger Zone", tone = SectionTone.Error)
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text("Remove Dumbify",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W500),
                            color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This will remove all restrictions and uninstall the app. Requires Removal PIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.startRemoval() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor   = MaterialTheme.colorScheme.onError,
                        ),
                    ) { Text("Remove Dumbify") }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Section header ───────────────────────────────────────────────────────────

private enum class SectionTone { Normal, Error }

@Composable
private fun SectionHeader(text: String, tone: SectionTone = SectionTone.Normal) {
    Text(
        text     = text.uppercase(),
        style    = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
        color    = if (tone == SectionTone.Error) MaterialTheme.colorScheme.error
                   else MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 20.dp, bottom = 8.dp),
    )
}

// ── Radio row ────────────────────────────────────────────────────────────────

@Composable
private fun RadioRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Settings list item ───────────────────────────────────────────────────────

@Composable
private fun SettingsListItem(
    leadingIcon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        leadingIcon()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
    }
}

// ── Audit event row ──────────────────────────────────────────────────────────

@Composable
private fun AuditEventRow(event: Event) {
    val (chipColor, chipOnColor) = auditChipColors(event.type)
    val timeStr = event.timestamp.let {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
    }
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            timeStr,
            style    = MaterialTheme.typography.bodySmall.copy(
                fontFeatureSettings = "tnum",
            ),
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(38.dp),
        )
        // Event type chip
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(11.dp))
                .background(chipColor)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                event.type.name.take(8),
                style = MaterialTheme.typography.labelSmall,
                color = chipOnColor,
            )
        }
        Text(
            buildString {
                event.packageName?.let { append(it.substringAfterLast('.').take(12)) }
                event.detail?.let { if (isNotEmpty()) append(" · "); append(it) }
            },
            style   = MaterialTheme.typography.bodySmall,
            color   = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun auditChipColors(type: EventType): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    val cs = MaterialTheme.colorScheme
    return when (type) {
        EventType.UNBLOCK_GRANTED  -> com.dumbify.app.ui.theme.LocalDumbifyColors.current.amberContainer to
                com.dumbify.app.ui.theme.LocalDumbifyColors.current.onAmberContainer
        EventType.BLOCK_HIT,
        EventType.UNBLOCK_DENIED,
        EventType.PIN_FAIL,
        EventType.REMOVAL_ATTEMPT  -> cs.errorContainer to cs.onErrorContainer
        EventType.UNBLOCK_REQUEST  -> cs.surfaceContainerHigh to cs.onSurfaceVariant
        EventType.RULE_EDIT,
        EventType.MODE_CHANGE      -> cs.secondaryContainer to cs.onSecondaryContainer
    }
}
