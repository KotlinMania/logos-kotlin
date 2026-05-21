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
// + Combines all token definitions into a single deterministic state machine
//   (https://en.wikipedia.org/wiki/Deterministic_finite_automaton).
// + Optimizes branches into lookup tables (https://en.wikipedia.org/wiki/Lookup_table)
//   or jump tables (https://en.wikipedia.org/wiki/Branch_table).
// + Prevents backtracking (https://en.wikipedia.org/wiki/ReDoS) inside token definitions.
// + Unwinds loops (https://en.wikipedia.org/wiki/Loop_unrolling), and batches reads
//   to minimize bounds checking.
// + Does all of that heavy lifting at lexer-build time.
//
// See the Logos handbook (https://maciejhirsz.github.io/logos/) for additional
// documentation and usage examples.

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
 * Type that can be returned from a callback, informing the [Lexer], to skip
 * current token match. See also [skip].
 *
 * # Example
 *
 * ```kotlin
 * sealed class Token : Logos<Unit> {
 *     // We will treat "abc" as if it was whitespace.
 *     // This is identical to using `skip`.
 *     // #[regex(" |abc", { Skip() }, priority = 3)]
 *     object Ignored : Token()
 *
 *     // #[regex("[a-zA-Z]+")]
 *     data class Text(val value: String) : Token()
 * }
 *
 * val tokens: List<Result<Token>> = Token.lexer("Hello abc world").toList()
 *
 * check(
 *     tokens == listOf(
 *         Result.success(Token.Text("Hello")),
 *         Result.success(Token.Text("world")),
 *     ),
 * )
 * ```
 */
class Skip {
    companion object {
        val SKIP: Skip = Skip()
    }
}

/**
 * Type that can be returned from a callback, either producing a field
 * for a token, or skipping it.
 *
 * # Example
 *
 * ```kotlin
 * sealed class Token : Logos<Unit> {
 *     // #[regex(r"[ \n\f\t]+", ::skip)]
 *     object Ignored : Token()
 *
 *     // #[regex("[0-9]+", { lex ->
 *     //     val n: Long = lex.slice().toLong()
 *     //
 *     //     // Only emit a token if `n` is an even number
 *     //     if (n % 2 == 0L) Filter.Emit(n) else Filter.Skip()
 *     // })]
 *     data class EvenNumber(val value: Long) : Token()
 * }
 *
 * val tokens: List<Result<Token>> = Token.lexer("20 11 42 23 100 8002").toList()
 *
 * check(
 *     tokens == listOf(
 *         Result.success(Token.EvenNumber(20)),
 *         // skipping 11
 *         Result.success(Token.EvenNumber(42)),
 *         // skipping 23
 *         Result.success(Token.EvenNumber(100)),
 *         Result.success(Token.EvenNumber(8002)),
 *     ),
 * )
 * ```
 */
sealed class Filter<T> {
    /** Emit a token with a given value `T`. Use [Unit] for unit variants without fields. */
    class Emit<T>(val value: T) : Filter<T>()

    /** Skip current match, analog to [Skip]. */
    class Skip<T> : Filter<T>()
}

/**
 * Type that can be returned from a callback, either producing a field
 * for a token, skipping it, or emitting an error.
 *
 * # Example
 *
 * ```kotlin
 * sealed class LexingError {
 *     object NumberParseError : LexingError()
 *     object NumberIsTen : LexingError()
 *     object Other : LexingError()
 *
 *     companion object {
 *         val DEFAULT: LexingError = Other
 *     }
 * }
 *
 * // logos(error = LexingError)
 * sealed class Token : Logos<LexingError> {
 *     // #[regex(r"[ \n\f\t]+", ::skip)]
 *     object Ignored : Token()
 *
 *     // #[regex("[0-9]+", { lex ->
 *     //     val n: Long = lex.slice().toLong()
 *     //
 *     //     // Only emit a token if `n` is an even number.
 *     //     if (n % 2 == 0L) {
 *     //         // Emit an error if `n` is 10.
 *     //         if (n == 10L) FilterResult.Error(LexingError.NumberIsTen)
 *     //         else FilterResult.Emit(n)
 *     //     } else FilterResult.Skip()
 *     // })]
 *     data class NiceEvenNumber(val value: Long) : Token()
 * }
 *
 * val tokens: List<Result<Token>> = Token.lexer("20 11 42 23 100 10").toList()
 *
 * check(
 *     tokens == listOf(
 *         Result.success(Token.NiceEvenNumber(20)),
 *         // skipping 11
 *         Result.success(Token.NiceEvenNumber(42)),
 *         // skipping 23
 *         Result.success(Token.NiceEvenNumber(100)),
 *         // error at 10
 *         Result.failure(LexingError.NumberIsTen),
 *     ),
 * )
 * ```
 */
sealed class FilterResult<T, E> {
    /** Emit a token with a given value `T`. Use [Unit] for unit variants without fields. */
    class Emit<T, E>(val value: T) : FilterResult<T, E>()

    /** Skip current match, analog to [Skip]. */
    class Skip<T, E> : FilterResult<T, E>()

    /** Emit a `<Token as Logos>::ERROR` token. */
    class Error<T, E>(val error: E) : FilterResult<T, E>()
}

/**
 * Predefined callback that will inform the [Lexer] to skip a definition.
 *
 * # Example
 *
 * ```kotlin
 * sealed class Token : Logos<Unit> {
 *     // We will treat "abc" as if it was whitespace
 *     // #[regex(" |abc", ::skip, priority = 3)]
 *     object Ignored : Token()
 *
 *     // #[regex("[a-zA-Z]+")]
 *     data class Text(val value: String) : Token()
 * }
 *
 * val tokens: List<Result<Token>> = Token.lexer("Hello abc world").toList()
 *
 * check(
 *     tokens == listOf(
 *         Result.success(Token.Text("Hello")),
 *         Result.success(Token.Text("world")),
 *     ),
 * )
 * ```
 */
fun skip(): Skip = Skip.SKIP
