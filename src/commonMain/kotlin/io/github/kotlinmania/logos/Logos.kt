// port-lint: source src/lib.rs
package io.github.kotlinmania.logos

/*
 * Copyright (c) Maciej Hirsz, logos contributors.
 * Licensed under either of Apache-2.0 OR MIT.
 */

// # Logos
//
// _Create ridiculously fast Lexers._
//
// **Logos** has two goals:
//
// + To make it easy to create a Lexer, so you can focus on more complex problems.
// + To make the generated Lexer faster than anything you'd write by hand.
//
// To achieve those, **Logos**:
//
// + Combines all token definitions into a single deterministic state machine.
// + Optimizes branches into lookup tables or jump tables.
// + Prevents backtracking inside token definitions.
// + Unwinds loops, and batches reads to minimize bounds checking.
// + Does all of that heavy lifting at lexer-build time.

import io.github.kotlinmania.logos.lexer.Lexer
import io.github.kotlinmania.logos.source.Source

/**
 * Trait implemented for an enum representing all tokens. You should never have to implement it
 * manually; use the `Logos` derive on your enum.
 *
 * The type parameter `E` is the error type associated with this token (defaults to [Unit] when
 * not customised).
 */
interface Logos<E> {
    /** The source slice type the lexer reads from. */
    val sourceSliceType: SourceSliceType

    /** Construct the default error value (the analog of a no-arg default constructor). */
    fun defaultError(): E
}

/** Marker for the kind of source the lexer expects (string vs. byte-array). */
enum class SourceSliceType {
    /** UTF-8 string source; slices are [String]. */
    STR,
    /** Binary byte source; slices are [ByteArray]. */
    BYTES,
}

/** Per-token lexer definition: lexer construction plus the `lex` step the [Lexer] drives. */
interface LexerDefinition<TToken : Logos<E>, TSlice, E> {
    /**
     * Create a new instance of a [Lexer] that will produce tokens implementing this [Logos],
     * using the default extras value.
     */
    fun lexer(source: Source<TSlice>): Lexer<TToken, TSlice, E, Unit> = lexerWithExtras(source, Unit)

    /**
     * Create a new instance of a [Lexer] with the provided extras that will produce tokens
     * implementing this [Logos].
     */
    fun <Extras> lexerWithExtras(source: Source<TSlice>, extras: Extras): Lexer<TToken, TSlice, E, Extras>

    /** The heart of Logos. Called by the [Lexer]. */
    fun <Extras> lex(lexer: Lexer<TToken, TSlice, E, Extras>): Result<TToken>?
}

/**
 * Type that can be returned from a callback, informing the [Lexer] to skip the current token match.
 *
 * See also [skip].
 */
class Skip {
    companion object {
        val SKIP: Skip = Skip()
    }
}

/**
 * Type that can be returned from a callback, either producing a field for a token,
 * or skipping it.
 */
sealed class Filter<T> {
    /** Emit a token with a given value `T`. Use [Unit] for unit variants without fields. */
    class Emit<T>(val value: T) : Filter<T>()

    /** Skip current match, analog to [Skip]. */
    class Skip<T> : Filter<T>()
}

/**
 * Type that can be returned from a callback, either producing a field for a token,
 * skipping it, or emitting an error.
 */
sealed class FilterResult<T, E> {
    /** Emit a token with a given value `T`. Use [Unit] for unit variants without fields. */
    class Emit<T, E>(val value: T) : FilterResult<T, E>()

    /** Skip current match, analog to [Skip]. */
    class Skip<T, E> : FilterResult<T, E>()

    /** Emit the token's [Logos] error variant. */
    class Error<T, E>(val error: E) : FilterResult<T, E>()
}

/** Predefined callback that will inform the [Lexer] to skip a definition. */
fun skip(): Skip = Skip.SKIP
