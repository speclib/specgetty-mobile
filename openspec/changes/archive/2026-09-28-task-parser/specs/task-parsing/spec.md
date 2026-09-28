## Purpose

Reading a change's `tasks.md` into items and counts. This capability owns which
lines are tasks, because the same rule has to decide both what is drawn as a
checkbox and what the progress figure counts.

## ADDED Requirements

### Requirement: A task is a checkbox at column zero

The system SHALL count a line as a task only when it begins, at column zero,
with `- [ ] ` or `- [x] `, and SHALL NOT count any other shape.

#### Scenario: The two shapes that count

- **WHEN** a line reads `- [ ] 1.1 Do it` or `- [x] 1.1 Do it`
- **THEN** it is a task, done in the second case and not in the first

#### Scenario: An indented checkbox

- **WHEN** a checkbox line is indented
- **THEN** it is not a task of its own

#### Scenario: Another list marker

- **WHEN** a checkbox line begins with `*` or `+`
- **THEN** it is not a task

#### Scenario: An upper-case cross

- **WHEN** a line reads `- [X] 1.1 Do it`
- **THEN** it is not a task, because the counter matches the lower-case form

#### Scenario: A checkbox with no space after it

- **WHEN** a line reads `- [x]1.1 Do it`
- **THEN** it is not a task

#### Scenario: A line that merely mentions a checkbox

- **WHEN** a line of prose contains `- [ ]` somewhere after its first column
- **THEN** it is not a task

### Requirement: A task carries the lines that continue it

The system SHALL treat the indented, non-blank lines after a checkbox line as
part of that task, and SHALL end the task at a blank line, an unindented line,
a heading or the next checkbox.

#### Scenario: A wrapped task

- **WHEN** a task's text is wrapped onto an indented second line
- **THEN** both lines belong to the one task

#### Scenario: A blank line ends it

- **WHEN** a blank line follows a task
- **THEN** the task ends there

#### Scenario: The next checkbox ends it

- **WHEN** another checkbox line follows immediately
- **THEN** the first task ends and the second begins

#### Scenario: An indented checkbox is a continuation

- **WHEN** an indented checkbox line follows a task
- **THEN** it belongs to that task rather than being a task of its own

### Requirement: Counts are done and total

The system SHALL report how many tasks are done and how many there are, and
SHALL report zero of zero for a file with no tasks.

#### Scenario: A mixed file

- **WHEN** a file holds three done tasks and two not done
- **THEN** the count is 3 of 5

#### Scenario: No tasks

- **WHEN** a file holds no checkbox at column zero
- **THEN** the count is 0 of 0

#### Scenario: An absent file

- **WHEN** a change has no `tasks.md`
- **THEN** its count is 0 of 0 rather than an error

### Requirement: Counts add up across changes

The system SHALL sum the counts of several changes into one figure, which is
what a repository's row shows.

#### Scenario: Two changes

- **WHEN** one change is 3 of 5 and another is 2 of 4
- **THEN** the aggregate is 5 of 9

#### Scenario: Nothing to add

- **WHEN** there are no changes, or none of them holds tasks
- **THEN** the aggregate is 0 of 0

### Requirement: The file's own sections are kept

The system SHALL record the heading each task sits under, so the tasks tab can
group them as the file does.

#### Scenario: Tasks under headings

- **WHEN** a file groups its tasks under `## 1. Something` headings
- **THEN** each task records the heading above it

#### Scenario: A task before any heading

- **WHEN** a task appears before the first heading
- **THEN** it records no heading rather than the wrong one

### Requirement: A task keeps its place in the file

The system SHALL record the source line index of each task's checkbox line, so
that a later phase can rewrite that line in place.

#### Scenario: The index is the source line

- **WHEN** a task is read from the fourth line of the file
- **THEN** it records index 3, counting from zero

#### Scenario: Reading changes nothing

- **WHEN** a `tasks.md` is parsed
- **THEN** the file on disk is unchanged
