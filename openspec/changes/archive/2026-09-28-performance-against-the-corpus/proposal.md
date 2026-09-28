## Why

Bean `specgetty-mobile-nrao`, milestone `specgetty-mobile-pddz`.

`BRIEFING.md` puts a performance claim in the definition of done: the specgetty
repo itself, about 30 specs and its archive, loads and scrolls without
noticeable lag. That claim is currently unmeasured, and an unmeasured
performance claim is a hope.

The honest version of it has two halves that need different instruments. What
the app computes, loading, indexing, searching and parsing, is measurable on the
JVM against the vendored corpus. What the app draws, scrolling a list of eighty
changes, is not: a build machine is not a phone, and a number from one says
nothing about the other.

So this change measures the first half and records the numbers, and states the
second as what it is: a claim that needs a device, checked in the instrumented
suite.

The thresholds are deliberately loose. A tight threshold on a shared build
machine fails on a busy afternoon and teaches everyone to ignore it. These are
set to catch an order-of-magnitude regression, which is the kind that matters.

## What Changes

- `PerformanceTest`: measures loading the vendored project, building its index,
  a name search, a body search over every change, and parsing every spec and
  every delta. Each is reported, and each is bounded by a loose ceiling.
- `docs/performance.md`: the numbers as measured, with the machine they came
  from, so a later reading has something to compare against.

## Capabilities

### New Capabilities

- `performance`: what the app's work costs on a real project, measured rather
  than asserted.

## Impact

- No production code changes and no new dependencies.
- The measurements run in the ordinary unit suite, so a regression shows up in
  the gate rather than in a report nobody runs.
