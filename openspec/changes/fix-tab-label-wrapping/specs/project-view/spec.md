## ADDED Requirements

### Requirement: A tab label is legible whatever the screen

The system SHALL show each tab's label in full on one line, and SHALL NOT break
a label across lines or cut a word in half, whatever the width of the screen or
the size of the reader's type.

#### Scenario: A narrow screen

- **WHEN** the four tabs do not fit side by side at their natural widths
- **THEN** the row scrolls rather than squeezing a label into a column narrower
  than the word

#### Scenario: A label is never broken mid-word

- **WHEN** any tab is drawn
- **THEN** its label is on one line, and no word in it is split

#### Scenario: Large type

- **WHEN** the reader has set a larger system font size
- **THEN** the labels are still whole

#### Scenario: A wide screen

- **WHEN** the tabs fit comfortably
- **THEN** they are shown as they were
