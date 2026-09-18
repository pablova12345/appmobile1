package bo.gob.gamc.catastro.digid.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import bo.gob.gamc.catastro.digid.core.network.TokenManager
import bo.gob.gamc.catastro.digid.core.theme.DigiDTheme
import bo.gob.gamc.catastro.digid.presentation.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isAuthenticated = tokenManager.getAccessToken() != null

        setContent {
            DigiDTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    isAuthenticated = isAuthenticated
                )
            }
        }
    }
}
