package bo.gob.gamc.catastro.digid.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import bo.gob.gamc.catastro.digid.presentation.ai.AIStudyScreen
import bo.gob.gamc.catastro.digid.presentation.auth.LoginScreen
import bo.gob.gamc.catastro.digid.presentation.geoextract.GeoextractScreen
import bo.gob.gamc.catastro.digid.presentation.home.HomeScreen
import bo.gob.gamc.catastro.digid.presentation.pdf.PdfViewerScreen
import bo.gob.gamc.catastro.digid.presentation.resoluciones.ResolucionesScreen
import java.net.URLDecoder

@Composable
fun AppNavHost(
    navController: NavHostController,
    isAuthenticated: Boolean
) {
    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) Screen.Home.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPdf = { path, name ->
                    navController.navigate(Screen.PdfViewer.createRoute(path, name))
                },
                onNavigateToResoluciones = {
                    navController.navigate(Screen.Resoluciones.route)
                },
                onNavigateToGeoextract = {
                    navController.navigate(Screen.Geoextract.route)
                },
                onLoggedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.PdfViewer.route,
            arguments = listOf(
                navArgument("filePath") { type = NavType.StringType },
                navArgument("fileName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawPath = backStackEntry.arguments?.getString("filePath") ?: ""
            val rawName = backStackEntry.arguments?.getString("fileName") ?: ""
            val filePath = URLDecoder.decode(rawPath, "UTF-8")
            val fileName = URLDecoder.decode(rawName, "UTF-8")

            PdfViewerScreen(
                filePath = filePath,
                fileName = fileName,
                onBack = { navController.popBackStack() },
                onStudyWithAi = {
                    navController.navigate(Screen.AIStudy.createRoute(fileName))
                }
            )
        }

        composable(Screen.Resoluciones.route) {
            ResolucionesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AIStudy.route,
            arguments = listOf(
                navArgument("fileName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val rawName = backStackEntry.arguments?.getString("fileName") ?: ""
            val fileName = URLDecoder.decode(rawName, "UTF-8")

            AIStudyScreen(
                fileName = fileName,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Geoextract.route) {
            GeoextractScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
