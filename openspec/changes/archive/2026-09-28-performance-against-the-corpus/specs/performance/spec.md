## Purpose

What the app's work costs on a real OpenSpec project, measured rather than
asserted, and which parts of the claim a build machine cannot settle.

## ADDED Requirements

### Requirement: The cost of reading a real project is measured

The system SHALL measure, against a vendored project of about 30 specs and an
archive of more than 50 changes, the time to load it, to build its index, to
search it by name and by text, and to parse every spec and delta.

#### Scenario: Loading

- **WHEN** the project is loaded from disk
- **THEN** the time is measured and reported

#### Scenario: Searching by name

- **WHEN** a name query runs over every change
- **THEN** the time is measured, and no artifact file is read

#### Scenario: Searching the text

- **WHEN** a text query runs over every change
- **THEN** the time is measured, the files it had to read being the cost

#### Scenario: Parsing everything

- **WHEN** every spec and every delta in the project is parsed
- **THEN** the total time and the number of files are reported

#### Scenario: Parsing twice

- **WHEN** the same spec is parsed again through the cache
- **THEN** the second time costs nothing measurable

### Requirement: A regression fails the gate

The system SHALL bound each measurement, loosely enough not to fail on a busy
machine and tightly enough to catch an order-of-magnitude regression.

#### Scenario: A large regression

- **WHEN** a change makes loading the project ten times slower
- **THEN** the measurement fails

#### Scenario: Ordinary variation

- **WHEN** the machine is busy
- **THEN** the measurement still passes

### Requirement: What a build machine cannot settle is said so

The system SHALL NOT claim that scrolling is smooth on the basis of a
measurement taken off a device.

#### Scenario: The scrolling claim

- **WHEN** the performance record is read
- **THEN** it says scrolling is checked on a device and not inferred from the
  JVM measurements

#### Scenario: The recorded numbers

- **WHEN** the performance record is read
- **THEN** it names the machine the numbers came from
