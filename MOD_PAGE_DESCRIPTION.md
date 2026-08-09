# Create Nuclear Tech

Create Nuclear Tech is a nuclear progression addon for Minecraft 1.21.1 on NeoForge, built around Create and Create: Crowns. The mod adds a persistent radiation system, uranium and plutonium processing, protective equipment, irradiation machinery, nuclear materials, and a large configurable nuclear bomb.

It is designed to make nuclear industry feel dangerous and deliberate: radioactive items contaminate players and machines, reactors and nuclear fluids can leak radiation, shielding matters, and late-game materials require controlled irradiation instead of simple crafting.

## Main Features

- Persistent chunk radiation that spreads, decays, and is saved with the world.
- Radiation exposure for players and living entities, with escalating symptoms such as weakness, hunger, slowness, nausea, poison, wither, blindness, and lethal doses at extreme levels.
- Radioactive items, blocks, fluids, dropped materials, Create inventories, and Create pipe spills can become radiation sources.
- Containment checks for lead, concrete, and partial shielding blocks.
- Radiation Scanner Goggles that locate nearby radiation sources and show whether they are blocked, contained, or leaking.
- Geiger Counter for checking local chunk radiation and the player's accumulated body radiation.
- Radiation Tongs for safely handling radioactive materials without dropping them from bare hands.
- Basic, Advanced, and Elite Hazmat suits with different protection levels and durability loss under dangerous exposure.
- Antiradin medicine that removes accumulated body radiation and briefly reduces further exposure.
- Lead Irradiation Box, a 3x3 processing block that uses a nearby radiation field to irradiate nuclear targets.
- Uranium processing chain from crushed uranium and impure dust into yellowcake, natural uranium, uranium hexafluoride, enriched and depleted uranium.
- Plutonium breeding chain: U-238 irradiates into Neptunium-239, then into Plutonium-239, with possible overexposure into Plutonium-240.
- Cobalt and iridium irradiation targets that become Cobalt-60 and Iridium-192 sources.
- Neutron reflector items and blocks in early, advanced, and elite tiers.
- Custom Create: Crowns fuel rod and fuel assembly recipes that preserve isotope composition data.
- MOX, plutonium, reactor-grade plutonium, natural uranium, mid-enriched, and military uranium fuel rod recipes.
- Nuclear bomb with fuse, redstone/fire/projectile activation, custom sound, flash, smoke, mushroom-cloud particles, crater generation, shockwave damage, fire, and radioactive fallout.
- JEI support for Lead Irradiation recipes.
- Configurable radiation, containment, hazmat protection, antiradin, irradiation, and nuclear bomb values.

## Radiation And Safety

Radiation is not just a status effect. The mod tracks radiation in chunks and on entities. Contaminated chunks slowly spread radiation to neighboring chunks and decay over time, while living entities accumulate body radiation based on the local field, nearby radioactive blocks and fluids, and radioactive materials carried in their inventory.

Improper handling is dangerous. If a player holds radioactive material without enough hazmat protection or Radiation Tongs, the item is dropped, the player is hurt, and radiation symptoms are applied.

Shielding is important. Lead blocks and lead glass can fully block lines of radiation, concrete can contain sources if thick enough, and heavy industrial materials can reduce transmission. Scanner Goggles help diagnose whether a source is properly contained or leaking into the world.

## Nuclear Processing

Create Nuclear Tech expands the uranium chain into a longer industrial process. Uranium can be crushed, chemically processed into yellowcake, converted into natural uranium, turned into uranium hexafluoride, and separated into enriched and depleted uranium outputs through machine recipes.

The Lead Irradiation Box uses a real radiation field as its power source. It can process U-238 into Neptunium-239, Neptunium-239 into Plutonium-239, Plutonium-239 into Plutonium-240, and irradiation targets into Cobalt-60 or Iridium-192. The process depends on field strength and accumulated exposure, so containment and source placement become part of the factory design.

## Create: Crowns Integration

The mod integrates with Create: Crowns nuclear blocks, items, fluids, and fuel assemblies. Fuel assemblies and solid corium can act as radiation sources, hot or supercritical Crowns machinery can increase contamination pressure, and radioactive fluids such as corium and uranium hexafluoride can contaminate areas through Create pipe collisions or spills.

Fuel rod recipes include several nuclear compositions, and the custom fuel assembly recipe averages isotope composition from eight rods into the resulting Crowns fuel assembly instead of losing the data.

## Nuclear Bomb

The nuclear bomb is an endgame device built through Create mechanical crafting. It can be armed with redstone, flint and steel, fire charges, burning projectiles, or other explosions. After detonation it creates a large staged explosion with severe entity damage, knockback, fire, crater generation, custom sound and visual effects, and long-range radioactive fallout across nearby chunks.

The bomb behavior is configurable, including fuse length, crater radius, shockwave radius, thermal radius, fallout radius, fallout strength, and block removal limits.

## Added Content

- Nuclear Bomb
- Lead Irradiation Box
- Early, Advanced, and Elite Neutron Reflector blocks
- Plutonium Core and Inactive Plutonium Core
- Yellowcake
- Impure Uranium Dust
- Neptunium-239
- Plutonium-239 Ingot
- Plutonium-240 Ingot
- Cobalt-60 Source
- Iridium-192 Source
- Geiger Counter
- Radiation Scanner Goggles
- Radiation Tongs
- Antiradin
- Basic Hazmat set
- Advanced Hazmat set
- Elite Sealed Hazmat set
- Neutron reflector components

## Requirements

- Minecraft 1.21.1
- NeoForge 21+
- Create
- Create: Crowns

The mod also includes optional integrations and recipes for TFMG, Chemica, Formic API, JEI, and compatible uranium/material tags when those mods are present.

