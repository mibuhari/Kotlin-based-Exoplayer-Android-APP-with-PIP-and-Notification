package com.ExoPlayerPIPexample

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util


class TvActivity :AppCompatActivity() {
    companion object {
        @JvmField
        val ARG_VIDEO_POSITION = "VideoActivity.POSITION"
    }

    var isInPipMode:Boolean = false
    lateinit var player : SimpleExoPlayer
    private var videoPosition:Long = 0L
    var isPIPModeeEnabled:Boolean = true //Has the user disabled PIP mode in AppOpps?
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    lateinit var playerView: PlayerView

    var title = arrayOf("", "","")
    var link = arrayOf("", "", "")
    var keys = arrayOf("", "", "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        Log.e("TvActivity","onCreate11")
        val intent = intent
        title[0] = intent.getStringExtra("firstName")
        title[1] = intent.getStringExtra("secondName")
        link[0] = intent.getStringExtra("firstLink")
        link[1] = intent.getStringExtra("secondLink")
        keys[0] = intent.getStringExtra("firstKey")
        keys[1] = intent.getStringExtra("secondKey")
        playerView = findViewById(R.id.playerView)

    }

    private fun initializePlayer() {
        player = SimpleExoPlayer.Builder(this).build()
        playerView.player = player
        val mediaItem: MediaItem = MediaItem.fromUri(link[0])
        player.setMediaItem(mediaItem)

        player.setPlayWhenReady(playWhenReady)
        player.seekTo(currentWindow, playbackPosition)
        player.prepare()
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
        player.playWhenReady = true

        //Use Media Session Connector from the EXT library to enable MediaSession Controls in PIP.
        val mediaSession = MediaSessionCompat(this, packageName)
        val mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(player)
        mediaSession.isActive = true
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        playerView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    override fun onPause() {
        videoPosition = player.currentPosition
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e("TvActivity","onResume")

        hideSystemUi()
        initializePlayer()
        if(videoPosition > 0L && !isInPipMode){
            player.seekTo(videoPosition)
        }
        //Makes sure that the media controls pop up on resuming and when going between PIP and non-PIP states.
        playerView.useController = true
    }

    override fun onStop() {
        super.onStop()
        playerView.player = null
        player.release()

        //PIPmode activity.finish() does not remove the activity from the recents stack.
        //Only finishAndRemoveTask does this.
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            finishAndRemoveTask()
        }
    }

    private fun releasePlayer() {
//        if (player != null) {
            playWhenReady = player.playWhenReady
            playbackPosition = player.currentPosition
            currentWindow = player.currentWindowIndex
            player.release()
//        }
    }

//    override fun onSaveInstanceState(outState: Bundle?) {
//        super.onSaveInstanceState(outState.apply {
//            this?.putLong(ARG_VIDEO_POSITION, player.currentPosition)
//        })
//    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        videoPosition = savedInstanceState!!.getLong(ARG_VIDEO_POSITION)
    }

    override fun onBackPressed(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                && isPIPModeeEnabled) {
            enterPIPMode()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        if(newConfig !=null){
            videoPosition = player.currentPosition
            isInPipMode = !isInPictureInPictureMode
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    //Called when the user touches the Home or Recents button to leave the app.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPIPMode()
    }

    @Suppress("DEPRECATION")
    fun enterPIPMode(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            videoPosition = player.currentPosition
            playerView.useController = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                this.enterPictureInPictureMode(params.build())
            } else {
                this.enterPictureInPictureMode()
            }
            /* We need to check this because the system permission check is publically hidden for integers for non-manufacturer-built apps
               https://github.com/aosp-mirror/platform_frameworks_base/blob/studio-3.1.2/core/java/android/app/AppOpsManager.java#L1640

               ********* If we didn't have that problem *********
                val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                if(appOpsManager.checkOpNoThrow(AppOpManager.OP_PICTURE_IN_PICTURE, packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA).uid, packageName) == AppOpsManager.MODE_ALLOWED)

                30MS window in even a restricted memory device (756mb+) is more than enough time to check, but also not have the system complain about holding an action hostage.
             */
            Handler().postDelayed({checkPIPPermission()}, 30)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun checkPIPPermission(){
        isPIPModeeEnabled = isInPictureInPictureMode
        if(!isInPictureInPictureMode){
            onBackPressed()
        }
    }
}