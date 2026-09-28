## 1. Extraction

- [x] 1.1 Find the first `http` or `https` address in arbitrary text
- [x] 1.2 Trim trailing punctuation from the match
- [x] 1.3 Report absence rather than returning an empty string

## 2. Normalisation

- [x] 2.1 Drop the fragment and the query string
- [x] 2.2 Strip credentials from the authority
- [x] 2.3 Truncate at a `/-/` marker
- [x] 2.4 Truncate at a known view segment, from the third path segment onward only
- [x] 2.5 Drop a trailing slash and leave `.git` exactly as found
- [x] 2.6 Return a host with no path, and an unrecognised host, unchanged

## 3. Result

- [x] 3.1 `CaptureResult` distinguishing found from nothing found
- [x] 3.2 Capturing performs no side effect

## 4. Proof

- [x] 4.1 Table-driven tests over every URL in the proposal's measurement
- [x] 4.2 A test for the repository named `issues`
- [x] 4.3 A test that credentials never survive
- [x] 4.4 Coverage of the `capture` package at or above 80 percent
- [x] 4.5 The gate passes
