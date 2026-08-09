package cattodream.createnucleartech.radiation;

import cattodream.createnucleartech.CNTTags;
import cattodream.createnucleartech.Config;
import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.items.RadioactiveItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Central material lookup for exposure. Raw uranium is an irradiation target,
 * while actual field sources are Crowns fuel, radioactive fluids, and waste.
 */
public final class RadiationMaterials {
    private RadiationMaterials() {
    }

    public static double radiationFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0D;
        }

        double materialStrength = 0.0D;
        if (stack.is(CNTTags.Items.NUCLEAR_FUEL)) {
            materialStrength = Math.max(materialStrength, Config.uraniumDustRadiation * 2.5D);
        }
        if (stack.is(CNTTags.Items.NUCLEAR_WASTE)) {
            materialStrength = Math.max(materialStrength, Config.nuclearWasteRadiation * 1.75D);
        }
        if (stack.is(CNTTags.Items.URANIUM_DUST)) {
            materialStrength = Math.max(materialStrength, Config.uraniumDustRadiation);
        }
        if (stack.is(CNTTags.Items.URANIUM_CRUSHED)) {
            materialStrength = Math.max(materialStrength, Config.crushedUraniumRadiation);
        }
        if (stack.is(CNTTags.Items.URANIUM)) {
            materialStrength = Math.max(materialStrength, Config.uraniumRadiation);
        }
        if (stack.getItem() instanceof RadioactiveItem radioactive) {
            materialStrength = Math.max(materialStrength, radioactive.radiationStrength());
        }

        return materialStrength * Config.radioactiveItemStrength * stack.getCount();
    }

    public static double perItemRadiationFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0D;
        }
        ItemStack one = stack.copyWithCount(1);
        return radiationFor(one);
    }

    public static double radiationForBlock(BlockState state) {
        if (state.isAir()) {
            return 0.0D;
        }
        if (state.is(ModRegistry.WASTE_EARTH.get())) {
            return Config.nuclearWasteRadiation * Config.radioactiveItemStrength * 1.5D;
        }
        if (state.is(CNTTags.Blocks.RADIOACTIVE_BLOCKS)) {
            return Config.nuclearWasteRadiation * Config.radioactiveItemStrength * 8.0D;
        }
        return 0.0D;
    }

    public static boolean canBecomePlutonium(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(CNTTags.Items.URANIUM) || stack.is(CNTTags.Items.URANIUM_CRUSHED) || stack.is(CNTTags.Items.URANIUM_DUST));
    }

    public static double radiationForFluid(FluidState state) {
        if (state.isEmpty()) {
            return 0.0D;
        }
        if (state.is(CNTTags.Fluids.RADIOACTIVE_FLUIDS)) {
            return Config.nuclearWasteRadiation * Config.radioactiveItemStrength * 7.0D;
        }
        return 0.0D;
    }
}
