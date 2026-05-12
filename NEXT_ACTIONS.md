# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 4/4 (100.0%)
- **Function parity:** 23/33 matched (target 57) — 69.7%
- **Class/type parity:** 14/19 matched (target 27) — 73.7%
- **Combined symbol parity:** 37/52 matched (target 84) — 71.2%
- **Average inline-code cosine:** 0.26 (function body across 3 matched files)
- **Average documentation cosine:** 0.61 (doc text across 3 matched files)
- **Cheat-zeroed Files:** 2
- **Critical Issues:** 4 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. source

- **Target:** `source.Source [PROVENANCE-FALLBACK]`
- **Similarity:** 0.29
- **Dependents:** 1
- **Priority Score:** 1031107.1
- **Functions:** 6/8 matched (target 19)
- **Missing functions:** `from_ptr`, `from_slice`
- **Types:** 2/3 matched (target 7)
- **Missing types:** `Slice`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/source.rs` vs expected `source.rs`
- **Proposed provenance header:** `// port-lint: source source.rs` (current: `// port-lint: source src/source.rs`)
- **Lint issues:** 1

### 2. lexer

- **Target:** `lexer.Lexer [PROVENANCE-FALLBACK]`
- **Similarity:** 0.49
- **Dependents:** 0
- **Priority Score:** 82605.1
- **Functions:** 15/20 matched
- **Missing functions:** `fmt`, `range`, `clone`, `deref`, `deref_mut`
- **Types:** 3/6 matched (target 3)
- **Missing types:** `Item`, `Target`, `Token`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/lexer.rs` vs expected `lexer.rs`
- **Proposed provenance header:** `// port-lint: source lexer.rs` (current: `// port-lint: source src/lexer.rs`)
- **Lint issues:** 1

### 3. lib

- **Target:** `logos.Logos [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 20810.0
- **Functions:** 2/3 matched (target 2)
- **Missing functions:** `lexer_with_extras`
- **Types:** 4/5 matched (target 8)
- **Missing types:** `ReadMe`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/lib.rs` vs expected `lib.rs`
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source src/lib.rs`)
- **Lint issues:** 1

### 4. internal

- **Target:** `logos.Internal [ZERO] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 20710.0
- **Functions:** 0/2 matched (target 16)
- **Missing functions:** `construct`, `from`
- **Types:** 5/5 matched (target 9)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/internal.rs` vs expected `internal.rs`
- **Proposed provenance header:** `// port-lint: source internal.rs` (current: `// port-lint: source src/internal.rs`)
- **Lint issues:** 1

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Next Commands

```bash
# Initialize task queue for systematic porting
cd tools/ast_distance
./ast_distance --init-tasks ../../src rust ../../src/commonMain/kotlin/io/github/kotlinmania/logos kotlin tasks.json ../../AGENTS.md

# Get next high-priority task
./ast_distance --assign tasks.json <agent-id>
```
