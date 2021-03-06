## Kotlin-based-Exoplayer-Android-APP-with-PIP-and-Notification
Android APP that supports mobile devices as well as Android TV. App is developed using Kotlin and supports Picture-In-Picture and Notification.

### Disclaimer  
I am not claiming to be an expert in this area. I have collected information from various sources on the Internet to accomplish these requirements.

### Min SDK
Min sdk is API 21 (Lollipop)

### Table of contents
1. [Requirements for this project](#requirements)
2. [How to use the app](#how-to)
3. [Google Play concerns](#google-play)
4. [Lessons learnt from the code](#lessons-learnt)
   - [Manifest file related](#manifest-file-related)
   - [Gradle related](#gradle-related)
   - [Kotlin related](#kotlin-related)
   - [Image related](#image-related)
   - [Android TV related](#android-tv-related)
   - [Exoplayer related](#exoplayer-related)
   - [Notification related](#notification-related)
   - [Service related](#service-related)
   - [General information](#general-info)
5. [References](#references)

### Requirements for this project<a name="requirements"></a>
1. Kotlin based Exoplayer development
2. Picture-In-Picture (PIP) support
3. Notification support with playing audio 
4. Support for Android TV
5. Support for YouTube Video
6. Access JSON file to identify the necessary M3U8 or MP4 file to be played

### How to use the app<a name="how-to"></a>
1. This github repository contains ExtraFiles folder that contains the sample video files (M3U8 and MP4). M3U8 being a playlist, the necessary TS files are also provided there.
2. JSON file is provided as “sampleMedia.json”
   - Change the link towards the necessary video link in this JSON file. 
   - For YouTube video: Provide the necessary YouTube video link and the APIKEY.
3. This app runs the YouTube video only when the first link is empty.
4. For YouTube video to be played, an YouTubeAndroidPlayerApi.jar file is provided in the libs folder of the app.

### Google Play concerns<a name="google-play"></a>
Concerns raised | Deprecation Issue  | Solution
----------------- | ------------- | -------------
No Now Playing notification [card]: What happens when the HOME button is pressed? | Some of the information present in this [page](https://developer.android.com/training/tv/playback/now-playing#card) is deprecated. | mediaSession.isActive = false
Update of metadata | Content in the second column |  ``` val builder = MediaMetadataCompat.Builder()``` <br/> ```builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, "ExoPlayer PIP example") ```
No full-size app banner: xhdpi banner with size 320px X 180px | Not Applicable | This could be handled using a banner image in the app drawable folder and refer it in the manifest file as: ``` <application android:banner="@drawable/image" ...  ```
Audio plays after selecting “stops” | Not Applicable | Check if the relation between the background service and the activity. Maybe, they are not properly bind or unbind.
Crashing after launch | Not Applicable | This might be due to Volley not obtaining the right json file. Repeat the volley call if the volley request error for the first time.
version already exist | Not Applicable | Update the versionCode and versionCode in the build.grade to the version more than the already available version on PlayStore.
Reduce your app size and boost installs with the Android App Bundle | Not Applicable | Generate AAB file instead of APK from your Android Studio

### Lessons learnt from the code<a name="lessons-learnt"></a>
#### 1. Manifest file related:<a name="manifest-file-related"></a>
   - For Android TV: What are the requirements to be done in the Manifest file? 
     - Leanback and touchscreen:
       ```
       <uses-feature android:name="android.software.leanback" android:required="false" />
       <uses-feature android:name="android.hardware.touchscreen" android:required="false" />
       ```
     - At the activity level:  
	```
        android:configChanges="keyboard|keyboardHidden|orientation|screenSize|screenLayout|smallestScreenSize|uiMode|navigation"
	``` 
     - In order to support PIP: 
        ``` android:supportsPictureInPicture="true"   ``` 
     - Leanback launcher is required: <br/>
       ```
        <intent-filter>
           <action android:name="android.intent.action.MAIN" />
           <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
        </intent-filter>
       ```

#### 2. Gradle related:<a name="gradle-related"></a> 
   - What happens if i get an error "Cannot inline bytecode built with JVM target 1.8 into bytecode that is being built with JVM target 1.6"? Also, What could be done with errors like "Kotlin gradle Could not initialize class class org.jetbrains.kotlin.gradle.internal.KotlinSourceSetProviderImplKt"? What about "Super calls to Java default methods are prohibited in JVM target 1.6. Recompile with -jvm-target 1.8" error?
     - add these to the build.grade of your app
       ```
       compileOptions {
          targetCompatibility JavaVersion.VERSION_1_8
       }
       kotlinOptions {
         jvmTarget = '1.8'
       }
       ```
   - How to enable androidX in your project?
     - add these lines in the gradle.properties file
     ```
       android.enableJetifier=true
       android.useAndroidX=true
     ```
     after enabling AndroidX, you might need to change ``` import android.support.v7.app.AppCompatActivity ``` to ``` import androidx.appcompat.app.AppCompatActivity; ```
   - What to do for "Android version release warning message: This App Bundle contains Java/Kotlin code, which might be obfuscated" error?
     - Change minifyEnabled to true in build.grade file
   - What should I do, if i get the error "app:mergeDebugNativeLibs'. > A failure occurred while executing com.android.build.gradle.internal.tasks.MergeJavaResWorkAction > org/apache/commons/codec/binary/Base64"
     - add these lines to build.grade of your app
        ```
	classpath "commons-codec:commons-codec:1.11"
	```

#### 3. Kotlin related:<a name="kotlin-related"></a> 
   - What could be done with errors like "Kotlin Error : Could not find org.jetbrains.kotlin:kotlin-stdlib-jre7:1.0.7"?
     - In your build.grade, replace
        ```
         implementation "org.jetbrains.kotlin:kotlin-stdlib-jre7:$kotlin_version"
        ```
       with
        ```
         implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"
        ```

#### 4. Image related:<a name="image-related"></a> 
   - How to generate the necessary images for various devices?
     - Right-Click on the res folder, click New and then click Image Asset. A suitable image can be selected and appropriate images can be selected.  
   - Even though the appropriate images have been provided, the necessary images are not displayed. How to handle it?
     - XML files like ic_launcher_foreground.xml and ic_launcher_background.xml might be present in the drawable folder. This shows the default image type. Once you remove these files, the newly generated images will appear.

#### 5. Android TV related:<a name="android-tv-related"></a> 
   - How to handle the deprecation in MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS?   
     - This is handled using MediaSessionCompat.Callback() and capturing the keys.  
   - How to handle pressing of the Back key in the app?  	  
   - How to catch and handle various keys pressed in the app?  
      ```
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
        override fun onPause() {
           // called when the Stop button in the Notification bar is pressed.
           if (!isReleasePlayerCalled) player.stop()
        }
        override fun onMediaButtonEvent(mediaButtonIntent: Intent): Boolean {
          val ev: KeyEvent = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
             when (ev.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {player.pause()}
                   KeyEvent.KEYCODE_MEDIA_STOP ->  { }
                   else -> return false
             }
          return super.onMediaButtonEvent(mediaButtonIntent)
         }
      })
     ```

#### 6. Exoplayer related:<a name="exoplayer-related"></a> 
   - SimpleExoplayer.Builder(…) is to be used instead of ExoPlayerFactory.newSimpleInstance(…)
   - What is the difference between prepare() and play() methods in ExoPlayer?
     - Exoplayer prepare() method is used to acquire all the resources required for playback.
     - Exoplayer play() method is used to play when the stream is ready. The option player.playWhenReady could be enabled true to play once the stream is ready.
   - What is the use of setPlayWhenReady(true)?
     - Once getPlaybackState() returns STATE_READY, the player should play.
   - What is the use of MediaSession in ExoPlayer instance?
     - MediaSession is used to provide various details to the media player, like meta data, handling keys, etc. 
   - Sometimes the audio/media buttons like play, pause and others are not visible. How to handle this?
     - playerView.setUseController(true)
     - Also, there is showController() method in PlayerView
   - What is the solution for the error "HLS video playback throws "Unexpected runtime error" after video complete : "E/ExoPlayerImplInternal: Playback error""?
     - This is an ExoPlayer related error. Please make sure to update your ExoPlayer.
   - Playstate in the MediaSession should be updated. How could it be done?
     ```
      val mediaController: MediaControllerCompat = mediaSession.getController()
      val stateCompat: PlaybackStateCompat = mediaController.getPlaybackState()
      val stateBuilder = PlaybackStateCompat.Builder()
          .setActions(getAvailableActions()).apply {
             setState(stateCompat.getState(), player.currentPosition.toLong(), 1.0f) }
      mediaSession.setPlaybackState(stateBuilder.build())
     ```
     ```
      var actions = (PlaybackState.ACTION_PLAY_PAUSE
        or PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE
        or PlaybackState.ACTION_REWIND or PlaybackState.ACTION_STOP )
      val mediaController: MediaControllerCompat = mediaSession.getController()
      val stateCompat: PlaybackStateCompat = mediaController.getPlaybackState()
      val mState = stateCompat.getState()
        actions = if (mState == PlaybackState.STATE_PLAYING) {
           actions or PlaybackState.ACTION_PAUSE 
        } else { actions or PlaybackState.ACTION_PLAY }
      ```

#### 7. Notification related:<a name="notification-related"></a> 
   - How to make sure only audio bandwidth is consumed when the app is playing in the background?
      ```
       trackSelector = DefaultTrackSelector(this)
       mPlayer = SimpleExoPlayer.Builder(this)
          .setTrackSelector(trackSelector).build()
       trackSelector.setParameters(
          trackSelector.buildUponParameters().setMaxVideoBitrate(0))
       }
      ```
   - Can the notification be removed as it is tied to a foreground service?
     - stopForeground(…) must be used. This could be checked using isPlaying in onIsPlayingChanged(…)
   - Notification Service: What is the use of stopWithTask option?
      ```
        <service android:name="com.ExoPlayerPIPexample.PlayerNotificationService" android:stopWithTask="true"/>
      ```
      Service will be automatically stopped when the user remove a task rooted in an activity owned by the application. 
      Refer: https://developer.android.com/reference/android/R.attr#stopWithTask
   - How to display elements in Notification bar when playing the audio in background?
      ```
       override fun getCurrentContentText(player: Player): String? {
          return "ExoPlayer PIP example"
       }
      ```
   - The space in the notification bar is limited and thus if you wish to make the stop button visible, how to do this?
       ```
          playerNotificationManager.setUseStopAction(true)
          playerNotificationManager.setFastForwardIncrementMs(0)
          playerNotificationManager.setRewindIncrementMs(0)
       ```
   - onNotificationPosted is called every time a notification is posted and/or created. How to handle this?
      - The method onDestroy() calls playerNotificationManager.setPlayer(null)

#### 8. Service related:<a name="service-related"></a> 
   - How to make sure that all the services or processes or activities are closed?
     - You could get the current state of the activity: lifecycle.currentState.toString()
     - We could get the details of the running app processes:
        ```
         val mngr = getSystemService(ACTIVITY_SERVICE) as ActivityManager
         for (service in mngr.getRunningAppProcesses()) {
             Log.e("messages",service.processName)
         }
        ```
   - What is the difference between bindService and startService?
     - startService() is used to start a service but bindService() is used to bound a service.
     - Bounded services will be automatically destroyed when all clients have detached.
     - Services can be stopped using stopSelf() and stopService().
     - startService() calls onStartCommand() while bindService() does not.
   - Why is that onServiceConnected() is not called after bindService() method?
     - Note that there can a delay before the onServiceConnected() method is called after calling bindService() method
     - Also, if the bindService() method is calling from onCreate(), then the onServiceConnected() is invoked only after the completion of onCreate() method.
   - Why is onServiceDisconnected() invoked after calling stopSelf() or stopService() method?
     - Note whether the service was started using startService() or bindService(). If bindService() was used, unbindService() is needed to invoke the onServiceConnected() method. 

#### 9. General information:<a name="general-info"></a>
   - Is development in Kotlin difficult?
      - Not so. Even Android Studio converts the Java code into Kotlin code for us.
   - What if a specific code is applicable to a specific version of Android only?
     ```
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N {}
     ```
   - What happens when we swipe the app to stop it?
      - The onTaskRemoved() method is called.
   - What is the difference between non-null and null type of variables in Kotlin?
      - As an example, String cannot hold null. 
        ```
         var a: String = "abc" // Regular initialization means non-null by default
         a = null // compilation error
         var b: String? = "abc" // can be set null
         b = null // ok
   - How to handle the support or absence of support of PIP in Android TV and mobile devices?
      - Check the Android version to make sure PIP is supported.
      - It should be noted that certain SMART TVs also don’t support PIP properly. Thus, validation can be done using
        ```
          packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ```
   - On a TV, it is necessary to cater for NOISY AUDIO. How to handle this?
      - In the Manifest file, add the necessary intent-filter.
        ```
         <intent-filter>
              <action android:name="android.media.AUDIO_BECOMING_NOISY" />
         </intent-filter>
        ```
   - Then, handle it using BroadcastReceiver.
      ```
        private val BecomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
               if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                  // ...
                }
            }
        }
      ```
   - Register this receiver during the onPlay() or other methods.
     ```
       override fun onPlay() {
         registerReceiver(BecomingNoisyReceiver, intentFilter)
       }
     ```
   - How to validate whether we are running the app on a TV or a mobile device?
     ```
       val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
       deviceIsTV = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
     ```
   - How to simulate various keys in emulator, to test the application?
     - C:\Users\<username>\AppData\Local\Android\Sdk\platform-tools\adb shell input keyevent 127
     - Here, 126 is play, 127 is stop, 85 is Play/Pause, 86 is Stop
     - KeyEvent details are available at: https://developer.android.com/reference/android/view/KeyEvent
   - What if the Volley returns the request with an error? Can we request the volley in a loop?
     ```
      private fun volleyRequest()
      {
        var requestQueue: RequestQueue = Volley.newRequestQueue(applicationContext)
           jsonObjectRequest = JsonObjectRequest(
             Request.Method.GET, mJSONURLString, null,
              { response ->
                try {
                  // Get the JSON array
                } catch (e: JSONException) {   e.printStackTrace()  }
              },
              { error ->
                 // TODO: Handle error: Repeat retrieving the json
                 volleyRequest()
              }
         )
         // Add JsonObjectRequest to the RequestQueue
         requestQueue.add(jsonObjectRequest)
       }
      ```
   - How to upload the APP into github?
     - consider using .gitignore where you ignore the build, captures, caches and few other folders.
   - Is it advisable to use system.exit(0) to stop services or activity?
     - It is not advisable. It might not even work as expected.
   - How to check the network, CPU, memory and other usage of your application?
     - You could use profiler option available in Android Studio. Please refer: https://developer.android.com/studio/profile/cpu-profiler
   - What is the use of android:launchmode="singleInstance" ?
     - This is to make sure only one instance. This activity will be the single and only member of the task. Please refer to: https://developer.android.com/guide/topics/manifest/activity-element

### References<a name="references"></a>
1. Former approach of Exoplayer with PIP in Kotlin.
   - https://medium.com/s23nyc-tech/drop-in-android-video-exoplayer2-with-picture-in-picture-e2d4f8c1eb30
2. Kotlin tutorials
   - https://www.javatpoint.com/kotlin-android-toast
3. JSON parsing in Kotlin
   - https://android--code.blogspot.com/2020/10/android-kotlin-volley-jsonobjectrequest.html
4. Exoplayer on Kotlin - Latest
   - https://developer.android.com/guide/topics/media/exoplayer
5. Track Selection
   - https://exoplayer.dev/track-selection.html
6. Notification examples in Java
   - https://github.com/yalematta/ExoPlayback/blob/master/app/src/main/java/com/yalematta/android/ExoPlayback/MainActivity.java
7. Android Exoplayer Background Play using Kotlin
   - https://intensecoder.com/android-exoplayer-background-play-using-kotlin/
8. ExoPlayer example: github page example…
   - https://github.com/anandwana001/exoplayer-example
9. Further discussion examples:
   - https://github.com/google/ExoPlayer/issues/5301
   - https://github.com/google/ExoPlayer/issues/6693	
