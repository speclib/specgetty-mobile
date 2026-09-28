# release-process Specification

## Purpose
Cutting a release: bumping the version, dating the changelog, keeping the two
vendor hashes in step, and committing, tagging and pushing, whether the working
copy is managed by git or by jj.

## Requirements

### Requirement: The release works in a jj workspace
The release script SHALL detect a jj workspace and use jj to commit, tag, move
the bookmark and push, rather than assuming git owns the working copy.

#### Scenario: Releasing from a jj colocated workspace
- **WHEN** the script runs in a directory where `jj root` succeeds
- **THEN** it SHALL create the release commit, tag and bookmark through jj, and
  push them to the remote

#### Scenario: Releasing from a plain git repository
- **WHEN** the script runs in a directory with no jj workspace
- **THEN** it SHALL behave exactly as it does today, using git throughout

#### Scenario: The tag reaches the remote
- **WHEN** a release is made from a jj workspace
- **THEN** the `vX.Y.Z` tag SHALL exist on the remote, because the release is
  published by a tag and a release that does not tag does not publish

### Requirement: A dirty working copy stops the release
The script SHALL refuse to release when the working copy has changes that would
not be part of the release commit, whichever tool manages it.

#### Scenario: Uncommitted changes under jj
- **WHEN** the jj working copy commit contains changes
- **THEN** the script SHALL stop and say so, without editing any file

#### Scenario: Untracked files
- **WHEN** the working copy contains a file that is not tracked
- **THEN** the script SHALL stop and say so, because an untracked file that
  belongs in the release would otherwise be silently left out

### Requirement: File edits are portable
The in-place edits to the version file, the changelog and the vendor hashes
SHALL produce the same result on GNU and BSD userlands.

#### Scenario: Dating the changelog on macOS
- **WHEN** the script inserts the new version heading into `CHANGELOG.md` on a
  system with BSD sed
- **THEN** the heading SHALL be inserted on its own line, not with a literal `n`
  where the line break belongs

#### Scenario: The two vendor hashes stay in step
- **WHEN** the vendor hash is recomputed during a release
- **THEN** both `package.nix` and `flake.nix` SHALL receive the new value, since
  a stale hash in either one fails `nix flake check`

### Requirement: A release publishes the changelog entry it wrote
The release SHALL publish, as the release notes, the changelog section for the
version being released: the lines from its `## [X.Y.Z]` heading to the next
`## [` heading. The notes SHALL NOT be assembled from commit subjects, a release
publishing one account of itself rather than two that can disagree.

#### Scenario: The notes are the entry
- **WHEN** a release is published for a version whose changelog section names
  what changed
- **THEN** the release notes SHALL be that section, and SHALL NOT be a list of
  commit hashes and subjects

#### Scenario: Only this version's entry
- **GIVEN** a changelog holding several versions
- **WHEN** one of them is released
- **THEN** the notes SHALL hold that version's section alone, and neither the
  `## [Unreleased]` heading above it nor the version below it

#### Scenario: Housekeeping is not published
- **GIVEN** a release whose commits include bookkeeping that names no user
  visible change
- **WHEN** the release is published
- **THEN** those commits SHALL NOT appear in the notes, the notes being what the
  changelog says rather than what the history contains

### Requirement: A release refuses to publish an empty entry
A version whose changelog section holds nothing means the changelog was not
written before the release was cut. The script SHALL stop and say so, rather
than publishing a heading with nothing under it.

#### Scenario: Nothing was written
- **WHEN** the section for the version being released holds no entry
- **THEN** the release SHALL stop before tagging, and SHALL say that the
  changelog is empty

#### Scenario: Nothing is left half done
- **WHEN** the release stops for an empty entry
- **THEN** no tag SHALL have been created and no commit pushed, so that writing
  the entry and running the release again is all that is needed

### Requirement: One release is created, and both platforms add to it
The release is built for two platforms by two jobs. One of them SHALL create the
release, and the other SHALL add its artifacts to the release that already
exists rather than racing to create it. Both SHALL be given the same notes, so
that neither can publish an account of the release that the other would
contradict.

This is how the workflow already behaves, and it is written down here because
publishing the changelog entry is what makes it load-bearing: two runs that
disagreed about the notes would publish whichever finished last.

#### Scenario: Both platforms' artifacts are published
- **WHEN** a release completes
- **THEN** the artifacts and checksums for both platforms SHALL be attached to
  it

#### Scenario: The notes are written once
- **WHEN** a release completes
- **THEN** the notes SHALL be the changelog entry, whichever order the two jobs
  finished in
