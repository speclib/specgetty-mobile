## ADDED Requirements

### Requirement: A code is scanned with the camera

The system SHALL offer to read a QR code with the camera, SHALL ask for camera
access when scanning is first chosen rather than at launch, and SHALL release
the camera when the scanner leaves the screen.

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

### Requirement: A URL is shared into the app

The system SHALL accept plain text shared from another application, SHALL
accept it whether the app was running or not, and SHALL claim no other intent.

#### Scenario: Sharing while the app is closed

- **WHEN** text is shared into the app and the app was not running
- **THEN** the add form opens with the URL from that text

#### Scenario: Sharing while the app is open

- **WHEN** text is shared into the app while it is already running
- **THEN** the same happens, rather than the share being dropped

#### Scenario: Only plain text is claimed

- **WHEN** the system offers share targets
- **THEN** this app appears for plain text and not for every tapped link

#### Scenario: Shared text with no URL

- **WHEN** the shared text holds no web address
- **THEN** the person is told and nothing is added
