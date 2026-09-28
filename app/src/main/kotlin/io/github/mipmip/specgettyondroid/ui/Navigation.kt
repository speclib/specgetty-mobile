package io.github.mipmip.specgettyondroid.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.nav.Destinations
import io.github.mipmip.specgettyondroid.ui.screen.ChangeScreen
import io.github.mipmip.specgettyondroid.ui.screen.NotBuiltYetScreen
import io.github.mipmip.specgettyondroid.ui.screen.ProjectScreen
import io.github.mipmip.specgettyondroid.ui.screen.RepoListScreen
import io.github.mipmip.specgettyondroid.viewmodel.ChangeViewModel
import io.github.mipmip.specgettyondroid.viewmodel.ProjectViewModel
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

        composable(
            route = Destinations.PROJECT_PATTERN,
            arguments = listOf(navArgument(Destinations.PROJECT_ARG_REPO) { type = NavType.StringType }),
        ) { entry ->
            val repoId = Destinations.decode(
                entry.arguments?.getString(Destinations.PROJECT_ARG_REPO).orEmpty(),
            )
            val model: ProjectViewModel =
                viewModel(factory = ProjectViewModel.factory(projects, repoId))

            val repos by projects.repos.collectAsStateWithLifecycle(
                initialValue = io.github.mipmip.specgettyondroid.store.RepoList(),
            )
            val label = repos.repos.firstOrNull { it.id == repoId }?.label ?: "Project"

            ProjectScreen(
                viewModel = model,
                label = label,
                onBack = { navController.popBackStack() },
                onOpenChange = { navController.navigate(Destinations.change(repoId, it.dir.name)) },
                onOpenSpec = { navController.navigate(Destinations.spec(repoId, it)) },
            )
        }

        composable(
            route = Destinations.CHANGE_PATTERN,
            arguments = listOf(
                navArgument(Destinations.CHANGE_ARG_REPO) { type = NavType.StringType },
                navArgument(Destinations.CHANGE_ARG_NAME) { type = NavType.StringType },
            ),
        ) { entry ->
            val repoId = Destinations.decode(
                entry.arguments?.getString(Destinations.CHANGE_ARG_REPO).orEmpty(),
            )
            val name = Destinations.decode(
                entry.arguments?.getString(Destinations.CHANGE_ARG_NAME).orEmpty(),
            )
            val model: ChangeViewModel =
                viewModel(factory = ChangeViewModel.factory(projects, repoId, name))
            ChangeScreen(
                viewModel = model,
                onBack = { navController.popBackStack() },
                onOpenDelta = { navController.navigate(Destinations.delta(repoId, name)) },
            )
        }

        composable(
            route = Destinations.DELTA_PATTERN,
            arguments = listOf(
                navArgument(Destinations.DELTA_ARG_REPO) { type = NavType.StringType },
                navArgument(Destinations.DELTA_ARG_NAME) { type = NavType.StringType },
            ),
        ) { entry ->
            val name = Destinations.decode(
                entry.arguments?.getString(Destinations.DELTA_ARG_NAME).orEmpty(),
            )
            NotBuiltYetScreen(
                title = name,
                what = "Comparing a change's spec deltas arrives next.",
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Destinations.SPEC_PATTERN,
            arguments = listOf(
                navArgument(Destinations.SPEC_ARG_REPO) { type = NavType.StringType },
                navArgument(Destinations.SPEC_ARG_CAPABILITY) { type = NavType.StringType },
            ),
        ) { entry ->
            val capability = Destinations.decode(
                entry.arguments?.getString(Destinations.SPEC_ARG_CAPABILITY).orEmpty(),
            )
            NotBuiltYetScreen(
                title = capability,
                what = "Reading a spec arrives with milestone 08.",
                onBack = { navController.popBackStack() },
            )
        }
    }
}
