---
# specgetty-mobile-8swu
title: 04 Parsers
status: in-progress
type: milestone
priority: normal
created_at: 2026-09-28T19:47:14Z
updated_at: 2026-09-28T20:21:12Z
---

The only code that knows the OpenSpec grammar.

First read specgetty: src/ui/specparse.go, src/ui/deltaparse.go and
src/ui/taskitems.go, and the fixtures in src/ui/testdata/specs/. Do not derive
the grammar from the briefing. Where specgetty and the briefing disagree,
specgetty wins and the difference is noted in design.md.
