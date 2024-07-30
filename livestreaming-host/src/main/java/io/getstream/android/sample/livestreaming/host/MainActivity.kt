package io.getstream.android.sample.livestreaming.host

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.getstream.android.sample.livestreaming.common.CallActionButton
import io.getstream.log.Priority
import io.getstream.video.android.compose.permission.LaunchCallPermissions
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.controls.actions.ToggleCameraAction
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.ParticipantState
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.model.VideoTrack
import io.getstream.video.android.model.User
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiKey = "7yfytyds9tas"
        val userId = "Livestreaming-Host"
        val userToken = StreamVideo.devToken(userId)
        val callId = "K8iPXZNKK5lCxxaabbc"

        // Create a user.
        val user = User(
            id = userId, // any string
            name = userId, // name and image are used in the UI
            role = "admin",
        )


        setContent {
            // Initialize StreamVideo. For a production app we recommend adding the client to your Application class or DI module.
            val client = StreamVideoBuilder(
                context = applicationContext,
                apiKey = apiKey,
                geo = GEO.GlobalEdgeNetwork,
                user = user,
                token = userToken,
                loggingLevel = LoggingLevel(priority = Priority.VERBOSE),
            ).build()

            VideoTheme {
                // Request permissions and join a call with type as `livestream` and id is callId.
                val call = client.call("livestream", callId)
                var arePermissionsGranted by remember { mutableStateOf(false) }

                LaunchCallPermissions(call = call) {
                    // All permissions are granted
                    arePermissionsGranted = true
                    call.camera.setEnabled(true)
                }

                val connection by call.state.connection.collectAsState()
                val activeCall by client.state.activeCall.collectAsState()
                val hasActiveCall by remember { derivedStateOf { activeCall != null } }
                val totalParticipants by call.state.totalParticipants.collectAsState()
                val backstage by call.state.backstage.collectAsState(initial = true)
                val localParticipant by call.state.localParticipant.collectAsState()
                val duration by call.state.duration.collectAsState()
                val isCameraEnabled by call.camera.isEnabled.collectAsState()
                val scope = rememberCoroutineScope()

                // Local video track - emitted only after join:
                val localParticipantVideo = localParticipant?.video?.collectAsState()?.value

                // Local video track - available even before join:
                val mediaManagerVideo = ParticipantState.Video(
                    sessionId = call.sessionId,
                    track = VideoTrack(
                        streamId = call.sessionId,
                        video = call.camera.mediaManager.videoTrack,
                    ),
                    enabled = true,
                )

                // Define the UI
                Box(Modifier.background(color = VideoTheme.colors.baseSheetPrimary)) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentColor = Color.White,
                        containerColor = VideoTheme.colors.baseSheetPrimary,
                        topBar = {
                            if (connection is RealtimeConnection.Connected) {
                                Log.d("MainActivityHostDebug", "backstage: $backstage")
                                if (!backstage) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(6.dp),
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .background(
                                                    color = VideoTheme.colors.brandPrimary,
                                                    shape = RoundedCornerShape(6.dp),
                                                )
                                                .padding(horizontal = 12.dp, vertical = 4.dp),
                                            text = "Live: $totalParticipants",
                                            color = Color.White,
                                        )

                                        Text(
                                            modifier = Modifier.align(Alignment.Center),
                                            text = "Live for $duration",
                                            color = VideoTheme.colors.basePrimary,
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Backstage",
                                        color = VideoTheme.colors.basePrimary,
                                    )
                                }
                            } else if (connection is RealtimeConnection.Failed) {
                                Text(
                                    text = "Connection failed",
                                    color = VideoTheme.colors.basePrimary,
                                )
                            }
                        },
                        bottomBar = {
                            Row {
                                ToggleCameraAction(
                                    isCameraEnabled = isCameraEnabled,
                                    onCallAction = { call.camera.setEnabled(!isCameraEnabled) }
                                )

                                Spacer(modifier = Modifier.width(20.dp))

                                CallActionButton(
                                    text = if (!hasActiveCall) "Join Call" else "Already Joined",
                                    isEnabled = arePermissionsGranted && !hasActiveCall,
                                    onClick = {
                                        call.join(create = true).let { result ->
                                            result
                                                .onSuccess { Log.d(TAG, "Call join success") }
                                                .onError { Log.e(TAG, "Call join failed: ${it.message}") }
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.width(20.dp))

                                CallActionButton(
                                    text = if (backstage) "Go Live" else "Stop Live",
                                    onClick = {
                                        scope.launch {
                                            if (backstage) call.goLive() else call.stopLive()
                                        }
                                    }
                                )
                            }
                        },
                        content = {
                            if (isCameraEnabled) {
                                VideoRenderer(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(it)
                                        .clip(RoundedCornerShape(6.dp)),
                                    call = call,
                                    video = mediaManagerVideo,
                                    videoFallbackContent = {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "Will render local video here...")
                                        }
                                    },
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

private const val TAG = "LivestreamingSample"