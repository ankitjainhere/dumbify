package com.dumbify.app.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dumbify.app.data.entities.BlockMode
import com.dumbify.app.data.entities.BypassMode
import com.dumbify.app.ui.common.PinDialog
import com.dumbify.app.ui.theme.LocalDumbifyColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    uiState.pinPrompt?.let { prompt ->
        PinDialog(
            state     = prompt,
            onConfirm = { pin -> viewModel.submitPin(pin) },
            onDismiss = { viewModel.dismissPinDialog() },
        )
    }

    uiState.editingRule?.let { rule ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissSheet() },
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor   = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            EditRuleSheetContent(
                rule      = rule,
                onDismiss = { viewModel.dismissSheet() },
                onSave    = { new -> viewModel.saveEdit(rule, new) },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dumbify",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.W500),
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { paddingValues ->
        if (uiState.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                StatusBannerCard(
                    isDeviceOwner = uiState.isDeviceOwner,
                    mode          = uiState.config?.mode,
                    ruleCount     = uiState.rules.size,
                    onTap         = onNavigateToSettings,
                    modifier      = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 8.dp),
                )
            }
            itemsIndexed(uiState.rules, key = { _, r -> r.packageName }) { index, rule ->
                RuleRow(
                    rule     = rule,
                    onTap    = { viewModel.openEditSheet(rule) },
                    onToggle = { viewModel.onDirectToggle(rule) },
                )
                if (index < uiState.rules.lastIndex) {
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    )
                }
            }
        }
    }
}

// ── Dashboard card status banner ─────────────────────────────────────────────

@Composable
private fun StatusBannerCard(
    isDeviceOwner: Boolean,
    mode: BlockMode?,
    ruleCount: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dumbifyColors = LocalDumbifyColors.current
    val iconBg   = if (isDeviceOwner) MaterialTheme.colorScheme.primaryContainer
                   else MaterialTheme.colorScheme.errorContainer
    val iconTint = if (isDeviceOwner) MaterialTheme.colorScheme.onPrimaryContainer
                   else MaterialTheme.colorScheme.onErrorContainer
    val headline = if (isDeviceOwner) "Device Owner active" else "Device Owner missing"
    val modeLabel = when (mode) {
        BlockMode.ALLOWLIST -> "Allowlist mode"
        BlockMode.DENYLIST  -> "Denylist mode"
        null                -> "—"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = if (isDeviceOwner) 0.25f else 0f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseOut), RepeatMode.Restart),
        label         = "pulseAlpha",
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = if (isDeviceOwner) 1.8f else 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseOut), RepeatMode.Restart),
        label         = "pulseScale",
    )
    val dotColor = dumbifyColors.successDot

    Surface(
        modifier       = modifier.fillMaxWidth().clickable(onClick = onTap),
        shape          = RoundedCornerShape(16.dp),
        color          = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier              = Modifier.padding(14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .drawBehind {
                        if (isDeviceOwner) {
                            drawCircle(
                                color  = dotColor.copy(alpha = pulseAlpha),
                                radius = (size.minDimension / 2f) * pulseScale,
                                style  = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector      = if (isDeviceOwner) Icons.Filled.VerifiedUser else Icons.Filled.Warning,
                    contentDescription = null,
                    tint             = iconTint,
                    modifier         = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    headline,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W500),
                )
                Text(
                    "$modeLabel · $ruleCount app${if (ruleCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector      = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint             = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier         = Modifier.size(20.dp),
            )
        }
    }
}

// ── Rule row — trailing toggle variant ───────────────────────────────────────

@Composable
private fun RuleRow(
    rule: RuleUiItem,
    onTap: () -> Unit,
    onToggle: () -> Unit,
) {
    val dumbifyColors = LocalDumbifyColors.current
    val muted = !rule.isAllowed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val painter = remember(rule.appIcon) {
            rule.appIcon?.let { drawable ->
                val size = 128
                val bmp  = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(Canvas(bmp))
                BitmapPainter(bmp.asImageBitmap())
            }
        } ?: rememberVectorPainter(Icons.Filled.Shield)

        Image(
            painter          = painter,
            contentDescription = rule.displayName,
            modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)),
            alpha            = if (muted) 0.55f else 1f,
            colorFilter      = if (muted) ColorFilter.colorMatrix(
                ColorMatrix().apply { setToSaturation(0.5f) }
            ) else null,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = rule.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W500),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (muted) 0.60f else 1f),
                maxLines = 1,
            )
            Text(
                text  = rule.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (muted) 0.65f else 1f),
                maxLines = 1,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Switch(checked = rule.isAllowed, onCheckedChange = { onToggle() })
            if (rule.grantedUntil != null) {
                val time = remember(rule.grantedUntil) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(rule.grantedUntil))
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(dumbifyColors.amberContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint     = dumbifyColors.onAmberContainer,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        "Granted until $time",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = dumbifyColors.onAmberContainer,
                    )
                }
            }
        }
    }
}

