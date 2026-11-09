package com.example.kharcha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.kharcha.ui.AppViewModel
import com.example.kharcha.ui.KharchaApp
import com.example.kharcha.ui.KharchaTheme

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KharchaTheme {
                KharchaApp(vm)
            }
        }
    }
}
