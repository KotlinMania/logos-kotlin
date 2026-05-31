// port-lint: source src/source.rs
package io.github.kotlinmania.logos.source

/*
 * Copyright (c) Maciej Hirsz, logos contributors.
 * Licensed under either of Apache-2.0 OR MIT.
 */

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Tests porting the documented examples on the upstream `Source` trait. The upstream tests
// live on Rust trait docstrings (`pub trait Source` in `src/source.rs`); each `assert_eq!`
// in those docstrings is the public contract for the trait. The Kotlin port keeps the same
// contract on the StringSource / ByteArraySource implementations.

class StringSourceReadTest {
    // Read a chunk of bytes into an array. Returns `None` when reading
    // out of bounds would occur.
    //
    // assert_eq!(foo.read(0), Some(b"foo"));
    // assert_eq!(foo.read(0), Some(b"fo"));
    // assert_eq!(foo.read(2), Some(b'o'));
    // assert_eq!(foo.read::<&[u8; 4]>(0), None);
    // assert_eq!(foo.read::<&[u8; 2]>(2), None);
    @Test
    fun readThreeByteChunkAtStart() {
        val foo = StringSource("foo")
        val three = foo.read(0, ChunkBytes.kind(3))
        assertContentEquals("foo".encodeToByteArray(), three?.bytes)
    }

    @Test
    fun readTwoByteChunkAtStart() {
        val foo = StringSource("foo")
        val two = foo.read(0, ChunkBytes.kind(2))
        assertContentEquals("fo".encodeToByteArray(), two?.bytes)
    }

    @Test
    fun readSingleByteAtOffset() {
        val foo = StringSource("foo")
        val one = foo.read(2, ChunkByte.Kind)
        assertEquals(ChunkByte('o'.code.toByte()), one)
    }

    @Test
    fun readFourByteChunkAtStartIsOutOfBounds() {
        val foo = StringSource("foo")
        assertNull(foo.read(0, ChunkBytes.kind(4)))
    }

    @Test
    fun readTwoByteChunkAtOffsetTwoIsOutOfBounds() {
        val foo = StringSource("foo")
        assertNull(foo.read(2, ChunkBytes.kind(2)))
    }
}

class StringSourceSliceTest {
    private val foo = StringSource("It was the year when they finally immanentized the Eschaton.")

    // Get a slice of the source at given range. This is analogous to
    // `slice::get(range)`.
    //
    // assert_eq!(<str as Source>::slice(&foo, 51..59), Some("Eschaton"));
    @Test
    fun sliceReturnsSubstringWhenInBounds() {
        assertEquals("Eschaton", foo.slice(51, 59))
    }

    // Get a slice of the source at given range. This is analogous to
    // `slice::get_unchecked(range)`.
    //
    // assert_eq!(<str as Source>::slice_unchecked(&foo, 51..59), "Eschaton");
    @Test
    fun sliceUncheckedReturnsSubstring() {
        assertEquals("Eschaton", foo.sliceUnchecked(51, 59))
    }

    @Test
    fun sliceReturnsNullWhenOutOfBounds() {
        assertNull(foo.slice(0, foo.len() + 1))
    }

    @Test
    fun sliceReturnsNullForReversedRange() {
        assertNull(foo.slice(10, 5))
    }
}

class StringSourceBoundaryTest {
    // For string sources, `is_boundary(index)` is true when `index` does not land in the
    // middle of a UTF-8 code point, and `find_boundary` returns the next valid boundary
    // at or after `index`.
    @Test
    fun asciiSourceHasBoundaryAtEveryByte() {
        val s = StringSource("abc")
        assertTrue(s.isBoundary(0))
        assertTrue(s.isBoundary(1))
        assertTrue(s.isBoundary(2))
        assertTrue(s.isBoundary(3))
    }

    @Test
    fun multiByteCodepointHasNoBoundaryInTheMiddle() {
        // "é" encodes as 0xC3 0xA9 — two bytes; offset 1 is a continuation byte.
        val s = StringSource("é")
        assertTrue(s.isBoundary(0))
        assertFalse(s.isBoundary(1))
        assertTrue(s.isBoundary(2))
    }

