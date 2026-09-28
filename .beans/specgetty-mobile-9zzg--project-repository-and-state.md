---
# specgetty-mobile-9zzg
title: Project repository and state
status: completed
openspec-link: openspec/changes/archive/2026-09-28-project-repository-and-state
type: epic
priority: normal
created_at: 2026-09-28T19:47:15Z
updated_at: 2026-09-28T20:49:30Z
parent: specgetty-mobile-61i0
blocked_by:
    - specgetty-mobile-hqy0
---

data/ProjectRepository. App state and its transitions.

- [ ] Load the project into an in-memory index on each clone and refresh
- [ ] Empty, loading and error states as distinct values, not flags
- [ ] Auth failure, network error and no-OpenSpec-project each carry their own message
- [ ] Coroutines, and a ViewModel per screen consuming this
