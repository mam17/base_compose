package com.example.basecompose.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Thay `Dispatchers.Main` (mà `viewModelScope` dùng) bằng TestDispatcher trong unit test.
 *
 * `runTest` tự dùng chung scheduler với dispatcher này, nên `advanceUntilIdle()`,
 * `advanceTimeBy()`, `delay()`... điều khiển được cả coroutine trong ViewModel.
 *
 * ```kotlin
 * @get:Rule val mainDispatcherRule = MainDispatcherRule()
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
