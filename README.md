# ParticleForge

The central particle-effects engine for the TTS-Studio Minecraft plugin suite — one tick-budgeted scheduler, 18 step primitives, and 56 ready-to-play effects every suite plugin can soft-depend on.

- Paper **1.21.10** · Java **21** · Adventure / MiniMessage
- Free on **SpigotMC** · open jar, no obfuscation
- Consumed by other suite plugins via Bukkit's `ServicesManager` (soft-depend)
- 220 unit tests (34 test classes) · per-handle crash isolation · per-player and global budget caps

> **What this is — and isn't.** ParticleForge is a *server-side engine* driven by other plugins or by YAML configs, not a player-facing cosmetics product. It ships **no in-game GUI**, no player-triggered trails (elytra/move/kill listeners), and no per-player persistence — those live on the [roadmap](#roadmap-coming-later). What it does today, it does cleanly: register an effect once, play it from anywhere, and let the engine handle LOD, budgets and crash isolation.

## Why this exists

Every plugin in the suite wants particles. Shipping a bespoke spawner inside each jar means inconsistent visuals, no central budget, and 13 different bugs the day someone forgets to clean up a `BukkitRunnable`. ParticleForge gives the suite one engine: register an effect once (YAML or code), play it from anywhere, and trust that LOD culling, viewer filtering, and budget eviction are handled by the host plugin.

If ParticleForge isn't installed, every consumer soft-degrades to a no-op — they keep working, just without the eye candy. This is intentional: ParticleForge is a "free upgrade" you drop into your suite, not a hard prerequisite.

## Install

1. Drop `particleforge-1.0.3.jar` into your server's `plugins/` folder.
2. Restart the server.
3. `/pf list` will show the 56 pre-built effects loaded from the jar.

User-authored effects go in `plugins/ParticleForge/effects/<category>/<name>.yml` and are picked up on `/pf reload`.

## Programmatic use — fluent API

```java
import com.cristian.particleforge.api.Effects;
import com.cristian.particleforge.api.EffectHandle;

EffectHandle h = Effects.burst()
    .at(player.getLocation())
    .particle("FLAME")
    .count(64)
    .speed(0.7)
    .duration(10)
    .play();

// Later
if (h.isActive()) h.cancel();
```

For pre-registered effects (loaded from YAML), use `Effects.named(...)`:

```java
Effects.named("crate/legendary-win")
    .at(crateLocation)
    .param("color", "#ffaa00")
    .play();
```

## Programmatic use — service lookup (for soft-depending plugins)

When your plugin soft-depends on ParticleForge, fetch the API service from Bukkit:

```java
RegisteredServiceProvider<ParticleForgeApi> rsp =
    Bukkit.getServicesManager().getRegistration(ParticleForgeApi.class);
if (rsp != null) {
    ParticleForgeApi pf = rsp.getProvider();
    pf.play("crate/legendary-win", location);
}
```

See the **Integration snippet** below for the full copy-paste hook.

## YAML effects

Effects live in `plugins/ParticleForge/effects/<category>/<name>.yml`:

```yaml
name: my-cool-burst
category: custom
defaults:
  particle: FLAME
  count: 32
  speed: 0.5
steps:
  - type: BURST
    duration: 10
    params:
      particle: ${particle}
      count: ${count}
      speed: ${speed}
  - type: RING
    duration: 20
    params:
      particle: END_ROD
      radius: 1.5
```

Supports `extends:` for inheritance and `${param}` substitution for parameterised effects.

## `/pf` command reference

| Subcommand | Permission | Description |
|---|---|---|
| `/pf preview <effect>` | `particleforge.admin` | Play an effect at your current location |
| `/pf list [category]` | `particleforge.use` | List all loaded effects, optionally filtered |
| `/pf info <effect>` | `particleforge.use` | Show details (category, steps, duration, defaults) |
| `/pf debug` | `particleforge.admin` | Toggle debug logging |
| `/pf reload` | `particleforge.admin` | Reload config and effects from disk |
| `/pf stop [player\|all]` | `particleforge.admin` | Stop active effects |

`particleforge.use` defaults to `true`, `particleforge.admin` defaults to `op`.

## Integration snippet (copy into your plugin)

```java
package your.plugin.integration;

import com.cristian.particleforge.api.EffectHandle;
import com.cristian.particleforge.api.ParticleForgeApi;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParticleForgeHook {
    private final JavaPlugin owner;
    private volatile ParticleForgeApi api;

    public ParticleForgeHook(JavaPlugin owner) { this.owner = owner; }

    public boolean isAvailable() {
        if (api != null) return true;
        if (!Bukkit.getPluginManager().isPluginEnabled("ParticleForge")) return false;
        RegisteredServiceProvider<ParticleForgeApi> rsp =
            Bukkit.getServicesManager().getRegistration(ParticleForgeApi.class);
        if (rsp == null) return false;
        this.api = rsp.getProvider();
        return api != null;
    }

    public EffectHandle play(String effect, Location at) {
        return isAvailable() ? api.play(effect, at) : null;
    }
}
```

Don't forget `softdepend: [ParticleForge]` in your `plugin.yml`, and gate runtime access with `isPluginEnabled("ParticleForge")` — not `getPlugin() != null` — so disabled plugins don't trip you up.

