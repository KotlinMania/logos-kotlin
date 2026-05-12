// port-lint: source src/source.rs
package io.github.kotlinmania.logos.source

/*
 * Copyright (c) Maciej Hirsz, logos contributors.
 * Licensed under either of Apache-2.0 OR MIT.
 */

// This module contains a bunch of traits necessary for processing byte strings.
//
// Most notable are:
// * [Source] - implemented by default for [String] and [ByteArray] and wrapper types, used by the `Lexer`.
// * Slice - slices of [Source], returned by `Lexer.slice`.

/**
 * Trait for types the `Lexer` can read from.
 *
 * Most notably this is implemented for [String] and [ByteArray]. It is unlikely you will
 * ever want to use this Trait yourself, unless implementing a new [Source]
 * the `Lexer` can use.
 */
interface Source<TSlice> {
    /** Length of the source. */
    fun len(): Int

    /**
     * Read a chunk of bytes into a fixed-size container. Returns null when reading
     * out of bounds would occur.
     *
     * This is very useful for matching fixed-size byte arrays.
     */
    fun <C : Chunk> read(offset: Int, chunk: ChunkKind<C>): C?

    /** Get a slice of the source at given range, or `null` if the range is out of bounds. */
    fun slice(start: Int, end: Int): TSlice?

    /**
     * Get a slice of the source at given range, without bounds checking.
     *
     * The caller is responsible for ensuring `start` and `end` are within bounds and on a valid
     * boundary.
     */
    fun sliceUnchecked(start: Int, end: Int): TSlice

    /**
     * For string sources attempts to find the closest character boundary at which the source
     * can be sliced, starting from `index`.
     *
     * For binary sources this should just return `index` back.
     */
    fun findBoundary(index: Int): Int = index

    /**
     * Check if `index` is valid for this [Source], that is:
     *
     * + It's not larger than the byte length of the [Source].
     * + (string only) It doesn't land in the middle of a UTF-8 code point.
     */
    fun isBoundary(index: Int): Boolean
}

/**
 * A [Source] backed by a [String]. The slice type is [String]; the source is interpreted as a
 * sequence of UTF-8 bytes.
 */
class StringSource(private val source: String) : Source<String> {
    private val bytes: ByteArray = source.encodeToByteArray()

    override fun len(): Int = bytes.size

    override fun <C : Chunk> read(offset: Int, chunk: ChunkKind<C>): C? {
        return if (offset + (chunk.size - 1) < bytes.size) {
            chunk.fromBytes(bytes, offset)
        } else {
            null
        }
    }

    override fun slice(start: Int, end: Int): String? {
        if (start < 0 || end < start || end > bytes.size) return null
        if (!isBoundary(start) || !isBoundary(end)) return null
        return bytes.decodeToString(start, end)
    }

    override fun sliceUnchecked(start: Int, end: Int): String {
        check(start <= bytes.size && end <= bytes.size) {
            "Reading out of bounds [$start..$end) for ${bytes.size}!"
        }
        return bytes.decodeToString(start, end)
    }

    override fun findBoundary(index: Int): Int {
        var i = index
        while (!isBoundary(i)) {
            i += 1
        }
        return i
    }

    override fun isBoundary(index: Int): Boolean {
        if (index < 0 || index > bytes.size) return false
        if (index == 0 || index == bytes.size) return true
        // A byte is a UTF-8 char-boundary if it's not a continuation byte (top two bits != 10).
        val b = bytes[index].toInt() and 0xC0
        return b != 0x80
    }

    fun bytes(): ByteArray = bytes
}

/**
 * A [Source] backed by a [ByteArray]. The slice type is [ByteArray]; the source is binary.
 */
class ByteArraySource(private val bytes: ByteArray) : Source<ByteArray> {
    override fun len(): Int = bytes.size

    override fun <C : Chunk> read(offset: Int, chunk: ChunkKind<C>): C? {
        return if (offset + (chunk.size - 1) < bytes.size) {
            chunk.fromBytes(bytes, offset)
        } else {
            null
        }
    }

    override fun slice(start: Int, end: Int): ByteArray? {
        if (start < 0 || end < start || end > bytes.size) return null
        return bytes.copyOfRange(start, end)
    }

    override fun sliceUnchecked(start: Int, end: Int): ByteArray {
        check(start <= bytes.size && end <= bytes.size) {
            "Reading out of bounds [$start..$end) for ${bytes.size}!"
        }
        return bytes.copyOfRange(start, end)
    }

    override fun isBoundary(index: Int): Boolean = index in 0..bytes.size

    fun bytes(): ByteArray = bytes
}

/**
 * A fixed, statically sized chunk of data that can be read from the [Source].
 *
 * Implemented for [ChunkByte] (size 1) and [ChunkBytes] (variable size byte arrays).
 */
interface Chunk

/** Per-chunk-type metadata: size and constructor. */
abstract class ChunkKind<C : Chunk>(val size: Int) {
    abstract fun fromBytes(bytes: ByteArray, offset: Int): C
}

/** Single-byte chunk. */
data class ChunkByte(val value: Byte) : Chunk {
    companion object Kind : ChunkKind<ChunkByte>(size = 1) {
        override fun fromBytes(bytes: ByteArray, offset: Int): ChunkByte = ChunkByte(bytes[offset])
    }
}

/** N-byte chunk. */
class ChunkBytes(val bytes: ByteArray) : Chunk {
    override fun equals(other: Any?): Boolean = other is ChunkBytes && bytes.contentEquals(other.bytes)
    override fun hashCode(): Int = bytes.contentHashCode()

    companion object {
        /** Build a [ChunkKind] for chunks of exactly [n] bytes. */
        fun kind(n: Int): ChunkKind<ChunkBytes> = object : ChunkKind<ChunkBytes>(size = n) {
            override fun fromBytes(bytes: ByteArray, offset: Int): ChunkBytes =
                ChunkBytes(bytes.copyOfRange(offset, offset + n))
        }
    }
}
