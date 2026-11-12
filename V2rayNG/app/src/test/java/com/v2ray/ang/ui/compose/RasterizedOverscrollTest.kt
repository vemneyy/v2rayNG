package com.v2ray.ang.ui.compose

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RasterizedOverscrollTest {
    private class PlatformEffect : OverscrollEffect {
        override val node: DelegatableNode = object : Modifier.Node() {}
        override var isInProgress = false
        var scrollCalls = 0
        var flingCalls = 0
        override fun applyToScroll(
            delta: Offset,
            source: NestedScrollSource,
            performScroll: (Offset) -> Offset,
        ): Offset {
            scrollCalls++
            return performScroll(delta)
        }
        override suspend fun applyToFling(
            velocity: Velocity,
            performFling: suspend (Velocity) -> Velocity,
        ) {
            flingCalls++
            performFling(velocity)
        }
    }

    private data class PlatformFactory(val effect: OverscrollEffect) : OverscrollFactory {
        override fun createOverscrollEffect() = effect
    }

    @Test
    fun wrapperPreservesScrollConsumptionAndPlatformProgress() {
        val platform = PlatformEffect()
        val wrapped = RasterizedOverscrollFactory(PlatformFactory(platform)).createOverscrollEffect()
        var calls = 0
        val result = wrapped.applyToScroll(Offset(0f, -30f), NestedScrollSource.UserInput) {
            calls++
            assertEquals(Offset(0f, -30f), it)
            Offset(0f, -10f)
        }
        assertEquals(Offset(0f, -10f), result)
        assertEquals(1, calls)
        assertEquals(1, platform.scrollCalls)
        assertEquals(false, wrapped.isInProgress)
        platform.isInProgress = true
        assertTrue(wrapped.isInProgress)
        assertNotSame(platform.node, wrapped.node)
    }

    @Test
    fun wrapperPreservesFlingVelocityAndCallsCallbackOnce() = runBlocking {
        val platform = PlatformEffect()
        val wrapped = RasterizedOverscrollFactory(PlatformFactory(platform)).createOverscrollEffect()
        var calls = 0
        wrapped.applyToFling(Velocity(0f, 100f)) {
            calls++
            assertEquals(Velocity(0f, 100f), it)
            Velocity.Zero
        }
        assertEquals(1, calls)
        assertEquals(1, platform.flingCalls)
    }
}
