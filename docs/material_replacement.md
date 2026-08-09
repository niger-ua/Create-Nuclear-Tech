# Create Nuclear Tech Datapack Hooks

Create: Crowns is a required dependency. The mod metadata marks `crowns` as required, so this addon does not load without it.

Recipes use mod-owned material tags instead of direct item dependencies in Java.
The default datapack now accepts TFMG lead and Chemica composites when those mods are installed.

Replace or extend these tags from a datapack:

- `createnucleartech:heat_resistant_material`
- `createnucleartech:radiation_shielding_material`
- `createnucleartech:advanced_alloy`
- `createnucleartech:chemical_lining`
- `createnucleartech:nuclear_fuel`
- `createnucleartech:uranium`
- `createnucleartech:uranium_238`
- `createnucleartech:uranium_crushed`
- `createnucleartech:uranium_dust`
- `createnucleartech:nuclear_waste`
- `createnucleartech:radioactive_blocks`
- `createnucleartech:radioactive_fluids`
- `createnucleartech:lead_radiation_shielding`
- `createnucleartech:concrete_radiation_shielding`
- `createnucleartech:partial_radiation_shielding`

Example datapack override for lead shielding:

```json
{
  "replace": false,
  "values": [
    {
      "id": "some_tech_mod:lead_ingot",
      "required": false
    }
  ]
}
```

Put that in:

`data/createnucleartech/tags/item/radiation_shielding_material.json`

The same pattern works for Create-style alloys or mechanically produced parts without hardcoding Create items in Java.

Built-in optional examples:

- `createnucleartech:radiation_shielding_material`: `#c:ingots/lead`, `#c:plates/lead`, `tfmg:lead_ingot`, `tfmg:lead_sheet`
- `createnucleartech:heat_resistant_material`: `#c:ingots/heat_resistant_alloy`, `#c:plates/graphite`, `chemica:heat_resistant_tough_alloy`, `chemica:graphite_sheet`
- `createnucleartech:advanced_alloy`: `#c:ingots/composite_alloy`, `#c:plates/carbon_fiber`, `tfmg:heavy_plate`, Chemica composite plates/alloys
- `createnucleartech:chemical_lining`: TFMG rubber sheet plus Chemica rubber/polymer/composite sheets for hazmat recipes
- `createnucleartech:uranium`: `#c:raw_materials/uranium`, `#c:ingots/uranium`, Crowns uranium items when installed
- `createnucleartech:uranium_238`: only Crowns `crowns:uranium_ingot`, which Crowns names Natural Uranium Ingot. Nuggets, depleted uranium, and broad common tags are intentionally excluded.
- `createnucleartech:uranium_crushed`: `create:crushed_raw_uranium` and any datapack-provided crushed uranium
- `createnucleartech:uranium_dust`: `#c:dusts/uranium`, optional TFMG/Chemica uranium dust ids
- `createnucleartech:nuclear_fuel`: this mod's plutonium core plus Crowns `fuel_rod` / `fuel_assembly`
- `createnucleartech:nuclear_waste`: Crowns corium and uranium hexafluoride buckets
- `createnucleartech:radioactive_fluids`: Crowns corium and uranium hexafluoride fluids

Create recipes are placed under `data/createnucleartech/recipe/create/...` and use only these tags. To replace the uranium chain, override the tags or provide higher-priority recipes in a datapack; Java radiation strength will follow the tags automatically.

Crowns integration recipe is under `data/createnucleartech/recipe/crowns/...`:

- `plutonium_core_reprocessing.json`: heated Create mixing reprocesses Crowns uranium hexafluoride, depleted uranium, an enriched uranium seed, a fuel rod, shielding, and an advanced alloy vessel into this mod's plutonium core.

Processing recipe:

- `processing/lead_irradiation_box.json`: crafts the lead irradiation box. It has a 3x3 inventory; each slot accepts one item tagged `createnucleartech:uranium_238` near a strong radiation field, then converts that ingot into a plutonium core. JEI shows this as the `Lead Irradiation` category when JEI is installed.

Create mechanical crafting recipe:

- `create/mechanical_crafting/nuclear_bomb.json`: assembles the redstone-armed nuclear bomb from the plutonium core, Crowns fuel rods/fuel assembly, TNT, shielding, heat-resistant material, and advanced alloy.

Hazmat recipes:

- `hazmat/*.json`: craft basic suits from lead shielding and chemical lining, then upgrade them with advanced alloys and more lining. These recipes load when TFMG and Chemica are present.

Containment tags:

- `lead_radiation_shielding`: one block blocks a ray completely.
- `concrete_radiation_shielding`: requires 3 to 5 blocks of thickness depending on source strength.
- `partial_radiation_shielding`: reduces leakage but cannot fully seal a source alone.

TFMG lead pipe ids are included as optional lead shielding entries. If a pack uses different lead pipe ids, add them to `lead_radiation_shielding` for blocks and `radiation_shielding_material` for items.

The runtime source scan reads Crowns radioactive activity, temperature, and effective K from Crowns block entities, then creates cached local radiation volumes. Contained volumes build up internally; leaking volumes contaminate chunks.
