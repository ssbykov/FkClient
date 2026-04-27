package ru.faserkraft.client.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Собрать значения из StateFlow с учетом жизненного цикла Fragment/Activity
 * Автоматически отписывается когда Fragment/Activity разрушается
 *
 * @param lifecycleOwner Владелец жизненного цикла (Fragment/Activity)
 * @param state Минимальное состояние жизненного цикла для сбора (по умолчанию STARTED)
 * @param action Действие которое выполняется для каждого значения
 */
fun <T> StateFlow<T>.collectIn(
    lifecycleOwner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: suspend (T) -> Unit
) {
    var job: Job? = null

    val observer = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onStart() {
            if (state == Lifecycle.State.STARTED) {
                job = lifecycleOwner.lifecycleScope.launch {
                    collect { action(it) }
                }
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResume() {
            if (state == Lifecycle.State.RESUMED) {
                job = lifecycleOwner.lifecycleScope.launch {
                    collect { action(it) }
                }
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            job?.cancel()
        }
    }

    lifecycleOwner.lifecycle.addObserver(observer)
}

/**
 * Упростить collectIn - использовать простой launch
 */
fun <T> StateFlow<T>.collectInSimple(
    lifecycleOwner: LifecycleOwner,
    action: suspend (T) -> Unit
) {
    lifecycleOwner.lifecycleScope.launch {
        collect { action(it) }
    }
}

/**
 * Собрать события из SharedFlow с учетом жизненного цикла
 * Используется для одноразовых событий (ошибки, навигация)
 *
 * @param lifecycleOwner Владелец жизненного цикла
 * @param state Минимальное состояние жизненного цикла
 * @param action Действие для каждого события
 */
fun <T> SharedFlow<T>.collectEventsIn(
    lifecycleOwner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    action: (T) -> Unit
) {
    var job: Job? = null

    val observer = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onStart() {
            if (state == Lifecycle.State.STARTED) {
                job = lifecycleOwner.lifecycleScope.launch {
                    collect { action(it) }
                }
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResume() {
            if (state == Lifecycle.State.RESUMED) {
                job = lifecycleOwner.lifecycleScope.launch {
                    collect { action(it) }
                }
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            job?.cancel()
        }
    }

    lifecycleOwner.lifecycle.addObserver(observer)
}
