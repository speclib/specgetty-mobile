package io.github.mipmip.specgettyondroid.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.nav.Destinations
import io.github.mipmip.specgettyondroid.ui.screen.RepoListScreen
import io.github.mipmip.specgettyondroid.viewmodel.RepoListViewModel

@Composable
fun SpecgettyNavHost(projects: ProjectRepository) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Destinations.REPO_LIST) {
        composable(Destinations.REPO_LIST) {
            val model: RepoListViewModel =
                viewModel(factory = RepoListViewModel.factory(projects))
            RepoListScreen(
                viewModel = model,
                onOpenRepo = { navController.navigate(Destinations.project(it)) },
            )
        }
    }
}
