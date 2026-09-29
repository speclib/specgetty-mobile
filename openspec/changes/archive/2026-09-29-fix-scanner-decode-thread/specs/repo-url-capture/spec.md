## MODIFIED Requirements

### Requirement: A code is scanned with the camera

The system SHALL offer to read a QR code with the camera, SHALL ask for camera
access when scanning is first chosen rather than at launch, and SHALL release
the camera when the scanner leaves the screen.

A decoded code SHALL be delivered on the main thread, and SHALL be delivered
once, however many frames recognise it.

#### Scenario: Scanning is chosen

- **WHEN** the scan action is used and camera access has not been granted
- **THEN** the system asks for it

#### Scenario: Access refused

- **WHEN** camera access is refused
- **THEN** the scanner says why it cannot scan and typing and pasting still work

#### Scenario: A device with no camera

- **WHEN** the app is installed on a device with no camera
- **THEN** it installs, the camera being declared as not required

#### Scenario: Leaving the scanner

- **WHEN** the scanner leaves the screen
- **THEN** the camera is released

#### Scenario: A code that is not a URL

- **WHEN** a decoded code holds no web address
- **THEN** the person is told, and scanning continues

#### Scenario: A code that decodes

- **WHEN** a frame is recognised as a QR code
- **THEN** the add form opens with the URL in it and the scanner closes
- **AND** the app does not crash

#### Scenario: The decode arrives from the camera's thread

- **WHEN** the image analyser recognises a code
- **THEN** the navigation and the form are touched on the main thread, not on
  the analyser's thread

#### Scenario: Several frames recognise the same code

- **WHEN** more than one frame decodes before the scanner has closed
- **THEN** the code is acted on once
