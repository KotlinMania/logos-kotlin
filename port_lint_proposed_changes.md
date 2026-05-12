# port-lint Proposed Changes

**Generated:** 2026-05-07
**Source:** src
**Target:** src/commonMain/kotlin/io/github/kotlinmania/logos

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `src/commonMain/kotlin/io/github/kotlinmania/logos/source/Source.kt` | `// port-lint: source src/source.rs` | `// port-lint: source source.rs` | `source.rs` | `port-lint provenance header matched only after fallback normalization: 'src/source.rs' vs expected 'source.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/logos/lexer/Lexer.kt` | `// port-lint: source src/lexer.rs` | `// port-lint: source lexer.rs` | `lexer.rs` | `port-lint provenance header matched only after fallback normalization: 'src/lexer.rs' vs expected 'lexer.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/logos/Logos.kt` | `// port-lint: source src/lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'src/lib.rs' vs expected 'lib.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/logos/Internal.kt` | `// port-lint: source src/internal.rs` | `// port-lint: source internal.rs` | `internal.rs` | `port-lint provenance header matched only after fallback normalization: 'src/internal.rs' vs expected 'internal.rs'` |
