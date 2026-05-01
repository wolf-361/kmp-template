package com.yourcompany.kmptemplate.core.navigation

import kotlinx.serialization.Serializable

sealed interface Destination

sealed interface AuthDestination : Destination {
    @Serializable data object Login : AuthDestination
    @Serializable data object Register : AuthDestination
}

sealed interface TopLevelGraph : Destination {
    @Serializable data object Dashboard : TopLevelGraph
    @Serializable data object Settings : TopLevelGraph
}
