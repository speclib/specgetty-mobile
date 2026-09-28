## Purpose

How an outline and the card for the node selected in it are arranged, and what
the back action means in each arrangement.

## ADDED Requirements

### Requirement: A narrow window is two levels, a wide one is two panes

The system SHALL show the outline and the card as two navigation levels when the
window is narrow, and side by side when it is wide, deciding from the window's
width rather than from the kind of device.

#### Scenario: A phone

- **WHEN** the window is narrow and no node is selected
- **THEN** the outline fills the screen

#### Scenario: A phone with a node selected

- **WHEN** the window is narrow and a node is selected
- **THEN** the card fills the screen and the outline is not shown

#### Scenario: A tablet

- **WHEN** the window is wide
- **THEN** the outline and the card are shown side by side

#### Scenario: A window that changes width

- **WHEN** a window is resized from wide to narrow with a node selected
- **THEN** the card is shown, the selection being kept

#### Scenario: The decision is the window's, not the device's

- **WHEN** an app runs in a narrow freeform window on a large screen
- **THEN** it is laid out as narrow

### Requirement: Back means what the arrangement makes it mean

The system SHALL return from the card to the outline when they are two levels,
and SHALL leave the screen when they are side by side, there being no level to
return from.

#### Scenario: Back from a card on a phone

- **WHEN** the window is narrow, a node is selected, and back is used
- **THEN** the outline is shown and the screen is not left

#### Scenario: Back from the outline on a phone

- **WHEN** the window is narrow, nothing is selected, and back is used
- **THEN** the screen is left

#### Scenario: Back on a tablet

- **WHEN** the window is wide and back is used
- **THEN** the screen is left, whether or not a node is selected

### Requirement: The detail pane is never blank for no reason

The system SHALL show, in the detail pane of a wide window with nothing
selected, what the pane is for.

#### Scenario: Nothing selected on a tablet

- **WHEN** the window is wide and no node has been selected
- **THEN** the detail pane says to choose something from the outline

#### Scenario: Something selected

- **WHEN** a node is selected
- **THEN** the detail pane shows it
