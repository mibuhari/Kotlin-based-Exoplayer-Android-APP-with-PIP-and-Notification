package com.ExoPlayerPIPexample

import android.content.Intent
import android.os.Bundle
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE
import com.google.android.youtube.player.YouTubePlayer.FULLSCREEN_FLAG_CONTROL_ORIENTATION
import com.google.android.youtube.player.YouTubePlayerView
import android.content.pm.ActivityInfo

class YouTubeVideoActivity :YouTubeBaseActivity() {
    companion object {
        @JvmField
        val ARG_VIDEO_POSITION = "VideoActivity.POSITION"
    }

    lateinit var youTubePlayerView: YouTubePlayerView

    var title = arrayOf("", "", "")
    var link = arrayOf("", "", "")
    var keys = arrayOf("", "", "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_play)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        val intent = intent
        title[0] = intent.getStringExtra("firstName")
        title[1] = intent.getStringExtra("secondName")
        link[0] = intent.getStringExtra("firstLink")
        link[1] = intent.getStringExtra("secondLink")
        keys[0] = intent.getStringExtra("firstKey")
        keys[1] = intent.getStringExtra("secondKey")


        youTubePlayerView = findViewById(R.id.player) as YouTubePlayerView

        youTubePlayerView.initialize(
            keys[1],
            object : YouTubePlayer.OnInitializedListener {
                override fun onInitializationSuccess(
                    provider: YouTubePlayer.Provider,
                    youTubePlayer: YouTubePlayer, b: Boolean
                ) {
                    youTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.MINIMAL)
                    youTubePlayer.fullscreenControlFlags = FULLSCREEN_FLAG_CONTROL_ORIENTATION
                    youTubePlayer.addFullscreenControlFlag(
                        FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE
                    )
                    // do any work here to cue video, play video, etc.
                    youTubePlayer.loadVideo(link[1])
                    if (youTubePlayer.isPlaying) {
                        val intent = Intent(applicationContext, TvActivity::class.java)
                        startActivity(intent)
                    }
                }

                override fun onInitializationFailure(
                    provider: YouTubePlayer.Provider,
                    youTubeInitializationResult: YouTubeInitializationResult
                ) {
                }
            })
    }

    override fun onStart() {
        super.onStart()
    }

}