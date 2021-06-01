package com.ExoPlayerPIPexample

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.app.UiModeManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ServiceCompat
import androidx.core.app.ServiceCompat.stopForeground
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.MediaMetadataProvider
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.Log
import org.json.JSONException


class StarterActivity : AppCompatActivity() {

    var isInPipMode:Boolean = false
    var isPIPModeeEnabled:Boolean = true //Has the user disabled PIP mode in AppOpps?
    var isReleasePlayerCalled: Boolean = false //Was release player called

    lateinit var playerView: PlayerView
    private lateinit var player : SimpleExoPlayer
    private var playWhenReady = true
    private var playbackPosition: Long = 0
    private var videoPosition:Long = 0L

    // url for youtube link information
    private val mJSONURLString = "https://raw.githubusercontent.com/mibuhari/Kotlin-based-Exoplayer-Android-APP-with-PIP-and-Notification/main/ExtraFiles/sampleMedia.json"
    private var title = arrayOf("", "Live M3u8 Video")
    private var link = arrayOf("", "")
    private var keys = arrayOf("", "")
    private var deviceIsTV: Boolean = false
    private lateinit var mService: PlayerNotificationService
    private var mBound: Boolean = false
    private lateinit var jsonObjectRequest: JsonObjectRequest

    // initialize SharedPreferences var
    var sharedPref: SharedPreferences? = null

    private lateinit var mediaSession : MediaSessionCompat

