package io.github.mipmip.specgettyondroid.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.mipmip.specgettyondroid.auth.DeviceFlow
import io.github.mipmip.specgettyondroid.auth.Installations
import io.github.mipmip.specgettyondroid.data.ProjectRepository
import io.github.mipmip.specgettyondroid.nav.Destinations
import io.github.mipmip.specgettyondroid.ui.screen.AuthorizeScreen
import io.github.mipmip.specgettyondroid.ui.screen.ChangeScreen
import io.github.mipmip.specgettyondroid.ui.screen.DeltaScreen
import io.github.mipmip.specgettyondroid.ui.screen.ProjectScreen
import io.github.mipmip.specgettyondroid.ui.screen.RepoListScreen
import io.github.mipmip.specgettyondroid.ui.screen.ScannerScreen
import io.github.mipmip.specgettyondroid.ui.screen.SpecScreen
import io.github.mipmip.specgettyondroid.viewmodel.AuthorizeViewModel
import io.github.mipmip.specgettyondroid.viewmodel.ChangeViewModel
import io.github.mipmip.specgettyondroid.viewmodel.DeltaViewModel
import io.github.mipmip.specgettyondroid.viewmodel.ProjectViewModel
import io.github.mipmip.specgettyondroid.viewmodel.RepoListViewModel
import io.github.mipmip.specgettyondroid.viewmodel.SpecViewModel

@Composable
fun SpecgettyNavHost(
    projects: ProjectRepository,
    deviceFlow: DeviceFlow,
    installations: Installations,
    sharedText: String? = null,
    onSharedTextHandled: () -> Unit = {},
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Destinations.REPO_LIST) {
        composable(Destinations.REPO_LIST) {
            val model: RepoListViewModel =
                viewModel(factory = RepoListViewModel.factory(projects))

            // A share fills the add form and clones nothing, which is the same
            // path a scan takes.
            LaunchedEffect(sharedText) {
                sharedText?.let {
                    model.capture(it)
                    onSharedTextHandled()
                }
            }

            RepoListScreen(
                viewModel = model,
                onOpenRepo = { navController.navigate(Destinations.project(it)) },
                onScan = { navController.navigate(Destinations.SCANNER) },
                onAuthorize = { navController.navigate(Destinations.authorize(it)) },
            )
        }

        composable(Destinations.SCANNER) { entry ->
            // Keyed on this entry: the scanner shares the repository list's view
            // model so a scanned URL lands in the form the list already owns.
            val parent = remember(entry) {
                navController.getBackStackEntry(Destinations.REPO_LIST)
            }
            val model: RepoListViewModel =
                viewModel(viewModelStoreOwner = parent, factory = RepoListViewModel.factory(projects))
            ScannerScreen(
                onResult = {
                    model.openForm(it)
                    navController.popBackStack()
                },
                onDismiss = { navController.popBackStack() },
            )
        }

        composable(
            route = Destinations.AUTHORIZE_PATTERN,
            arguments = listOf(
                navArgument(Destinations.AUTHORIZE_ARG_URL) { type = NavType.StringType },
            ),
        ) { entry ->
            val repoUrl = Destinations.decode(
                entry.arguments?.getString(Destinations.AUTHORIZE_ARG_URL).orEmpty(),
            )
            // The list's own view model, so the credential lands in the form
            // that is still open behind this screen.
            val parent = remember(entry) {
                navController.getBackStackEntry(Destinations.REPO_LIST)
            }
            val list: RepoListViewModel =
                viewModel(viewModelStoreOwner = parent, factory = RepoListViewModel.factory(projects))
            val model: AuthorizeViewModel = viewModel(
                factory = AuthorizeViewModel.factory(deviceFlow, installations, repoUrl),
            )

            AuthorizeScreen(
                viewModel = model,
                onAuthorized = {
                    list.onAuthorized(it)
                    navController.popBackStack()
                },
                onDismiss = { navController.popBackStack() },
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
            val repoId = Destinations.decode(
                entry.arguments?.getString(Destinations.DELTA_ARG_REPO).orEmpty(),
            )
            val name = Destinations.decode(
                entry.arguments?.getString(Destinations.DELTA_ARG_NAME).orEmpty(),
            )
            val model: DeltaViewModel =
                viewModel(factory = DeltaViewModel.factory(projects, repoId, name))
            DeltaScreen(viewModel = model, onBack = { navController.popBackStack() })
        }

        composable(
            route = Destinations.SPEC_PATTERN,
            arguments = listOf(
                navArgument(Destinations.SPEC_ARG_REPO) { type = NavType.StringType },
                navArgument(Destinations.SPEC_ARG_CAPABILITY) { type = NavType.StringType },
            ),
        ) { entry ->
            val repoId = Destinations.decode(
                entry.arguments?.getString(Destinations.SPEC_ARG_REPO).orEmpty(),
            )
            val capability = Destinations.decode(
                entry.arguments?.getString(Destinations.SPEC_ARG_CAPABILITY).orEmpty(),
            )
            val model: SpecViewModel =
                viewModel(factory = SpecViewModel.factory(projects, repoId, capability))
            SpecScreen(viewModel = model, onBack = { navController.popBackStack() })
        }
    }
}
