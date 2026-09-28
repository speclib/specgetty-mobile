package io.github.mipmip.specgettyondroid.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.mipmip.specgettyondroid.nav.Destinations
import io.github.mipmip.specgettyondroid.ui.screen.RepoListScreen

@Composable
fun SpecgettyNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Destinations.REPO_LIST) {
        composable(Destinations.REPO_LIST) { RepoListScreen() }
    }
}
