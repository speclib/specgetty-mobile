---
# specgetty-mobile-ggx5
title: Project loader
status: completed
openspec-link: openspec/changes/archive/2026-09-28-project-loader
type: epic
priority: normal
created_at: 2026-09-28T19:47:15Z
updated_at: 2026-09-28T20:36:53Z
parent: specgetty-mobile-61i0
blocked_by:
    - specgetty-mobile-2fuj
---

project/ProjectLoader. One project per repo, at openspec/ in the repo root.

- [ ] Walk openspec/specs, openspec/changes and openspec/changes/archive
- [ ] Read config.yaml or config.yml, and project.md when present
- [ ] Read each change .openspec.yaml for its workflow schema name
- [ ] No directory scanning, no store resolution, no openspec CLI calls
- [ ] A repo with no openspec/ at its root is a distinct outcome from an empty project
- [ ] Specs and deltas parse lazily, when opened
