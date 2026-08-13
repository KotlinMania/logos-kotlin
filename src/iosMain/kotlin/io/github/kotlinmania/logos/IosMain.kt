// Empty marker for the iosMain intermediate source set so the Kotlin
// Multiplatform metadata compile task has input rather than reporting
// NO-SOURCE. All implementation lives in commonMain; this file contributes
// only a package directive so no symbol is added to the published .klib's
// linkdata.
package io.github.kotlinmania.logos

internal const val IosMain_MARKER = "logos.IosMain"
