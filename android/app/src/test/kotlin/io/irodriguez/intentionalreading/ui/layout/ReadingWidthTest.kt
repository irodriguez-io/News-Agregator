package io.irodriguez.intentionalreading.ui.layout

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingWidthTest {
    @Test
    fun `handsets are unchanged Given 360 dp When padding is resolved Then inset is 18 dp`() {
        assertEquals(18.dp, readingHorizontalPadding(360.dp))
    }

    @Test
    fun `handsets are unchanged Given 430 dp When padding is resolved Then inset is 18 dp`() {
        assertEquals(18.dp, readingHorizontalPadding(430.dp))
    }

    @Test
    fun `handsets are unchanged Given 599 dp When padding is resolved Then inset is 18 dp`() {
        assertEquals(18.dp, readingHorizontalPadding(599.dp))
    }

    @Test
    fun `wide screens keep a reading width Given 600 dp When padding is resolved Then inset is 24 dp`() {
        assertEquals(24.dp, readingHorizontalPadding(600.dp))
    }

    @Test
    fun `wide screens keep a reading width Given 728 dp When padding is resolved Then inset is 24 dp`() {
        assertEquals(24.dp, readingHorizontalPadding(728.dp))
    }

    @Test
    fun `wide screens keep a reading width Given 768 dp When padding is resolved Then inset is 44 dp`() {
        assertEquals(44.dp, readingHorizontalPadding(768.dp))
    }

    @Test
    fun `wide screens keep a reading width Given 1200 dp When padding is resolved Then inset is 260 dp`() {
        assertEquals(260.dp, readingHorizontalPadding(1200.dp))
    }

}
