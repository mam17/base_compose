package com.example.basecompose.base

import com.example.basecompose.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LoadStateTest {

    @Test
    fun `loadState tra ve Success khi block thanh cong`() = runTest {
        val result = loadState { 42 }

        assertEquals(LoadState.Success(42), result)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `loadState tra ve Error khi block nem exception`() = runTest {
        val result = loadState<Int> { throw IOException() }

        assertTrue(result is LoadState.Error)
        assertEquals(UiText.Resource(R.string.error_network), (result as LoadState.Error).message)
        assertNull(result.getOrNull())
    }

    @Test(expected = CancellationException::class)
    fun `loadState nem lai CancellationException`() = runTest {
        loadState<Int> { throw CancellationException() }
    }

    @Test
    fun `toUiText dung message cua exception, fallback ve loi chung`() {
        assertEquals(UiText.Dynamic("Boom"), IllegalStateException("Boom").toUiText())
        assertEquals(UiText.Resource(R.string.error_unknown), IllegalStateException().toUiText())
        assertEquals(UiText.Resource(R.string.error_unknown), IllegalStateException(" ").toUiText())
    }
}
