---
# specgetty-mobile-cjzs
title: Repository store over JGit
status: todo
type: epic
priority: normal
created_at: 2026-09-28T19:47:14Z
updated_at: 2026-09-28T19:47:14Z
parent: specgetty-mobile-4cpl
blocked_by:
    - specgetty-mobile-vo5e
---

repo/RepoStore. Copied from beans-on-droid and kept close to it.

- [ ] JGit 6.4.0 pinned, with the SystemReader from beans-on-droid
- [ ] Shallow clone at depth 1 into app-private storage
- [ ] Refresh is fetch plus hard reset to the remote branch
- [ ] Delete removes the working copy
- [ ] HTTPS with an optional personal access token, no SSH
- [ ] Errors are distinguishable: auth failure, network error, not a git repo
- [ ] Tests against a real local HTTP git server, as beans-on-droid does
