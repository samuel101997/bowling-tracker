package com.bowlingtracker.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Faint alignment trapezoid + stumps marker shown before calibration. */
fun DrawScope.drawAlignGuide() {
    val gl = size.width * 0.30f; val gr = size.width * 0.70f
    val gtl = size.width * 0.42f; val gtr = size.width * 0.58f
    val top = size.height * 0.20f; val bot = size.height * 0.80f
    val guide = Color(0x66FFFFFF)
    drawLine(guide, Offset(gl, bot), Offset(gr, bot), strokeWidth = 3f)
    drawLine(guide, Offset(gr, bot), Offset(gtr, top), strokeWidth = 3f)
    drawLine(guide, Offset(gtr, top), Offset(gtl, top), strokeWidth = 3f)
    drawLine(guide, Offset(gtl, top), Offset(gl, bot), strokeWidth = 3f)
    // stumps marker at the far edge
    val cx = (gtl + gtr) / 2f; val h = size.height * 0.10f; val w = (gtr - gtl) * 0.5f
    for (k in -1..1) {
        val sx = cx + k * (w / 2f)
        drawLine(Color.White, Offset(sx, top), Offset(sx, top - h), strokeWidth = 6f)
    }
    drawLine(Color.White, Offset(cx - w / 2, top - h), Offset(cx + w / 2, top - h), strokeWidth = 4f)
}

/**
 * Draws the calibrated pitch as 4 colored length zones (Yorker/Full/Good/Short)
 * between the batsman stumps (far taps 2,3) and the near crease (taps 0,1).
 * Zone fractions approximate the standard bands over a ~20 m pitch as seen from
 * the bowler's end (most of the frame is the back two-thirds).
 */
fun DrawScope.drawPitchZones(taps: List<Offset>) {
    if (taps.size < 4) return
    val nearL = taps[0]; val nearR = taps[1]
    val farR = taps[2]; val farL = taps[3]   // batsman stumps line

    // Fractions from the batsman stumps (far) toward the bowler (near):
    // Yorker 0–1m, Full 1–3m, Good 3–6m, Short 6m+ over the ~17.7m to the crease.
    val bounds = listOf(0f, 0.06f, 0.17f, 0.34f, 1.0f)
    val colors = listOf(
        Color(0x66E0C341), // Yorker (gold)
        Color(0x6643A047), // Full (green)
        Color(0x66D32F2F), // Good (red)
        Color(0x661565C0), // Short (blue)
    )

    fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

    for (z in 0 until 4) {
        val t0 = bounds[z]; val t1 = bounds[z + 1]
        // left edge runs farL→nearL, right edge farR→nearR
        val l0 = lerp(farL, nearL, t0); val l1 = lerp(farL, nearL, t1)
        val r0 = lerp(farR, nearR, t0); val r1 = lerp(farR, nearR, t1)
        val path = Path().apply {
            moveTo(l0.x, l0.y); lineTo(r0.x, r0.y); lineTo(r1.x, r1.y); lineTo(l1.x, l1.y); close()
        }
        drawPath(path, colors[z])
    }
    // outline
    val outline = Color(0xCCFFFFFF)
    drawLine(outline, farL, farR, strokeWidth = 4f)   // batsman stumps line
    drawLine(outline, farR, nearR, strokeWidth = 3f)
    drawLine(outline, nearR, nearL, strokeWidth = 3f)
    drawLine(outline, nearL, farL, strokeWidth = 3f)
}
