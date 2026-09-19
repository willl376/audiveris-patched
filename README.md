# Audiveris (patched)

Personal copy/fork of [Audiveris](https://github.com/Audiveris/audiveris) (branch
`development`) carrying a set of local improvements, with the compiled application
attached to [Releases](https://github.com/willl376/audiveris-patched/releases/latest).

- Base commit: `223a94c` (Merge pull request #1025 from SilverGreen93/mihai_fit_width)
- Branch: `development` (rebased onto the full upstream history at the base commit)
- Compiled Linux x86_64 distribution: see Release assets (`audiveris-app-patched.tar.gz`)
- Raw patch: [`PATCH.diff`](PATCH.diff)

## Changes in this patch

### 1. `Grades.java` — new `goodBarlineGrade` constant
Added a named constant `goodBarlineGrade` (default `0.6`) for barline interpretation grading.

### 2. `Rational.java` — overflow-safe, faster `compareTo`
The cross-products `this.num * that.den` and `this.den * that.num` were computed in
`int`, which can silently overflow for large numerators/denominators, and only fell back
to a slow `BigInteger` re-computation when sign bits disagreed. The products are now
computed in `long` — the product of any two `int` values always fits in a `long` — so the
comparison is correct, simpler, and avoids `BigInteger` entirely.

### 3. `TimeRational.java` — corrected error message
The parser's exception message printed `num` where the offending denominator `den` was
meant. Fixed.

### 4. `StemsRetriever.java` — simplified abnormal-head marking
Removed a disabled and unreliable block that attempted to re-link a stem-less head to a
vertical seed. A stem-needing head with no `HeadStemRelation` is now simply marked
abnormal (see comment in code for the reasoning).

### 5. `BarlineInter.java` — use the named grading constant
`isGood()` now uses `Grades.goodBarlineGrade` instead of a hard-coded anonymous `0.6`
(which previously carried a `// TODO, quick & dirty` comment).

### 6. `TextLine.java` — tunable character-gap ratio
The maximum horizontal gap between two characters in a word is now computed as
`maxCharDx * pointSize * charGapFontRatio` using a new `charGapFontRatio` constant
(default `0.5`), replacing the previous rough `pointSize / 2` heuristic.

### 7. `RationalTest.java` — regression test for `compareTo` overflow
Added `testCompareToOverflow`, which exercises cross-products that overflow the `int`
range (e.g. `1_500_000_000/1` vs `1_000_000_000/3`).

## How it was built

Requires a JDK and Gradle (wrapper included):

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
./gradlew :app:installDist
```

Distribution output: `app/build/install/app/` (contains the `bin/Audiveris` launcher and
`lib/audiveris.jar`). The release asset is a tarball of that directory.