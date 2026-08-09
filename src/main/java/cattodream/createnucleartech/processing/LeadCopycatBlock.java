package cattodream.createnucleartech.processing;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

import java.util.function.Consumer;

public class LeadCopycatBlock extends BaseEntityBlock {
    public static final MapCodec<LeadCopycatBlock> CODEC = simpleCodec(LeadCopycatBlock::new);
    public static final BooleanProperty COPIED = BooleanProperty.create("copied");

    public LeadCopycatBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(COPIED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LeadCopycatBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COPIED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty()) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof LeadCopycatBlockEntity copycat) {
                ItemStack extracted = copycat.extractCopiedStack();
                if (!extracted.isEmpty() && !player.getInventory().add(extracted)) {
                    player.drop(extracted, false);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockState copiedState = blockItem.getBlock().defaultBlockState();
        if (copiedState.is(this) || copiedState.hasBlockEntity()) {
            return ItemInteractionResult.FAIL;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LeadCopycatBlockEntity copycat) {
            if (!copycat.copiedState().isAir()) {
                return ItemInteractionResult.FAIL;
            }
            copycat.setCopiedState(copiedState);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {
            @Override
            public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
                BlockState copiedState = copiedState(level, ((BlockHitResult) target).getBlockPos());
                if (copiedState.isAir()) {
                    return false;
                }
                manager.crack(((BlockHitResult) target).getBlockPos(), ((BlockHitResult) target).getDirection());
                return true;
            }

            @Override
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
                BlockState copiedState = copiedState(level, pos);
                if (copiedState.isAir()) {
                    return false;
                }
                manager.destroy(pos, copiedState);
                return true;
            }

            private BlockState copiedState(Level level, BlockPos pos) {
                if (level.getBlockEntity(pos) instanceof LeadCopycatBlockEntity copycat) {
                    return copycat.copiedState();
                }
                return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            }
        });
    }
}
