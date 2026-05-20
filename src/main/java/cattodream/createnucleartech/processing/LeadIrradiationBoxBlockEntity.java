package cattodream.createnucleartech.processing;

import cattodream.createnucleartech.CNTTags;
import cattodream.createnucleartech.Config;
import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.menu.LeadIrradiationBoxMenu;
import cattodream.createnucleartech.radiation.RadiationData;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Arrays;

public class LeadIrradiationBoxBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int SLOT_COUNT = 9;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final double[] exposure = new double[SLOT_COUNT];
    private final int[] neptuniumAge = new int[SLOT_COUNT];
    private double lastField;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.round(maxProgress() * 10000.0D);
                case 1 -> (int) Math.round(lastField * 100.0D);
                case 2 -> activeInputCount();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 1) {
                lastField = value / 100.0D;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public LeadIrradiationBoxBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModRegistry.LEAD_IRRADIATION_BOX_ENTITY.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LeadIrradiationBoxBlockEntity box) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (level.getGameTime() % Math.max(1, Config.leadBoxTickInterval) != 0 || box.isEmpty()) {
            return;
        }

        box.lastField = RadiationData.get(serverLevel).radiationAt(serverLevel, pos);
        if (box.lastField < Config.leadBoxMinimumFieldStrength) {
            boolean changed = box.decayNeptunium();
            if (changed) {
                box.setChanged();
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
            return;
        }

        boolean changed = false;
        for (int slot = 0; slot < box.items.size(); slot++) {
            ItemStack stack = box.items.get(slot);

            if (stack.isEmpty()) {
                box.exposure[slot] = 0.0D;
                box.neptuniumAge[slot] = 0;
                continue;
            }

            // Np-239 decays by age, but a strong field can finish it sooner.
            if (stack.is(ModRegistry.NEPTUNIUM_239.asItem())) {
                box.neptuniumAge[slot] += Math.max(1, Config.leadBoxTickInterval);
                box.exposure[slot] += box.lastField * Config.leadBoxExposureGainMultiplier;
                if (box.exposure[slot] >= requiredExposure() || box.neptuniumAge[slot] >= neptuniumDecayTicks()) {
                    box.items.set(slot, new ItemStack(ModRegistry.PLUTONIUM_239_INGOT.get()));
                    box.exposure[slot] = 0.0D;
                    box.neptuniumAge[slot] = 0;
                }
                changed = true;
                continue;
            }

            ItemStack result = irradiationResult(stack);
            if (result.isEmpty()) {
                box.exposure[slot] = 0.0D;
                box.neptuniumAge[slot] = 0;
                continue;
            }

            // Normal irradiation progress
            box.exposure[slot] += box.lastField * Config.leadBoxExposureGainMultiplier;

            if (box.exposure[slot] >= requiredExposure()) {
                box.items.set(slot, result);
                box.exposure[slot] = 0.0D;
                box.neptuniumAge[slot] = 0;
                changed = true;
            } else {
                changed = true;
            }
        }

        if (changed) {
            box.setChanged();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public static boolean isValidInput(ItemStack stack) {
        return !irradiationResult(stack).isEmpty();
    }

    public static ItemStack irradiationResult(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (stack.is(CNTTags.Items.URANIUM_238)) {
            // U-238 -> Np-239 intermediate
            return new ItemStack(ModRegistry.NEPTUNIUM_239.get());
        }
        if (stack.is(ModRegistry.NEPTUNIUM_239.asItem())) {
            // Np-239 -> Pu-239
            return new ItemStack(ModRegistry.PLUTONIUM_239_INGOT.get());
        }
        if (stack.is(ModRegistry.PLUTONIUM_239_INGOT.asItem())) {
            // Pu-239 -> Pu-240 (overexposure)
            return new ItemStack(ModRegistry.PLUTONIUM_240_INGOT.get());
        }
        if (stack.is(CNTTags.Items.COBALT_IRRADIATION_TARGET)) {
            return new ItemStack(ModRegistry.COBALT_60_SOURCE.get());
        }
        if (stack.is(CNTTags.Items.IRIDIUM_IRRADIATION_TARGET)) {
            return new ItemStack(ModRegistry.IRIDIUM_192_SOURCE.get());
        }
        return ItemStack.EMPTY;
    }

    public ContainerData dataAccess() {
        return data;
    }

    public double progress() {
        return maxProgress();
    }

    public double fieldStrength() {
        return lastField;
    }

    public Component statusMessage() {
        if (isEmpty()) {
            return Component.translatable("message.createnucleartech.lead_box.empty");
        }
        if (hasFinishedOutput()) {
            return Component.translatable("message.createnucleartech.lead_box.done");
        }
        return Component.translatable(
                "message.createnucleartech.lead_box.progress",
                Math.round(maxProgress() * 100.0D),
                String.format("%.1f", lastField)
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.createnucleartech.lead_irradiation_box");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new LeadIrradiationBoxMenu(containerId, playerInventory, this);
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
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
            if (items.get(slot).isEmpty()) {
                exposure[slot] = 0.0D;
                neptuniumAge[slot] = 0;
            }
            setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        exposure[slot] = 0.0D;
        neptuniumAge[slot] = 0;
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        exposure[slot] = 0.0D;
        neptuniumAge[slot] = 0;
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isValidInput(stack);
    }

    @Override
    public void clearContent() {
        items.clear();
        Arrays.fill(exposure, 0.0D);
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        Arrays.fill(exposure, 0.0D);
        Arrays.fill(neptuniumAge, 0);
        ListTag exposureTags = tag.getList("SlotExposure", Tag.TAG_COMPOUND);
        for (int i = 0; i < exposureTags.size(); i++) {
            CompoundTag exposureTag = exposureTags.getCompound(i);
            int slot = exposureTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < exposure.length) {
                exposure[slot] = exposureTag.getDouble("Exposure");
            }
        }
        ListTag ageTags = tag.getList("SlotNeptuniumAge", Tag.TAG_COMPOUND);
        for (int i = 0; i < ageTags.size(); i++) {
            CompoundTag ageTag = ageTags.getCompound(i);
            int slot = ageTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < neptuniumAge.length) {
                neptuniumAge[slot] = ageTag.getInt("Age");
            }
        }
        lastField = tag.getDouble("LastField");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        ListTag exposureTags = new ListTag();
        for (int slot = 0; slot < exposure.length; slot++) {
            if (exposure[slot] > 0.0D) {
                CompoundTag exposureTag = new CompoundTag();
                exposureTag.putByte("Slot", (byte) slot);
                exposureTag.putDouble("Exposure", exposure[slot]);
                exposureTags.add(exposureTag);
            }
        }
        tag.put("SlotExposure", exposureTags);

        ListTag ageTags = new ListTag();
        for (int slot = 0; slot < neptuniumAge.length; slot++) {
            if (neptuniumAge[slot] > 0) {
                CompoundTag ageTag = new CompoundTag();
                ageTag.putByte("Slot", (byte) slot);
                ageTag.putInt("Age", neptuniumAge[slot]);
                ageTags.add(ageTag);
            }
        }
        tag.put("SlotNeptuniumAge", ageTags);
        tag.putDouble("LastField", lastField);
    }

    private double maxProgress() {
        double required = requiredExposure();
        if (required <= 0.0D) {
            return hasValidInput() ? 1.0D : 0.0D;
        }
        double max = 0.0D;
        for (double value : exposure) {
            max = Math.max(max, value / required);
        }
        return Math.min(1.0D, max);
    }

    private static double requiredExposure() {
        return Config.leadBoxRequiredExposure * Config.irradiationCostMultiplier;
    }

    private static int neptuniumDecayTicks() {
        return 30 * 60 * 20;
    }

    private boolean decayNeptunium() {
        boolean changed = false;
        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (!stack.is(ModRegistry.NEPTUNIUM_239.asItem())) {
                continue;
            }
            neptuniumAge[slot] += Math.max(1, Config.leadBoxTickInterval);
            if (neptuniumAge[slot] >= neptuniumDecayTicks()) {
                items.set(slot, new ItemStack(ModRegistry.PLUTONIUM_239_INGOT.get()));
                exposure[slot] = 0.0D;
                neptuniumAge[slot] = 0;
                changed = true;
            }
        }
        return changed;
    }

    private int activeInputCount() {
        int count = 0;
        for (ItemStack stack : items) {
            if (!irradiationResult(stack).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private boolean hasValidInput() {
        return activeInputCount() > 0;
    }

    private boolean hasFinishedOutput() {
        for (ItemStack stack : items) {
            if (stack.is(ModRegistry.PLUTONIUM_CORE.asItem())
                    || stack.is(ModRegistry.COBALT_60_SOURCE.asItem())
                    || stack.is(ModRegistry.IRIDIUM_192_SOURCE.asItem())
                    || stack.is(ModRegistry.PLUTONIUM_239_INGOT.asItem())
                    || stack.is(ModRegistry.PLUTONIUM_240_INGOT.asItem())) {
                return true;
            }
        }
        return false;
    }
}
