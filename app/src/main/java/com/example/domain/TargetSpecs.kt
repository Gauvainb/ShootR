package com.example.domain

/**
 * Physical dimensions of the supported targets.
 *
 * @property blackVisualDiameterMm diameter of the black visual; 0 when the target has none
 * @property cardSizeMm side of the (square) target card, or null when unknown
 */
data class TargetSpec(
    val blackVisualDiameterMm: Float,
    val cardSizeMm: Float?
)

object TargetSpecs {
    /** Returns null when the target type has no known dimensions. */
    fun forTargetType(targetType: String): TargetSpec? = when {
        targetType.contains("Carabine 10m") -> TargetSpec(30.5f, 80f)
        targetType.contains("Pistolet 10m") -> TargetSpec(59.5f, 170f)
        targetType.contains("C200") -> TargetSpec(400f, 800f)
        targetType.contains("C50") -> TargetSpec(200f, 500f)
        targetType.contains("IPSC") -> TargetSpec(0f, null)
        else -> null
    }
}

object Caliber {
    private val number = Regex("""\d*\.\d+|\d+""")

    /**
     * Bullet diameter in millimeters read from a free-form caliber label, or null when it cannot be
     * determined. Examples: ".22 LR" -> 5.6, "9x19 mm" -> 9.0, "4.5 mm (.177)" -> 4.5,
     * "5.56x45 mm / .223 Rem" -> 5.56, "38 Special" -> 9.7.
     */
    fun diameterMm(label: String): Float? {
        val match = number.find(label) ?: return null
        val token = match.value
        val value = token.toFloatOrNull() ?: return null
        val rest = label.substring(match.range.last + 1).trimStart().lowercase()
        val metric = rest.startsWith("mm") || rest.startsWith("x")

        val mm = when {
            token.startsWith(".") -> value * 25.4f              // .22, .308: decimal inches
            metric -> value                                     // 9x19, 4.5 mm
            '.' in token -> value                               // 7.62, 5.56
            value in 17f..99f -> value / 100f * 25.4f           // 22 LR, 38 Special, 45 ACP
            value in 100f..999f -> value / 1000f * 25.4f        // 357 Magnum, 308 Win
            else -> value
        }
        return mm.takeIf { it in 3f..20f }
    }
}
