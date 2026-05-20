package cattodream.createnucleartech.explosion;

import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.menu.NuclearBombMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class NuclearBombBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SLOT_COUNT = 10;
    public static final int CORE_SLOT = 4;
    public static final int DETONATOR_SLOT = 9;
    public static final int MAX_TIMER_SECONDS = 60 * 60;
    public static final int MIN_TIMER_SECONDS = 5;
    private static final int DEFAULT_TIMER_SECONDS = 60;
    private static final String LINKED_KEY = "CreateNuclearTechLinkedDetonator";
    private static final String PHANTOM_KEY = "CreateNuclearTechPhantomDetonator";
    private static final String DIMENSION_KEY = "CreateNuclearTechBombDimension";
    private static final String X_KEY = "CreateNuclearTechBombX";
    private static final String Y_KEY = "CreateNuclearTechBombY";
    private static final String Z_KEY = "CreateNuclearTechBombZ";
    private static final ResourceLocation TFMG_NAPALM_BOMB = ResourceLocation.fromNamespaceAndPath("tfmg", "napalm_bomb");

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int timerSeconds = DEFAULT_TIMER_SECONDS;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> timerSeconds;
                case 1 -> hasCore() ? 1 : 0;
                case 2 -> explosiveCount();
                case 3 -> nuclearChancePercent();
                case 4 -> hasTimerActivator() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                setTimerSeconds(value);
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public NuclearBombBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModRegistry.NUCLEAR_BOMB_BLOCK_ENTITY.get(), pos, blockState);
    }

    public ContainerData dataAccess() {
        return data;
    }

    public int timerSeconds() {
        return timerSeconds;
    }

    public void adjustTimer(int deltaSeconds) {
        setTimerSeconds(timerSeconds + deltaSeconds);
    }

    public void setTimerSeconds(int seconds) {
        timerSeconds = Math.max(MIN_TIMER_SECONDS, Math.min(MAX_TIMER_SECONDS, seconds));
        setChanged();
    }

    public boolean hasCore() {
        return items.get(CORE_SLOT).is(ModRegistry.PLUTONIUM_CORE.asItem());
    }

    public int tntCount() {
        int count = 0;
        for (int slot = 0; slot < DETONATOR_SLOT; slot++) {
            if (slot != CORE_SLOT && items.get(slot).is(Items.TNT)) {
                count++;
            }
        }
        return count;
    }

    public int explosiveCount() {
        int count = 0;
        for (int slot = 0; slot < DETONATOR_SLOT; slot++) {
            if (slot != CORE_SLOT && isExplosiveInput(items.get(slot))) {
                count++;
            }
        }
        return count;
    }

    public int nuclearChancePercent() {
        if (!hasCore()) {
            return 0;
        }
        int chance = 0;
        for (int slot = 0; slot < DETONATOR_SLOT; slot++) {
            if (slot == CORE_SLOT) {
                continue;
            }
            ItemStack stack = items.get(slot);
            if (stack.is(Items.TNT)) {
                chance += 10;
            } else if (isNapalmBomb(stack)) {
                chance += 12;
            } else if (stack.is(ModRegistry.SUPER_EXPLOSIVE_LENS_SEGMENT.get())) {
                chance += 50;
            } else if (stack.is(ModRegistry.EXPLOSIVE_LENS_SEGMENT.get())) {
                chance += 25;
            }
        }
        return Math.min(100, chance);
    }

    public boolean canLaunch() {
        return true;
    }

    public boolean canDetonate() {
        return nuclearChancePercent() > 0;
    }

    public double dudChance() {
        double nuclearChance = nuclearChancePercent() / 100.0D;
        return 1.0D - nuclearChance;
    }

    public boolean hasTimerActivator() {
        return items.get(DETONATOR_SLOT).is(Items.CLOCK);
    }

    public boolean hasLinkedDetonator() {
        return isPhantomDetonator(items.get(DETONATOR_SLOT));
    }

    public ItemStack createLinkedDetonator(ItemStack source) {
        ItemStack linked = source.copyWithCount(1);
        CompoundTag tag = linked.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(LINKED_KEY, true);
        tag.putBoolean(PHANTOM_KEY, false);
        if (level != null) {
            tag.putString(DIMENSION_KEY, level.dimension().location().toString());
        }
        tag.putInt(X_KEY, worldPosition.getX());
        tag.putInt(Y_KEY, worldPosition.getY());
        tag.putInt(Z_KEY, worldPosition.getZ());
        linked.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return linked;
    }

    public void installLinkedDetonator(ItemStack linked) {
        ItemStack phantom = linked.copyWithCount(1);
        CompoundTag tag = phantom.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(PHANTOM_KEY, true);
        phantom.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        items.set(DETONATOR_SLOT, phantom);
        setChanged();
    }

    public void clearPhantomDetonator() {
        if (isPhantomDetonator(items.get(DETONATOR_SLOT))) {
            items.set(DETONATOR_SLOT, ItemStack.EMPTY);
            setChanged();
        }
    }

    public void consumeAssembly() {
        clearContent();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.createnucleartech.nuclear_bomb");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NuclearBombMenu(containerId, playerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
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
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
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
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == CORE_SLOT) {
            return stack.is(ModRegistry.PLUTONIUM_CORE.asItem());
        }
        if (slot == DETONATOR_SLOT) {
            return isDetonatorInput(stack);
        }
        return isExplosiveInput(stack);
    }

    public static boolean isExplosiveInput(ItemStack stack) {
        return stack.is(Items.TNT) || isNapalmBomb(stack) || isExplosiveLens(stack);
    }

    public static boolean isExplosiveLens(ItemStack stack) {
        return stack.is(ModRegistry.EXPLOSIVE_LENS_SEGMENT.get())
                || stack.is(ModRegistry.SUPER_EXPLOSIVE_LENS_SEGMENT.get());
    }

    public static boolean isDetonatorInput(ItemStack stack) {
        return stack.is(Items.CLOCK) || stack.is(ModRegistry.BOMB_DETONATOR.get());
    }

    public static boolean isPhantomDetonator(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return !stack.isEmpty()
                && stack.is(ModRegistry.BOMB_DETONATOR.get())
                && data != null
                && data.copyTag().getBoolean(PHANTOM_KEY);
    }

    public static boolean isLinkedDetonator(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return !stack.isEmpty()
                && stack.is(ModRegistry.BOMB_DETONATOR.get())
                && data != null
                && data.copyTag().getBoolean(LINKED_KEY);
    }

    public static ResourceLocation linkedDimension(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        String value = data.copyTag().getString(DIMENSION_KEY);
        return value.isBlank() ? null : ResourceLocation.tryParse(value);
    }

    public static BlockPos linkedPos(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(X_KEY) || !tag.contains(Y_KEY) || !tag.contains(Z_KEY)) {
            return null;
        }
        return new BlockPos(tag.getInt(X_KEY), tag.getInt(Y_KEY), tag.getInt(Z_KEY));
    }

    public static boolean isNapalmBomb(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(TFMG_NAPALM_BOMB);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        timerSeconds = tag.contains("TimerSeconds") ? tag.getInt("TimerSeconds") : DEFAULT_TIMER_SECONDS;
        setTimerSeconds(timerSeconds);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("TimerSeconds", timerSeconds);
    }
}
