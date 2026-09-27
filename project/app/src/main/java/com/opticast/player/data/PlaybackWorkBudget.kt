package com.opticast.player.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

/** Session-owned gate for optional library work. No polling, disk deletion or decoder changes. */
internal class OptionalWorkGate {
    private val active = MutableStateFlow(false)
    fun setSessionActive(value: Boolean) { active.value = value }
    suspend fun awaitIdle() { active.first { !it } }
    suspend fun awaitIdleOrPriority(priority: StateFlow<Boolean>) {
        combine(active, priority) { playing, requested -> !playing || requested }.first { it }
    }
}
internal object PlaybackWorkBudget {
    private val gate = OptionalWorkGate()
    fun setSessionActive(value: Boolean) = gate.setSessionActive(value)
    suspend fun awaitIdle() = gate.awaitIdle()
    suspend fun awaitIdleOrPriority(priority: StateFlow<Boolean>) = gate.awaitIdleOrPriority(priority)
}
