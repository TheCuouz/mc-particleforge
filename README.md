# ParticleForge

Central particle-effects engine for the TTS-Studio Minecraft plugin suite.

- Paper 1.21.10 · Java 21
- Free on SpigotMC · no obfuscation
- Consumed by other suite plugins via soft-dependency (`ParticleForge` service)

## Build

Maven and JDK 21 are not on PATH on this workstation. See the suite-root `CLAUDE.md` for the exact `JAVA_HOME` / `PATH` exports. Then:

```powershell
mvn -B -DskipTests package
```

The jar lands in `target/particleforge-1.0.0.jar`.

## License

Proprietary — © Cristian
