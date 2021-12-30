package com.example.download_file_from_server

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.text.ExoplayerCuesDecoder
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.jar.Manifest
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    lateinit var retro: retro_interface
    lateinit var infoData:String
    var STORAGE_PERMISSION_CODE=1000
    var player:SimpleExoPlayer?=null
    lateinit var playerView:PlayerView



//    s is video url
//    var s="https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8"
    var s="https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"
//    var s="https://newsapp-xcube.s3.ap-south-1.amazonaws.com/1640078024799-1640077878347_Video6.mp4?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20211230T105959Z&X-Amz-SignedHeaders=host&X-Amz-Expires=604800&X-Amz-Credential=AKIA5MKKRQHZJD2MBAU3%2F20211230%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Signature=7c19dd83798d6d84e9d402769d2d4d41974a0abdb7a5c235406d99d6828f2c17"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById<PlayerView>(R.id.exop)

        var urll = "https://fakestoreapi.com/products/1/"

        retro = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(urll)
            .build()
            .create(retro_interface::class.java)

        infoData=" "
        lifecycleScope.launch {
             infoData= retro.getdata().image
            Log.d("info","data is $infoData")
            Glide.with(this@MainActivity)
                .load(infoData)
                .circleCrop()
                .into(iv)
        }

        initPlayer()
        btn.setOnClickListener {
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                if(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED){

                    requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
                }else{
                    startDownloading(infoData)
                }
            }

        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            STORAGE_PERMISSION_CODE->{
                Log.d("res","${grantResults[0]} c")
                if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    startDownloading(infoData)
                }else{
                    var sb=Snackbar.make(root_layout,"permission denied",Snackbar.LENGTH_LONG)
                    sb.show()

                }
            }
        }
    }

    private fun startDownloading(url:String) {
        if(url!=""){
        var req=DownloadManager.Request(Uri.parse(url))
            req.setTitle("file Download")
            req.setDescription("Downloading........")
        req.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)


//            use this if the it is video
//        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"${System.currentTimeMillis()}"+".Mp4")
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"${System.currentTimeMillis()}"+".jpg")

        var manager=getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(req)
    }else{
        Thread.sleep(500)
            startDownloading(infoData)
        }
    }

    private fun initPlayer() {

        val dataSourceFactory=DefaultDataSourceFactory(this, Util.getUserAgent(this,"Exo"),
            DefaultBandwidthMeter()
        )

        player = SimpleExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()

        val videoUri = Uri.parse(s)

//        val videoUri = Uri.parse(newsList[AppImpl.randomInt(newsList.size - 1)])
        val mediaItem = /*MediaItem.fromUri(videoUri)*/MediaItem.Builder().setMediaId(generate_key(videoUri))
            .setCustomCacheKey(generate_key(videoUri))
            .setUri(videoUri).build()
//        mediaItem.buildUpon().setCustomCacheKey(generate_key(videoUri)).setMediaId(generate_key(videoUri))
        val mediaSource =
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)

        player?.playWhenReady = true
        player?.seekTo(0, 0)
        player?.repeatMode = Player.REPEAT_MODE_ONE
        player?.playWhenReady = false
        player?.setMediaSource(mediaSource, true)
        player?.prepare()

        playerView.player=player
        player?.playWhenReady = true


    }

    private fun generate_key(uri: Uri): String {
        val url = uri.path
        //        String extension = getExtension(url);
        val string = uri.toString()
        val parts = string.split("\\?".toRegex()).toTypedArray()
        val part1 = parts[0]
        return computeMD5(part1)
//        // String part2 = parts[1];
//        return part1;
    }

    fun computeMD5(string: String): String {
        return try {
            val messageDigest = MessageDigest.getInstance("MD5")
            val digestBytes = messageDigest.digest(string.toByteArray())
            bytesToHexString(digestBytes)
        } catch (e: NoSuchAlgorithmException) {
            ""
        }
    }

    private fun bytesToHexString(bytes: ByteArray): String {
        val sb = StringBuffer()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}