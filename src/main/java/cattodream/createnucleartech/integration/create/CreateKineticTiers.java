package cattodream.createnucleartech.integration.create;

import cattodream.createnucleartech.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class CreateKineticTiers {
    private static final int DEFAULT_MAX_ROTATION_SPEED = 8192;
    private static final double DEFAULT_WOODEN_COG_MAX_RPM = 256.0D;
    private static final double DEFAULT_WOODEN_COG_MAX_STRESS = 16384.0D;
    private static final double DEFAULT_STEEL_COG_STRESS = 2.0D;
    private static final double DEFAULT_LARGE_STEEL_COG_STRESS = 4.0D;
    private static final double STEEL_STRESS_SCALE = 1.0D / 16.0D;
    private static final double ALUMINUM_STRESS_SCALE = STEEL_STRESS_SCALE / 4.0D;
    private static final double ALUMINUM_COG_MAX_RPM = 4096.0D;

    private static final ResourceLocation CREATE_COGWHEEL = ResourceLocation.fromNamespaceAndPath("create", "cogwheel");
    private static final ResourceLocation CREATE_LARGE_COGWHEEL = ResourceLocation.fromNamespaceAndPath("create", "large_cogwheel");
    private static final ResourceLocation TFMG_ALUMINUM_COGWHEEL = ResourceLocation.fromNamespaceAndPath("tfmg", "aluminum_cogwheel");
    private static final ResourceLocation TFMG_LARGE_ALUMINUM_COGWHEEL = ResourceLocation.fromNamespaceAndPath("tfmg", "large_aluminum_cogwheel");
    private static final ResourceLocation TFMG_STEEL_COGWHEEL = ResourceLocation.fromNamespaceAndPath("tfmg", "steel_cogwheel");
    private static final ResourceLocation TFMG_LARGE_STEEL_COGWHEEL = ResourceLocation.fromNamespaceAndPath("tfmg", "large_steel_cogwheel");

    private CreateKineticTiers() {
    }

    public static int maxRotationSpeed() {
        return Config.createMaxRotationSpeed > 0 ? Config.createMaxRotationSpeed : DEFAULT_MAX_ROTATION_SPEED;
    }

    public static float additionalStressImpact(BlockState state) {
        ResourceLocation id = blockId(state);
        if (TFMG_ALUMINUM_COGWHEEL.equals(id)) {
            return (float) (valueOrDefault(Config.createSteelCogwheelStressImpact, DEFAULT_STEEL_COG_STRESS) * ALUMINUM_STRESS_SCALE);
        }
        if (TFMG_LARGE_ALUMINUM_COGWHEEL.equals(id)) {
            return (float) (valueOrDefault(Config.createLargeSteelCogwheelStressImpact, DEFAULT_LARGE_STEEL_COG_STRESS) * ALUMINUM_STRESS_SCALE);
        }
        if (TFMG_STEEL_COGWHEEL.equals(id)) {
            return (float) (valueOrDefault(Config.createSteelCogwheelStressImpact, DEFAULT_STEEL_COG_STRESS) * STEEL_STRESS_SCALE);
        }
        if (TFMG_LARGE_STEEL_COGWHEEL.equals(id)) {
            return (float) (valueOrDefault(Config.createLargeSteelCogwheelStressImpact, DEFAULT_LARGE_STEEL_COG_STRESS) * STEEL_STRESS_SCALE);
        }
        return 0.0F;
    }

    public static void breakCogwheelIfOverloaded(BlockState state, Level level, BlockPos pos, float speed, float networkStress) {
        if (isAluminumCogwheel(state)) {
            breakIfOverSpeed(level, pos, Math.abs(speed), ALUMINUM_COG_MAX_RPM);
            return;
        }

        if (!isWoodenCogwheel(state)) {
            return;
        }

        double rpm = Math.abs(speed);
        double maxRpm = valueOrDefault(Config.createWoodenCogwheelMaxRpm, DEFAULT_WOODEN_COG_MAX_RPM);
        double maxStress = valueOrDefault(Config.createWoodenCogwheelMaxStress, DEFAULT_WOODEN_COG_MAX_STRESS);
        if (rpm <= maxRpm + 0.001D && networkStress <= maxStress) {
            return;
        }

        breakIfOverloaded(level, pos, true);
    }

    private static boolean isWoodenCogwheel(BlockState state) {
        ResourceLocation id = blockId(state);
        return CREATE_COGWHEEL.equals(id) || CREATE_LARGE_COGWHEEL.equals(id);
    }

    private static boolean isAluminumCogwheel(BlockState state) {
        ResourceLocation id = blockId(state);
        return TFMG_ALUMINUM_COGWHEEL.equals(id) || TFMG_LARGE_ALUMINUM_COGWHEEL.equals(id);
    }

    private static void breakIfOverSpeed(Level level, BlockPos pos, double rpm, double maxRpm) {
        if (rpm > maxRpm + 0.001D) {
            breakIfOverloaded(level, pos, true);
        }
    }

    private static void breakIfOverloaded(Level level, BlockPos pos, boolean dropBlock) {
        if (level instanceof ServerLevel serverLevel && serverLevel.isLoaded(pos)) {
            serverLevel.destroyBlock(pos, dropBlock);
        }
    }

    private static ResourceLocation blockId(BlockState state) {
        Block block = state.getBlock();
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    private static double valueOrDefault(double value, double fallback) {
        return value > 0.0D ? value : fallback;
    }
}
