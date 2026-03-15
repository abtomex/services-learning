package com.example.serviceslearning

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private companion object {
        const val SONG_URL = "https://files.freemusicarchive.org/storage-freemusicarchive-org/tracks/h3vL7veZhrT8ghGqKN0s02D0RFqOGPftD7fFtCga.mp3"
    }

    private var musicService: MusicService? = null

    private var playerState: PlayerState = PlayerState.Default()

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicServiceBinder
            musicService = binder.getService()
            musicService?.setPlayerStateListener(object : MusicService.PlayerStateListener {

                override fun onStateChanged(state: PlayerState) {
                    playerState = state
                    updateButtonAndProgress()
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindMusicService()

        findViewById<Button>(R.id.playButton).setOnClickListener {
            if (playerState is PlayerState.Prepared || playerState is PlayerState.Paused) {
                musicService?.startPlayer()
            } else {
                musicService?.pausePlayer()
            }
        }
    }

    override fun onDestroy() {
        unbindMusicService()
        super.onDestroy()
    }

    private fun bindMusicService() {
        val intent = Intent(this, MusicService::class.java).apply {
            putExtra("song_url", SONG_URL)
        }

        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindMusicService() {
        unbindService(serviceConnection)
    }

    private fun updateButtonAndProgress() {
        findViewById<Button>(R.id.playButton)?.apply {
            text = playerState.buttonText
            isEnabled = playerState.buttonState
        }
        findViewById<TextView>(R.id.timerTextView)?.text = playerState.progress
    }
}
