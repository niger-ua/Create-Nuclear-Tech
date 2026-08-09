# Create Nuclear Tech

Create Nuclear Tech is a NeoForge addon for Minecraft 1.21.1 focused on nuclear fuel processing, radiation, protective equipment, industrial machinery, and nuclear explosions.

## Features

- Persistent radiation sources, contamination, shielding, and radiation sickness
- Nuclear fuel rods, burnup, spent fuel, and reprocessing
- Integration with Create, Create: The Factory Must Grow, Chemica, and Create: Crowns
- Blast furnace, high-speed mixer, lead irradiation box, and fuel holder
- Geiger counter, scanner goggles, antiradin, and several hazmat suit tiers
- Configurable nuclear explosions with blast, thermal, fallout, and terrain effects
- JEI recipe categories and datapack-friendly material tags

## Requirements

- Minecraft 1.21.1
- Java 21
- NeoForge 21.1 or newer
- Create 6.0.9 or newer
- Create: The Factory Must Grow 1.2.0 or newer
- Chemica 0.5.1 or newer
- Create: Crowns 2.1.5 or newer
- Formic API 2.2.3 or newer (optional)
- JEI 19.25 or newer (optional)

The development build currently resolves several mod dependencies from local extracted copies. Place the required development dependencies beside the project using the paths declared in `build.gradle`, or replace those entries with matching Maven artifacts for your environment.

## Building

```shell
./gradlew build
```

The resulting mod JAR is written to `build/libs`.

## License

Create Nuclear Tech is available under the [MIT License](LICENSE).
