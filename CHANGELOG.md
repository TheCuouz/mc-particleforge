# Changelog

All notable changes to ParticleForge are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This project adheres to [Semantic Versioning](https://semver.org/).

## [1.1.0] — 2026-08-19

### Added
- Effect library expanded from 51 → 56 with showcase effects:
  - **combat** (1): `anime-slash` — anime-style curved sweep arc with a crit flash.
  - **skills** (2): `anime-cast` (3-phase charge → beam → impact), `limit-break` (rising energy pillar + helix sparks).
  - **quests** (1): `ascension` — ascending column for a major milestone.
  - **teleport** (1): `portal-arcano` — arcane portal swirl.

### Changed
- **Default interface language is now English** (`lang: en`), including the shared
  SDK navigation strings (`Page X/Y`, Close/Back/Next). `lang/es.yml` ships complete;
  set `lang: es` for Spanish.

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

## [1.0.2] — 2026-05-16

### Added
- Effect library expanded from 24 → 51 so the rest of the suite can hook in with names that already exist out of the box. New effects by category:
  - **skills** (7): `ability-rank-up`, `mana-low`, `mana-full`, `challenge-complete`, `respec`, `synergy`, `crit`.
  - **shops** (5): `buy-success`, `sell-success`, `insufficient-funds`, `big-purchase`, `locked-item`.
  - **claims** (4): `claim-created`, `trust-add`, `trust-remove`, `own-claim-enter`.
  - **combat** (1): `tag-refresh`.
  - **quests** (2): `abandoned`, `npc-aura`.
  - **enchants** (8): `fusion-success`, `fusion-fail`, `orb-burst`, `lifesteal`, `wings`, `thunderstrike-plus`, `cleave`, `crit-iridescent`.
- `enchants/orb-burst` uses `${particle}` interpolation so EFV2Addon can override the particle per orb tier (common/rare/epic/legendary).

## [1.0.1] — 2026-05-16

### Changed
- bStats plugin id `31357` assigned and live. Dropped the pre-release no-op guard in `BStatsBootstrap` (and its accompanying test) — metrics report on next enable.

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
