// This file can be created but left empty for now, or merged into PinnApiClient.kt
// If you want to separate input data, you can add something like this later:

package com.example.android.architecture.blueprints.todoapp.network

/**
 * Data class for PINN input parameters (optional, can be inferred from Task)
 */
data class PinnInput(
    val conductivity: Double,
    val radius: Double,
    val depth: Double,
    val geometryFilePath: String,
    val surfaceTempFilePath: String
)