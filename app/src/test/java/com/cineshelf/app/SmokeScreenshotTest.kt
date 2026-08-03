package com.cineshelf.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
class SmokeScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersToPng() {
        composeRule.setContent {
            Box(
                Modifier.fillMaxSize().background(Color(0xFF06060A)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(180.dp, 90.dp)
                        .background(Color(0xFF6366F1), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("CineShelf", color = Color.White)
                }
            }
        }
        composeRule.onRoot().captureRoboImage("smoke.png")
    }
}
