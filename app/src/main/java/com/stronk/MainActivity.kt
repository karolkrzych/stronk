package com.stronk

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stronk.ui.StronkNavHost
import com.stronk.ui.theme.StronkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Apka jest DARK ONLY (mocki) — paski systemowe zawsze z jasnymi ikonami,
        // niezależnie od motywu systemu.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            StronkTheme {
                StronkNavHost()
            }
        }
    }
}
