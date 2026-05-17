# Changelog

All notable changes to ParticleForge are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This project adheres to [Semantic Versioning](https://semver.org/).

## [1.0.3] — 2026-05-17

### Fixed
- `skills/mana-full` no longer dies silently in Paper 1.21.10. `Particle.INSTANT_EFFECT` now requires `Particle.Spell(color, power)` data; `SpawnUtil` branches on `Particle.getDataType()` and synthesizes the marker from new `color` + `power` YAML params (defaults: cyan `#66E1FF`, power `1.0`).

### Added
- `ParamUtil.color(...)` parses `#RRGGBB`, `RRGGBB`, `0xRRGGBB`, and a small set of named colors (RED, AQUA, LIGHT_BLUE, …) with safe fallback.
- `VortexStep` reads `color` and `power` from YAML params (used by Spell-data particles; ignored by no-data particles).
- `UnsupportedParticleDataException` — typed signal from `SpawnUtil` when a particle requires a data class the engine does not yet synthesize (BLOCK/ITEM/VIBRATION/etc.).

### Changed
- `EffectEngine.tickOnce` distinguishes `UnsupportedParticleDataException` from generic `Throwable`. The former is logged once per effect name and the handle continues — a single mis-curated step no longer cancels the entire effect timeline. Generic throwables still cancel as before.
- `mana-full.yml` carries explicit `color: "#66E1FF"` + `power: 1.0` for the curated commercial look.

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
