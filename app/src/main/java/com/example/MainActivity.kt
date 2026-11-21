package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.example.model.AssetCategory
import com.example.ui.screens.MainPortfolioScreen
import com.example.ui.theme.PortPulseTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        )
        setContent {
            var activeCategoryTheme by remember { mutableStateOf<AssetCategory?>(AssetCategory.THAI_STOCK) }
            PortPulseTheme(selectedCategory = activeCategoryTheme) {
                MainPortfolioScreen(
                    onCategoryChanged = { activeCategoryTheme = it }
                )
            }
        }
    }
}
