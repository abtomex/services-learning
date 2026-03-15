package com.example.serviceslearning

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private companion object {
        const val SONG_URL = "https://files.freemusicarchive.org/storage-freemusicarchive-org/tracks/h3vL7veZhrT8ghGqKN0s02D0RFqOGPftD7fFtCga.mp3"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Если выдали разрешение — запускаем сервис.
            startMusicService()
        } else {
            // Иначе просто покажем ошибку
            Toast.makeText(this, "Can't start foreground service!", Toast.LENGTH_LONG).show()
        }
    }

    private fun startMusicService() {
        // Создаём intent для запуска сервиса
        val intent = Intent(this, MusicService::class.java).apply {
            putExtra("song_url", SONG_URL)
        }

        // Стартуем сервис
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.startMusicServiceButton).setOnClickListener {

            // На версии Android 13 и выше — сначала запросим разрешение
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // На версиях ниже Android 13 —
                // можно сразу стартовать сервис.
                startMusicService()
            }
        }

        findViewById<Button>(R.id.startProgressServiceButton).setOnClickListener {
            // Создаём intent для запуска сервиса
            val intent = Intent(this, ProgressService::class.java)
            // Стартуем сервис
            ContextCompat.startForegroundService(this, intent)
        }
    }

}