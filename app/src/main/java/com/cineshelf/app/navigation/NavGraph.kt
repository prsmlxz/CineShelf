package com.cineshelf.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cineshelf.app.ui.detail.ShowDetailScreen
import com.cineshelf.app.ui.library.LibraryScreen
import com.cineshelf.app.ui.player.PlayerScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val LIBRARY = "library"
    const val DETAIL = "detail/{folderPath}"
    const val PLAYER = "player/{filePath}"

    fun detail(folderPath: String) = "detail/${URLEncoder.encode(folderPath, "UTF-8")}"
    fun player(filePath: String) = "player/${URLEncoder.encode(filePath, "UTF-8")}"
}

@Composable
fun CineShelfNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LIBRARY,
        modifier = modifier,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(180)) }
    ) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onOpenShow = { folderPath ->
                    navController.navigate(Routes.detail(folderPath))
                },
                onPlay = { filePath ->
                    navController.navigate(Routes.player(filePath))
                }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("folderPath") { type = NavType.StringType }),
            enterTransition = { fadeIn(tween(240)) + scaleIn(tween(240), initialScale = 0.97f) },
            exitTransition = { fadeOut(tween(180)) },
            popExitTransition = { fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.97f) }
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("folderPath") ?: ""
            val folderPath = URLDecoder.decode(encoded, "UTF-8")
            ShowDetailScreen(
                folderPath = folderPath,
                onBack = { navController.popBackStack() },
                onPlay = { filePath ->
                    navController.navigate(Routes.player(filePath))
                }
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("filePath") { type = NavType.StringType }),
            enterTransition = { fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.94f) },
            exitTransition = { fadeOut(tween(160)) },
            popExitTransition = { fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.94f) }
        ) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("filePath") ?: ""
            val filePath = URLDecoder.decode(encoded, "UTF-8")
            PlayerScreen(
                filePath = filePath,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

