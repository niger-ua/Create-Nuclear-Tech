package cattodream.createnucleartech.processing;

import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.items.CntFuelRodItem;
import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlock;
import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

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

        if (stack.getItem() instanceof CntFuelRodItem rod && state.getValue(FUEL) == CntFuelType.EMPTY) {
            CntFuelType type = rod.fuelType();
            if (!type.isFreshFuel()) {
                return ItemInteractionResult.FAIL;
            }
            if (!level.isClientSide && consumeRods(player, hand, type, CntFuelHolderBlockEntity.RODS_PER_HOLDER)) {
                holder.loadFuel(type);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.isEmpty() && state.getValue(FUEL) != CntFuelType.EMPTY) {
            if (!level.isClientSide) {
                CntFuelType type = holder.fuelType();
                ItemStack rods = type.newRodStack(CntFuelHolderBlockEntity.RODS_PER_HOLDER);
                if (type == CntFuelType.SPENT && !rods.isEmpty()) {
                    CompoundTag root = new CompoundTag();
                    root.put("spent", holder.spentRodData());
                    rods.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
                }
                holder.clearFuel();
                if (!rods.isEmpty() && !player.getInventory().add(rods)) {
                    player.drop(rods, false);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
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

    private static boolean consumeRods(Player player, InteractionHand hand, CntFuelType type, int count) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (countMatching(player, hand, type) < count) {
            return false;
        }
        int remaining = count;
        ItemStack handStack = player.getItemInHand(hand);
        if (isRod(handStack, type)) {
            int used = Math.min(remaining, handStack.getCount());
            handStack.shrink(used);
            remaining -= used;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack candidate = inventory.getItem(i);
            if (!isRod(candidate, type)) {
                continue;
            }
            int used = Math.min(remaining, candidate.getCount());
            candidate.shrink(used);
            remaining -= used;
        }
        return remaining == 0;
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
}
