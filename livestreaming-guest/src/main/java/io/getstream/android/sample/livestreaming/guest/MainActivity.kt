package io.getstream.android.sample.livestreaming.guest

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.livestream.LivestreamPlayer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiKey = "7yfytyds9tas"
        val userId = "Livestreaming-Guest"
        val userToken = StreamVideo.devToken(userId)
        val callId = "K8iPXZNKK5lCxx"

        // Create a user.
        val user = User(
            id = userId, // any string
            name = "Tutorial", // name and image are used in the UI
            role = "guest",
        )

        // Initialize StreamVideo. For a production app we recommend adding the client to your Application class or DI module.
        val client = StreamVideoBuilder(
            context = applicationContext,
            apiKey = apiKey,
            geo = GEO.GlobalEdgeNetwork,
            user = user,
            token = userToken,
            runForegroundServiceForCalls = false,
        ).build()

        val call = client.call("livestream", callId)
        runBlocking { call.get() } // Using runBlocking for simplicity. In a real app, you should use a loading state.

        setContent {
            VideoTheme {
                val backstage by call.state.backstage.collectAsState()
                val activeCall by client.state.activeCall.collectAsState()
                val hasActiveCall by remember { derivedStateOf { activeCall != null } }
                val canJoin by remember { derivedStateOf { !backstage && !hasActiveCall } }
                val canLeave by remember { derivedStateOf { hasActiveCall } }

                Log.d(TAG, "backstage: $backstage, hasActiveCall: $hasActiveCall, canJoin: $canJoin, canLeave: $canLeave")

                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    CallControls(call, canJoin, canLeave)

                    Spacer(Modifier.height(20.dp))

                    Livestream(backstage, hasActiveCall, call)

                    Spacer(Modifier.height(20.dp))

                    State(hasActiveCall, call)
                }
            }
        }
    }

    @Composable
    private fun CallControls(
        call: Call,
        canJoin: Boolean,
        canLeave: Boolean
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CallControlsButton(
                text = when {
                    canJoin -> "Join Live Call"
                    canLeave -> "Leave Live Call"
                    else -> "Call in Backstage"
                },
                isEnabled = canJoin || canLeave,
                onClick = {
                    when {
                        canJoin -> {
                            try {
                                withTimeout(5000) {
                                    call.join().let { result ->
                                        result
                                            .onSuccess { Log.d(TAG, "Call join success") }
                                            .onError { Log.e(TAG, "Call join failed: ${it.message}") }
                                    }
                                }
                            } catch (e: TimeoutCancellationException) {
                                Log.d(TAG, "Call join timed out")
                            } catch (e: IllegalStateException) {
                                Log.e(TAG, "Call join failed: ${e.message}")
                            }
                        }
                        canLeave -> {
                            call.leave()
                        }
                        else -> Log.e(TAG, "Invalid join/leave state")
                    }
                },
            )

            Spacer(modifier = Modifier.width(20.dp))

            CallControlsButton(
                text = "Get Call",
                isEnabled = true,
                onClick = {
                    call.get().let { result ->
                        result
                            .onSuccess { Log.d(TAG, "Call get success") }
                            .onError { Log.e(TAG, "Call get failed: ${it.message}") }
                    }
                }
            )
        }
    }

    @Composable
    private fun CallControlsButton(
        text: String,
        isEnabled: Boolean,
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
                    onClick()
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

    @Composable
    private fun ColumnScope.Livestream(backstage: Boolean, hasActiveCall: Boolean, call: Call) {
        val placeholder = @Composable { message: String ->
            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = message)
            }
        }

        if (backstage) {
            placeholder("Call is in backstage mode. Press Go Live on the host app.")
        } else {
            if (hasActiveCall) {
                LivestreamPlayer(
                    modifier = Modifier.weight(0.8f),
                    call = call,
                )
            } else {
                placeholder("No active call. Join the call first.")
            }
        }
    }

    @Composable
    private fun ColumnScope.State(hasActiveCall: Boolean, call: Call) {
        val backstage by call.state.backstage.collectAsState()
        val duration by call.state.duration.collectAsState()
        val participants by call.state.participants.collectAsState()

        Column(
            modifier = Modifier
                .weight(0.2f)
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Is Live: ${!backstage}")
            Text("Active Call: $hasActiveCall")
            Text("Live for: $duration")
            Text("Participants: ${participants.joinToString { it.userId.value }.ifEmpty { "(None)" } }")
        }
    }
}

private const val TAG = "Livestreaming"