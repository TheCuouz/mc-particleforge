# Changelog

All notable changes to ParticleForge are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This project adheres to [Semantic Versioning](https://semver.org/).

## [1.0.0] — 2026-05-15

Initial release.

### Added
- Core effect engine with central per-tick scheduler and per-handle crash isolation.
- 15 geometric primitives (BURST, RING, SPHERE, HELIX, VORTEX, SHOCKWAVE, CONE, RAIN, CYLINDER, CUBE, LINE, TRAIL, ORBIT, TEXT, SHAPE) and 3 flow steps (DELAY, REPEAT, PARALLEL).
- YAML effect definitions with `extends:` inheritance and `${param}` substitution.
- Fluent `Effects` static API for programmatic use.
- 24 pre-built effects across 7 categories (crate, claims, skills, combat, quests, teleport, homes).
- `/pf` command suite with `preview`, `list`, `info`, `debug`, `reload`, `stop` subcommands.
- Per-player and global budget caps with FIFO / NEWEST_FIRST eviction policies.
- LOD culling (near / mid / far) with per-bucket particle multipliers.
- English and Spanish translations.
- bStats integration (custom charts queued until plugin ID is assigned).

[1.0.0]: https://github.com/Cristian/mc-particleforge/releases/tag/v1.0.0
