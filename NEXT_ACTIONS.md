# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 4/4 (100.0%)
- **Function parity:** 33/33 matched (target 108) — 100.0%
- **Class/type parity:** 19/19 matched (target 42) — 100.0%
- **Combined symbol parity:** 52/52 matched (target 150) — 100.0%
- **Average inline-code cosine:** 0.47 (function body across 4 matched files)
- **Average documentation cosine:** 0.62 (doc text across 4 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 2 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. source

- **Target:** `source.Source`
- **Similarity:** 0.33
- **Dependents:** 1
- **Priority Score:** 1001106.8
- **Functions:** 8/8 matched (target 48)
- **Missing functions:** _none_
- **Types:** 3/3 matched (target 14)
- **Missing types:** _none_

### 2. lexer

- **Target:** `lexer.Lexer`
- **Similarity:** 0.69
- **Dependents:** 0
- **Priority Score:** 2603.1
- **Functions:** 20/20 matched (target 26)
- **Missing functions:** _none_
- **Types:** 6/6 matched
- **Missing types:** _none_

### 3. lib

- **Target:** `logos.Logos`
- **Similarity:** 0.73
- **Dependents:** 0
- **Priority Score:** 802.7
- **Functions:** 3/3 matched (target 15)
- **Missing functions:** _none_
- **Types:** 5/5 matched (target 13)
- **Missing types:** _none_

### 4. internal

- **Target:** `logos.Internal`
- **Similarity:** 0.14
- **Dependents:** 0
- **Priority Score:** 708.6
- **Functions:** 2/2 matched (target 19)
- **Missing functions:** _none_
- **Types:** 5/5 matched (target 9)
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

