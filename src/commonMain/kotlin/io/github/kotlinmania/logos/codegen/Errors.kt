// port-lint: source logos-codegen/src/error.rs
package io.github.kotlinmania.logos.codegen

/*
 * Copyright (c) Maciej Hirsz, logos contributors.
 * Licensed under either of Apache-2.0 OR MIT.
 */

/**
 * Source-span pointing at a location in the lexer declaration: an integer byte offset from the
 * start of the input declaration string. Builders that don't have span info pass [NONE].
 */
data class CodegenSpan(val offset: Int = -1) {
    companion object {
        val NONE: CodegenSpan = CodegenSpan(-1)
    }
}

/** A single error message with its associated [CodegenSpan]. */
data class SpannedError(
    val message: String,
    val span: CodegenSpan,
)

/** Collects errors raised while parsing the lexer declaration. */
class Errors {
    private val collected: MutableList<SpannedError> = mutableListOf()

    fun err(message: String, span: CodegenSpan): Errors {
        collected.add(SpannedError(message = message, span = span))
        return this
    }

    /**
     * Render all collected errors as a single multi-line message the caller can throw or surface
     * in diagnostic output. Returns `null` when the collection is empty.
     */
    fun render(): String? {
        if (collected.isEmpty()) return null
        val out = StringBuilder()
        for ((i, e) in collected.withIndex()) {
            if (i > 0) out.append('\n')
            out.append("error")
            if (e.span.offset >= 0) {
                out.append(" at offset ").append(e.span.offset)
            }
            out.append(": ").append(e.message)
        }
        return out.toString()
    }

    /** Number of errors collected so far. */
    fun count(): Int = collected.size

    /** Snapshot of currently collected errors. */
    fun snapshot(): List<SpannedError> = collected.toList()
}
