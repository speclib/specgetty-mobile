# github-authorization Specification

## Purpose
Obtaining a credential for a GitHub repository by authorizing on github.com
rather than by typing one, and keeping that credential usable for as long as the
repository is in the list. This capability owns what the app asks GitHub for,
what it does with the answer, and what it tells the person when the answer is
not enough.

## Requirements

### Requirement: The app never holds a secret of its own

The system SHALL authorize as a public client, and SHALL NOT ship, embed or
obfuscate any credential that identifies the application privately.

#### Scenario: What is in the source

- **WHEN** the application's source is read
- **THEN** it carries a public client identifier and no client secret

#### Scenario: A flow that would need a secret

- **WHEN** an authorization flow requires a client secret to complete
- **THEN** it is not used

#### Scenario: Somebody builds their own copy

- **WHEN** a person builds the app from source
- **THEN** authorization works without them being given any secret

### Requirement: A credential is obtained by authorizing on github.com

The system SHALL obtain a credential by asking GitHub for a code, showing that
code to the person, sending them to GitHub to enter it, and waiting for GitHub
to say they approved.

#### Scenario: The code is shown

- **WHEN** authorization begins
- **THEN** a short code is shown, along with the address to enter it at

#### Scenario: Getting to GitHub

- **WHEN** the person chooses to continue
- **THEN** the address opens outside the app, in their browser

#### Scenario: Approval

- **WHEN** the person approves on GitHub
- **THEN** the app obtains a credential without further typing

#### Scenario: The code can be copied

- **WHEN** the code is on screen
- **THEN** it can be copied, so it need not be transcribed by hand

#### Scenario: Waiting

- **WHEN** the app is waiting for approval
- **THEN** it says so, rather than appearing to have stopped

#### Scenario: Returning from the browser

- **WHEN** the person leaves the app to approve and comes back
- **THEN** the app is still waiting, and completes when approval arrives

### Requirement: Every way authorization can fail is accounted for

The system SHALL recognise each outcome GitHub reports while waiting, SHALL act
on it, and SHALL NOT treat an unfinished authorization as a failed one.

#### Scenario: Not yet approved

- **WHEN** GitHub reports that authorization is still pending
- **THEN** the app keeps waiting

#### Scenario: Asked to wait longer

- **WHEN** GitHub asks the app to slow down
- **THEN** the app waits longer between asking again

#### Scenario: The code expired

- **WHEN** the code expires before it is used
- **THEN** the person is told, and offered a fresh one

#### Scenario: Refused

- **WHEN** the person declines on GitHub
- **THEN** the app says so and stops waiting

#### Scenario: Authorization is unavailable

- **WHEN** GitHub reports that this kind of authorization is not enabled
- **THEN** the person is told the app cannot authorize, and typing a token still
  works

#### Scenario: Giving up

- **WHEN** the person abandons the attempt
- **THEN** waiting stops and nothing is stored

### Requirement: The app asks only to read

The system SHALL request the ability to read repository contents and nothing
else, and SHALL NOT request the ability to write while it cannot write.

#### Scenario: What is granted

- **WHEN** a credential has been obtained
- **THEN** it permits reading repository contents

#### Scenario: What is not granted

- **WHEN** a credential has been obtained
- **THEN** it does not permit writing to any repository

#### Scenario: Asking for more later

- **WHEN** a later version of the app needs to write
- **THEN** the additional access is requested explicitly and the person approves
  it, rather than it having been taken in advance

### Requirement: Authorizing is not the same as granting access to a repository

The system SHALL treat approval and the choice of repositories as two separate
things, and SHALL NOT report success when it holds a credential that reaches
nothing.

#### Scenario: Approved but nothing selected

- **WHEN** the person approves but has selected no repositories
- **THEN** the app says the credential reaches nothing yet, and says how to fix
  it

#### Scenario: The repository being added is not among them

- **WHEN** the credential does not cover the repository the person is adding
- **THEN** the app says so before cloning, rather than letting it fail later

#### Scenario: What was granted is reported

- **WHEN** authorization succeeds
- **THEN** the person is told which repositories the credential reaches

#### Scenario: Changing the selection

- **WHEN** the person changes which repositories are selected on GitHub
- **THEN** the app reflects the change without the credential being obtained
  again

### Requirement: A credential that expires renews itself

The system SHALL renew a credential that is near expiry before using it, without
the person being involved, and SHALL say so plainly when it cannot.

#### Scenario: Still valid

- **WHEN** a credential is used well before it expires
- **THEN** it is used as it is

#### Scenario: Near expiry

- **WHEN** a credential is close to expiring and is needed
- **THEN** it is renewed first, and the operation proceeds

#### Scenario: Renewal needs no secret

- **WHEN** a credential is renewed
- **THEN** it is renewed without any secret the app does not have

#### Scenario: Renewal is no longer possible

- **WHEN** renewal is refused, for instance because too long has passed
- **THEN** the person is told the repository must be authorized again

#### Scenario: Renewal is invisible when it works

- **WHEN** renewal succeeds
- **THEN** the person is not interrupted
