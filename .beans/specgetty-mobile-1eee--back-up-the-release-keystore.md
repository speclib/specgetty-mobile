---
# specgetty-mobile-1eee
title: Back up the release keystore
status: completed
type: task
priority: critical
created_at: 2026-09-29T17:38:24Z
updated_at: 2026-09-29T19:08:29Z
---

The release keystore was generated and loaded into repository secrets while
shipping `add-continuous-integration`. It has not been backed up.

```
~/.android/keystores/specgetty-mobile-release.jks
```

`v0.1.0` is published and signed with it, so this key is now the only thing that
can ever update the app for anyone who installed that APK. Losing it ends
updates for them. The only remedy is a new application ID, and `BRIEFING.md`
says the application ID is permanent once published, so there is no remedy.

- [ ] Copy the keystore to a second medium, kept apart from this machine
- [ ] Move the password into the password manager. It is in a `/tmp` scratchpad
      file that a reboot clears, and without it the keystore is useless
- [ ] Open the backup with that password on the second medium, so the backup is
      known to work rather than assumed to

`docs/fdroid.md` records the fingerprint the published APK carries, which is
what a backup has to still produce:

```
SHA-256  88:39:7b:84:d9:e0:8a:6d:b1:eb:e3:39:8c:27:b9:dd:
         27:2b:f9:43:59:cc:49:bc:6f:15:43:a0:47:c7:50:a9
```
