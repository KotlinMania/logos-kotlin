# logos in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Flogos--kotlin-blue.svg)](https://github.com/KotlinMania/logos-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/logos)](https://central.sonatype.com/artifact/io.github.kotlinmania/logos)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/logos-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/logos-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`maciejhirsz/logos`](https://github.com/maciejhirsz/logos).

**Original Project:** This port is based on [`maciejhirsz/logos`](https://github.com/maciejhirsz/logos). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `maciejhirsz/logos`

> The text below is reproduced and lightly edited from [`https://github.com/maciejhirsz/logos`](https://github.com/maciejhirsz/logos). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

<img src="https://raw.githubusercontent.com/maciejhirsz/logos/master/logos.svg?sanitize=true" alt="Logos logo" width="250" align="right">

## Logos

[![Book](https://github.com/maciejhirsz/logos/actions/workflows/pages.yml/badge.svg?branch=master)](https://logos.maciej.codes/)
[![Crates.io version shield](https://img.shields.io/crates/v/logos.svg)](https://crates.io/crates/logos)
[![Docs](https://docs.rs/logos/badge.svg)](https://docs.rs/logos)
[![Crates.io license shield](https://img.shields.io/crates/l/logos.svg)](https://crates.io/crates/logos)
[![Code coverage](https://codecov.io/gh/maciejhirsz/logos/branch/master/graph/badge.svg)](https://codecov.io/gh/maciejhirsz/logos)

_Create ridiculously fast Lexers._

**Logos** has two goals:

+ To make it easy to create a Lexer, so you can focus on more complex problems.
+ To make the generated Lexer faster than anything you'd write by hand.

To achieve those, **Logos**:

+ Combines all token definitions into a single [deterministic state machine](https://en.wikipedia.org/wiki/Deterministic_finite_automaton).
+ Optimizes branches into [lookup tables](https://en.wikipedia.org/wiki/Lookup_table) or [jump tables](https://en.wikipedia.org/wiki/Branch_table).
+ Prevents [backtracking](https://en.wikipedia.org/wiki/ReDoS) inside token definitions.
+ [Unwinds loops](https://en.wikipedia.org/wiki/Loop_unrolling), and batches reads to minimize bounds checking.
+ Does all of that heavy lifting at compile time.

## Example

```rust
use logos::Logos;

#[derive(Logos, Debug, PartialEq)]
#[logos(skip r"[ \t\n\f]+")] // Ignore this regex pattern between tokens
enum Token {
    // Tokens can be literal strings, of any length.
    #[token("fast")]
    Fast,

    #[token(".")]
    Period,

    // Or regular expressions.
    #[regex("[a-zA-Z]+")]
    Text,
}

fn main() {
    let mut lex = Token::lexer("Create ridiculously fast Lexers.");

    assert_eq!(lex.next(), Some(Ok(Token::Text)));
    assert_eq!(lex.span(), 0..6);
    assert_eq!(lex.slice(), "Create");

    assert_eq!(lex.next(), Some(Ok(Token::Text)));
    assert_eq!(lex.span(), 7..19);
    assert_eq!(lex.slice(), "ridiculously");

    assert_eq!(lex.next(), Some(Ok(Token::Fast)));
    assert_eq!(lex.span(), 20..24);
    assert_eq!(lex.slice(), "fast");

    assert_eq!(lex.next(), Some(Ok(Token::Text)));
    assert_eq!(lex.slice(), "Lexers");
    assert_eq!(lex.span(), 25..31);

    assert_eq!(lex.next(), Some(Ok(Token::Period)));
    assert_eq!(lex.span(), 31..32);
    assert_eq!(lex.slice(), ".");

    assert_eq!(lex.next(), None);
}
```

For more examples and documentation, please refer to the
[Logos handbook](https://maciejhirsz.github.io/logos/) or the
[crate documentation](https://docs.rs/logos/latest/logos/).

## How fast?

Ridiculously fast!

```norust
test identifiers                       ... bench:         647 ns/iter (+/- 27) = 1204 MB/s
test keywords_operators_and_punctators ... bench:       2,054 ns/iter (+/- 78) = 1037 MB/s
test strings                           ... bench:         553 ns/iter (+/- 34) = 1575 MB/s
```

## Acknowledgements

+ [Pedrors](https://pedrors.pt/) for the **Logos** logo.

## Thank you

**Logos** is very much a labor of love. If you find it useful, consider
[getting me some coffee](https://github.com/sponsors/maciejhirsz). ☕

If you'd like to contribute to Logos, then consider reading the
[Contributing guide](https://maciejhirsz.github.io/logos/contributing).

## Contributing

**Logos** welcome any kind of contribution: bug reports, suggestions,
or new features!

Please use the
[issues](https://github.com/maciejhirsz/logos/issues) or
[pull requests](https://github.com/maciejhirsz/logos/pulls) tabs,
when appropriate.

To release a new version, follow the [RELEASE-PROCESS](https://github.com/maciejhirsz/logos/blob/HEAD/RELEASE-PROCESS.md)

## License

This code is distributed under the terms of both the MIT license
and the Apache License (Version 2.0), choose whatever works for you.

See [LICENSE-APACHE](https://github.com/maciejhirsz/logos/blob/HEAD/LICENSE-APACHE) and [LICENSE-MIT](https://github.com/maciejhirsz/logos/blob/HEAD/LICENSE-MIT) for details.

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:logos:0.1.0")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same MIT license as the upstream [`maciejhirsz/logos`](https://github.com/maciejhirsz/logos). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the logos authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`maciejhirsz/logos`](https://github.com/maciejhirsz/logos) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
