// port-lint: source logos-codegen/src/util.rs
package io.github.kotlinmania.logos.codegen

import io.github.kotlinmania.procmacro2.Delimiter
import io.github.kotlinmania.procmacro2.Group
import io.github.kotlinmania.procmacro2.Span
import io.github.kotlinmania.procmacro2.Spacing
import io.github.kotlinmania.procmacro2.TokenStream
import io.github.kotlinmania.procmacro2.TokenTree
import io.github.kotlinmania.quote.ToTokens
import io.github.kotlinmania.syn.Ident

class MaybeVoid(private var stream: TokenStream?) : ToTokens {

    fun replace(stream: TokenStream): MaybeVoid {
        val old = this.stream
        this.stream = stream
        return MaybeVoid(old)
    }

    fun take(): MaybeVoid {
        val old = stream
        this.stream = null
        return MaybeVoid(old)
    }

    override fun toTokens(tokens: TokenStream) {
        val s = stream
        if (s != null) {
            tokens.extendTokenStreams(listOf(s))
        } else {
            tokens.extendTokenTrees(listOf(TokenTree.Group(Group(Delimiter.Parenthesis, TokenStream.new()))))
        }
    }

    override fun toTokenStream(): TokenStream {
        val s = stream
        return if (s != null) {
            TokenStream.fromTokenStreams(listOf(s))
        } else {
            TokenStream.fromTokenTree(TokenTree.Group(Group(Delimiter.Parenthesis, TokenStream.new())))
        }
    }

    override fun intoTokenStream(): TokenStream {
        val s = stream
        return if (s != null) {
            s
        } else {
            TokenStream.fromTokenTree(TokenTree.Group(Group(Delimiter.Parenthesis, TokenStream.new())))
        }
    }

    companion object {
        val VOID: MaybeVoid = MaybeVoid(null)
        fun some(stream: TokenStream): MaybeVoid = MaybeVoid(stream)
    }
}

fun isPunct(tt: TokenTree, expect: Char): Boolean =
    tt is TokenTree.Punct && tt.value.asChar() == expect && tt.value.spacing() == Spacing.Alone

fun expectPunct(tt: TokenTree?, expect: Char): TokenTree? =
    tt?.takeIf { tree -> !isPunct(tree, expect) }

interface ToIdent {
    fun toIdent(): Ident
}

fun String.toIdent(): Ident = Ident.new(this, Span.callSite())