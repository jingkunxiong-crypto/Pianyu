package com.example.newandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.newandroidapp.permissions.PianyuRoot
import com.example.newandroidapp.ui.components.PianyuSunriseSplash
import com.example.newandroidapp.ui.theme.PianyuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PianyuTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }

                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 320),
                    label = "Pianyu launch transition",
                ) { isSplashVisible ->
                    if (isSplashVisible) {
                        PianyuSunriseSplash(onFinished = { showSplash = false })
                    } else {
                        PianyuRoot()
                    }
                }
            }
        }
    }
}
