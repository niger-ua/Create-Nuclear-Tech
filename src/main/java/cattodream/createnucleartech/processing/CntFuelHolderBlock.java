package cattodream.createnucleartech.processing;

import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.items.CntFuelRodItem;
import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlock;
import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

public class CntFuelHolderBlock extends AssemblyBlock {
    public static final EnumProperty<CntFuelType> FUEL = EnumProperty.create("fuel", CntFuelType.class);

    public CntFuelHolderBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FUEL, CntFuelType.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FUEL);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<AssemblyBlockEntity> getBlockEntityClass() {
        return (Class<AssemblyBlockEntity>) (Class<?>) CntFuelHolderBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AssemblyBlockEntity> getBlockEntityType() {
        return ModRegistry.FUEL_HOLDER_ENTITY.get();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CntFuelHolderBlockEntity holder)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        if (stack.getItem() instanceof CntFuelRodItem rod) {
            CntFuelType type = rod.fuelType();
            if (!type.isFreshFuel()) {
                return ItemInteractionResult.FAIL;
            }
            if (!level.isClientSide) {
                List<CntFuelHolderBlockEntity> targets = fuelColumn(level, pos).stream()
                        .filter(candidate -> candidate.fuelType() == CntFuelType.EMPTY)
                        .toList();
                int holdersToFill = Math.min(targets.size(), countMatching(player, hand, type));
                if (holdersToFill > 0 && player.getAbilities().instabuild) {
                    float burnup = CntFuelRodItem.burnup(stack);
                    for (int i = 0; i < holdersToFill; i++) {
                        targets.get(i).loadFuel(type, burnup);
                    }
                } else if (holdersToFill > 0) {
                    List<Float> burnups = consumeRods(player, hand, type, holdersToFill);
                    for (int i = 0; i < burnups.size(); i++) {
                        targets.get(i).loadFuel(type, burnups.get(i));
                    }
                }
                forceColumnRecheck(level, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.isEmpty()) {
            if (!level.isClientSide) {
                for (CntFuelHolderBlockEntity candidate : fuelColumn(level, pos)) {
                    CntFuelType type = candidate.fuelType();
                    if (type == CntFuelType.EMPTY) {
                        continue;
                    }
                    ItemStack rods = type == CntFuelType.SPENT
                            ? CntFuelType.newSpentRodStack(candidate.spentFrom(), CntFuelHolderBlockEntity.RODS_PER_HOLDER)
                            : type.newRodStack(CntFuelHolderBlockEntity.RODS_PER_HOLDER);
                    if (type == CntFuelType.SPENT && !rods.isEmpty()) {
                        CompoundTag root = new CompoundTag();
                        root.put("spent", candidate.spentRodData());
                        rods.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
                    } else if (!rods.isEmpty()) {
                        CntFuelRodItem.setBurnup(rods, candidate.burnup());
                    }
                    candidate.clearFuel();
                    if (!rods.isEmpty() && !player.getInventory().add(rods)) {
                        player.drop(rods, false);
                    }
                }
                forceColumnRecheck(level, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }

        if (level.getBlockEntity(pos) instanceof CntFuelHolderBlockEntity holder) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null) {
                CompoundTag root = data.copyTag();
                CntFuelType type = CntFuelType.byName(root.getString("fuel"));
                if (type != CntFuelType.EMPTY) {
                    holder.restoreFuel(type, root.getFloat("burnup"), root.getCompound("spent"));
                }
            }
        }
        forceColumnRecheck(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(state.getBlock())) {
            forceColumnRecheck(level, pos);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof CntFuelHolderBlockEntity holder && holder.fuelType() != CntFuelType.EMPTY) {
            CompoundTag composition = holder.saveComposition();
            CompoundTag root = new CompoundTag();
            root.put("composition", composition);
            root.putString("fuel", holder.fuelType().getSerializedName());
            root.putFloat("burnup", holder.burnup());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
        return stack;
    }

    private static List<Float> consumeRods(Player player, InteractionHand hand, CntFuelType type, int count) {
        List<Float> burnups = new ArrayList<>(count);
        if (countMatching(player, hand, type) < count) {
            return burnups;
        }
        int remaining = count;
        ItemStack handStack = player.getItemInHand(hand);
        if (isRod(handStack, type)) {
            int used = Math.min(remaining, handStack.getCount());
            for (int i = 0; i < used; i++) {
                burnups.add(CntFuelRodItem.burnup(handStack));
            }
            handStack.shrink(used);
            remaining -= used;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack candidate = inventory.getItem(i);
            if (candidate == handStack || !isRod(candidate, type)) {
                continue;
            }
            int used = Math.min(remaining, candidate.getCount());
            for (int j = 0; j < used; j++) {
                burnups.add(CntFuelRodItem.burnup(candidate));
            }
            candidate.shrink(used);
            remaining -= used;
        }
        return remaining == 0 ? burnups : List.of();
    }

    private static int countMatching(Player player, InteractionHand hand, CntFuelType type) {
        int count = 0;
        ItemStack handStack = player.getItemInHand(hand);
        if (isRod(handStack, type)) {
            count += handStack.getCount();
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack candidate = inventory.getItem(i);
            if (candidate != handStack && isRod(candidate, type)) {
                count += candidate.getCount();
            }
        }
        return count;
    }

    private static boolean isRod(ItemStack stack, CntFuelType type) {
        return stack.getItem() instanceof CntFuelRodItem rod && rod.fuelType() == type;
    }

    private static void forceColumnRecheck(Level level, BlockPos origin) {
        for (CntFuelHolderBlockEntity candidate : fuelColumn(level, origin)) {
            candidate.forceReactorRecheck();
            level.updateNeighborsAt(candidate.getBlockPos(), candidate.getBlockState().getBlock());
        }
    }

    private static List<CntFuelHolderBlockEntity> fuelColumn(Level level, BlockPos origin) {
        BlockState originState = level.getBlockState(origin);
        List<CntFuelHolderBlockEntity> holders = new ArrayList<>();
        BlockPos bottom = origin;
        while (bottom.getY() > level.getMinBuildHeight() && level.getBlockState(bottom.below()).is(originState.getBlock())) {
            bottom = bottom.below();
        }
        BlockPos cursor = bottom;
        while (cursor.getY() < level.getMaxBuildHeight() && level.getBlockState(cursor).is(originState.getBlock())) {
            if (level.getBlockEntity(cursor) instanceof CntFuelHolderBlockEntity candidate) {
                holders.add(candidate);
            }
            cursor = cursor.above();
        }
        return holders;
    }
}
