package com.cineshelf.app.navigation

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
        modifier = modifier
    ) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onOpenShow = { folderPath ->
                    navController.navigate(Routes.detail(folderPath))
                }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("folderPath") { type = NavType.StringType })
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
            arguments = listOf(navArgument("filePath") { type = NavType.StringType })
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
