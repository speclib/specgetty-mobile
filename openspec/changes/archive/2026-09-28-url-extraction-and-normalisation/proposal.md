## Why

Bean `specgetty-mobile-y7rx`, milestone `specgetty-mobile-acg7`.

`BRIEFING.md` says QR scan, paste and share-into work as in beans-on-droid.
beans-on-droid's archived change `add-repo-by-scan-and-share` measured why that
is more than reading a code correctly, and the measurement transfers unchanged:

```
https://github.com/speclib/specgetty.git                 clones
https://github.com/speclib/specgetty                     clones
https://github.com/speclib/specgetty?tab=readme-ov-file  fails
https://github.com/speclib/specgetty/issues              fails
https://github.com/speclib/specgetty/tree/main/src       fails
```

Every realistic producer of a code or a share emits a **page** URL. A browser's
"QR code for this page" gives whatever is in the address bar, and GitHub
routinely leaves `?tab=readme-ov-file` there. Shipping capture without the
normaliser gives a feature that reads the code perfectly and then fails to
clone, thirty seconds later, blaming the network.

This is the pure half: extraction and normalisation, strings in and strings out,
with no Android in it. The camera and the share intent come after the add form
exists to receive them, in milestone 06's neighbourhood.

## What Changes

- `capture/UrlCapture`: find the first web address in arbitrary text, normalise
  a forge page URL into a clone URL, and report when there was no address.
- A `CaptureResult` that distinguishes "found this" from "nothing here", so a
  caller cannot mistake an empty string for a URL.

## Capabilities

### New Capabilities

- `repo-url-capture`: getting a repository URL into the app without typing it,
  covering extraction from arbitrary text, normalisation of forge page URLs, and
  the rule that a captured URL is never acted on by itself.

## Impact

- No new dependencies. No Android imports, so the forge URL table above is
  expressible as unit tests rather than as a paragraph.
- Credentials embedded in a captured URL are stripped here, which is the reason
  this code and not the UI owns normalisation.
- The `capture` package is already named in the 80 percent rule, so this lands
  under the stricter floor from its first line.
