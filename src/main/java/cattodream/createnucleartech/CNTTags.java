package cattodream.createnucleartech;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.Block;

public final class CNTTags {
    private CNTTags() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, path);
    }

    public static final class Items {
        public static final TagKey<Item> HEAT_RESISTANT_MATERIAL = tag("heat_resistant_material");
        public static final TagKey<Item> RADIATION_SHIELDING_MATERIAL = tag("radiation_shielding_material");
        public static final TagKey<Item> ADVANCED_ALLOY = tag("advanced_alloy");
        public static final TagKey<Item> NUCLEAR_FUEL = tag("nuclear_fuel");
        public static final TagKey<Item> URANIUM = tag("uranium");
        public static final TagKey<Item> URANIUM_238 = tag("uranium_238");
        public static final TagKey<Item> URANIUM_CRUSHED = tag("uranium_crushed");
        public static final TagKey<Item> URANIUM_DUST = tag("uranium_dust");
        public static final TagKey<Item> NUCLEAR_WASTE = tag("nuclear_waste");
        public static final TagKey<Item> COBALT_IRRADIATION_TARGET = tag("cobalt_irradiation_target");
        public static final TagKey<Item> IRIDIUM_IRRADIATION_TARGET = tag("iridium_irradiation_target");
        public static final TagKey<Item> TOXIC_LEAD = tag("toxic_lead");

        private Items() {
        }

        private static TagKey<Item> tag(String path) {
            return TagKey.create(Registries.ITEM, id(path));
        }
    }

    public static final class Blocks {
        public static final TagKey<Block> RADIATION_SHIELDING = tag("radiation_shielding");
        public static final TagKey<Block> LEAD_RADIATION_SHIELDING = tag("lead_radiation_shielding");
        public static final TagKey<Block> CONCRETE_RADIATION_SHIELDING = tag("concrete_radiation_shielding");
        public static final TagKey<Block> PARTIAL_RADIATION_SHIELDING = tag("partial_radiation_shielding");
        public static final TagKey<Block> RADIOACTIVE_BLOCKS = tag("radioactive_blocks");
        public static final TagKey<Block> NEUTRON_REFLECTORS = tag("neutron_reflectors");
        public static final TagKey<Block> NEUTRON_MODERATORS = tag("neutron_moderators");
        public static final TagKey<Block> NEUTRON_ABSORBERS = tag("neutron_absorbers");

        private Blocks() {
        }

        private static TagKey<Block> tag(String path) {
            return TagKey.create(Registries.BLOCK, id(path));
        }
    }

    public static final class Fluids {
        public static final TagKey<Fluid> RADIOACTIVE_FLUIDS = tag("radioactive_fluids");

        private Fluids() {
        }

        private static TagKey<Fluid> tag(String path) {
            return TagKey.create(Registries.FLUID, id(path));
        }
    }
}
