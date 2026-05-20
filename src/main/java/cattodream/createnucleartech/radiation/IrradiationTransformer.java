package cattodream.createnucleartech.radiation;

import cattodream.createnucleartech.Config;
import cattodream.createnucleartech.ModRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public final class IrradiationTransformer {
    private static final String EXPOSURE_KEY = "CreateNuclearTechExposure";
    private static final String TIME_KEY = "CreateNuclearTechIrradiationTime";

    private IrradiationTransformer() {
    }

    public static void irradiateInventory(IItemHandler handler, double localField) {
        if (localField < Config.plutoniumMinimumFieldStrength) {
            return;
        }

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!RadiationMaterials.canBecomePlutonium(stack)) {
                continue;
            }
            ItemStack updated = irradiateStack(stack, localField);
            if (handler instanceof IItemHandlerModifiable modifiable) {
                modifiable.setStackInSlot(slot, updated);
            }
        }
    }

    public static ItemStack irradiateStack(ItemStack stack, double localField) {
        if (!RadiationMaterials.canBecomePlutonium(stack)) {
            return stack;
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        double exposure = tag.getDouble(EXPOSURE_KEY) + localField;
        int time = tag.getInt(TIME_KEY) + 1;

        if (exposure >= requiredExposure() && time >= 12) {
            int output = Math.max(1, stack.getCount() / 8);
            return new ItemStack(ModRegistry.NEPTUNIUM_239.get(), Math.min(64, output));
        }

        tag.putDouble(EXPOSURE_KEY, exposure);
        tag.putInt(TIME_KEY, time);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static double requiredExposure() {
        return Config.plutoniumExposureThreshold * Config.irradiationCostMultiplier;
    }
}