## Pre-built effects

56 effects across 9 categories. They are named after the suite consumers that hook them, but any plugin can play any of them by name.

| Name | Category | Description |
|---|---|---|
| `claims/border-show` | claims | Trace claim borders with falling dust |
| `claims/claim-created` | claims | Flourish on a freshly created claim |
| `claims/intruder` | claims | Red alert pulse where someone trespasses |
| `claims/own-claim-enter` | claims | Welcome pulse on entering your own claim |
| `claims/protect` | claims | Soft blue shield ring on protect actions |
| `claims/trust-add` | claims | Green accent when trust is granted |
| `claims/trust-remove` | claims | Cooling accent when trust is revoked |
| `claims/unclaim` | claims | Falling ash for territory release |
| `combat/anime-slash` | combat | Anime-style curved sweep arc with a crit flash |
| `combat/combat-tag-enter` | combat | Red ring when combat tag is applied |
| `combat/combat-tag-exit` | combat | Calm green dissipation on tag clear |
| `combat/critical-hit` | combat | Yellow sparks burst on crit |
| `combat/kill` | combat | Dark vortex over a fallen opponent |
| `combat/tag-refresh` | combat | Brief pulse when an existing combat tag is refreshed |
| `crate/common-win` | crate | Modest white-and-grey sparkle |
| `crate/rare-win` | crate | Cyan helix with confetti trail |
| `crate/epic-win` | crate | Purple shockwave + orbit ring |
| `crate/legendary-win` | crate | Gold burst, vortex column, and text flourish |
| `enchants/cleave` | enchants | Wide sweeping arc for cleave-style hits |
| `enchants/crit-iridescent` | enchants | Iridescent crit sparkle |
| `enchants/fusion-fail` | enchants | Smoky fizzle on a failed fusion |
| `enchants/fusion-success` | enchants | Bright burst on a successful fusion |
| `enchants/lifesteal` | enchants | Red drain pulse toward the attacker |
| `enchants/orb-burst` | enchants | Tier-coloured orb burst (particle overridable via `${particle}`) |
| `enchants/thunderstrike-plus` | enchants | Charged lightning accent |
| `enchants/wings` | enchants | Sweeping wing-shaped trail |
| `homes/set` | homes | Soft warm-light pulse on home creation |
| `homes/teleport` | homes | Brief teleport flash on arrival |
| `quests/abandoned` | quests | Muted dissipation when a quest is abandoned |
| `quests/accept` | quests | Green sparkle on accept |
| `quests/ascension` | quests | Ascending column for a major quest milestone |
| `quests/complete` | quests | Triumphant gold burst on quest complete |
| `quests/npc-aura` | quests | Ambient aura around a quest NPC |
| `quests/objective-final` | quests | Bright pulse on the last objective |
| `quests/objective-tick` | quests | Subtle pip on objective progress |
| `shops/big-purchase` | shops | Larger celebratory burst for a big buy |
| `shops/buy-success` | shops | Green confirm sparkle on purchase |
| `shops/insufficient-funds` | shops | Red denial puff when funds are short |
| `shops/locked-item` | shops | Locked-item shake/accent |
| `shops/sell-success` | shops | Coin-toned sparkle on a sale |
| `skills/ability-rank-up` | skills | Accent on ranking up an ability |
| `skills/anime-cast` | skills | 3-phase anime cast: charge → beam → impact |
| `skills/cast-fire` | skills | Spiraling flame cone in front of caster |
| `skills/cast-ice` | skills | Cold cyan shockwave around caster |
| `skills/challenge-complete` | skills | Bright finisher on a completed challenge |
| `skills/crit` | skills | Quick crit sparkle |
| `skills/level-up` | skills | Vertical end-rod column on level gain |
| `skills/level-up-major` | skills | Same as above with crit-magic accent |
| `skills/limit-break` | skills | Anime power-up: rising energy pillar + helix sparks |
| `skills/mana-full` | skills | Cyan Spell-data pulse when mana refills |
| `skills/mana-low` | skills | Dim warning flicker on low mana |
| `skills/respec` | skills | Reset swirl on a skill respec |
| `skills/synergy` | skills | Linking accent for a triggered synergy |
| `teleport/arrival` | teleport | Burst-and-ring drop at destination |
| `teleport/departure` | teleport | Vortex pull-up at origin |
| `teleport/portal-arcano` | teleport | Arcane portal swirl |

## Roadmap (coming later)

ParticleForge is an engine first. The following are **not implemented today** — they are the direction for a future player-facing tier, and are listed here so expectations are honest:

- In-game GUI (`/pf menu`) with live preview before applying.
- Player event triggers (elytra / moving / kill / death / take-damage) — no Bukkit listeners ship today.
- Per-player presets / favourites and persistence (SQLite / MySQL).
- Granular per-effect permissions and Vault / LuckPerms integration.
- PlaceholderAPI provider and math expressions in YAML numeric fields.
- Async geometry computation.

## Build

Maven and JDK 21 are not on PATH on this workstation. See the suite-root `CLAUDE.md` for the exact `JAVA_HOME` / `PATH` exports, then:

```powershell
mvn -B -DskipTests package
```

The jar lands in `target/particleforge-1.0.3.jar`.

## License

Proprietary — Copyright (c) Cristian. All rights reserved.
