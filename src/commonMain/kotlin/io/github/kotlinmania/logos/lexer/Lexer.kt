// port-lint: source src/lexer.rs
package io.github.kotlinmania.logos.lexer

/*
 * Copyright (c) Maciej Hirsz, logos contributors.
 * Licensed under either of Apache-2.0 OR MIT.
 */

import io.github.kotlinmania.logos.LexerInternal
import io.github.kotlinmania.logos.LexerDefinition
import io.github.kotlinmania.logos.Logos
import io.github.kotlinmania.logos.source.Chunk
import io.github.kotlinmania.logos.source.ChunkKind
import io.github.kotlinmania.logos.source.Source

/** Byte range in the source. */
data class Span(val start: Int, val endExclusive: Int) {
    val length: Int get() = endExclusive - start
}

/**
 * [Lexer] is the main struct of the crate that allows you to read through a [Source]
 * and produce tokens for types implementing [Logos].
 *
 * The Rust upstream uses associated types `Token::Source`, `Token::Extras`, and `Token::Error` on
 * the [Logos] trait. Kotlin doesn't have associated types, so the Kotlin port carries those as
 * generic parameters on the [Lexer] type:
 *
 * - [TToken] — the token type (must implement [Logos]).
 * - [TSlice] — the source slice type (e.g. [String] or [ByteArray]).
 * - [E] — the error type returned by the lexer.
 * - [Extras] — extras associated with the token (defaults to [Unit]).
 */
class Lexer<TToken : Logos<E>, TSlice, E, Extras> internal constructor(
    private val sourceRef: Source<TSlice>,
    private val definition: LexerDefinition<TToken, TSlice, E>,
    private var tokenStart: Int = 0,
    private var tokenEnd: Int = 0,
    /** Extras associated with the token. */
    var extras: Extras,
) : LexerInternal<TToken>, Iterator<Result<TToken>> {
    companion object {
        /**
         * Create a new [Lexer].
         *
         * Due to type inference, it might be more ergonomic to construct it by calling
         * [LexerDefinition.lexer] on any token type with a generated [LexerDefinition].
         */
        fun <TToken : Logos<E>, TSlice, E> new(
            source: Source<TSlice>,
            definition: LexerDefinition<TToken, TSlice, E>,
        ): Lexer<TToken, TSlice, E, Unit> {
            return withExtras(source, definition, Unit)
        }

        /** Create a new [Lexer] with the provided extras. */
        fun <TToken : Logos<E>, TSlice, E, Extras> withExtras(
            source: Source<TSlice>,
            definition: LexerDefinition<TToken, TSlice, E>,
            extras: Extras,
        ): Lexer<TToken, TSlice, E, Extras> {
            return Lexer(
                sourceRef = source,
                definition = definition,
                tokenStart = 0,
                tokenEnd = 0,
                extras = extras,
            )
        }
    }

    /** Source from which this Lexer is reading tokens. */
    fun source(): Source<TSlice> = sourceRef

    /**
     * Wrap the [Lexer] in an [Iterator] that produces pairs of (Token, [Span]).
     */
    fun spanned(): SpannedIter<TToken, TSlice, E, Extras> = SpannedIter(this)

    /** Get the range for the current token in [Source]. */
    fun span(): Span = Span(tokenStart, tokenEnd)

    /** Get a string slice of the current token. */
    fun slice(): TSlice {
        return sourceRef.sliceUnchecked(tokenStart, tokenEnd)
    }

    /** Get a slice of remaining source, starting at the end of current token. */
    fun remainder(): TSlice {
        return sourceRef.sliceUnchecked(tokenEnd, sourceRef.len())
    }

    /**
     * Turn this lexer into a lexer for a new token type.
     *
     * The new lexer continues to point at the same span as the current lexer,
     * and the current token becomes the error token of the new token type.
     */
    fun <TToken2 : Logos<E2>, E2, Extras2> morph(
        definition2: LexerDefinition<TToken2, TSlice, E2>,
        extrasAdapter: (Extras) -> Extras2,
    ): Lexer<TToken2, TSlice, E2, Extras2> {
        return Lexer(
            sourceRef = sourceRef,
            definition = definition2,
            tokenStart = tokenStart,
            tokenEnd = tokenEnd,
            extras = extrasAdapter(extras),
        )
    }

    /**
     * Bumps the end of currently lexed token by `n` bytes.
     *
     * Panics if adding `n` to the current offset would place the [Lexer] beyond the last byte,
     * or in the middle of a UTF-8 code point (does not apply when lexing raw [ByteArray]).
     */
    fun bump(n: Int) {
        tokenEnd += n
        check(sourceRef.isBoundary(tokenEnd)) { "Invalid Lexer bump" }
    }

    // -- Iterator<Result<TToken>> --

    private var nextItem: Result<TToken>? = null
    private var nextItemReady: Boolean = false

    override fun hasNext(): Boolean {
        if (!nextItemReady) {
            nextItem = computeNext()
            nextItemReady = true
        }
        return nextItem != null
    }

    override fun next(): Result<TToken> {
        if (!nextItemReady) {
            nextItem = computeNext()
        }
        val item = nextItem ?: throw NoSuchElementException()
        nextItem = null
        nextItemReady = false
        return item
    }

    private fun computeNext(): Result<TToken>? {
        tokenStart = tokenEnd
        return definition.lex(this)
    }

    // -- LexerInternal<TToken> --

    /**
     * Read a [Chunk] at the current position of the [Lexer]. If the end of the [Source] has been
     * reached, this will return null.
     */
    override fun <C : Chunk> read(offset: Int, chunk: ChunkKind<C>): C? {
        return sourceRef.read(offset, chunk)
    }

    /** Reset `tokenStart` to `tokenEnd`. */
    override fun trivia() {
        tokenStart = tokenEnd
    }

    /**
     * Set the current token to the appropriate error variant.
     * Guarantee that `tokenEnd` is at a char boundary for string sources.
     */
    override fun endToBoundary(offset: Int) {
        tokenEnd = sourceRef.findBoundary(offset)
    }

    override fun end(offset: Int) {
        tokenEnd = offset
    }

    override fun offset(): Int = tokenStart
}

/**
 * Iterator that pairs tokens with their position in the source.
 *
 * Look at [Lexer.spanned] for documentation.
 */
class SpannedIter<TToken : Logos<E>, TSlice, E, Extras> internal constructor(
    private val lexer: Lexer<TToken, TSlice, E, Extras>,
) : Iterator<Pair<Result<TToken>, Span>> {
    override fun hasNext(): Boolean = lexer.hasNext()

    override fun next(): Pair<Result<TToken>, Span> {
        val token = lexer.next()
        return Pair(token, lexer.span())
    }

    /** Access the underlying lexer. */
    fun lexer(): Lexer<TToken, TSlice, E, Extras> = lexer
}
