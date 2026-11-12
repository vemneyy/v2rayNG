package com.v2ray.ang.ui.compose

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.unit.Constraints

/** Owns the rendering boundary of each scroll viewport, retaining platform gesture physics. */
internal data class RasterizedOverscrollFactory(
    private val platformFactory: OverscrollFactory,
) : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect =
        RasterizedOverscrollEffect(platformFactory.createOverscrollEffect())
}

private class RasterizedOverscrollEffect(
    platformEffect: OverscrollEffect,
) : OverscrollEffect by platformEffect {
    // Android 12+ RenderNode stretch can distort glyphs and translucent primitives.
    // Rasterize the viewport INSIDE the platform effect, before it stretches the child layer.
    // Remove when edge-drag/release tests show stable glyphs and colours without this layer
    // on the affected Android/GPU versions. Never rasterize the unbounded scroll content.
    override val node: DelegatableNode = object : DelegatingNode(), LayoutModifierNode {
        init {
            delegate(platformEffect.node)
        }

        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) {
                placeable.placeWithLayer(0, 0) {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
            }
        }
    }
}
