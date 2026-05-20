package cattodream.createnucleartech.integration.crowns;

import cattodream.createnucleartech.CNTTags;
import cattodream.createnucleartech.Createnucleartech;
import cattodream.createnucleartech.ModRegistry;
import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lightweight addon layer over Crowns' two-group neutron model.
 *
 * Crowns exposes fast/slow neutrons as a Couple<Float>. Replacing that API would
 * be fragile, so the addon records a third "medium" band and injects it as a
 * delayed extra absorption contribution through AssemblyBlockEntityMixin.
 */
@EventBusSubscriber(modid = Createnucleartech.MODID)
public final class CrownsNeutronDiagnostics {
    private static final ResourceLocation CREATE_GOGGLES = ResourceLocation.fromNamespaceAndPath("create", "goggles");
    private static final Map<Level, Long2ObjectOpenHashMap<Snapshot>> SNAPSHOTS = new WeakHashMap<>();

    private CrownsNeutronDiagnostics() {
    }

    public static void record(Level level, BlockPos pos, float fast, float medium, float slow, float k, float temperature, float activity, String profileId, String profileName, boolean started) {
        if (level == null || level.isClientSide()) {
            return;
        }
        synchronized (SNAPSHOTS) {
            SNAPSHOTS.computeIfAbsent(level, ignored -> new Long2ObjectOpenHashMap<>())
                    .put(pos.asLong(), new Snapshot(pos.immutable(), fast, medium, slow, k, temperature, activity, profileId, profileName, started, level.getGameTime()));
        }
    }

    public static float mediumFactor(Level level, BlockPos pos) {
        float factor = 0.0F;
        for (Direction direction : Direction.values()) {
            BlockState state = level.getBlockState(pos.relative(direction));
            if (state.is(ModRegistry.EARLY_NEUTRON_REFLECTOR.get())) {
                factor += 0.20F;
            } else if (state.is(ModRegistry.ADVANCED_NEUTRON_REFLECTOR.get())) {
                factor += 0.12F;
            } else if (state.is(ModRegistry.ELITE_NEUTRON_REFLECTOR.get())) {
                factor += 0.05F;
            } else if (state.is(CNTTags.Blocks.NEUTRON_MODERATORS)) {
                factor += 0.18F;
            }

            if (!state.getFluidState().isEmpty() && state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                factor += 0.10F;
            }
        }
        return Math.min(0.85F, factor);
    }

    public static float reflectorMultiplier(Level level, BlockPos pos) {
        float bonus = 0.0F;
        for (Direction direction : Direction.values()) {
            BlockState state = level.getBlockState(pos.relative(direction));
            if (state.is(ModRegistry.EARLY_NEUTRON_REFLECTOR.get())) {
                bonus += 0.10F;
            } else if (state.is(ModRegistry.ADVANCED_NEUTRON_REFLECTOR.get())) {
                bonus += 0.28F;
            } else if (state.is(ModRegistry.ELITE_NEUTRON_REFLECTOR.get())) {
                bonus += 0.48F;
            }
        }
        return Math.min(3.0F, 1.0F + bonus);
    }

    public static float reflectedNeutronFeedback(Level level, BlockPos pos) {
        float feedback = 0.0F;
        for (Direction direction : Direction.values()) {
            BlockState state = level.getBlockState(pos.relative(direction));
            if (state.is(ModRegistry.EARLY_NEUTRON_REFLECTOR.get())) {
                feedback += 0.10F;
            } else if (state.is(ModRegistry.ADVANCED_NEUTRON_REFLECTOR.get())) {
                feedback += 0.22F;
            } else if (state.is(ModRegistry.ELITE_NEUTRON_REFLECTOR.get())) {
                feedback += 0.36F;
            } else if (state.is(CNTTags.Blocks.NEUTRON_REFLECTORS)) {
                feedback += 0.08F;
            }
        }
        return Math.min(1.60F, feedback);
    }

    public static float absorberDamping(Level level, BlockPos pos) {
        float damping = 0.0F;
        for (Direction direction : Direction.values()) {
            BlockState state = level.getBlockState(pos.relative(direction));
            if (state.is(CNTTags.Blocks.NEUTRON_ABSORBERS)) {
                damping += 0.18F;
            }
        }
        return Math.max(0.20F, 1.0F - Math.min(0.80F, damping));
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)
                || !(player.level() instanceof ServerLevel level)
                || player.tickCount % 20 != 0
                || !hasCreateGoggles(player)) {
            return;
        }

        Snapshot snapshot = nearestSnapshot(level, player.blockPosition(), 18);
        if (snapshot == null) {
            return;
        }

        player.displayClientMessage(Component.literal(
                snapshot.profileName() + (snapshot.started() ? " " : " cold ")
                        + "| F " + fmt(snapshot.fast())
                        + " | M " + fmt(snapshot.medium())
                        + " | S " + fmt(snapshot.slow())
                        + " | k " + fmt(snapshot.k())
                        + " | " + Math.round(snapshot.temperature()) + "K"
        ).withStyle(snapshot.k() >= 1.0F ? ChatFormatting.RED : ChatFormatting.AQUA), true);
    }

    private static Snapshot nearestSnapshot(ServerLevel level, BlockPos center, int radius) {
        Snapshot best = null;
        double bestDistance = Double.MAX_VALUE;
        long now = level.getGameTime();
        synchronized (SNAPSHOTS) {
            Long2ObjectOpenHashMap<Snapshot> map = SNAPSHOTS.get(level);
            if (map == null) {
                return null;
            }
            for (Snapshot snapshot : map.values()) {
                if (now - snapshot.gameTime() > 80) {
                    continue;
                }
                double distance = snapshot.pos().distSqr(center);
                if (distance <= radius * radius && distance < bestDistance) {
                    best = snapshot;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private static boolean hasCreateGoggles(Player player) {
        return isCreateGoggles(player.getItemBySlot(EquipmentSlot.HEAD))
                || isCreateGoggles(player.getMainHandItem())
                || isCreateGoggles(player.getOffhandItem());
    }

    private static boolean isCreateGoggles(ItemStack stack) {
        return !stack.isEmpty() && CREATE_GOGGLES.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static String fmt(float value) {
        if (!Float.isFinite(value)) {
            return "0.00";
        }
        return String.format("%.2f", value);
    }

    public static Snapshot snapshotFor(AssemblyBlockEntity assembly) {
        BlockEntity blockEntity = assembly;
        Level level = blockEntity.getLevel();
        if (level == null) {
            return null;
        }
        synchronized (SNAPSHOTS) {
            Long2ObjectOpenHashMap<Snapshot> map = SNAPSHOTS.get(level);
            return map == null ? null : map.get(blockEntity.getBlockPos().asLong());
        }
    }

    public record Snapshot(BlockPos pos, float fast, float medium, float slow, float k, float temperature, float activity, String profileId, String profileName, boolean started, long gameTime) {
    }
}
