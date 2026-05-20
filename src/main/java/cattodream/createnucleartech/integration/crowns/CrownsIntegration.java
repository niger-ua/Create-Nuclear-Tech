package cattodream.createnucleartech.integration.crowns;

import cattodream.createnucleartech.Config;
import cattodream.createnucleartech.radiation.RadiationMaterials;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Bridge into Create: Crowns without linking to unstable implementation
 * classes. The dependency is still mandatory in mods.toml; reflection is only
 * used so small Crowns API shifts do not crash our class loading.
 */
public final class CrownsIntegration {
    public static final String MOD_ID = "crowns";

    private CrownsIntegration() {
    }

    public static void assertRequiredDependencyLoaded() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            throw new IllegalStateException("Create Nuclear Tech requires Create: Crowns (mod id 'crowns') to be installed.");
        }
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static double radiationLeakFor(BlockEntity blockEntity) {
        CrownsRadiationSource source = sourceFor(blockEntity);
        return source.strength();
    }

    public static CrownsRadiationSource blockSourceFor(BlockState state) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!MOD_ID.equals(id.getNamespace())) {
            return CrownsRadiationSource.NONE;
        }
        return switch (id.getPath()) {
            case "fuel_assembly" -> new CrownsRadiationSource(24.0D, 22.0D, 1.55D);
            case "solid_corium" -> new CrownsRadiationSource(180.0D, 48.0D, 3.25D);
            default -> CrownsRadiationSource.NONE;
        };
    }

    public static CrownsRadiationSource sourceFor(BlockEntity blockEntity) {
        if (!isLoaded() || !isCrownsBlockEntity(blockEntity)) {
            return CrownsRadiationSource.NONE;
        }

        double activity = invokeNumber(blockEntity, "getRadioactiveActivity");
        double temperature = invokeNumber(blockEntity, "getTemperature");
        double effectiveK = invokeNumber(blockEntity, "getEffectiveK");
        double strength = activity * Config.crownsActivityLeakScale;

        if (temperature > Config.crownsHotTemperatureThreshold) {
            double overheating = (temperature - Config.crownsHotTemperatureThreshold) / 1000.0D;
            strength += overheating * Config.crownsHeatLeakScale;
        }
        if (effectiveK > 1.0D && strength > 0.0D) {
            strength *= 1.0D + Math.min(4.0D, (effectiveK - 1.0D) * 3.0D);
        }
        if (strength <= 0.0D) {
            return CrownsRadiationSource.NONE;
        }

        double radius = Math.min(Config.radiationContainmentRadius, 4.0D + Math.sqrt(strength) * 2.0D);
        double sensitivity = 1.0D + Math.max(0.0D, effectiveK - 1.0D) + Math.max(0.0D, (temperature - 700.0D) / 1800.0D);
        return new CrownsRadiationSource(strength, radius, sensitivity);
    }

    public static double fuelOrByproductRadiation(ItemStack stack) {
        if (!isCrownsStack(stack)) {
            return 0.0D;
        }
        return RadiationMaterials.radiationFor(stack);
    }

    public static double fluidRadiation(FluidStack stack) {
        if (stack.isEmpty() || !stack.is(cattodream.createnucleartech.CNTTags.Fluids.RADIOACTIVE_FLUIDS)) {
            return 0.0D;
        }
        double buckets = stack.getAmount() / 1000.0D;
        return Math.max(0.15D, buckets) * Config.nuclearWasteRadiation * Config.radioactiveItemStrength * 8.0D;
    }

    private static boolean isCrownsStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return MOD_ID.equals(id.getNamespace());
    }

    private static boolean isCrownsBlockEntity(BlockEntity blockEntity) {
        return blockEntity.getClass().getName().startsWith("com.rae.crowns.");
    }

    private static double invokeNumber(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | LinkageError ignored) {
            return 0.0D;
        }
        return 0.0D;
    }

    public record CrownsRadiationSource(double strength, double radius, double containmentSensitivity) {
        public static final CrownsRadiationSource NONE = new CrownsRadiationSource(0.0D, 0.0D, 1.0D);

        public boolean active() {
            return strength > 0.0D && radius > 0.0D;
        }
    }
}
