package com.zayn.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zayn.launcher.data.Store
import com.zayn.launcher.ui.AppRoot
import com.zayn.launcher.ui.ZaynTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        setContent {
            ZaynTheme {
                AppRoot()
            }
        }
    }
}
