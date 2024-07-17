package io.getstream.android.sample.livestreaming.host

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.getstream.log.Priority
import io.getstream.video.android.compose.permission.LaunchCallPermissions
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.video.VideoRenderer
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.RealtimeConnection
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.model.User
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val apiKey = "7yfytyds9tas"
        val userId = "Livestreaming-Host"
        val userToken = StreamVideo.devToken(userId)
        val callId = "K8iPXZNKK5lCxx"

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

                LaunchCallPermissions(call = call) {
                    // All permissions are granted so that we can join the call.
                    val result = call.join(create = true)
                    result.onError {
                        Toast.makeText(applicationContext, "uh oh $it", Toast.LENGTH_SHORT).show()
                    }
                }

                val connection by call.state.connection.collectAsState()
                val totalParticipants by call.state.totalParticipants.collectAsState()
                val backstage by call.state.backstage.collectAsState(initial = true)
                val localParticipant by call.state.localParticipant.collectAsState()
                val video = localParticipant?.video?.collectAsState()?.value
                val duration by call.state.duration.collectAsState()
                val scope = rememberCoroutineScope()

                // Define the UI
                Box(Modifier.background(color = VideoTheme.colors.baseSheetPrimary)) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentColor = Color.White,
                        containerColor = VideoTheme.colors.baseSheetPrimary,
                        topBar = {
                            if (connection == RealtimeConnection.Connected) {
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
                            } else {
                                Text(
                                    text = "Loading...",
                                    color = VideoTheme.colors.basePrimary,
                                )
                            }
                        },
                        bottomBar = {
                            Button(
                                colors = ButtonDefaults.buttonColors(
                                    contentColor = VideoTheme.colors.brandPrimary,
                                    containerColor = VideoTheme.colors.brandPrimary,
                                ),
                                onClick = {
                                    scope.launch {
                                        if (backstage) call.goLive() else call.stopLive()
                                    }
                                },
                            ) {
                                Text(
                                    text = if (backstage) "Go Live" else "Stop Broadcast",
                                    color = Color.White,
                                )
                            }
                        },
                    ) {
                        VideoRenderer(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(it)
                                .clip(RoundedCornerShape(6.dp)),
                            call = call,
                            video = video,
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
                }
            }
        }
    }
}