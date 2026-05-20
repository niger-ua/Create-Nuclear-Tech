package cattodream.createnucleartech.menu;

import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.processing.LeadIrradiationBoxBlockEntity;
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

public class LeadIrradiationBoxMenu extends AbstractContainerMenu {
    private static final int BOX_SLOT_COUNT = LeadIrradiationBoxBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = BOX_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final ContainerLevelAccess access;
    private final Container container;
    private final ContainerData data;

    public LeadIrradiationBoxMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, readContext(playerInventory, buffer));
    }

    public LeadIrradiationBoxMenu(int containerId, Inventory playerInventory, LeadIrradiationBoxBlockEntity box) {
        this(containerId, playerInventory, box, box.dataAccess(), ContainerLevelAccess.create(box.getLevel(), box.getBlockPos()));
    }

    private LeadIrradiationBoxMenu(int containerId, Inventory playerInventory, MenuContext context) {
        this(containerId, playerInventory, context.container(), context.data(), context.access());
    }

    private LeadIrradiationBoxMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, ContainerLevelAccess access) {
        super(ModRegistry.LEAD_IRRADIATION_BOX_MENU.get(), containerId);
        checkContainerSize(container, BOX_SLOT_COUNT);
        checkContainerDataCount(data, 3);
        this.access = access;
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new IrradiationInputSlot(container, column + row * 3, 83 + column * 18, 31 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 29 + column * 18, 103 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 29 + column * 18, 161));
        }

        addDataSlots(data);
    }

    public double progress() {
        return data.get(0) / 10000.0D;
    }

    public double fieldStrength() {
        return data.get(1) / 100.0D;
    }

    public int activeInputCount() {
        return data.get(2);
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
        if (index < BOX_SLOT_COUNT) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (LeadIrradiationBoxBlockEntity.isValidInput(source)) {
            if (!moveItemStackTo(source, 0, BOX_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_END, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(source, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
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
        return stillValid(access, player, ModRegistry.LEAD_IRRADIATION_BOX.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    private static MenuContext readContext(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof LeadIrradiationBoxBlockEntity box) {
            ContainerData data = playerInventory.player.level().isClientSide ? new SimpleContainerData(3) : box.dataAccess();
            return new MenuContext(box, data, ContainerLevelAccess.create(playerInventory.player.level(), pos));
        }
        return new MenuContext(new SimpleContainer(BOX_SLOT_COUNT), new SimpleContainerData(3), ContainerLevelAccess.NULL);
    }

    private record MenuContext(Container container, ContainerData data, ContainerLevelAccess access) {
    }

    private static class IrradiationInputSlot extends Slot {
        private IrradiationInputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return LeadIrradiationBoxBlockEntity.isValidInput(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
