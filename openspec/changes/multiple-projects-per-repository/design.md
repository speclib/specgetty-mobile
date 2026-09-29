## Context

See proposal.md for motivation.

One hash does four jobs today. `repoIdFor(url)` names the clone directory, keys
the token vault, forms the navigation route and identifies the list entry, and
`RepoList.add` treats the URL as the thing a list holds one of. Four rows from
one URL breaks every one of those at once, so the shape of this change is
settled before any screen is drawn.

The repository that prompted it holds four projects, one per top-level
directory, each with `openspec/specs/`, `openspec/changes/` and
`openspec/config.yaml`. It is 4 MB with 1.4 MB of history. Nothing in it
declares `store:`; these are the directories a store declaration points at, not
pointers themselves.

specgetty owns the behaviour this borrows, in `openspec-scanning` and
`store-resolution`. Where its specs and `BRIEFING.md` disagree, its specs win.

## Goals / Non-Goals

**Goals:**

- One clone, one credential, one fetch per repository, however many of its
  projects are in the list.
- A repository holding one project behaves exactly as it does now, with no
  extra step and no extra tap.
- A list stored before this change reads back and keeps using the working copy
  already on the device.

**Non-Goals:**

- Following a `store:` declaration. Reporting one honestly is in scope;
  resolving it is not, and cannot be.
- Reading `.openspec-store/store.yaml` for anything, including a name.
- Any use of the word "store" in the interface.
- Combining several projects into one view, or showing a repository's projects
  as a tree. Each chosen project is a row like any other.

## Decisions

### Identity splits in two: the clone by URL, the entry by URL and path

```
                    ┌──────────────────────────────┐
  url ─────────────►│  clone id = hash(url)        │──► working copy, token
                    └──────────────────────────────┘
                                   │
         ┌─────────────────┬───────┴───────┬──────────────────┐
         ▼                 ▼               ▼                  ▼
   entry(url,"")   entry(url,"nivis")  entry(url,"registry")  ...
   entry id = hash(url + NUL + path)   ──► route, list key, index, cache
```

The separator is NUL, the one byte a path cannot hold, so that `a` with `b/c`
and `a/b` with `c` cannot collide.

The empty path hashes to the id the URL alone produced before, so every entry in
a stored list keeps its id and its already-cloned working copy. That is what
makes the migration nothing at all.

Alternatives considered. **One entry per repository, with the projects inside
it** keeps identity as it is, but a repository row then has to show the combined
statistics of four projects, which are meaningless added together, and every
screen below it gains a repository-or-project question. **A synthetic URL per
project**, such as appending a fragment, keeps one id but makes the URL a lie:
it would be shown, edited and cloned from.

### Removal is conditional, and so is the token

Deleting the working copy and the token when any entry goes would break the
sibling entries still using them. Both deletions become conditional on the
removed entry being the last one for its URL.

This is the trap most likely to be missed, because it only appears with two
entries on one repository and looks like a working feature until then. Its
scenarios are in `repo-registry`.

### The whole tree is swept, bounded by what qualifies rather than by a depth

A depth limit is a number that is wrong for somebody. The bound that works is
the qualification rule itself: `openspec/` must hold `config.yaml`, `config.yml`
or `project.md`, which is what specgetty's scanner requires. A repository full
of directories called `openspec` holding other things contributes nothing.

`.git` is skipped. The clone is `depth 1`, so the tree is the working copy and
nothing more.

Alternative considered: **depth 1 only**, which covers the repository that
prompted this exactly. It is rejected because `clients/acme/openspec` is the
obvious next shape and there is no principle that makes one level right.

### The qualification rule is stricter than the one used at the root

`OpenSpecLayout` today calls any directory named `openspec` a project, and the
empty-project state depends on that. Applied to a sweep, that rule turns every
stray `openspec/` directory into a row.

The strict rule therefore governs discovery. Loading an entry keeps the existing
behaviour, so a project that qualifies but holds nothing still loads as an empty
project rather than an absent one. The two rules answer different questions:
"is this worth offering?" and "what is here?".

### Choosing happens after the clone, because the clone is what reveals the choice

Nothing can be listed before the repository is on the device. The order becomes:

```
  confirm ──► register ──► clone ──► survey
                                       │
                          ┌────────────┼─────────────┐
                          ▼            ▼             ▼
                      0 projects   1 project    N projects
                          │            │             │
                     "no project   add it       offer them,
                      here"        (as now)     add the chosen
```

The clone is kept whichever way the choice goes, since it is the thing that
would have to be fetched again. A repository whose projects are all declined
leaves a working copy and no entry, which the next add of that URL reuses.

Alternative considered: **survey before registering**, which avoids that
orphan. It requires a clone with nowhere to put it, so it invents a second
staging location and a second failure path for no gain.

### A `store:` declaration is reported, not followed

A declaration is followed through a registry of local paths under
`$XDG_DATA_HOME`, naming directories on the machine that wrote it. A phone has
neither. The report names the id and the file, which is what specgetty's
`store-resolution` requires of a declaration that cannot be followed.

It rides along in this change rather than waiting, because it is one branch in
the code path being rewritten anyway, and because leaving it produces a message
that is known to be false.

### Nothing is named from `.openspec-store/store.yaml`

specgetty's rule: a directory is a store only when that file names an id **and**
the registry resolves that id back to that directory. The requirement says why
in its own words, and names the case: "a clone ... keeps its copy of that file
and would otherwise wear the registered store's name".

The app holds clones. It has no registry. So it can never satisfy the rule, and
a row named from that file would be exactly the misnaming the rule forbids.
Rows are named by directory.

## Risks / Trade-offs

- **Two entries on one repository refresh it twice.** Pull-to-refresh walks the
  rows. → Refreshing collapses rows by clone id, so a repository is fetched once
  per refresh whatever its row count.

- **A survey on a large repository walks its whole working copy.** → The clone
  is `depth 1`, the walk skips `.git`, and it happens once per add rather than
  per open. Should a repository ever make it slow, the walk is the thing to
  bound, not the rule.

- **A person adds four rows and later cannot tell why removing one kept the
  clone.** → Nothing in the interface claims otherwise; the behaviour is that
  removal frees nothing until the last row goes. Worth saying in the README.

- **The strict qualification rule hides a project that has content but no
  configuration file.** A directory holding `openspec/specs/` and nothing else
  is not offered. → This is specgetty's rule and the same projects appear in
  both tools. A root project keeps the loose rule, so nothing that loads today
  stops loading.

- **Discovery is settled at add time, so a project added later in the remote
  does not appear.** → Adding the repository again surveys it again and offers
  what is there now; the entries already held are untouched.

## Migration Plan

No migration step runs. An entry stored without a path decodes with the empty
path, whose id equals the id that entry already has, so it keeps its row, its
working copy, its token and its route. The scenarios are in `repo-registry`.

Rollback is the same in reverse for any entry at a repository root. An entry
carrying a non-empty path would be dropped by an older build, leaving its
working copy behind; that is acceptable for a build that never shipped.
