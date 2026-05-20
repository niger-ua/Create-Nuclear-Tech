package cattodream.createnucleartech.processing;

import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.menu.BlastFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class BlastFurnaceBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SLOT_COUNT = 4;
    public static final int SLOT_UPPER_INPUT = 0;
    public static final int SLOT_LOWER_INPUT = 1;
    public static final int SLOT_FUEL = 2;
    public static final int SLOT_OUTPUT = 3;
    private static final int MAX_FUEL = 12800;
    private static final int MAX_PROGRESS = 400;
    private static final ResourceLocation STEEL_INGOT = ResourceLocation.fromNamespaceAndPath("tfmg", "steel_ingot");
    private static final ResourceLocation ZINC_INGOT = ResourceLocation.fromNamespaceAndPath("create", "zinc_ingot");
    private static final ResourceLocation BRASS_INGOT = ResourceLocation.fromNamespaceAndPath("create", "brass_ingot");
    private static final ResourceLocation GOLDEN_SHEET = ResourceLocation.fromNamespaceAndPath("create", "golden_sheet");
    private static final ItemLikeRecipe[] RECIPES = {
            new ItemLikeRecipe(ModRegistry.STEEL_MIX::asItem, () -> Items.COAL, () -> BuiltInRegistries.ITEM.get(STEEL_INGOT), 1),
            new ItemLikeRecipe(() -> BuiltInRegistries.ITEM.get(ZINC_INGOT), () -> Items.COPPER_INGOT, () -> BuiltInRegistries.ITEM.get(BRASS_INGOT), 1),
            new ItemLikeRecipe(ModRegistry.REDSTONE_INGOT::asItem, () -> Items.COPPER_INGOT, ModRegistry.RED_COPPER_INGOT::asItem, 1),
            new ItemLikeRecipe(ModRegistry.RED_COPPER_INGOT::asItem, () -> BuiltInRegistries.ITEM.get(STEEL_INGOT), ModRegistry.ADVANCED_ALLOY_INGOT::asItem, 1),
            new ItemLikeRecipe(() -> BuiltInRegistries.ITEM.get(GOLDEN_SHEET), ModRegistry.MIXED_PLATE::asItem, ModRegistry.PAA_ALLOY_PLATE::asItem, 2)
    };

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int progress;
    private int fuel;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> fuel;
                case 2 -> canProcess() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            } else if (index == 1) {
                fuel = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public BlastFurnaceBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModRegistry.BLAST_FURNACE_ENTITY.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlastFurnaceBlockEntity furnace) {
        boolean changed = false;

        int fuelPower = fuelPower(furnace.items.get(SLOT_FUEL));
        if (fuelPower > 0 && furnace.fuel <= MAX_FUEL - fuelPower) {
            furnace.items.get(SLOT_FUEL).shrink(1);
            furnace.fuel += fuelPower;
            changed = true;
        }

        if (furnace.canProcess()) {
            furnace.fuel = Math.max(0, furnace.fuel - 1);
            furnace.progress++;
            changed = true;
            if (furnace.progress >= MAX_PROGRESS) {
                furnace.progress -= MAX_PROGRESS;
                furnace.processItem();
            }
        } else if (furnace.progress != 0) {
            furnace.progress = 0;
            changed = true;
        }

        boolean lit = furnace.canProcess();
        if (state.hasProperty(BlastFurnaceBlock.LIT) && state.getValue(BlastFurnaceBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(BlastFurnaceBlock.LIT, lit), Block.UPDATE_CLIENTS);
            state = level.getBlockState(pos);
            changed = true;
        }

        if (changed) {
            furnace.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public ContainerData dataAccess() {
        return data;
    }

    public int progress() {
        return progress;
    }

    public int fuel() {
        return fuel;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.createnucleartech.blast_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BlastFurnaceMenu(containerId, playerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(items, slot, amount);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_UPPER_INPUT, SLOT_LOWER_INPUT -> isAnyRecipeInput(stack);
            case SLOT_FUEL -> fuelPower(stack) > 0;
            default -> false;
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        progress = 0;
        fuel = 0;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = tag.getInt("Progress");
        fuel = tag.getInt("Fuel");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Progress", progress);
        tag.putInt("Fuel", fuel);
    }

    private boolean canProcess() {
        if (fuel <= 0 || !isRecipeInput(items.get(SLOT_UPPER_INPUT), items.get(SLOT_LOWER_INPUT))) {
            return false;
        }
        ItemStack output = result();
        if (output.isEmpty()) {
            return false;
        }
        ItemStack currentOutput = items.get(SLOT_OUTPUT);
        return currentOutput.isEmpty()
                || ItemStack.isSameItemSameComponents(currentOutput, output) && currentOutput.getCount() + output.getCount() <= currentOutput.getMaxStackSize();
    }

    private void processItem() {
        if (!canProcess()) {
            return;
        }
        ItemStack output = result();
        items.get(SLOT_UPPER_INPUT).shrink(1);
        items.get(SLOT_LOWER_INPUT).shrink(1);
        ItemStack currentOutput = items.get(SLOT_OUTPUT);
        if (currentOutput.isEmpty()) {
            items.set(SLOT_OUTPUT, output.copy());
        } else {
            currentOutput.grow(output.getCount());
        }
    }

    private static boolean isRecipeInput(ItemStack upper, ItemStack lower) {
        return matchingRecipe(upper, lower) != null;
    }

    private ItemStack result() {
        ItemLikeRecipe recipe = matchingRecipe(items.get(SLOT_UPPER_INPUT), items.get(SLOT_LOWER_INPUT));
        if (recipe == null) {
            return ItemStack.EMPTY;
        }
        Item item = recipe.output().get();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, recipe.outputCount());
    }

    private static boolean isAnyRecipeInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (ItemLikeRecipe recipe : RECIPES) {
            if (stack.is(recipe.first().get()) || stack.is(recipe.second().get())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static ItemLikeRecipe matchingRecipe(ItemStack upper, ItemStack lower) {
        if (upper.isEmpty() || lower.isEmpty()) {
            return null;
        }
        for (ItemLikeRecipe recipe : RECIPES) {
            Item first = recipe.first().get();
            Item second = recipe.second().get();
            if (first == Items.AIR || second == Items.AIR || recipe.output().get() == Items.AIR) {
                continue;
            }
            if ((upper.is(first) && lower.is(second)) || (upper.is(second) && lower.is(first))) {
                return recipe;
            }
        }
        return null;
    }

    private static int fuelPower(ItemStack stack) {
        if (stack.is(Items.COAL)) {
            return 200;
        }
        if (stack.is(Items.COAL_BLOCK)) {
            return 2000;
        }
        return 0;
    }

    private record ItemLikeRecipe(java.util.function.Supplier<Item> first, java.util.function.Supplier<Item> second, java.util.function.Supplier<Item> output, int outputCount) {
    }
}