// ── Edit rule sheet content ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRuleSheetContent(
    rule: RuleUiItem,
    onDismiss: () -> Unit,
    onSave: (RuleUiItem) -> Unit,
) {
    var editAllowed by remember { mutableStateOf(rule.isAllowed) }
    var editBypass  by remember { mutableStateOf(rule.bypassMode) }
    var editDelay   by remember { mutableFloatStateOf(rule.delaySeconds.toFloat()) }

    val reducesProtection = !rule.isAllowed && editAllowed
    val hasDelay          = editBypass != BypassMode.PIN

    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        // Title row
        Row(
            modifier              = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val painter = remember(rule.appIcon) {
                rule.appIcon?.let { drawable ->
                    val size = 144
                    val bmp  = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    drawable.setBounds(0, 0, size, size)
                    drawable.draw(Canvas(bmp))
                    BitmapPainter(bmp.asImageBitmap())
                }
            } ?: rememberVectorPainter(Icons.Filled.Shield)
            Image(
                painter          = painter,
                contentDescription = null,
                modifier         = Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)),
            )
            Column {
                Text(rule.displayName, style = MaterialTheme.typography.titleLarge)
                Text(
                    rule.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Allow toggle card
        SheetCard(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (reducesProtection) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                tint     = LocalDumbifyColors.current.amber,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Text(
                            "Allow this app",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W500),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (editAllowed) "App can open immediately (subject to bypass)"
                        else "App will be hidden and blocked from launch",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = editAllowed, onCheckedChange = { editAllowed = it })
            }
        }

        // Bypass mode card
        SheetCard(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
            Text(
                "BYPASS MODE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            val bypassOptions = listOf(
                BypassMode.DELAY         to "Delay",
                BypassMode.PIN           to "PIN",
                BypassMode.DELAY_AND_PIN to "Delay + PIN",
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                bypassOptions.forEachIndexed { idx, (mode, label) ->
                    SegmentedButton(
                        selected = editBypass == mode,
                        onClick  = { editBypass = mode },
                        shape    = SegmentedButtonDefaults.itemShape(idx, bypassOptions.size),
                        label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                when (editBypass) {
                    BypassMode.DELAY         -> "User waits the delay period, then app opens."
                    BypassMode.PIN           -> "User enters Bypass PIN to open."
                    BypassMode.DELAY_AND_PIN -> "User waits the delay, then enters Bypass PIN."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Delay slider card
        SheetCard(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            alpha = if (hasDelay) 1f else 0.4f,
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Bottom,
            ) {
                Text(
                    "DELAY DURATION",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.4.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(formatDelay(editDelay.toInt()), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            Slider(
                value         = editDelay,
                onValueChange = { if (hasDelay) editDelay = it },
                valueRange    = 0f..300f,
                steps         = 59,
                enabled       = hasDelay,
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("0s", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("5m", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Protection reduction warning
        if (reducesProtection) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LocalDumbifyColors.current.amber.copy(alpha = 0.10f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.Top,
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint     = LocalDumbifyColors.current.amber,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Changes that reduce protection require your Removal PIN.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // Actions
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Button(
                onClick = {
                    onSave(rule.copy(
                        isAllowed    = editAllowed,
                        bypassMode   = editBypass,
                        delaySeconds = editDelay.toInt(),
                    ))
                },
            ) { Text("Save") }
        }
    }
}

@Composable
private fun SheetCard(
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier       = modifier.fillMaxWidth(),
        shape          = RoundedCornerShape(16.dp),
        color          = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = alpha),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            content()
        }
    }
}

internal fun formatDelay(seconds: Int): String = when {
    seconds == 0 -> "No delay"
    seconds < 60 -> "${seconds}s"
    else         -> {
        val m = seconds / 60
        val s = seconds % 60
        if (s == 0) "${m}m" else "${m}m ${s}s"
    }
}
