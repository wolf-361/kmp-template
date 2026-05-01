package com.yourcompany.kmptemplate.core.presentation

import com.yourcompany.kmptemplate.core.data.network.GlobalUiEffect
import com.yourcompany.kmptemplate.core.data.network.GlobalUiEffectsHandler
import com.yourcompany.kmptemplate.core.navigation.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseViewModel<State, Action, Effect>(initialState: State) : KoinComponent {

    val viewModelScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val globalEffectsHandler: GlobalUiEffectsHandler by inject()

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<Effect>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<Effect> = _effects.asSharedFlow()

    abstract fun onAction(action: Action)

    protected fun setState(update: State.() -> State) {
        _state.value = _state.value.update()
    }

    protected fun emitEffect(effect: Effect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    protected fun navigateTo(destination: Destination) {
        viewModelScope.launch { globalEffectsHandler.emit(GlobalUiEffect.NavigateTo(destination)) }
    }

    protected fun navigateBack() {
        viewModelScope.launch { globalEffectsHandler.emit(GlobalUiEffect.NavigateBack) }
    }

    protected fun navigateBackTo(destination: Destination, inclusive: Boolean = false) {
        viewModelScope.launch {
            globalEffectsHandler.emit(GlobalUiEffect.NavigateBackTo(destination, inclusive))
        }
    }

    open fun onCleared() {
        viewModelScope.cancel()
    }
}
