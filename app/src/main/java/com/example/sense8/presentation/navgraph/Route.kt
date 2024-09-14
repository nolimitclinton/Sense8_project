package com.example.sense8.presentation.navgraph

import com.example.sense8.presentation.utils.Routes

/**
 * Routing objects for each UI screen for NavGraph
 */
sealed class Route(
    val route: String
) {
    // Starting point
    object AppStartNavigation: Route(route = Routes.ROUTE_APP_START_NAVIGATION)
    object HomeNavigation: Route(route = Routes.ROUTE_HOME_NAVIGATION)

    object OnBoardingScreen: Route(route = Routes.ROUTE_ONBOARDING_SCREEN)
    object HomeScreen: Route(route = Routes.ROUTE_HOME_SCREEN)
    object NavigationScreen: Route(route = Routes.ROUTE_NAVIGATION_SCREEN)
}
