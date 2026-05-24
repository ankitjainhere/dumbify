package com.dumbify.app.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

@Composable
fun PinDialog(
    state: PinPromptState,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    val shakeOffset = remember { Animatable(0f) }

    // Shake on wrong attempt (attemptsRemaining decreases below MAX)
    LaunchedEffect(state.attemptsRemaining) {
        if (state.attemptsRemaining < 3) {
            for (dx in listOf(10f, -8f, 6f, -4f, 2f, 0f)) {
                shakeOffset.animateTo(dx, tween(40))
            }
        }
    }

    val locked = state.lockedForMinutes > 0
    val showError = state.attemptsRemaining < 3 && !locked
    val errorText = when {
        locked    -> "Too many attempts. Try again in ${state.lockedForMinutes} min."
        showError -> "Incorrect PIN. ${state.attemptsRemaining} attempt${if (state.attemptsRemaining == 1) "" else "s"} remaining."
        else      -> null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape    = CircleShape,
                    color    = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector    = Icons.Filled.Lock,
                            contentDescription = null,
                            tint           = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(state.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    state.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value           = pin,
                    onValueChange   = { v -> if (!locked) pin = v.filter { it.isDigit() }.take(6) },
                    modifier        = Modifier.fillMaxWidth(),
                    enabled         = !locked,
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle       = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily    = FontFamily.Monospace,
                        fontSize      = 22.sp,
                        letterSpacing = 0.4.sp,
                        textAlign     = TextAlign.Center,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine      = true,
                    isError         = showError || locked,
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape           = RoundedCornerShape(8.dp),
                )

                if (errorText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick  = { onConfirm(pin); pin = "" },
                        enabled  = pin.isNotBlank() && !locked,
                    ) { Text("Confirm") }
                }
            }
        }
    }
}
