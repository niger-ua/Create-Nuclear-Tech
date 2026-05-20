package cattodream.createnucleartech.items;

import cattodream.createnucleartech.Config;
import cattodream.createnucleartech.radiation.RadiationEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AntiradinItem extends Item {
    public AntiradinItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel) {
            double removed = RadiationEvents.applyAntiradin(player, Config.antiradinDoseReduction, Config.antiradinCooldownTicks);
            player.displayClientMessage(Component.translatable("message.createnucleartech.antiradin", String.format("%.1f", removed)).withStyle(ChatFormatting.GREEN), true);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (Config.antiradinCooldownTicks > 0) {
                player.getCooldowns().addCooldown(this, Config.antiradinCooldownTicks);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
