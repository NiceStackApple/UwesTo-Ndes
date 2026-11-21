package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.components.InterceptionModalOverlay
import com.example.ui.screens.BattlesScreen
import com.example.ui.screens.BlocksScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.StatsScreen
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.flow.collectLatest

private const val ROUTE_HOME = "home"
private const val ROUTE_STATS = "stats"
private const val ROUTE_BLOCKS = "blocks"
private const val ROUTE_BATTLES = "battles"

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val UwesToTabs = listOf(
    NavTab(ROUTE_HOME, R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home, "nav_tab_home"),
    NavTab(ROUTE_STATS, R.string.nav_stats, Icons.Rounded.BarChart, Icons.Outlined.BarChart, "nav_tab_stats"),
    NavTab(ROUTE_BLOCKS, R.string.nav_blocks, Icons.Rounded.Shield, Icons.Outlined.Shield, "nav_tab_blocks"),
    NavTab(ROUTE_BATTLES, R.string.nav_battles, Icons.Rounded.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_tab_battles")
)

@Composable
fun UwesToApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Automatically refresh Accessibility & Overlay permission health every time user returns from Settings
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissionHealth(logTransitions = true)
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (!uiState.isLoaded) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    if (!uiState.settings.onboardingCompleted) {
        OnboardingScreen(
            permissionHealth = uiState.permissionHealth,
            onFinish = { limit -> viewModel.completeOnboarding(limit) }
        )
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            UwesToTabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                val label = stringResource(tab.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            UwesToTabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                val label = stringResource(tab.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(ROUTE_HOME) {
                            HomeScreen(
                                uiState = uiState,
                                onSelectTestApp = viewModel::selectTestApp,
                                onSimulateScroll = viewModel::simulateScroll,
                                onResetToday = viewModel::resetTodayCount,
                                onToggleTestOverlay = viewModel::toggleTestOverlayPill,
                                onPreviewBlocker = viewModel::previewBlockerModal
                            )
                        }
                        composable(ROUTE_STATS) {
                            StatsScreen(uiState = uiState)
                        }
                        composable(ROUTE_BLOCKS) {
                            BlocksScreen(
                                uiState = uiState,
                                onUpdateDailyLimit = viewModel::updateDailyLimit,
                                onSetLimitEnforcement = viewModel::setLimitEnforcement,
                                onSetNearLimitWarning = viewModel::setNearLimitWarning,
                                onSetFloatingOverlayEnabled = viewModel::setFloatingOverlayEnabled,
                                onSetBreakConfig = viewModel::setBreakConfig,
                                onToggleBlockRule = viewModel::toggleBlockRule,
                                onToggleBlockRuleStrict = viewModel::toggleBlockRuleStrict,
                                onAddCustomBlock = viewModel::addCustomBlockRule,
                                onDeleteBlockRule = viewModel::deleteBlockRule,
                                onPreviewBlocker = viewModel::previewBlockerModal,
                                onPreviewBreak = viewModel::previewBreakModal
                            )
                        }
                        composable(ROUTE_BATTLES) {
                            BattlesScreen(
                                uiState = uiState,
                                onAddRival = viewModel::addBattleRival,
                                onUpdateRivalScore = viewModel::updateRivalScore,
                                onSimulateRivals = viewModel::simulateRivalsActivity,
                                onDeleteRival = viewModel::deleteBattleRival,
                                onBuildShareSummary = viewModel::buildBattleShareSummary
                            )
                        }
                    }
                }
            }
        }

        // Full-screen in-app Blocker / Break Pause overlay when triggered
        InterceptionModalOverlay(
            interception = uiState.activeInterception,
            onDismiss = { skipped -> viewModel.dismissInterception(skipped) },
            onEmergencyUnlock = { viewModel.completeEmergencyUnlock() },
            onDisableAllDayRule = { ruleId -> viewModel.disableAllDayFromLockScreen(ruleId) }
        )
    }
}
