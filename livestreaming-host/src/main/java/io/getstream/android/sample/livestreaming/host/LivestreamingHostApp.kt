package io.getstream.android.sample.livestreaming.host

import android.app.Application
import io.getstream.log.Priority
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.model.User

class LivestreamingHostApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initVideoClient()
    }

    private fun initVideoClient() {
        if (StreamVideo.instanceOrNull() == null) {
            val apiKey = "7yfytyds9tas"
            val userId = "Livestreaming-Host"
            val userToken = StreamVideo.devToken(userId)

            // Create a user.
            val user = User(
                id = userId, // any string
                name = userId, // name and image are used in the UI
                role = "admin",
            )

            StreamVideoBuilder(
                context = applicationContext,
                apiKey = apiKey,
                geo = GEO.GlobalEdgeNetwork,
                user = user,
                token = userToken,
                loggingLevel = LoggingLevel(priority = Priority.VERBOSE),
                runForegroundServiceForCalls = true,
            ).build()
        }
    }
}