    @Test
    fun findBoundaryAdvancesPastContinuationBytes() {
        // "aé" = 0x61 0xC3 0xA9, length 3.
        val s = StringSource("aé")
        assertEquals(0, s.findBoundary(0))
        assertEquals(1, s.findBoundary(1))
        // Offset 2 is mid-codepoint; the next boundary is at 3.
        assertEquals(3, s.findBoundary(2))
        assertEquals(3, s.findBoundary(3))
    }

    @Test
    fun isBoundaryRejectsNegativeAndOversizedIndex() {
        val s = StringSource("abc")
        assertFalse(s.isBoundary(-1))
        assertFalse(s.isBoundary(s.len() + 1))
    }

    @Test
    fun sliceRefusesToCutInsideACodepoint() {
        val s = StringSource("é") // 2 bytes
        assertNull(s.slice(0, 1))
        assertEquals("é", s.slice(0, 2))
    }
}

class StringSourceLenTest {
    @Test
    fun lenIsByteLengthOfUtf8Encoding() {
        assertEquals(0, StringSource("").len())
        assertEquals(3, StringSource("abc").len())
        // "é" is two bytes in UTF-8 even though it is one codepoint.
        assertEquals(2, StringSource("é").len())
    }
}

class ByteArraySourceTest {
    // Binary sources treat every index up to `len()` as a boundary.
    @Test
    fun lenMatchesByteArraySize() {
        assertEquals(0, ByteArraySource(byteArrayOf()).len())
        assertEquals(4, ByteArraySource(byteArrayOf(1, 2, 3, 4)).len())
    }

    @Test
    fun readReturnsByteAtOffset() {
        val s = ByteArraySource(byteArrayOf(10, 20, 30))
        assertEquals(ChunkByte(20), s.read(1, ChunkByte.Kind))
    }

    @Test
    fun readChunkReturnsContiguousBytes() {
        val s = ByteArraySource(byteArrayOf(10, 20, 30, 40))
        val pair = s.read(1, ChunkBytes.kind(2))
        assertContentEquals(byteArrayOf(20, 30), pair?.bytes)
    }

    @Test
    fun readChunkPastEndReturnsNull() {
        val s = ByteArraySource(byteArrayOf(10, 20, 30))
        assertNull(s.read(2, ChunkBytes.kind(2)))
    }

    @Test
    fun sliceReturnsCopyOfRange() {
        val s = ByteArraySource(byteArrayOf(10, 20, 30, 40))
        assertContentEquals(byteArrayOf(20, 30), s.slice(1, 3))
    }

    @Test
    fun sliceReturnsNullForOutOfBoundsRange() {
        val s = ByteArraySource(byteArrayOf(10, 20, 30))
        assertNull(s.slice(0, 4))
        assertNull(s.slice(-1, 2))
    }

    @Test
    fun isBoundaryAcceptsEveryIndexInRange() {
        val s = ByteArraySource(byteArrayOf(10, 20, 30))
        assertTrue(s.isBoundary(0))
        assertTrue(s.isBoundary(1))
        assertTrue(s.isBoundary(2))
        assertTrue(s.isBoundary(3))
        assertFalse(s.isBoundary(-1))
        assertFalse(s.isBoundary(4))
    }

    @Test
    fun findBoundaryIsIdentityForBinarySources() {
        val s = ByteArraySource(byteArrayOf(10, 20, 30))
        assertEquals(0, s.findBoundary(0))
        assertEquals(1, s.findBoundary(1))
        assertEquals(2, s.findBoundary(2))
    }
}

class ChunkBytesEqualityTest {
    // ChunkBytes overrides equals/hashCode to compare by content rather than identity, mirroring
    // the upstream `[u8; N]` chunk equality contract that the lexer relies on.
    @Test
    fun equalsByContent() {
        assertEquals(ChunkBytes(byteArrayOf(1, 2, 3)), ChunkBytes(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun notEqualWhenContentDiffers() {
        check(ChunkBytes(byteArrayOf(1, 2, 3)) != ChunkBytes(byteArrayOf(1, 2, 4)))
    }

    @Test
    fun hashCodeMatchesContentHashCode() {
        val a = ChunkBytes(byteArrayOf(1, 2, 3))
        val b = ChunkBytes(byteArrayOf(1, 2, 3))
        assertEquals(a.hashCode(), b.hashCode())
    }
}
