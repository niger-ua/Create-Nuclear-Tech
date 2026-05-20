package cattodream.createnucleartech.items;

import cattodream.createnucleartech.Config;
import cattodream.createnucleartech.integration.create.CreateRadiationIntegration;
import cattodream.createnucleartech.radiation.ContainmentScanner;
import cattodream.createnucleartech.radiation.ContainmentStatus;
import cattodream.createnucleartech.radiation.RadiationData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class RadiationScannerGogglesItem extends Item {
    public RadiationScannerGogglesItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            scan(serverLevel, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void scan(ServerLevel level, Player player) {
        RadiationData data = RadiationData.get(level);
        BlockPos observer = player.blockPosition();
        CreateRadiationIntegration.scanArea(level, observer, Math.min(Config.radiationScannerRange, 48), 18);
        List<RadiationData.SourceView> sources = data.nearbySources(observer, Config.radiationScannerRange, 6);
        if (sources.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.createnucleartech.scanner.empty").withStyle(ChatFormatting.GREEN), true);
            return;
        }

        for (RadiationData.SourceView source : sources) {
            ContainmentStatus lineStatus = ContainmentScanner.evaluateLine(level, observer, source.pos(), source.intensity());
            ContainmentStatus displayStatus = source.status() == ContainmentStatus.LEAKING ? ContainmentStatus.LEAKING : lineStatus;
            player.displayClientMessage(Component.translatable(
                    "message.createnucleartech.scanner.source",
                    source.pos().getX(),
                    source.pos().getY(),
                    source.pos().getZ(),
                    format(source.intensity()),
                    displayStatus.name()
            ).withStyle(color(displayStatus)), false);
            drawGradient(level, observer, source.pos(), displayStatus);
        }
    }

    private static void drawGradient(ServerLevel level, BlockPos from, BlockPos to, ContainmentStatus status) {
        int steps = Math.max(4, Math.min(18, (int) Math.sqrt(from.distSqr(to))));
        for (int step = 1; step <= steps; step++) {
            double t = step / (double) steps;
            double x = from.getX() + 0.5D + (to.getX() - from.getX()) * t;
            double y = from.getY() + 1.15D + (to.getY() - from.getY()) * t;
            double z = from.getZ() + 0.5D + (to.getZ() - from.getZ()) * t;
            int count = status == ContainmentStatus.LEAKING ? 3 : 1;
            level.sendParticles(status == ContainmentStatus.LEAKING ? ParticleTypes.WITCH : ParticleTypes.HAPPY_VILLAGER, x, y, z, count, 0.05D, 0.05D, 0.05D, 0.01D);
        }
    }

    private static String format(double value) {
        return String.format("%.2f", value);
    }

    private static ChatFormatting color(ContainmentStatus status) {
        return switch (status) {
            case BLOCKED -> ChatFormatting.GREEN;
            case CONTAINED -> ChatFormatting.YELLOW;
            case LEAKING -> ChatFormatting.RED;
        };
    }
}
