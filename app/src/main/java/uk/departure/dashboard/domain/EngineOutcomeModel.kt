package uk.departure.dashboard.domain

/**
 * Coefficients represent value of speed and power in percents in Float representation. From 0 to 1.0
 */
data class EngineOutcomeModel(val speedCoeff: Float, val powerCoeff: Float)