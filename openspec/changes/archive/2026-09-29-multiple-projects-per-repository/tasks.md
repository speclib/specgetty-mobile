## 1. Identity splits in two

- [x] 1.1 Derive a clone id from the URL alone and an entry id from the URL and
      the path. Verify unit tests that one URL with two paths gives two entry
      ids and one clone id
- [x] 1.2 Make the empty path give the id the URL alone gave before. Verify a
      unit test asserting the id of a URL with the empty path equals the value
      the current function returns for that URL
- [x] 1.3 Separate the two ids with a character no path can hold. Verify a unit
      test that `a` with `b/c` and `a/b` with `c` do not collide
- [x] 1.4 Keep both ids safe as directory names. Verify the existing id tests
      pass against both

## 2. The entry carries a path

- [x] 2.1 Add the path to the stored entry, defaulting to empty. Verify a unit
      test decodes a list written without the field and gets the empty path
- [x] 2.2 Hold one entry per URL and path rather than per URL. Verify a unit
      test adds two paths from one URL and gets two entries, and adds the same
      pair twice and gets one
- [x] 2.3 Keep the working copy and the token under the clone id. Verify a unit
      test that a second entry on a repository with a token stores no second
      copy
- [x] 2.4 Delete the working copy and the token only with the last entry for a
      URL. Verify unit tests for both cases: one of two removed keeps them, the
      last removed deletes them

## 3. Finding the projects

- [x] 3.1 Qualify a directory by its `openspec/` holding `config.yaml`,
      `config.yml` or `project.md`. Verify unit tests for each spelling and for
      a directory holding `specs/` and none of the three
- [x] 3.2 Sweep a working copy for every qualifying directory, skipping `.git`.
      Verify a unit test against a fixture repository holding four projects at
      its top level and one deeper
- [x] 3.3 Order the result by path, root first. Verify a unit test that two
      sweeps of one fixture return the same order
- [x] 3.4 Report a repository with none as holding no project. Verify a unit
      test asserting the existing message is unchanged
- [x] 3.5 Name a discovered project by its directory, reading nothing from
      `.openspec-store/store.yaml`. Verify a unit test against a fixture whose
      identity file names something other than its directory

## 4. Loading at a path

- [x] 4.1 Take the path when locating a project, treating empty as the root.
      Verify unit tests that a root project and a project at `nivis` both load
- [x] 4.2 Report a recorded path whose project is gone, without substituting a
      sibling. Verify a unit test with a fixture whose sibling does hold one
- [x] 4.3 Load two entries on one repository independently. Verify a unit test
      asserting each index holds its own specs
- [x] 4.4 Keep the empty-project state for a project that qualifies and holds
      nothing. Verify the existing empty-project tests pass

## 5. A declaration that cannot be followed

- [x] 5.1 Read `store:` from a project's configuration, accepting both file
      spellings. Verify unit tests for `config.yaml` and `config.yml`
- [x] 5.2 Report the declared id and the file that declared it rather than an
      absent project. Verify a unit test asserting both appear and that the
      "no OpenSpec project here" message does not
- [x] 5.3 Ignore a declaration when the project holds its own specs or changes.
      Verify a unit test with both present
- [x] 5.4 Report a `store` key that is a list or a mapping as malformed. Verify
      a unit test for each shape

## 6. Adding, and choosing

- [x] 6.1 Survey the clone after cloning and before creating entries. Verify a
      unit test that a repository with no projects creates no entry
- [x] 6.2 Add a repository holding one project exactly as now, with nothing to
      choose. Verify the existing add tests pass untouched
- [x] 6.3 Offer the projects by name when there is more than one, adding
      nothing yet. Verify a view model test asserting the list is unchanged
      while the choice is open
- [x] 6.4 Add the chosen projects on confirmation. Verify a view model test
      that choosing two of four gives two entries
- [x] 6.5 Add nothing when the choice is abandoned, keeping the working copy.
      Verify a view model test asserting an empty list and a clone still on
      disk
- [x] 6.6 Reuse a working copy already present when the same URL is added
      again. Verify a view model test asserting no second clone

## 7. The list screen

- [x] 7.1 Show the directory and the repository on a row for a project in a
      subdirectory. Verify an instrumented test asserting both are on screen
- [x] 7.2 Leave a row for a root project as it is. Verify the existing
      screenshot and end-to-end tests pass
- [x] 7.3 Give each row its own statistics rather than a repository total.
      Verify an instrumented test with two rows from one repository
- [x] 7.4 Present the choice when a repository holds several projects. Verify
      an instrumented test choosing two of four
- [x] 7.5 Show a declaration that cannot be followed on the row it belongs to.
      Verify an instrumented test asserting the declared id is shown

## 8. Refreshing

- [x] 8.1 Collapse rows by clone id when refreshing everything. Verify a unit
      test with two entries on one repository asserting one fetch
- [x] 8.2 Reload every entry on a repository after its working copy changes.
      Verify a unit test asserting both indexes are rebuilt

## 9. Documentation

- [x] 9.1 Amend the Phase 1 non-goal in `BRIEFING.md` so that scanning for
      projects within a repository is allowed and store resolution stays out.
      Verify the wording separates the two rather than dropping the pair
- [x] 9.2 Say in the README that one repository can contribute several rows,
      that they share one clone and one credential, and that removing one frees
      nothing until the last goes. Verify by following it against the app

## 10. End to end

- [x] 10.1 Add a repository holding four projects, choose two, and read both.
      Verify against a local fixture repository served over HTTP
- [x] 10.2 Remove one of two rows and confirm the other still loads. Verify an
      instrumented test asserting the working copy survived
- [x] 10.3 Read a list stored before this change and confirm nothing re-clones.
      Verify an instrumented test writing the old stored form first
- [x] 10.4 `scripts/gate.sh` passes, and coverage of the discovery code is at
      or above the floor its package carries
