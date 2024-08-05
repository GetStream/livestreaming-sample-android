package io.getstream.android.sample.livestreaming.guest

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.getstream.android.sample.livestreaming.common.CallActionButton
import io.getstream.log.Priority
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.livestream.LivestreamPlayer
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.model.User
import org.threeten.bp.OffsetDateTime
import kotlin.time.Duration

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiKey = "7yfytyds9tas"
        val userId = "Livestreaming-Guest"
        val userToken = StreamVideo.devToken(userId)
        val callId = "K8iPXZNKK5lC12"

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
            loggingLevel = LoggingLevel(priority = Priority.DEBUG)
        ).build()

        val call = client.call("livestream", callId)
//        runBlocking { call.get() } // Using runBlocking for simplicity. In a real app, you should use a loading state.

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

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun CallControls(
        call: Call,
        canJoin: Boolean,
        canLeave: Boolean
    ) {
        FlowRow(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CallActionButton(
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
                                call.join().let { result ->
                                    result
                                        .onSuccess { Log.d(TAG, "Call join success") }
                                        .onError { Log.e(TAG, "Call join failed: ${it.message}") }
                                }
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

            CallActionButton(
                text = "Get Call",
                onClick = {
                    call.get().let { result ->
                        result
                            .onSuccess { Log.d(TAG, "Call get success") }
                            .onError { Log.e(TAG, "Call get failed: ${it.message}") }
                    }
                }
            )

            Spacer(modifier = Modifier.width(20.dp))

            CallActionButton(
                text = "Remove Client",
                onClick = StreamVideo.Companion::removeClient
            )
        }
    }

    @Composable
    private fun ColumnScope.Livestream(backstage: Boolean, hasActiveCall: Boolean, call: Call) {
        val placeholder = @Composable { message: String ->
            Box(
                modifier = Modifier
                    .weight(1f)
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
        val participants by call.state.participants.collectAsState()
        val totalParticipants by call.state.totalParticipants.collectAsState()

        val durationInMs: Long? by call.state.durationInMs.collectAsState() // Uses session.startedAt
        val duration: Duration? by call.state.duration.collectAsState() // Based on durationInMs
        val durationInDateFormat: String? by call.state.durationInDateFormat.collectAsState() // Based on durationInMs
        val liveDurationInMs: Long? by call.state.liveDurationInMs.collectAsState()
        val liveDuration by call.state.liveDuration.collectAsState()
        val startedAt: OffsetDateTime? by call.state.startedAt.collectAsState()

        Column(
            modifier = Modifier
                .wrapContentHeight() // Not necessary
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Is Live: ${!backstage}")
            Text("Active Call: $hasActiveCall")
            Text("durationInMs: $durationInMs")
            Text("duration: $duration")
            Text("durationInDateFormat: $durationInDateFormat")
            Text("liveDurationInMs: $liveDurationInMs")
            Text("liveDuration: $liveDuration")
            Text("startedAt: $startedAt")
            Text("Participant Count: $totalParticipants")
            Text("Participants: ${participants.joinToString { it.userId.value }.ifEmpty { "(None)" } }")
        }
    }
}

private const val TAG = "Livestreaming"