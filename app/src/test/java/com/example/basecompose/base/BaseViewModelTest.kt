package com.example.basecompose.base

import app.cash.turbine.test
import com.example.basecompose.R
import com.example.basecompose.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ---------- ViewModel giả để test các API protected của BaseViewModel ----------

    private data class TestState(val value: Int = 0) : UiState

    private sealed interface TestEvent : UiEvent {
        data class Set(val value: Int) : TestEvent
    }

    private sealed interface TestEffect : UiEffect {
        data class Toast(val text: String) : TestEffect
    }

    private class TestViewModel : BaseViewModel<TestState, TestEvent, TestEffect>(TestState()) {

        override fun onEvent(event: TestEvent) {
            when (event) {
                is TestEvent.Set -> setState { copy(value = event.value) }
            }
        }

        fun runLaunch(
            showLoading: Boolean = true,
            onError: (suspend (Throwable) -> Unit)? = null,
            block: suspend () -> Unit,
        ): Job = launch(showLoading = showLoading, onError = onError) { block() }

        fun runLatest(value: Int, delayMillis: Long) = launchLatest("key", showLoading = false) {
            delay(delayMillis)
            setState { copy(value = value) }
        }

        fun emitEffect(effect: TestEffect) = sendEffect(effect)

        fun emitMessage(text: String) = showMessage(UiText(text))

        fun <T> observe(flow: MutableSharedFlow<T>, onValue: (T) -> Unit) = flow.collectIn { onValue(it) }
    }

    // ---------- State / Event ----------

    @Test
    fun `onEvent cap nhat uiState qua setState`() {
        val viewModel = TestViewModel()

        viewModel.onEvent(TestEvent.Set(42))

        assertEquals(TestState(42), viewModel.uiState.value)
    }

    // ---------- Loading ----------

    @Test
    fun `launch bat loading khi dang chay va tat khi xong`() = runTest {
        val viewModel = TestViewModel()
        val gate = CompletableDeferred<Unit>()

        viewModel.runLaunch { gate.await() }
        assertTrue(viewModel.commonState.value.isLoading)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.commonState.value.isLoading)
    }

    @Test
    fun `nhieu launch song song - loading chi tat khi tat ca xong`() = runTest {
        val viewModel = TestViewModel()
        val first = CompletableDeferred<Unit>()
        val second = CompletableDeferred<Unit>()

        viewModel.runLaunch { first.await() }
        viewModel.runLaunch { second.await() }
        assertEquals(2, viewModel.commonState.value.loadingCount)

        first.complete(Unit)
        advanceUntilIdle()
        assertTrue("Vẫn loading vì job thứ 2 chưa xong", viewModel.commonState.value.isLoading)

        second.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.commonState.value.isLoading)
    }

    @Test
    fun `launch voi showLoading = false khong bat loading`() = runTest {
        val viewModel = TestViewModel()
        val gate = CompletableDeferred<Unit>()

        viewModel.runLaunch(showLoading = false) { gate.await() }

        assertFalse(viewModel.commonState.value.isLoading)
        gate.complete(Unit)
    }

    // ---------- Error ----------

    @Test
    fun `exception trong launch hien dialog loi va tat loading`() = runTest {
        val viewModel = TestViewModel()

        viewModel.runLaunch { throw IllegalStateException("Boom") }
        advanceUntilIdle()

        assertEquals(UiText.Dynamic("Boom"), viewModel.commonState.value.error)
        assertFalse(viewModel.commonState.value.isLoading)
    }

    @Test
    fun `IOException duoc map thanh loi mang`() = runTest {
        val viewModel = TestViewModel()

        viewModel.runLaunch { throw IOException("timeout") }
        advanceUntilIdle()

        assertEquals(UiText.Resource(R.string.error_network), viewModel.commonState.value.error)
    }

    @Test
    fun `onError rieng duoc goi thay cho xu ly mac dinh`() = runTest {
        val viewModel = TestViewModel()
        var handled: Throwable? = null

        viewModel.runLaunch(onError = { handled = it }) { throw IllegalStateException("Boom") }
        advanceUntilIdle()

        assertEquals("Boom", handled?.message)
        assertNull(viewModel.commonState.value.error)
    }

    @Test
    fun `dismissError xoa loi`() = runTest {
        val viewModel = TestViewModel()
        viewModel.runLaunch { throw IllegalStateException("Boom") }
        advanceUntilIdle()

        viewModel.dismissError()

        assertNull(viewModel.commonState.value.error)
    }

    // ---------- Cancellation ----------

    @Test
    fun `huy job khong bi coi la loi va van tat loading`() = runTest {
        val viewModel = TestViewModel()

        val job = viewModel.runLaunch { CompletableDeferred<Unit>().await() }
        assertTrue(viewModel.commonState.value.isLoading)

        job.cancel()
        advanceUntilIdle()

        assertNull(viewModel.commonState.value.error)
        assertFalse(viewModel.commonState.value.isLoading)
    }

    @Test
    fun `launchLatest huy job truoc cung key`() = runTest {
        val viewModel = TestViewModel()

        val first = viewModel.runLatest(value = 1, delayMillis = 1_000)
        viewModel.runLatest(value = 2, delayMillis = 1_000)
        advanceUntilIdle()

        assertTrue(first.isCancelled)
        assertEquals(2, viewModel.uiState.value.value)
        assertNull(viewModel.commonState.value.error)
    }

    // ---------- Effect / Message ----------

    @Test
    fun `effect duoc gui toi UI dung mot lan`() = runTest {
        val viewModel = TestViewModel()

        viewModel.effect.test {
            viewModel.emitEffect(TestEffect.Toast("hi"))
            assertEquals(TestEffect.Toast("hi"), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `effect gui khi UI chua collect van duoc giu lai`() = runTest {
        val viewModel = TestViewModel()

        viewModel.emitEffect(TestEffect.Toast("a"))
        viewModel.emitEffect(TestEffect.Toast("b"))

        viewModel.effect.test {
            assertEquals(TestEffect.Toast("a"), awaitItem())
            assertEquals(TestEffect.Toast("b"), awaitItem())
        }
    }

    @Test
    fun `showMessage phat ra snackbar message`() = runTest {
        val viewModel = TestViewModel()

        viewModel.messages.test {
            viewModel.emitMessage("Saved")
            assertEquals(UiText.Dynamic("Saved"), awaitItem())
        }
    }

    // ---------- collectIn ----------

    @Test
    fun `collectIn nhan gia tri tu flow`() = runTest {
        val viewModel = TestViewModel()
        val source = MutableSharedFlow<Int>()
        val received = mutableListOf<Int>()

        viewModel.observe(source) { received += it }
        source.emit(1)
        source.emit(2)

        assertEquals(listOf(1, 2), received)
        assertFalse(viewModel.commonState.value.isLoading)
    }
}
