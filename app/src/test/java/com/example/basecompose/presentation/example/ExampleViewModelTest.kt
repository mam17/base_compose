package com.example.basecompose.presentation.example

import app.cash.turbine.test
import com.example.basecompose.base.LoadState
import com.example.basecompose.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Mẫu test cho ViewModel của một màn: gửi Event -> kiểm tra State / Effect. */
@OptIn(ExperimentalCoroutinesApi::class)
class ExampleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `khoi tao tu dong tai danh sach voi loading toan man`() = runTest {
        val viewModel = ExampleViewModel()
        assertTrue(viewModel.commonState.value.isLoading)

        advanceUntilIdle()

        assertEquals(20, viewModel.uiState.value.items.getOrNull()?.size)
        assertEquals(false, viewModel.commonState.value.isLoading)
    }

    @Test
    fun `go tim kiem lien tuc chi request lan cuoi`() = runTest {
        val viewModel = ExampleViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ExampleEvent.QueryChanged("1"))
        advanceTimeBy(100)
        viewModel.onEvent(ExampleEvent.QueryChanged("10"))
        advanceUntilIdle()

        assertEquals("10", viewModel.uiState.value.query)
        assertEquals(LoadState.Success(listOf("Item 10")), viewModel.uiState.value.items)
    }

    @Test
    fun `click item gui effect mo chi tiet`() = runTest {
        val viewModel = ExampleViewModel()

        viewModel.effect.test {
            viewModel.onEvent(ExampleEvent.ItemClicked("Item 3"))
            assertEquals(ExampleEffect.OpenDetail("Item 3"), awaitItem())
        }
    }
}
