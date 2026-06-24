package com.bowlingtracker.engine.analysis

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * Known-answer test mirroring the Python segmentation prototype: a 60s session
 * at 30fps with 8 motion bursts on a low-noise baseline must yield 8 segments.
 */
class SessionSegmentationTest {

    @Test fun `segments eight bursts`() {
        val fps = 30.0
        val n = (fps * 60).toInt()
        val rnd = Random(1)
        val motion = DoubleArray(n) { rnd.nextDouble(0.0, 2.0) }
        listOf(3, 10, 18, 25, 33, 41, 49, 55).forEach { bs ->
            for (k in 0 until (0.8 * fps).toInt()) {
                val idx = (bs * fps).toInt() + k
                if (idx < n) motion[idx] = rnd.nextDouble(8.0, 12.0)
            }
        }
        val segs = DeliverySegmenter.segment(motion.toList(), fps)
        assertEquals(8, segs.size)
    }

    @Test fun `empty motion yields no segments`() {
        assertEquals(0, DeliverySegmenter.segment(emptyList(), 30.0).size)
    }
}
