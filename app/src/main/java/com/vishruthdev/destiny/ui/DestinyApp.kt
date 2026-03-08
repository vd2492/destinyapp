package com.vishruthdev.destiny.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vishruthdev.destiny.data.AuthRepository
import com.vishruthdev.destiny.DestinyApplication
import com.vishruthdev.destiny.navigation.Routes
import com.vishruthdev.destiny.ui.theme.DestinyAccentBlue
import com.vishruthdev.destiny.viewmodel.HabitsViewModelFactory
import com.vishruthdev.destiny.viewmodel.HomeViewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun DestinyApp(
    darkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {},
    authRepository: AuthRepository? = null,
    onGoogleSignOut: () -> Unit = {}
) {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as DestinyApplication
    val repository = app.habitRepository
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            DestinyBottomNav(
                navController = navController,
                currentRoute = currentRoute
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    viewModel = viewModel(factory = HomeViewModelFactory(repository)),
                    darkTheme = darkTheme,
                    onThemeToggle = onThemeToggle,
                    onNavigateToHabits = {
                        navController.navigate(Routes.Habits) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(Routes.Habits) {
                HabitsScreen(
                    viewModel = viewModel(factory = HabitsViewModelFactory(repository)),
                    modifier = Modifier.fillMaxSize()
                )
            }
            composable(Routes.Revisions) {
                PlaceholderScreen(title = "Revisions")
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    authRepository = authRepository,
                    onLogout = {
                        authRepository?.logout()
                        onGoogleSignOut()
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = "$title – Coming soon",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun DestinyBottomNav(
    navController: NavController,
    currentRoute: String?
) {
    val items = listOf(
        BottomNavItem(Routes.Home, "Today", Icons.Filled.Home),
        BottomNavItem(Routes.Habits, "Habits", Icons.Filled.CheckCircle),
        BottomNavItem(Routes.Revisions, "Revisions", Icons.Filled.MenuBook),
        BottomNavItem(Routes.Settings, "Settings", Icons.Filled.Settings)
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) DestinyAccentBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = if (selected) DestinyAccentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedIconColor = DestinyAccentBlue,
                    selectedTextColor = DestinyAccentBlue,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
