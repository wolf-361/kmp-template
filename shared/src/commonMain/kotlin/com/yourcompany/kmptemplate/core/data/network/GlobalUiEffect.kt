package com.yourcompany.kmptemplate.core.data.network

import com.yourcompany.kmptemplate.core.navigation.Destination
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface GlobalUiEffect {
    data class NavigateTo(val destination: Destination) : GlobalUiEffect
    data object NavigateBack : GlobalUiEffect
    data class NavigateBackTo(val destination: Destination, val inclusive: Boolean) : GlobalUiEffect
    data class ShowToast(val message: String) : GlobalUiEffect
    data class ShowSnackbar(val message: String) : GlobalUiEffect

    // Emitted by NetworkClientImpl when token refresh fails — navigates to Login and clears back stack
    data object Unauthorized : GlobalUiEffect
}

class GlobalUiEffectsHandler {
    private val _effects = MutableSharedFlow<GlobalUiEffect>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<GlobalUiEffect> = _effects.asSharedFlow()

    suspend fun emit(effect: GlobalUiEffect) = _effects.emit(effect)
}
