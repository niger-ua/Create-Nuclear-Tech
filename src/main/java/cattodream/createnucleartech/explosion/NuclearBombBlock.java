package cattodream.createnucleartech.explosion;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;

public class NuclearBombBlock extends BaseEntityBlock {
    public static final MapCodec<NuclearBombBlock> CODEC = simpleCodec(NuclearBombBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public NuclearBombBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false).setValue(FACING, Direction.EAST));
    }

    @Override
    public MapCodec<NuclearBombBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NuclearBombBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(LIT, false)
                .setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable net.minecraft.core.Direction face, @Nullable LivingEntity igniter) {
        prime(level, pos, state, igniter);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock()) && level.hasNeighborSignal(pos)) {
            prime(level, pos, state, null);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) {
            prime(level, pos, state, null);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            openBomb(level, pos, player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        boolean launched = prime(level, pos, state, player);
        if (!launched) {
            openBomb(level, pos, player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        Item item = stack.getItem();
        if (stack.is(Items.FLINT_AND_STEEL)) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        } else {
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(item));
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide && projectile.isOnFire()) {
            Entity owner = projectile.getOwner();
            prime(level, hit.getBlockPos(), state, owner instanceof LivingEntity living ? living : null);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        prime(level, pos, state, null);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof NuclearBombBlockEntity bomb
                && serverLevel.random.nextDouble() >= bomb.dudChance()) {
            NuclearExplosion.detonate(serverLevel, pos);
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        openBomb(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void openBomb(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof NuclearBombBlockEntity bomb) {
            player.openMenu(bomb, pos);
        }
    }

    private boolean prime(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity igniter) {
        if (level.isClientSide || state.getValue(LIT)) {
            return false;
        }
        if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof NuclearBombBlockEntity bomb) {
            launchFromMenu(serverLevel, pos, state, bomb, igniter);
            return true;
        }
        return false;
    }

    public static void launchFromMenu(Level level, BlockPos pos, BlockState state, NuclearBombBlockEntity bomb, @Nullable LivingEntity igniter) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int fuseSeconds = bomb.hasTimerActivator() ? bomb.timerSeconds() : NuclearBombBlockEntity.MIN_TIMER_SECONDS;
        int fuseTicks = Math.max(1, fuseSeconds) * 20;
        double dudChance = bomb.dudChance();
        bomb.consumeAssembly();
        NuclearBombEntity.spawnPrimed(serverLevel, pos, state.getValue(FACING), fuseTicks, dudChance, igniter);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof NuclearBombBlockEntity bomb) {
            bomb.clearPhantomDetonator();
            Containers.dropContents(level, pos, bomb);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
