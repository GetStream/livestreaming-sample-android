package io.getstream.android.sample.livestreaming.common

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
fun CallActionButton(
    text: String,
    isEnabled: Boolean = true,
    onClick: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    Button(
        modifier = Modifier.widthIn(min = 150.dp),
        colors = ButtonDefaults.buttonColors(
            contentColor = VideoTheme.colors.brandPrimary,
            containerColor = VideoTheme.colors.brandPrimary,
        ),
        enabled = !isLoading && isEnabled,
        onClick = {
            scope.launch {
                isLoading = true
                try {
                    withTimeout(5000) {
                        onClick()
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.d(TAG, "Operation timed out. Pressed: $text.")
                }
                isLoading = false
            }
        },
        content = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 2.dp,
                    color = Color.Black
                )
            } else {
                Text(
                    text = text,
                    color = Color.White,
                )
            }
        },
    )
}

private const val TAG = "LivestreamingSample"