    private val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val BecomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                // Pause the playback
                player.pause()
            }
        }
    }

    private val abcd: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    /**
     * Create our connection to the service to be used in our bindService call.
     */
    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mBound = false
        }

        /**
         * Called after a successful bind with our VideoService.
         */
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            //We expect the service binder to be the video services binder.
            //As such we cast.

            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as PlayerNotificationService.VideoServiceBinder
            mService = binder.getService()
            player = binder.getExoPlayerInstance()

            mBound = true
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(abcd, IntentFilter("closeActivity"))
        setContentView(R.layout.activity_video)
        playerView = findViewById(R.id.playerView)

        if (getIntent().toString().contains("act")) {
            // reading youtube link from website
            // Initialize a new RequestQueue instance
            volleyRequest()
        }
        else
        {
            playerView.showController()

            val mediaItem: MediaItem =
                MediaItem.fromUri(Uri.parse(link[0]))
            player.setMediaItem(mediaItem)
            player.playWhenReady = true

            //Use Media Session Connector from the EXT library to enable MediaSession Controls in PIP.
            createMediaSession()

            player.prepare()
            player.play()
        }

    }

    private fun checkTV() {
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
        deviceIsTV = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }


    private fun initializePlayer() {
        checkTV()

        player = SimpleExoPlayer.Builder(this).build()
        playerView.player = player

        val mediaItem: MediaItem = MediaItem.fromUri(link[0])
        player.setMediaItem(mediaItem)
        player.playWhenReady = true

        createMediaSession()

        player.prepare()

    }

    override fun onStart() {
        super.onStart()
        if (mBound)
        {
            playerView.player = player
            val mediaItem: MediaItem = MediaItem.fromUri(link[0])
            player.setMediaItem(mediaItem)
            player.playWhenReady = true
            createMediaSession()
            player.prepare()
        }
    }

    override fun onResume() {
      super.onResume()
      if (!isInPipMode) {
        stopService(Intent(this@StarterActivity, PlayerNotificationService::class.java))
      }
      else {
          playerView.setUseController(true)
      }
    }

    // detach player
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(this.abcd)
        stopService(Intent(this@StarterActivity, PlayerNotificationService::class.java))
        if(mBound){
            unbindService(connection)
            stopForeground(mService, ServiceCompat.STOP_FOREGROUND_DETACH)
        }
        mediaSession.isActive = false
        mediaSession.release()
        releasePlayer()
        finish()
    }

    override fun onStop() {
        super.onStop()

        // if not double audio
        if(!mBound)
            releasePlayer()

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE))
        {
            // do when the device does not support PIP
            val intent = Intent(this, PlayerNotificationService::class.java)
            intent.putExtra("Url", link[0])
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            } else {
                startService(intent)
            }
        }
        else if (!deviceIsTV) {
            val intent = Intent(this, PlayerNotificationService::class.java)
            intent.putExtra("Url", link[0])
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            } else {
                startService(intent)
            }
        }
        else {
            // for TVs that supports pip
            mediaSession.isActive = false
            mediaSession.release()
            finish()
        }
  }

    private fun releasePlayer() {
        isReleasePlayerCalled = true

        playWhenReady = player.playWhenReady
        playbackPosition = player.currentPosition

        // if we pass list of videos then this returns the position of current video playing with respect to passing list
        // currentWindow = player.currentWindowIndex
        player.release()
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
        playerView.setUseController(false)

        if (!isReleasePlayerCalled)
        {
            createMediaSession()
            player.prepare()
            if (newConfig != null) {
                videoPosition = player.currentPosition
                isInPipMode = !isInPictureInPictureMode
            }
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

    @SuppressLint("WrongConstant")
    private fun createMediaSession() {
        //Use Media Session Connector from the EXT library to enable MediaSession Controls in PIP.
        mediaSession = MediaSessionCompat(this, packageName)
        val mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(player)
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.image)

        mediaSessionConnector.setMediaMetadataProvider( MediaMetadataProvider {
            val builder = MediaMetadataCompat.Builder()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    "ExoPlayer PIP example")
                builder.putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    "ExoPlayer PIP example Video")
                builder.putString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                    "ExoPlayer PIP example")
                builder.putString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                    "ExoPlayer PIP example Video")
                builder.putBitmap(
                    MediaMetadataCompat.METADATA_KEY_ART,
                    bitmap)
            } else {
                builder.putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    "ExoPlayer PIP example")
                builder.putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    "ExoPlayer PIP example Video")
            }
            // set everything you need and make sure it reflects what is currently playing in player.
            // Do not set the duration though.
            builder.build()
        })


        if (deviceIsTV) {
            val mediaController: MediaControllerCompat = mediaSession.getController()
            val stateCompat: PlaybackStateCompat = mediaController.getPlaybackState()

            val stateBuilder = PlaybackStateCompat.Builder()
                .setActions(getAvailableActions()).apply {
                    setState(stateCompat.getState(), player.currentPosition.toLong(), 1.0f)
                }

            mediaSession.setPlaybackState(stateBuilder.build())
        }

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPause() {
                // called when the Stop button in the Notification bar is pressed.
                if (!isReleasePlayerCalled) player.stop()
                // this@StarterActivity.onStop()
            }
            override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {

                val ev: KeyEvent = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)

                when (ev.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        // super.onPause()
                        player.pause()
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY ->  {
                        player.prepare()
                        player.play()
                        //  this@StarterActivity.onStart()
                    }
                    KeyEvent.KEYCODE_MOVE_HOME ->  {
                        player.prepare()
                        player.play()
                        //  this@StarterActivity.onStart()
                    }

                    //  KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    //     KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD, KeyEvent.KEYCODE_MEDIA_STEP_FORWARD, KeyEvent.KEYCODE_MEDIA_NEXT -> service.jumpForward()
                    //     KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD, KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> service.jumpBackward()
                    KeyEvent.KEYCODE_MEDIA_STOP ->  {
                        //   this@StarterActivity.onStop()
                        //  this@StarterActivity.onDestroy()
                    }
                    else -> return false
                }

                return super.onMediaButtonEvent(mediaButtonIntent)
            }

            override fun onPlay() {
                registerReceiver(BecomingNoisyReceiver, intentFilter)
            }

            //    override fun onSeekTo(pos: Long) {
            //    }

            override fun onStop() {
                unregisterReceiver(BecomingNoisyReceiver)
                // releasePlayer()
                //   this@StarterActivity.onStop()
            }
        })

        player.apply {
            // AudioAttributes here from exoplayer package !!!
            val attr = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build()
            // In 2.9.X you don't need to manually handle audio focus :D
            setAudioAttributes(attr, true)
        }
        mediaSession.isActive = true

    }

    private fun getAvailableActions(): Long {

        var actions = (PlaybackState.ACTION_PLAY_PAUSE
//                or PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                //              or PlaybackState.ACTION_PLAY_FROM_SEARCH
                or PlaybackState.ACTION_PLAY
                or PlaybackState.ACTION_PAUSE
                or PlaybackState.ACTION_SEEK_TO
                or PlaybackState.ACTION_FAST_FORWARD
                or PlaybackState.ACTION_REWIND
                or PlaybackState.ACTION_STOP
                or PlaybackState.ACTION_SKIP_TO_NEXT
                )
        val mediaController: MediaControllerCompat = mediaSession.getController()
        val stateCompat: PlaybackStateCompat = mediaController.getPlaybackState()

        val mState = stateCompat.getState()
        actions = if (mState == PlaybackState.STATE_PLAYING) {
            actions or PlaybackState.ACTION_PAUSE
        } else {
            actions or PlaybackState.ACTION_PLAY
        }
        return actions
    }

    private fun volleyRequest()
    {
        var requestQueue: RequestQueue = Volley.newRequestQueue(applicationContext)

        jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, mJSONURLString, null,
            { response ->
                try {
                    // Get the JSON array
                    val array = response.getJSONArray("service")

                    // Loop through the array elements
                    for (i in 0 until array.length()) {
                        // Get current json object
                        val student = array.getJSONObject(i)
                        // Get the current student (json object) data
                        title[i] = student.getString("title")
                        if (student.isNull("link")) {
                            link[i] = ""
                        } else
                            link[i] = student.getString("link")

                        if (student.isNull("key")) {
                            keys[i] = ""
                        } else
                            keys[i] = student.getString("key")
                    }

                    if (link[0] != "") {
                        // get or create SharedPreferences
                        sharedPref = getSharedPreferences("myPref", MODE_PRIVATE);

                        // save your string in SharedPreferences
                        sharedPref!!.edit().putString("url_link", link[0]).apply();
                        initializePlayer()
                    } else {
                        val inent = Intent(applicationContext, YouTubeVideoActivity::class.java)
                        inent.putExtra("firstName", title[0])
                        inent.putExtra("firstLink", link[0])
                        inent.putExtra("firstKey", keys[0])
                        inent.putExtra("secondName", title[1])
                        inent.putExtra("secondLink", link[1])
                        inent.putExtra("secondKey", keys[1])
                        startActivity(inent)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            { error ->
                // TODO: Handle error: Repeat retrieving the json
                volleyRequest()
            }
        )
        // Add JsonObjectRequest to the RequestQueue
        requestQueue.add(jsonObjectRequest)

    }

}
