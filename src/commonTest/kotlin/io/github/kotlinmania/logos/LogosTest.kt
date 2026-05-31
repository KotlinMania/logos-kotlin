// port-lint: source src/lib.rs
package io.github.kotlinmania.logos

/*
 * Copyright (c) Maciej Hirsz, logos contributors.
 * Licensed under either of Apache-2.0 OR MIT.
 */

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

// Tests for the Logos.kt runtime types. Upstream's tests on these types live behind the
// `#[derive(Logos)]` macro; the Kotlin port exposes the underlying sealed types directly,
// so the contract for `Skip`, `Filter<T>`, `FilterResult<T, E>`, and the predefined
// `skip()` callback is exercised in commonTest at the type-shape level.

class SkipTest {
    // Type that can be returned from a callback, informing the Lexer to skip the current token
    // match. Upstream Rust models this as `pub struct Skip;` (a zero-sized unit type).
    @Test
    fun skipSingletonIsStable() {
        // The companion `SKIP` is the canonical singleton handed back by the `skip()` predefined
        // callback; it must not be replaced by a fresh instance on every call.
        assertSame(Skip.SKIP, Skip.SKIP)
    }

    @Test
    fun predefinedSkipCallbackReturnsSingleton() {
        assertSame(Skip.SKIP, skip())
    }
}

class FilterTest {
    @Test
    fun emitCarriesValue() {
        val emit: Filter<Int> = Filter.Emit(42)
        assertTrue(emit is Filter.Emit)
        assertEquals(42, emit.value)
    }

    @Test
    fun skipCarriesNoValue() {
        val s: Filter<Int> = Filter.Skip()
        assertTrue(s is Filter.Skip)
    }

    @Test
    fun emitAndSkipAreDistinctBranches() {
        val emit: Filter<Int> = Filter.Emit(7)
        val s: Filter<Int> = Filter.Skip()
        check(emit !is Filter.Skip)
        check(s !is Filter.Emit)
    }
}

class FilterResultTest {
    // Upstream:
    //   pub enum FilterResult<T, E> { Emit(T), Skip, Error(E) }
    @Test
    fun emitCarriesValue() {
        val r: FilterResult<Int, String> = FilterResult.Emit(20)
        assertTrue(r is FilterResult.Emit)
        assertEquals(20, r.value)
    }

    @Test
    fun skipCarriesNoValue() {
        val r: FilterResult<Int, String> = FilterResult.Skip()
        assertTrue(r is FilterResult.Skip)
    }

    @Test
    fun errorCarriesErrorValue() {
        val r: FilterResult<Int, String> = FilterResult.Error("bad number")
        assertTrue(r is FilterResult.Error)
        assertEquals("bad number", r.error)
    }

    @Test
    fun whenExpressionExhaustsAllThreeBranches() {
        // Exercise that the sealed hierarchy is exhaustive in `when`: this would fail to compile
        // (or warn-as-error under allWarningsAsErrors) if a branch were missing.
        val cases: List<FilterResult<Int, String>> = listOf(
            FilterResult.Emit(1),
            FilterResult.Skip(),
            FilterResult.Error("oops"),
        )

        val labels = cases.map { case ->
            when (case) {
                is FilterResult.Emit -> "emit"
                is FilterResult.Skip -> "skip"
                is FilterResult.Error -> "error"
            }
        }
        assertEquals(listOf("emit", "skip", "error"), labels)
    }
}

class SourceSliceTypeTest {
    // Upstream `Logos::Source` is associated-type-driven; the Kotlin port models the same
    // distinction with the `SourceSliceType` enum so tokens declare whether their slice type
    // is `String` (UTF-8 source) or `ByteArray` (binary source).
    @Test
    fun strAndBytesAreDistinctEntries() {
        check(SourceSliceType.STR != SourceSliceType.BYTES)
    }

    @Test
    fun enumHasExactlyTwoEntries() {
        assertEquals(2, SourceSliceType.entries.size)
    }
}
