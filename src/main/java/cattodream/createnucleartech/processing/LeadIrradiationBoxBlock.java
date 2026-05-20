package cattodream.createnucleartech.processing;

import cattodream.createnucleartech.ModRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class LeadIrradiationBoxBlock extends BaseEntityBlock {
    public static final MapCodec<LeadIrradiationBoxBlock> CODEC = simpleCodec(LeadIrradiationBoxBlock::new);

    public LeadIrradiationBoxBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LeadIrradiationBoxBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide
                ? null
                : createTickerHelper(blockEntityType, ModRegistry.LEAD_IRRADIATION_BOX_ENTITY.get(), LeadIrradiationBoxBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack handStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        openBox(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        openBox(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void openBox(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof LeadIrradiationBoxBlockEntity box)) {
            return;
        }
        if (!level.isClientSide) {
            player.openMenu(box, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LeadIrradiationBoxBlockEntity box) {
            Containers.dropContents(level, pos, box);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
