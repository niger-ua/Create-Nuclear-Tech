package cattodream.createnucleartech.menu;

import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.processing.BlastFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BlastFurnaceMenu extends AbstractContainerMenu {
    // Change these two values to move every real Minecraft slot over the drawn GUI at once.
    public static final int GUI_SLOT_OFFSET_X = 0;
    public static final int GUI_SLOT_OFFSET_Y = 2;

    // Base slot coordinates are intentionally centralized for quick pixel tuning against the custom HBM-style GUI.
    public static final int UPPER_INPUT_SLOT_X = 80;
    public static final int UPPER_INPUT_SLOT_Y = 16;
    public static final int LOWER_INPUT_SLOT_X = 80;
    public static final int LOWER_INPUT_SLOT_Y = 52;
    public static final int FUEL_SLOT_X = 8;
    public static final int FUEL_SLOT_Y = 34;
    public static final int OUTPUT_SLOT_X = 134;
    public static final int OUTPUT_SLOT_Y = 34;
    public static final int PLAYER_INVENTORY_X = 8;
    public static final int PLAYER_INVENTORY_Y = 82;
    public static final int HOTBAR_X = 8;
    public static final int HOTBAR_Y = 140;

    private static final int FURNACE_SLOT_COUNT = BlastFurnaceBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = FURNACE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final Container container;
    private final ContainerData data;

    public BlastFurnaceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, readContext(playerInventory, buffer));
    }

    public BlastFurnaceMenu(int containerId, Inventory playerInventory, BlastFurnaceBlockEntity furnace) {
        this(containerId, playerInventory, furnace, furnace.dataAccess(), ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos()));
    }

    private BlastFurnaceMenu(int containerId, Inventory playerInventory, MenuContext context) {
        this(containerId, playerInventory, context.container(), context.data(), context.access());
    }

    private BlastFurnaceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, ContainerLevelAccess access) {
        super(ModRegistry.BLAST_FURNACE_MENU.get(), containerId);
        checkContainerSize(container, FURNACE_SLOT_COUNT);
        checkContainerDataCount(data, 3);
        this.access = access;
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        addSlot(new BlastFurnaceSlot(container, BlastFurnaceBlockEntity.SLOT_UPPER_INPUT, slotX(UPPER_INPUT_SLOT_X), slotY(UPPER_INPUT_SLOT_Y)));
        addSlot(new BlastFurnaceSlot(container, BlastFurnaceBlockEntity.SLOT_LOWER_INPUT, slotX(LOWER_INPUT_SLOT_X), slotY(LOWER_INPUT_SLOT_Y)));
        addSlot(new BlastFurnaceSlot(container, BlastFurnaceBlockEntity.SLOT_FUEL, slotX(FUEL_SLOT_X), slotY(FUEL_SLOT_Y)));
        addSlot(new OutputSlot(container, BlastFurnaceBlockEntity.SLOT_OUTPUT, slotX(OUTPUT_SLOT_X), slotY(OUTPUT_SLOT_Y)));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, slotX(PLAYER_INVENTORY_X + column * 18), slotY(PLAYER_INVENTORY_Y + row * 18)));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, slotX(HOTBAR_X + column * 18), slotY(HOTBAR_Y)));
        }

        addDataSlots(data);
    }

    public static int slotX(int baseX) {
        return baseX + GUI_SLOT_OFFSET_X;
    }

    public static int slotY(int baseY) {
        return baseY + GUI_SLOT_OFFSET_Y;
    }

    public int progress() {
        return data.get(0);
    }

    public int fuel() {
        return data.get(1);
    }

    public boolean canProcess() {
        return data.get(2) > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }

        ItemStack source = slot.getItem();
        result = source.copy();
        if (index < FURNACE_SLOT_COUNT) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (container.canPlaceItem(BlastFurnaceBlockEntity.SLOT_FUEL, source)) {
            if (!moveItemStackTo(source, BlastFurnaceBlockEntity.SLOT_FUEL, BlastFurnaceBlockEntity.SLOT_FUEL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (container.canPlaceItem(BlastFurnaceBlockEntity.SLOT_UPPER_INPUT, source)) {
            if (!moveItemStackTo(source, BlastFurnaceBlockEntity.SLOT_UPPER_INPUT, BlastFurnaceBlockEntity.SLOT_UPPER_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (container.canPlaceItem(BlastFurnaceBlockEntity.SLOT_LOWER_INPUT, source)) {
            if (!moveItemStackTo(source, BlastFurnaceBlockEntity.SLOT_LOWER_INPUT, BlastFurnaceBlockEntity.SLOT_LOWER_INPUT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (index < PLAYER_INVENTORY_END) {
                if (!moveItemStackTo(source, PLAYER_INVENTORY_END, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(source, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (source.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModRegistry.BLAST_FURNACE.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    private static MenuContext readContext(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof BlastFurnaceBlockEntity furnace) {
            ContainerData data = playerInventory.player.level().isClientSide ? new SimpleContainerData(3) : furnace.dataAccess();
            return new MenuContext(furnace, data, ContainerLevelAccess.create(playerInventory.player.level(), pos));
        }
        return new MenuContext(new SimpleContainer(FURNACE_SLOT_COUNT), new SimpleContainerData(3), ContainerLevelAccess.NULL);
    }

    private record MenuContext(Container container, ContainerData data, ContainerLevelAccess access) {
    }

    private static class BlastFurnaceSlot extends Slot {
        private BlastFurnaceSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }
    }

    private static class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
