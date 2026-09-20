package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.NewsScreen
import com.example.ui.NewsViewModel
import com.example.ui.components.IntroScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var hasExplored by rememberSaveable { mutableStateOf(false) }
                    Crossfade(
                        targetState = hasExplored,
                        animationSpec = tween(350),
                        label = "introNavigation"
                    ) { explored ->
                        if (explored) {
                            val viewModel: NewsViewModel = viewModel()
                            NewsScreen(viewModel = viewModel)
                        } else {
                            IntroScreen(onExplore = { hasExplored = true })
                        }
                    }
                }
            }
        }
    }
}
