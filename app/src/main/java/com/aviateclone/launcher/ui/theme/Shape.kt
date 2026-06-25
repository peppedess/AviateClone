package com.aviateclone.launcher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Scala di forme Material 3 Expressive: arrotondamenti generosi e i nuovi
// livelli "increased" introdotti con M3 Expressive, per superfici morbide
// e geometriche tipiche del design 2025/2026.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val AviateShapes = Shapes(
    extraSmall          = RoundedCornerShape(8.dp),
    small               = RoundedCornerShape(12.dp),
    medium              = RoundedCornerShape(18.dp),
    large               = RoundedCornerShape(24.dp),
    extraLarge          = RoundedCornerShape(32.dp),
    largeIncreased      = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(36.dp),
    extraExtraLarge     = RoundedCornerShape(48.dp)
)
