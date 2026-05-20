package cattodream.createnucleartech.items;

import cattodream.createnucleartech.explosion.NuclearBombBlock;
import cattodream.createnucleartech.explosion.NuclearBombBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BombDetonatorItem extends Item {
    public BombDetonatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        if (!NuclearBombBlockEntity.isLinkedDetonator(stack)) {
            player.displayClientMessage(Component.translatable("message.createnucleartech.detonator.unlinked"), true);
            return InteractionResultHolder.fail(stack);
        }

        ResourceLocation dimensionId = NuclearBombBlockEntity.linkedDimension(stack);
        BlockPos pos = NuclearBombBlockEntity.linkedPos(stack);
        if (dimensionId == null || pos == null) {
            player.displayClientMessage(Component.translatable("message.createnucleartech.detonator.invalid"), true);
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel targetLevel = level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (targetLevel == null || !(targetLevel.getBlockEntity(pos) instanceof NuclearBombBlockEntity bomb)) {
            player.displayClientMessage(Component.translatable("message.createnucleartech.detonator.missing"), true);
            return InteractionResultHolder.fail(stack);
        }

        BlockState state = targetLevel.getBlockState(pos);
        if (!(state.getBlock() instanceof NuclearBombBlock)) {
            player.displayClientMessage(Component.translatable("message.createnucleartech.detonator.missing"), true);
            return InteractionResultHolder.fail(stack);
        }

        NuclearBombBlock.launchFromMenu(targetLevel, pos, state, bomb, player);
        player.displayClientMessage(Component.translatable("message.createnucleartech.detonator.armed"), true);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
