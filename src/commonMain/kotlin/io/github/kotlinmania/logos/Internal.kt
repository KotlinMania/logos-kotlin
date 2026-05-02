// port-lint: source src/internal.rs
package io.github.kotlinmania.logos

/*
 * Copyright (c) Maciej Hirsz, logos contributors.
 * Licensed under either of Apache-2.0 OR MIT.
 */

import io.github.kotlinmania.logos.source.Chunk
import io.github.kotlinmania.logos.source.ChunkKind

/**
 * Trait used by the functions contained in the lexicon.
 *
 * # WARNING!
 *
 * **This trait, and its methods, are not meant to be used outside of the
 * code produced by the Kotlin builder/codegen for [Logos] tokens.**
 */
interface LexerInternal<TToken : Logos<*>> {
    /** Get the current offset of token_start. */
    fun offset(): Int

    /** Read a chunk. */
    fun <C : Chunk> read(offset: Int, chunk: ChunkKind<C>): C?

    /** Reset `tokenStart` to `tokenEnd`. */
    fun trivia()

    /**
     * Guarantee that `tokenEnd` is at char boundary for string sources.
     * Called before returning the default error variant.
     */
    fun endToBoundary(offset: Int)

    /** Set `tokenEnd` to an offset. */
    fun end(offset: Int)
}

/**
 * Result returned from a callback associated with a token variant.
 */
sealed class CallbackResult<L : Logos<E>, E> {
    class Emit<L : Logos<E>, E>(val token: L) : CallbackResult<L, E>()
    class Error<L : Logos<E>, E>(val error: E) : CallbackResult<L, E>()
    class DefaultError<L : Logos<E>, E> : CallbackResult<L, E>()
    class Skip<L : Logos<E>, E> : CallbackResult<L, E>()
}

/**
 * Trait converting a callback's return value into a [CallbackResult].
 *
 * The Rust upstream uses overlapping `impl` blocks on different return types (raw value, [Result],
 * [Filter], [FilterResult], [Skip], etc.). The Kotlin port exposes one constructor function per
 * return-type shape and the generated/builder code calls the matching one.
 */
object CallbackRetVal {
    /** Field-variant: callback returned a value `T`; emit `con(value)`. */
    fun <T, L : Logos<E>, E> emitValue(value: T, con: (T) -> L): CallbackResult<L, E> {
        return CallbackResult.Emit(con(value))
    }

    /** Field-variant: callback returned [Result]; emit on success, error on failure. */
    fun <T, L : Logos<E>, E> emitResult(result: Result<T>, con: (T) -> L, errorAdapter: (Throwable) -> E): CallbackResult<L, E> {
        return result.fold(
            onSuccess = { CallbackResult.Emit(con(it)) },
            onFailure = { CallbackResult.Error(errorAdapter(it)) },
        )
    }

    /** Field-variant: callback returned a nullable; emit if non-null else default error. */
    fun <T, L : Logos<E>, E> emitOption(value: T?, con: (T) -> L): CallbackResult<L, E> {
        return if (value != null) CallbackResult.Emit(con(value)) else CallbackResult.DefaultError()
    }

    /** Field-variant: callback returned a [Filter]; emit/skip per filter. */
    fun <T, L : Logos<E>, E> emitFilter(filter: Filter<T>, con: (T) -> L): CallbackResult<L, E> {
        return when (filter) {
            is Filter.Emit -> CallbackResult.Emit(con(filter.value))
            is Filter.Skip -> CallbackResult.Skip()
        }
    }

    /** Field-variant: callback returned a [FilterResult]; emit/skip/error per filter result. */
    fun <T, L : Logos<E>, E> emitFilterResult(filter: FilterResult<T, E>, con: (T) -> L): CallbackResult<L, E> {
        return when (filter) {
            is FilterResult.Emit -> CallbackResult.Emit(con(filter.value))
            is FilterResult.Skip -> CallbackResult.Skip()
            is FilterResult.Error -> CallbackResult.Error(filter.error)
        }
    }

    /** Unit-variant: callback returned [Boolean]; emit on true, default error on false. */
    fun <L : Logos<E>, E> emitBoolean(value: Boolean, con: () -> L): CallbackResult<L, E> {
        return if (value) CallbackResult.Emit(con()) else CallbackResult.DefaultError()
    }

    /** Unit-variant: callback returned [Skip]; always skip. */
    fun <L : Logos<E>, E> emitSkip(): CallbackResult<L, E> = CallbackResult.Skip()

    /** Unit-variant: callback returned [Result] of [Skip]; skip on success, error on failure. */
    fun <L : Logos<E>, E> emitResultSkip(
        result: Result<Skip>,
        errorAdapter: (Throwable) -> E,
    ): CallbackResult<L, E> {
        return result.fold(
            onSuccess = { CallbackResult.Skip() },
            onFailure = { CallbackResult.Error(errorAdapter(it)) },
        )
    }

    /** Unit-variant: callback returned a token directly; emit it. */
    fun <L : Logos<E>, E> emitToken(token: L): CallbackResult<L, E> {
        return CallbackResult.Emit(token)
    }

    /** Unit-variant: callback returned a [Filter] of token; emit/skip. */
    fun <L : Logos<E>, E> emitFilterToken(filter: Filter<L>): CallbackResult<L, E> {
        return when (filter) {
            is Filter.Emit -> CallbackResult.Emit(filter.value)
            is Filter.Skip -> CallbackResult.Skip()
        }
    }

    /** Unit-variant: callback returned a [FilterResult] of token; emit/skip/error. */
    fun <L : Logos<E>, E> emitFilterResultToken(filter: FilterResult<L, E>): CallbackResult<L, E> {
        return when (filter) {
            is FilterResult.Emit -> CallbackResult.Emit(filter.value)
            is FilterResult.Skip -> CallbackResult.Skip()
            is FilterResult.Error -> CallbackResult.Error(filter.error)
        }
    }
}

/** Result returned from a "skip" callback. */
sealed class SkipResult<L : Logos<E>, E> {
    class Skip<L : Logos<E>, E> : SkipResult<L, E>()
    class Error<L : Logos<E>, E>(val error: E) : SkipResult<L, E>()
}

internal fun <L : Logos<E>, E> SkipResult<L, E>.intoCallbackResult(): CallbackResult<L, E> = when (this) {
    is SkipResult.Skip -> CallbackResult.Skip()
    is SkipResult.Error -> CallbackResult.Error(error)
}

/** Trait for skip-callback return types. Mirrors the upstream `SkipRetVal`. */
object SkipRetVal {
    /** Skip on `Unit`/`Skip`. */
    fun <L : Logos<E>, E> ofUnit(): SkipResult<L, E> = SkipResult.Skip()

    fun <L : Logos<E>, E> ofSkip(): SkipResult<L, E> = SkipResult.Skip()

    /** Skip on `Ok`, error on `Err`. */
    fun <L : Logos<E>, E> ofResultUnit(result: Result<Unit>, errorAdapter: (Throwable) -> E): SkipResult<L, E> {
        return result.fold(
            onSuccess = { SkipResult.Skip() },
            onFailure = { SkipResult.Error(errorAdapter(it)) },
        )
    }

    fun <L : Logos<E>, E> ofResultSkip(result: Result<Skip>, errorAdapter: (Throwable) -> E): SkipResult<L, E> {
        return result.fold(
            onSuccess = { SkipResult.Skip() },
            onFailure = { SkipResult.Error(errorAdapter(it)) },
        )
    }
}
