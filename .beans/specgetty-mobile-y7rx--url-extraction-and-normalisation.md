---
# specgetty-mobile-y7rx
title: URL extraction and normalisation
status: todo
type: epic
priority: normal
created_at: 2026-09-28T19:47:14Z
updated_at: 2026-09-28T19:47:14Z
parent: specgetty-mobile-acg7
blocked_by:
    - specgetty-mobile-wvxc
---

Pure Kotlin, no Android, so the forge URL table is expressible as unit tests.

- [ ] Extract the first http or https address from arbitrary text, trailing punctuation trimmed
- [ ] Drop the fragment and the query string
- [ ] Strip credentials from the authority, never keep user:token@host
- [ ] Truncate at a /-/ marker for GitLab
- [ ] Truncate at a known view segment, considered only from the third segment onward
- [ ] Drop a trailing slash, leave .git exactly as found
- [ ] An unrecognised host is returned unchanged rather than guessed at
- [ ] Table-driven tests covering the URLs in beans-on-droid design.md
