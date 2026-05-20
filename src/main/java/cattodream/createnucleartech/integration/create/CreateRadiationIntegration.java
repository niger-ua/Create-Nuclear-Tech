package cattodream.createnucleartech.integration.create;

import cattodream.createnucleartech.CNTTags;
import cattodream.createnucleartech.Config;
import cattodream.createnucleartech.Createnucleartech;
import cattodream.createnucleartech.integration.crowns.CrownsIntegration;
import cattodream.createnucleartech.radiation.ContainmentResult;
import cattodream.createnucleartech.radiation.IrradiationTransformer;
import cattodream.createnucleartech.radiation.RadiationData;
import com.simibubi.create.api.event.PipeCollisionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

@EventBusSubscriber(modid = Createnucleartech.MODID)
public final class CreateRadiationIntegration {
    private CreateRadiationIntegration() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.getGameTime() % Math.max(10, Config.createRadiationScanInterval) != 0) {
            return;
        }

        Set<Long> visited = new HashSet<>();
        int radius = Config.createRadiationScanRadius;
        for (ServerPlayer player : level.players()) {
            scanArea(level, player.blockPosition(), radius, 10, visited);
        }
    }

    @SubscribeEvent
    public static void onPipeSpill(PipeCollisionEvent.Spill event) {
        if (event.getLevel() instanceof ServerLevel level
                && (isRadioactive(event.getPipeFluid()) || isRadioactive(event.getWorldFluid()))) {
            RadiationData.get(level).registerSource(level, event.getPos(), Config.createPipeLeak, 6.0D, 1.35D);
        }
    }

    @SubscribeEvent
    public static void onPipeFlow(PipeCollisionEvent.Flow event) {
        if (event.getLevel() instanceof ServerLevel level
                && (isRadioactive(event.getFirstFluid()) || isRadioactive(event.getSecondFluid()))) {
            RadiationData.get(level).registerSource(level, event.getPos(), Config.createPipeLeak * 0.35D, 4.0D, 1.1D);
        }
    }

    public static void leakRadiation(Level level, BlockPos pos, float amount) {
        if (level instanceof ServerLevel serverLevel) {
            RadiationData.get(serverLevel).registerSource(serverLevel, pos, amount, 6.0D, 1.0D);
        }
    }

    public static void scanArea(ServerLevel level, BlockPos center, int horizontalRadius, int verticalRadius) {
        scanArea(level, center, horizontalRadius, verticalRadius, new HashSet<>());
    }

    private static void scanArea(ServerLevel level, BlockPos center, int horizontalRadius, int verticalRadius, Set<Long> visited) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-horizontalRadius, -verticalRadius, -horizontalRadius), center.offset(horizontalRadius, verticalRadius, horizontalRadius))) {
            long key = pos.asLong();
            if (visited.add(key)) {
                inspectCreateMachine(level, pos);
            }
        }
    }

    private static void inspectCreateMachine(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        CrownsIntegration.CrownsRadiationSource blockSource = CrownsIntegration.blockSourceFor(state);
        if (blockSource.active()) {
            registerSource(level, pos, blockSource.strength(), blockSource.radius(), blockSource.containmentSensitivity());
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return;
        }

        CrownsIntegration.CrownsRadiationSource crownsSource = CrownsIntegration.sourceFor(blockEntity);
        double sourceStrength = 0.0D;
        if (crownsSource.active()) {
            registerSource(level, pos, crownsSource.strength(), crownsSource.radius(), crownsSource.containmentSensitivity());
            sourceStrength += crownsSource.strength();
        }

        double inventoryRadiation = inventoryRadiation(level, pos, blockEntity);
        if (inventoryRadiation > 0.0D) {
            double kineticLoad = kineticLoadFactor(blockEntity);
            registerSource(level, pos, inventoryRadiation * kineticLoad, 5.0D + Math.sqrt(inventoryRadiation), 1.0D);
            sourceStrength += inventoryRadiation;
        }

        double fluidRadiation = fluidRadiation(level, pos, blockEntity);
        if (fluidRadiation > 0.0D) {
            registerSource(level, pos, fluidRadiation, 6.0D + Math.sqrt(fluidRadiation), 1.25D);
            sourceStrength += fluidRadiation;
        }

        if (sourceStrength >= Config.plutoniumMinimumFieldStrength) {
            irradiateInventories(level, pos, blockEntity, sourceStrength);
        }
    }

    private static double kineticLoadFactor(BlockEntity blockEntity) {
        try {
            Object value = blockEntity.getClass().getMethod("getSpeed").invoke(blockEntity);
            if (value instanceof Number number) {
                double speed = Math.abs(number.doubleValue());
                if (speed <= 0.01D) {
                    return 0.0D;
                }
                return 1.0D + Math.min(4.0D, speed / 64.0D);
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return 1.0D;
        }
        return 1.0D;
    }

    private static double inventoryRadiation(ServerLevel level, BlockPos pos, BlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        Set<IItemHandler> seenHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
        double total = 0.0D;

        IItemHandler unsided = Capabilities.ItemHandler.BLOCK.getCapability(level, pos, state, blockEntity, null);
        if (unsided != null && seenHandlers.add(unsided)) {
            total += inventoryRadiation(unsided);
        }

        for (Direction direction : Direction.values()) {
            IItemHandler sided = Capabilities.ItemHandler.BLOCK.getCapability(level, pos, state, blockEntity, direction);
            if (sided != null && seenHandlers.add(sided)) {
                total += inventoryRadiation(sided);
            }
        }

        return total;
    }

    private static double inventoryRadiation(IItemHandler handler) {
        double total = 0.0D;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            total += CrownsIntegration.fuelOrByproductRadiation(stack);
        }
        return total;
    }

    private static double fluidRadiation(ServerLevel level, BlockPos pos, BlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        Set<IFluidHandler> seenHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
        double total = 0.0D;

        IFluidHandler unsided = Capabilities.FluidHandler.BLOCK.getCapability(level, pos, state, blockEntity, null);
        if (unsided != null && seenHandlers.add(unsided)) {
            total += fluidRadiation(unsided);
        }

        for (Direction direction : Direction.values()) {
            IFluidHandler sided = Capabilities.FluidHandler.BLOCK.getCapability(level, pos, state, blockEntity, direction);
            if (sided != null && seenHandlers.add(sided)) {
                total += fluidRadiation(sided);
            }
        }
        return total;
    }

    private static double fluidRadiation(IFluidHandler handler) {
        double total = 0.0D;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            total += CrownsIntegration.fluidRadiation(handler.getFluidInTank(tank));
        }
        return total;
    }

    private static void irradiateInventories(ServerLevel level, BlockPos pos, BlockEntity blockEntity, double localField) {
        BlockState state = blockEntity.getBlockState();
        Set<IItemHandler> seenHandlers = Collections.newSetFromMap(new IdentityHashMap<>());

        IItemHandler unsided = Capabilities.ItemHandler.BLOCK.getCapability(level, pos, state, blockEntity, null);
        if (unsided != null && seenHandlers.add(unsided)) {
            IrradiationTransformer.irradiateInventory(unsided, localField);
        }

        for (Direction direction : Direction.values()) {
            IItemHandler sided = Capabilities.ItemHandler.BLOCK.getCapability(level, pos, state, blockEntity, direction);
            if (sided != null && seenHandlers.add(sided)) {
                IrradiationTransformer.irradiateInventory(sided, localField);
            }
        }
    }

    private static ContainmentResult registerSource(ServerLevel level, BlockPos pos, double strength, double radius, double containmentSensitivity) {
        return RadiationData.get(level).registerSource(level, pos, strength, radius, containmentSensitivity);
    }

    private static boolean isRadioactive(Fluid fluid) {
        return fluid.defaultFluidState().is(CNTTags.Fluids.RADIOACTIVE_FLUIDS);
    }
}
