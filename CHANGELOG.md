# Changelog

All notable changes to this project are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Project scaffolding: OpenSpec, beans, the nix flake and the ship script.
- An Android application that builds and installs, showing an empty repository
  list. Nothing can be added to it yet.
- The git layer: shallow clone, refresh by fetch and hard reset, and delete,
  over HTTPS with an optional access token. Failures say whether they were an
  authentication problem, a network problem, or a repository with no OpenSpec
  project in it.
