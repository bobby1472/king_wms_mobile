package com.king.wms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.king.wms.kiosk.KioskManager
import com.king.wms.ui.screens.DispatchScreen
import com.king.wms.ui.screens.HomeScreen
import com.king.wms.ui.screens.InventoryCountScreen
import com.king.wms.ui.screens.InventoryPostingScreen
import com.king.wms.ui.screens.LoginScreen
import com.king.wms.ui.screens.LuxeDrawer
import com.king.wms.ui.screens.ReceivingScreen
import com.king.wms.ui.screens.SettingsScreen
import com.king.wms.ui.screens.StockCheckScreen
import com.king.wms.ui.screens.StockMovementsScreen
import com.king.wms.ui.screens.TransferScreen
import com.king.wms.ui.theme.KingWmsTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@ExperimentalGetImage
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var kiosk: KioskManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kiosk = KioskManager(this)

        // If we are Device Owner, harden into a true kiosk on first launch.
        if (kiosk.isDeviceOwner()) {
            kiosk.applyKioskPolicies()
        }

        setContent {
            KingWmsTheme {
                val navController = rememberNavController()
                var displayName by remember { mutableStateOf<String?>(null) }
                var allowed by remember { mutableStateOf<Set<String>>(emptySet()) }
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                val authed = currentRoute != null && currentRoute != "login"
                val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

                // Drawer destination switch (also handles Home + Sign out).
                fun navigateTo(route: String) {
                    scope.launch { drawerState.close() }
                    when (route) {
                        "logout" -> {
                            kiosk.stopKiosk(this@MainActivity)
                            navController.navigate("login") { popUpTo("home") { inclusive = true } }
                        }
                        "home" -> navController.navigate("home") {
                            popUpTo("home") { inclusive = true }; launchSingleTop = true
                        }
                        else -> navController.navigate(route) {
                            popUpTo("home"); launchSingleTop = true
                        }
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = authed,
                    drawerContent = { LuxeDrawer(displayName ?: "Operator", currentRoute, allowed, ::navigateTo) },
                ) {
                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            LoginScreen(onLoggedIn = { name, allow ->
                                displayName = name
                                allowed = allow
                                // Lock into single-app kiosk after sign-in.
                                kiosk.startKiosk(this@MainActivity)
                                navController.navigate("home") { popUpTo("login") { inclusive = true } }
                            })
                        }
                        composable("home") {
                            HomeScreen(
                                displayName = displayName ?: "Operator",
                                allowed = allowed,
                                onMenu = openDrawer,
                                onNavigate = { route -> navController.navigate(route) },
                            )
                        }
                        composable("dispatch") { DispatchScreen(onMenu = openDrawer) }
                        composable("receiving") { ReceivingScreen(onMenu = openDrawer) }
                        composable("transfer") { TransferScreen(onMenu = openDrawer) }
                        composable("stockcheck") { StockCheckScreen(onMenu = openDrawer) }
                        composable("movements") { StockMovementsScreen(onMenu = openDrawer) }
                        composable("count") { InventoryCountScreen(onMenu = openDrawer) }
                        composable("posting") { InventoryPostingScreen(onMenu = openDrawer) }
                        composable("settings") { SettingsScreen(onMenu = openDrawer) }
                    }
                }
            }
        }
    }

    // Re-assert lock task if the user is bounced back into the app.
    override fun onResume() {
        super.onResume()
        if (kiosk.isDeviceOwner()) kiosk.startKiosk(this)
    }
}
