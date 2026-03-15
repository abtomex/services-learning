package com.example.serviceslearning

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MusicService : Service() {

    private companion object {
        const val LOG_TAG = "MusicService"
        const val SERVICE_NOTIFICATION_ID = 100
        const val NOTIFICATION_CHANNEL_ID = "music_service_channel"

    }

    // Переменная для хранения MediaPlayer
    private var mediaPlayer: MediaPlayer? = null

    // Глобальная переменная для хранения ссылки на песню
    private var songUrl = ""

    private var playerState : PlayerState = PlayerState.Default()
    private var playerStateListener: PlayerStateListener? = null

    fun setPlayerStateListener(listener: PlayerStateListener) {
        playerStateListener = listener
    }

    private var timerJob: Job? = null


    private fun startTimer() {
        timerJob = CoroutineScope(Dispatchers.Default).launch {
            while (mediaPlayer?.isPlaying == true) {
                delay(300L)
                playerState = PlayerState.Playing(getCurrentPlayerPosition())
            }
        }
    }
    private fun getCurrentPlayerPosition(): String {
        return SimpleDateFormat("mm:ss", Locale.getDefault()).format(mediaPlayer?.currentPosition) ?: "00:00"
    }

    // Методы класса Service
    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
        createNotificationChannel()
    }

    override fun onDestroy() {
        releasePlayer()
    }

    private val binder = MusicServiceBinder()

    override fun onBind(intent: Intent?): IBinder? {
        songUrl = intent?.getStringExtra("song_url") ?: ""

        initMediaPlayer()

        ServiceCompat.startForeground(
            this,
            SERVICE_NOTIFICATION_ID,
            createNotification(),
            getForegroundServiceTypeConstant()
        )

        return binder
    }

    private fun getForegroundServiceTypeConstant(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Music foreground service")
            .setContentText("Our service is working right now!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }


    private fun createNotificationChannel() {
        // Создание каналов доступно только с Android 8.0
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            /* id= */ NOTIFICATION_CHANNEL_ID,
            /* name= */ "Music service",
            /* importance= */ NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "Service for playing music"

        // Регистрируем канал уведомлений
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        releasePlayer()
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        songUrl = intent?.getStringExtra("song_url") ?: ""
        initMediaPlayer()
        return START_NOT_STICKY
    }

    // Методы управления Media Player

    // Первичная инициализация плеера
    private fun initMediaPlayer() {
        if (songUrl.isEmpty()) return

        mediaPlayer?.setDataSource(songUrl)
        mediaPlayer?.prepareAsync()
        mediaPlayer?.setOnPreparedListener {
            playerState = PlayerState.Prepared()
        }
        mediaPlayer?.setOnCompletionListener {
            playerState = PlayerState.Prepared()
        }
    }

    // Запуск воспроизведения
    fun startPlayer() {
        mediaPlayer?.start()
        playerState = PlayerState.Playing(getCurrentPlayerPosition())
        playerStateListener?.onStateChanged(playerState)
    }

    fun pausePlayer() {
        mediaPlayer?.pause()
        playerState = PlayerState.Paused(getCurrentPlayerPosition())
        playerStateListener?.onStateChanged(playerState)
    }

    private fun releasePlayer() {
        mediaPlayer?.stop()
        timerJob?.cancel()
        playerState = PlayerState.Default()
        mediaPlayer?.setOnPreparedListener(null)
        mediaPlayer?.setOnCompletionListener(null)
        mediaPlayer?.release()
        mediaPlayer = null
    }

    inner class MusicServiceBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    interface PlayerStateListener {
        fun onStateChanged(state: PlayerState)
    }
}