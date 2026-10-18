package com.rowshr.siddur.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rowshr.siddur.AppContainer
import com.rowshr.siddur.fold.LocalFoldInfo
import com.rowshr.siddur.model.Nusach
import com.rowshr.siddur.ui.browse.LibraryScreen
import com.rowshr.siddur.ui.browse.SavedScreen
import com.rowshr.siddur.ui.browse.SearchScreen
import com.rowshr.siddur.ui.browse.SectionScreen
import com.rowshr.siddur.ui.home.HomeScreen
import com.rowshr.siddur.ui.reader.ReaderScreen
import com.rowshr.siddur.ui.settings.SettingsScreen
import com.rowshr.siddur.ui.theme.SiddurTheme
import kotlinx.serialization.Serializable

val LocalAppContainer = compositionLocalOf<AppContainer> { error("No AppContainer") }

// MARK: - Routes

@Serializable object TodayRoute
@Serializable object LibraryRoute
@Serializable object SavedRoute
@Serializable object SearchRoute
@Serializable object SettingsRoute
@Serializable data class SectionRoute(val nusach: String, val id: String)
@Serializable data class ReaderRoute(val nusach: String, val id: String)

enum class AppTab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector, val route: Any) {
    TODAY("Today", Icons.Outlined.WbTwilight, Icons.Filled.WbTwilight, TodayRoute),
    LIBRARY("Library", Icons.Outlined.LocalLibrary, Icons.Filled.LocalLibrary, LibraryRoute),
    SAVED("Saved", Icons.Outlined.Bookmark, Icons.Filled.Bookmark, SavedRoute),
    SEARCH("Search", Icons.Outlined.Search, Icons.Filled.Search, SearchRoute),
}

/** Routes the tab bar hides for: the reader wants the whole screen, as a page should. */
private val fullScreenRoutes = setOf(ReaderRoute::class.qualifiedName, SettingsRoute::class.qualifiedName)

@Composable
fun SiddurApp(initialRoute: String? = null) {
    val container = LocalAppContainer.current
    val settings by container.settings.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    LaunchedEffect(initialRoute) {
        val parts = initialRoute?.split('/') ?: return@LaunchedEffect
        when (parts.getOrNull(0)) {
            "reader" -> if (parts.size >= 3) navController.navigate(ReaderRoute(parts[1], parts[2]))
            "section" -> if (parts.size >= 3) navController.navigate(SectionRoute(parts[1], parts[2]))
            "settings" -> navController.navigate(SettingsRoute)
            "library" -> navController.navigateToTab(AppTab.LIBRARY)
            "search" -> navController.navigateToTab(AppTab.SEARCH)
            "saved" -> navController.navigateToTab(AppTab.SAVED)
        }
    }
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val currentTab = AppTab.entries.firstOrNull { tab -> currentRoute?.startsWith(tab.route::class.qualifiedName ?: "-") == true }
    val isFullScreen = fullScreenRoutes.any { it != null && currentRoute?.startsWith(it) == true }

    SiddurTheme(settings.theme) {
        val adaptive = currentWindowAdaptiveInfo()
        val layoutType = when {
            isFullScreen -> NavigationSuiteType.None
            else -> NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptive)
        }
        NavigationSuiteScaffold(
            layoutType = layoutType,
            containerColor = Color.Transparent,
            navigationSuiteColors = NavigationSuiteDefaults.colors(
                navigationBarContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                navigationRailContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ),
            navigationSuiteItems = {
                AppTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    item(
                        selected = selected,
                        onClick = { navController.navigateToTab(tab) },
                        icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                        alwaysShowLabel = true,
                    )
                }
            },
        ) {
            Box(Modifier.fillMaxSize()) {
                NavHost(navController, startDestination = TodayRoute) {
                    composable<TodayRoute> { HomeScreen(navController) }
                    composable<LibraryRoute> { LibraryScreen(navController) }
                    composable<SavedRoute> { SavedScreen(navController) }
                    composable<SearchRoute> { SearchScreen(navController) }
                    composable<SettingsRoute> { SettingsScreen(navController) }
                    composable<SectionRoute> { entry ->
                        val route = entry.toRoute<SectionRoute>()
                        SectionScreen(navController, Nusach.fromName(route.nusach) ?: settings.nusach, route.id)
                    }
                    composable<ReaderRoute> { entry ->
                        val route = entry.toRoute<ReaderRoute>()
                        ReaderScreen(navController, Nusach.fromName(route.nusach) ?: settings.nusach, route.id)
                    }
                }
            }
        }
    }
}

fun NavHostController.navigateToTab(tab: AppTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavHostController.openReader(nusach: Nusach, id: String) = navigate(ReaderRoute(nusach.name, id))
fun NavHostController.openSection(nusach: Nusach, id: String) = navigate(SectionRoute(nusach.name, id))
