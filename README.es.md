# ParticleForge

El motor central de efectos de partículas para la suite de plugins de Minecraft de TTS-Studio — un programador con presupuesto por tick, 18 primitivas de paso y 56 efectos listos para reproducir que cualquier plugin de la suite puede soft-depend.

- Paper **1.21.10** · Java **21** · Adventure / MiniMessage
- Gratis en **SpigotMC** · jar abierto, sin ofuscación
- Consumido por otros plugins de la suite mediante el `ServicesManager` de Bukkit (soft-depend)
- 220 tests unitarios (34 clases de test) · aislamiento de crashes por handle · tope de presupuesto por jugador y global

> **Qué es — y qué no.** ParticleForge es un *motor server-side* dirigido por otros plugins o por configs YAML, no un producto de cosméticos de cara al jugador. **No incluye GUI in-game**, ni trails activados por el jugador (listeners de elytra/movimiento/kill), ni persistencia por jugador — eso está en el [roadmap](#roadmap-próximamente). Lo que hace hoy, lo hace limpio: registras un efecto una vez, lo reproduces desde donde sea, y el motor maneja LOD, presupuesto y aislamiento de crashes.

## Por qué existe

Todos los plugins de la suite quieren partículas. Meter un spawner a medida dentro de cada jar significa visuales inconsistentes, sin presupuesto centralizado, y 13 bugs distintos el día que alguien se olvide de limpiar un `BukkitRunnable`. ParticleForge le da a la suite un único motor: registras un efecto una vez (YAML o código), lo reproduces desde donde sea, y confías en que el LOD culling, el filtrado por visor y el desalojo por presupuesto los maneja el plugin anfitrión.

Si ParticleForge no está instalado, cada consumidor degrada a no-op suave — siguen funcionando, sin el adorno visual. Es intencional: ParticleForge es una "mejora gratuita" que enchufas a tu suite, no un prerrequisito duro.

## Instalación

1. Suelta `particleforge-1.0.3.jar` en el `plugins/` de tu servidor.
2. Reinicia el servidor.
3. `/pf list` mostrará los 56 efectos prebuilt cargados desde el jar.

Los efectos de usuario van en `plugins/ParticleForge/effects/<categoría>/<nombre>.yml` y se recargan con `/pf reload`.

## Uso programático — API fluida

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

// Más tarde
if (h.isActive()) h.cancel();
```

Para efectos pre-registrados (cargados desde YAML), usa `Effects.named(...)`:

```java
Effects.named("crate/legendary-win")
    .at(crateLocation)
    .param("color", "#ffaa00")
    .play();
```

## Uso programático — service lookup (para plugins con soft-depend)

Cuando tu plugin tiene soft-depend en ParticleForge, busca el servicio API en Bukkit:

```java
RegisteredServiceProvider<ParticleForgeApi> rsp =
    Bukkit.getServicesManager().getRegistration(ParticleForgeApi.class);
if (rsp != null) {
    ParticleForgeApi pf = rsp.getProvider();
    pf.play("crate/legendary-win", location);
}
```

Mira el **Snippet de integración** abajo para el hook completo copy-paste.

## Efectos en YAML

Los efectos viven en `plugins/ParticleForge/effects/<categoría>/<nombre>.yml`:

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

Soporta `extends:` para herencia y sustitución `${param}` para efectos parametrizados.

## Referencia de comandos `/pf`

| Subcomando | Permiso | Descripción |
|---|---|---|
| `/pf preview <efecto>` | `particleforge.admin` | Reproduce un efecto en tu ubicación actual |
| `/pf list [categoría]` | `particleforge.use` | Lista todos los efectos cargados, opcionalmente filtrados |
| `/pf info <efecto>` | `particleforge.use` | Muestra detalles (categoría, pasos, duración, defaults) |
| `/pf debug` | `particleforge.admin` | Activa/desactiva el log de depuración |
| `/pf reload` | `particleforge.admin` | Recarga config y efectos desde disco |
| `/pf stop [jugador\|all]` | `particleforge.admin` | Detiene efectos activos |

`particleforge.use` por defecto en `true`, `particleforge.admin` por defecto en `op`.

## Snippet de integración (copia esto en tu plugin)

```java
package tu.plugin.integration;

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

No olvides `softdepend: [ParticleForge]` en tu `plugin.yml`, y gatear el acceso en runtime con `isPluginEnabled("ParticleForge")` — no `getPlugin() != null` — para que un plugin deshabilitado no te haga tropezar.

## Efectos prebuilt

56 efectos en 9 categorías. Llevan el nombre de los plugins de la suite que los enganchan, pero cualquier plugin puede reproducir cualquiera por su nombre.

| Nombre | Categoría | Descripción |
|---|---|---|
| `claims/border-show` | claims | Traza los bordes de un claim con dust cayendo |
| `claims/claim-created` | claims | Floritura al crear un claim nuevo |
| `claims/intruder` | claims | Pulso rojo de alerta donde alguien se mete |
| `claims/own-claim-enter` | claims | Pulso de bienvenida al entrar a tu propio claim |
| `claims/protect` | claims | Anillo azul suave de escudo al proteger |
| `claims/trust-add` | claims | Acento verde al conceder trust |
| `claims/trust-remove` | claims | Acento frío al revocar trust |
| `claims/unclaim` | claims | Ceniza cayendo al liberar territorio |
| `combat/anime-slash` | combat | Arco de tajo curvo estilo anime con destello de crítico |
| `combat/combat-tag-enter` | combat | Anillo rojo al aplicar combat tag |
| `combat/combat-tag-exit` | combat | Disipación verde tranquila al limpiar el tag |
| `combat/critical-hit` | combat | Chispas amarillas al hacer crítico |
| `combat/kill` | combat | Vórtice oscuro sobre el oponente caído |
| `combat/tag-refresh` | combat | Pulso breve al refrescar un combat tag existente |
| `crate/common-win` | crate | Brillo modesto en blanco y gris |
| `crate/rare-win` | crate | Hélice cian con trail de confeti |
| `crate/epic-win` | crate | Shockwave morado + anillo orbital |
| `crate/legendary-win` | crate | Estallido dorado, columna de vórtice y texto |
| `enchants/cleave` | enchants | Arco amplio de barrido para golpes tipo cleave |
| `enchants/crit-iridescent` | enchants | Chispa de crítico iridiscente |
| `enchants/fusion-fail` | enchants | Chisporroteo de humo al fallar una fusión |
| `enchants/fusion-success` | enchants | Estallido brillante al lograr una fusión |
| `enchants/lifesteal` | enchants | Pulso rojo de drenaje hacia el atacante |
| `enchants/orb-burst` | enchants | Estallido de orbe por tier (partícula sobreescribible con `${particle}`) |
| `enchants/thunderstrike-plus` | enchants | Acento de rayo cargado |
| `enchants/wings` | enchants | Trail con forma de alas |
| `homes/set` | homes | Pulso cálido suave al crear home |
| `homes/teleport` | homes | Flash breve de teleport al llegar |
| `quests/abandoned` | quests | Disipación apagada al abandonar una quest |
| `quests/accept` | quests | Brillo verde al aceptar |
| `quests/ascension` | quests | Columna ascendente para un hito mayor de quest |
| `quests/complete` | quests | Estallido dorado triunfal al completar |
| `quests/npc-aura` | quests | Aura ambiental alrededor de un NPC de quest |
| `quests/objective-final` | quests | Pulso brillante en el último objetivo |
| `quests/objective-tick` | quests | Pip sutil al progresar un objetivo |
| `shops/big-purchase` | shops | Estallido celebratorio mayor para una compra grande |
| `shops/buy-success` | shops | Chispa verde de confirmación al comprar |
| `shops/insufficient-funds` | shops | Bocanada roja de rechazo al faltar fondos |
| `shops/locked-item` | shops | Sacudida/acento de ítem bloqueado |
| `shops/sell-success` | shops | Chispa con tono de monedas al vender |
| `skills/ability-rank-up` | skills | Acento al subir de rango una habilidad |
| `skills/anime-cast` | skills | Casteo anime en 3 fases: carga → haz → impacto |
| `skills/cast-fire` | skills | Cono de llama en espiral delante del caster |
| `skills/cast-ice` | skills | Shockwave cian frío alrededor del caster |
| `skills/challenge-complete` | skills | Remate brillante al completar un reto |
| `skills/crit` | skills | Chispa rápida de crítico |
| `skills/level-up` | skills | Columna vertical de end-rod al subir de nivel |
| `skills/level-up-major` | skills | Igual con acento crit-magic |
| `skills/limit-break` | skills | Power-up anime: pilar de energía ascendente + chispas en hélice |
| `skills/mana-full` | skills | Pulso cian (Spell-data) al rellenar maná |
| `skills/mana-low` | skills | Parpadeo tenue de aviso con maná bajo |
| `skills/respec` | skills | Remolino de reset al respecear skills |
| `skills/synergy` | skills | Acento de enlace al disparar una sinergia |
| `teleport/arrival` | teleport | Burst-and-ring al caer en el destino |
| `teleport/departure` | teleport | Vórtice hacia arriba en el origen |
| `teleport/portal-arcano` | teleport | Remolino de portal arcano |

## Roadmap (próximamente)

ParticleForge es ante todo un motor. Lo siguiente **no está implementado hoy** — es la dirección de un futuro tier de cara al jugador, y se lista aquí para mantener las expectativas honestas:

- GUI in-game (`/pf menu`) con preview en vivo antes de aplicar.
- Triggers por evento de jugador (elytra / moverse / kill / muerte / recibir daño) — hoy no se registra ningún listener de Bukkit.
- Presets / favoritos por jugador y persistencia (SQLite / MySQL).
- Permisos granulares por efecto e integración Vault / LuckPerms.
- Provider de PlaceholderAPI y expresiones matemáticas en campos numéricos del YAML.
- Cálculo de geometría async.

## Build

Maven y JDK 21 no están en PATH en esta workstation. Mira el `CLAUDE.md` raíz de la suite para los exports exactos de `JAVA_HOME` / `PATH`, y luego:

```powershell
mvn -B -DskipTests package
```

El jar sale en `target/particleforge-1.0.3.jar`.

## Licencia

Propietario — Copyright (c) Cristian. Todos los derechos reservados.
