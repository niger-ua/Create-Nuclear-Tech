package cattodream.createnucleartech.menu;

import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.explosion.NuclearBombBlock;
import cattodream.createnucleartech.explosion.NuclearBombBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NuclearBombMenu extends AbstractContainerMenu {
    private static final int BOMB_SLOT_COUNT = NuclearBombBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INVENTORY_START = BOMB_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;
    private static final int[] BOMB_SLOT_X = {124, 146, 154, 146, 124, 102, 94, 102, 124};
    private static final int[] BOMB_SLOT_Y = {24, 32, 56, 80, 56, 80, 56, 32, 88};
    private static final int DETONATOR_SLOT_X = 200;
    private static final int DETONATOR_SLOT_Y = 24;

    private final ContainerLevelAccess access;
    private final Container container;
    private final ContainerData data;

    public NuclearBombMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, readContext(playerInventory, buffer));
    }

    public NuclearBombMenu(int containerId, Inventory playerInventory, NuclearBombBlockEntity bomb) {
        this(containerId, playerInventory, bomb, bomb.dataAccess(), ContainerLevelAccess.create(bomb.getLevel(), bomb.getBlockPos()));
    }

    private NuclearBombMenu(int containerId, Inventory playerInventory, MenuContext context) {
        this(containerId, playerInventory, context.container(), context.data(), context.access());
    }

    private NuclearBombMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, ContainerLevelAccess access) {
        super(ModRegistry.NUCLEAR_BOMB_MENU.get(), containerId);
        checkContainerSize(container, BOMB_SLOT_COUNT);
        checkContainerDataCount(data, 5);
        this.access = access;
        this.container = container;
        this.data = data;
        container.startOpen(playerInventory.player);

        for (int slot = 0; slot < BOMB_SLOT_COUNT; slot++) {
            if (slot == NuclearBombBlockEntity.DETONATOR_SLOT) {
                addSlot(new DetonatorSlot(container, slot, DETONATOR_SLOT_X, DETONATOR_SLOT_Y));
            } else {
                addSlot(slot == NuclearBombBlockEntity.CORE_SLOT
                        ? new CoreSlot(container, slot, BOMB_SLOT_X[slot], BOMB_SLOT_Y[slot])
                        : new TntSlot(container, slot, BOMB_SLOT_X[slot], BOMB_SLOT_Y[slot]));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 39 + column * 18, 119 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 39 + column * 18, 177));
        }

        addDataSlots(data);
    }

    public int timerSeconds() {
        return data.get(0);
    }

    public boolean hasCore() {
        return data.get(1) != 0;
    }

    public int tntCount() {
        return data.get(2);
    }

    public boolean canLaunch() {
        return true;
    }

    public int dudChancePercent() {
        return 100 - nuclearChancePercent();
    }

    public int nuclearChancePercent() {
        if (!hasCore()) {
            return 0;
        }
        return data.get(3);
    }

    public boolean hasTimerActivator() {
        return data.get(4) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return switch (id) {
            case 0 -> adjustTimer(-60);
            case 1 -> adjustTimer(-10);
            case 2 -> adjustTimer(10);
            case 3 -> adjustTimer(60);
            case 4 -> launch(player);
            default -> false;
        };
    }

    private boolean adjustTimer(int deltaSeconds) {
        if (container instanceof NuclearBombBlockEntity bomb && bomb.hasTimerActivator()) {
            bomb.adjustTimer(deltaSeconds);
            return true;
        }
        return false;
    }

    private boolean launch(Player player) {
        return access.evaluate((level, pos) -> {
            if (level.isClientSide || !(level.getBlockEntity(pos) instanceof NuclearBombBlockEntity bomb)) {
                return false;
            }
            NuclearBombBlock.launchFromMenu(level, pos, level.getBlockState(pos), bomb, player);
            return true;
        }, false);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == NuclearBombBlockEntity.DETONATOR_SLOT && container instanceof NuclearBombBlockEntity bomb) {
            ItemStack carried = getCarried();
            ItemStack slotStack = bomb.getItem(NuclearBombBlockEntity.DETONATOR_SLOT);
            if (!carried.isEmpty() && carried.is(ModRegistry.BOMB_DETONATOR.get())) {
                ItemStack linked = bomb.createLinkedDetonator(carried);
                bomb.installLinkedDetonator(linked);
                setCarried(linked);
                return;
            }
            if (NuclearBombBlockEntity.isPhantomDetonator(slotStack)) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
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
        if (index < BOMB_SLOT_COUNT) {
            if (!moveItemStackTo(source, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (source.is(ModRegistry.PLUTONIUM_CORE.asItem())) {
            if (!moveItemStackTo(source, NuclearBombBlockEntity.CORE_SLOT, NuclearBombBlockEntity.CORE_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (source.is(ModRegistry.BOMB_DETONATOR.get()) && container instanceof NuclearBombBlockEntity bomb) {
            ItemStack linked = bomb.createLinkedDetonator(source);
            bomb.installLinkedDetonator(linked);
            slot.setByPlayer(linked);
            return result;
        } else if (source.is(net.minecraft.world.item.Items.CLOCK)) {
            if (!moveItemStackTo(source, NuclearBombBlockEntity.DETONATOR_SLOT, NuclearBombBlockEntity.DETONATOR_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (NuclearBombBlockEntity.isExplosiveInput(source)) {
            if (!moveItemStackTo(source, 0, NuclearBombBlockEntity.DETONATOR_SLOT, false)) {
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
        return stillValid(access, player, ModRegistry.NUCLEAR_BOMB.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    private static MenuContext readContext(Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof NuclearBombBlockEntity bomb) {
            ContainerData data = playerInventory.player.level().isClientSide ? new SimpleContainerData(5) : bomb.dataAccess();
            return new MenuContext(bomb, data, ContainerLevelAccess.create(playerInventory.player.level(), pos));
        }
        return new MenuContext(new SimpleContainer(BOMB_SLOT_COUNT), new SimpleContainerData(5), ContainerLevelAccess.NULL);
    }

    private record MenuContext(Container container, ContainerData data, ContainerLevelAccess access) {
    }

    private static class CoreSlot extends Slot {
        private CoreSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ModRegistry.PLUTONIUM_CORE.asItem());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class TntSlot extends Slot {
        private TntSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return NuclearBombBlockEntity.isExplosiveInput(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class DetonatorSlot extends Slot {
        private DetonatorSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return NuclearBombBlockEntity.isDetonatorInput(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return !NuclearBombBlockEntity.isPhantomDetonator(getItem());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
