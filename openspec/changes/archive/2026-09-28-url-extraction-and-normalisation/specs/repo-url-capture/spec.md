## Purpose

Getting a repository URL into the app without typing it. This capability owns
what counts as a URL in arbitrary shared text, how a forge's page URL becomes a
clone URL, and the rule that a URL arriving from outside the app is a
suggestion rather than an instruction.

## ADDED Requirements

### Requirement: A web address is extracted from arbitrary text

The system SHALL find the first `http` or `https` address in a piece of text
and SHALL report that no address was found when there is none.

#### Scenario: Text surrounding a URL

- **WHEN** the text is `Look at https://github.com/speclib/specgetty nice one`
- **THEN** the extracted address is `https://github.com/speclib/specgetty`

#### Scenario: Text that is only a URL

- **WHEN** the text is a bare address
- **THEN** that address is extracted

#### Scenario: Trailing punctuation

- **WHEN** an address is followed by a full stop, a comma or a closing bracket
- **THEN** that character is not part of the extracted address

#### Scenario: Several addresses

- **WHEN** the text contains more than one address
- **THEN** the first is extracted

#### Scenario: No address

- **WHEN** the text contains no `http` or `https` address
- **THEN** the result reports that none was found

#### Scenario: Empty text

- **WHEN** the text is empty
- **THEN** the result reports that none was found

### Requirement: A page URL is normalised to a clone URL

The system SHALL convert a forge page URL into the URL of the repository it
belongs to, by discarding the query string and the fragment, and by truncating
a path that continues past the repository into a view of it.

Truncation SHALL only consider path segments from the third onward, so that a
repository whose own name matches a view segment is not destroyed. A `.git`
suffix SHALL be left as it is and SHALL NOT be added.

#### Scenario: Query string from a repository page

- **WHEN** the URL is `https://github.com/speclib/specgetty?tab=readme-ov-file`
- **THEN** the normalised URL is `https://github.com/speclib/specgetty`

#### Scenario: Fragment

- **WHEN** the URL ends with a `#readme` fragment
- **THEN** the fragment is removed

#### Scenario: A file view

- **WHEN** the URL is `https://github.com/speclib/specgetty/tree/main/src`
- **THEN** the normalised URL is `https://github.com/speclib/specgetty`

#### Scenario: An issues page

- **WHEN** the URL is `https://github.com/speclib/specgetty/issues/12`
- **THEN** the normalised URL is `https://github.com/speclib/specgetty`

#### Scenario: GitLab separates the view with a marker

- **WHEN** the URL is `https://gitlab.com/group/subgroup/proj/-/tree/main`
- **THEN** the normalised URL is `https://gitlab.com/group/subgroup/proj`

#### Scenario: A Gitea source view

- **WHEN** the URL is `https://codeberg.org/owner/repo/src/branch/main`
- **THEN** the normalised URL is `https://codeberg.org/owner/repo`

#### Scenario: A repository named after a view segment

- **WHEN** the URL is `https://github.com/someone/issues`
- **THEN** the normalised URL is unchanged

#### Scenario: An address that already clones

- **WHEN** the URL is `https://github.com/speclib/specgetty.git`
- **THEN** it is unchanged, and no `.git` is added to one that lacks it

#### Scenario: Trailing slash

- **WHEN** the URL ends with a slash
- **THEN** the slash is removed

#### Scenario: A host with no path

- **WHEN** the URL is a bare host
- **THEN** it is returned unchanged rather than mangled

#### Scenario: A host the rules do not recognise

- **WHEN** the URL is `https://git.example.test/team/repo`
- **THEN** it is returned unchanged rather than guessed at

### Requirement: Credentials are never taken from a captured URL

The system SHALL NOT carry an access token or a password out of captured text,
and SHALL remove any credentials embedded in a captured URL's authority.

#### Scenario: A captured URL carrying credentials

- **WHEN** a captured URL is `https://user:ghp_secret@github.com/a/b`
- **THEN** the normalised URL is `https://github.com/a/b`
- **AND** the credentials appear nowhere in the result

#### Scenario: A token in the query string

- **WHEN** a captured URL carries a token as a query parameter
- **THEN** the query string is discarded with it

### Requirement: A captured URL is a suggestion, not an instruction

The system SHALL report a captured URL as a result for a caller to present, and
SHALL NOT itself clone, add or modify anything.

#### Scenario: A successful capture

- **WHEN** text containing a repository URL is captured
- **THEN** the result carries the normalised URL
- **AND** capturing has changed nothing else

#### Scenario: Captured text that is not a URL

- **WHEN** the captured text contains no web address
- **THEN** the result says so rather than returning an empty address
