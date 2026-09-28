---
# specgetty-mobile-vo5e
title: Nix flake and the coverage gate
status: completed
openspec-link: openspec/changes/archive/2026-09-28-gradle-skeleton-and-coverage-gate
type: epic
priority: normal
created_at: 2026-09-28T19:47:14Z
updated_at: 2026-09-28T20:06:37Z
parent: specgetty-mobile-k5kk
blocked_by:
    - specgetty-mobile-e5xj
---

Plain nix, explicit systems, no flake-utils. nix flake check is what /mip:ship gates on.

- [ ] flake.nix with x86_64-linux, aarch64-linux, aarch64-darwin listed explicitly
- [ ] devShell with the JDK, Android SDK and the command line tools
- [ ] checks that run assemble, test and lint
- [ ] JaCoCo coverage gate: 70 percent overall, 80 percent on parser and index packages
- [ ] The gate fails loudly while there are no tests, which is intended
