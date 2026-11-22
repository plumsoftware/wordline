package ru.plumsoftware.wordline.ui.game

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Extra clearance for controls only; scenic backgrounds still reach the screen edges. */
@Composable
internal fun gameHeaderClearance(): Dp {
    val density=LocalDensity.current
    val cutout=with(density) {WindowInsets.displayCutout.getTop(this).toDp()}
    // Screen headers already provide around 12 dp; add only the missing clearance and a small gap.
    return (cutout-12.dp).coerceAtLeast(0.dp)+6.dp
}
