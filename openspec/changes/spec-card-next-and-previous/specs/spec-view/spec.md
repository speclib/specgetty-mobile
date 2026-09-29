## ADDED Requirements

### Requirement: A card can be stepped to the next and the previous node

The screen SHALL offer, on an open card, a way to move to the next node and to
the previous node in the outline's own order, so that a spec can be read
straight through without returning to the outline between every node.

The order SHALL be the outline's: Purpose, then each requirement followed by its
own scenarios, as the file gives them.

#### Scenario: Reading forward

- **WHEN** the card for a requirement is open and the next action is used
- **THEN** the card shows that requirement's first scenario

#### Scenario: Reading on past the last scenario

- **WHEN** the card for a requirement's last scenario is open and the next
  action is used
- **THEN** the card shows the following requirement

#### Scenario: Reading back

- **WHEN** the previous action is used
- **THEN** the card shows the node before this one in the same order

#### Scenario: The first node

- **WHEN** the card for the first node in the outline is open
- **THEN** there is no previous to go to, and the action says so by being
  unavailable rather than by doing nothing

#### Scenario: The last node

- **WHEN** the card for the last node in the outline is open
- **THEN** there is no next to go to, and the action is likewise unavailable

#### Scenario: The ends do not wrap

- **WHEN** the last node is reached
- **THEN** stepping forward does not return to the first, which would lose the
  reader's place in a long spec

#### Scenario: A file that does not fit the grammar

- **WHEN** the file is shown as a report rather than an outline
- **THEN** there are no nodes to step between and the actions are not offered

### Requirement: The outline follows the card

The screen SHALL show which node the card is for, so that stepping through a
spec does not lose the reader's place in the outline.

#### Scenario: Side by side

- **WHEN** the outline and the card are shown together and the card is stepped
  to another node
- **THEN** the outline marks the node the card is now on

#### Scenario: Going back to the outline

- **WHEN** the card is stepped several times and then left
- **THEN** the outline shows the node last read, rather than the one first
  chosen
