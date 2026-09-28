## Context

The first screen, and the first code in this project that is not a pure
function of its inputs. Two things had to be solved before the tests could say
anything true, and both are worth recording because neither is guessable from
the code that resulted.

## Decisions

### The test remote is served over HTTP, not `file://`

Every test below this milestone used a `file://` URL, which JGit clones happily
and which `LocalRemote` produces for free. The view model cannot use one.

`RepoUrl.validate` refuses anything that is not `http://` or `https://`, on
purpose, because an SSH URL is the most common wrong answer a person gives. So
a `file://` fixture never reaches the code under test: `submit()` stops at
validation and the test asserts against an empty list, which is exactly what it
did, thirteen times, before this was understood.

beans-on-droid already had the answer, a smart-HTTP git server on loopback, in
its instrumented tests. It is ported here into the **unit** tests, with
`android.util.Base64` swapped for `java.util.Base64`, which is the only Android
thing in it.

That buys more than a valid URL. The server takes an optional required token, so
the refused-token path is now testable, and `RepoError.Authentication` is
asserted against a real 401 rather than against a string this project made up:

| Token given | Token required | Outcome |
|---|---|---|
| none | none | clones |
| right | right | clones |
| wrong | right | authentication failed |
| none | right | authentication failed |

### The view model gets a scope of the test's own

The view model collects the repository flow for as long as it lives. A collect
that never completes cannot go in the test's own scope, because `runTest` waits
for its children and the test hangs, which it did.

`backgroundScope` is the documented answer and was tried first. In
kotlinx-coroutines-test 1.11.0 it does not work here: work launched into
`backgroundScope` is not run by `advanceUntilIdle()`. Measured directly, with
everything else held constant:

| How the test advances | Background work runs |
|---|---|
| `advanceUntilIdle()` | no |
| `testScheduler.advanceUntilIdle()` | no |
| `runCurrent()` | yes |
| `yield()` | yes |

Rather than sprinkle `runCurrent()` through twenty tests and hope none is
missed, the view model takes its scope by injection. The tests give it a
`CoroutineScope(UnconfinedTestDispatcher(testScheduler))`: it shares the test's
scheduler so virtual time still applies, it is not a child of the test scope so
nothing hangs, and it is cancelled in teardown. In the app the parameter is
omitted and `viewModelScope` is used, which is what it would have been anyway.

## Risks

- The injected scope is a seam that exists for the tests. It is one nullable
  parameter with the production default, which is the smallest shape that
  solves it, but it is still a seam and worth naming as one.
- The ported HTTP server speaks enough of the protocol for clone and shallow
  fetch and no more. A JGit change that used a capability it does not implement
  would fail the tests without the app being wrong.
