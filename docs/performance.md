# Performance

`BRIEFING.md` puts a claim in the definition of done: the specgetty repo itself,
about 30 specs and its archive, loads and scrolls without noticeable lag.

That claim has two halves, and they need different instruments.

## What the app computes

Measured on the vendored specgetty project: 28 specs, 1 active change, 57
archived changes, 111 delta files, 342 files in all. Run as part of the ordinary
unit suite, so a regression fails the gate rather than sitting in a report
nobody runs.

| What | Time |
|---|---|
| Load the project from disk | 35 ms |
| Build the index and read its counts | under 1 ms |
| A name query over all 58 changes | under 1 ms, reading no file |
| A text query over all 58 changes | 17 ms, reading 508 files |
| Parse all 28 specs and all 111 deltas | 27 ms |
| Load, index and count, which is what a refresh does | 21 ms |
| A spec already parsed, read 100 times from the cache | under 1 ms |

Measured on: 11th Gen Intel Core i7-1195G7, 8 cores, 15 GB RAM, Linux 7.1.7,
JDK 17. A phone is slower than this, by a factor that these numbers cannot tell
you.

The thresholds in `PerformanceTest` are set an order of magnitude above these
figures. A tight threshold on a machine that also runs a browser fails on a busy
afternoon and teaches everyone to ignore the test; a loose one still catches the
regression that matters, which is the accidental quadratic.

Two figures are worth reading together. A name query reads no file at all, and a
text query reads 508. That is the whole reason the matchers are separate, and it
is asserted rather than assumed: `ProjectIndex` takes its file reader by
injection and the test passes one that fails the test if it is called.

## What the app draws

Not measured here, and not inferable from the above. Scrolling a list of 58
changes or an outline of 90 nodes is a question about a phone's renderer, and a
number from a build machine would be an answer to a different question.

It is checked on a device in the instrumented suite, and by looking at it.

## Re-running these

```bash
nix develop --command ./gradlew testDebugUnitTest --tests '*PerformanceTest*' --rerun-tasks
```

Each measurement prints itself, so the run is readable rather than only pass or
fail.
