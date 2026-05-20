package cattodream.createnucleartech.processing;

import cattodream.createnucleartech.ModRegistry;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.Optional;

public class HighSpeedMixerBlockEntity extends MechanicalMixerBlockEntity {
    private static final float MIN_SPEED = 1024.0F;
    private static final ResourceLocation STEEL_INGOT = ResourceLocation.fromNamespaceAndPath("tfmg", "steel_ingot");
    private static final HighSpeedRecipe[] RECIPES = {
            new HighSpeedRecipe(ModRegistry.REDSTONE_INGOT::asItem, () -> Items.COPPER_INGOT, ModRegistry.RED_COPPER_INGOT::asItem, 2, 60),
            new HighSpeedRecipe(ModRegistry.RED_COPPER_INGOT::asItem, () -> BuiltInRegistries.ITEM.get(STEEL_INGOT), ModRegistry.ADVANCED_ALLOY_INGOT::asItem, 2, 60)
    };
    private int highSpeedProgress;
    private HighSpeedRecipe activeRecipe;

    public HighSpeedMixerBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.HIGH_SPEED_MIXER_ENTITY.get(), pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        BasinBlockEntity basin = basin();
        HighSpeedRecipe recipe = matchingRecipe(basin);
        if (basin == null || recipe == null || !canRun(basin, recipe)) {
            highSpeedProgress = 0;
            activeRecipe = null;
            if (running) {
                running = false;
                runningTicks = 0;
                sendData();
            }
            return;
        }

        if (activeRecipe != recipe) {
            highSpeedProgress = 0;
            activeRecipe = recipe;
            running = true;
            runningTicks = 0;
            processingTicks = requiredTicks(recipe);
            sendData();
        }

        running = true;
        if (runningTicks > 36) {
            runningTicks = 20;
        }
        highSpeedProgress++;
        int requiredTicks = requiredTicks(recipe);
        processingTicks = Math.max(1, requiredTicks - highSpeedProgress);
        if (highSpeedProgress >= requiredTicks) {
            process(basin, recipe);
            highSpeedProgress = 0;
            activeRecipe = null;
            processingTicks = -1;
            runningTicks = Math.max(runningTicks, 21);
            sendData();
        }
    }

    private boolean canRun(BasinBlockEntity basin, HighSpeedRecipe recipe) {
        if (Math.abs(getSpeed()) < MIN_SPEED || !isSuperheated(basin)) {
            return false;
        }
        Item item = recipe.output().get();
        if (item == Items.AIR) {
            return false;
        }
        return canInsertOutput(basin, new ItemStack(item, recipe.outputCount()), true);
    }

    private int requiredTicks(HighSpeedRecipe recipe) {
        float speed = Math.max(MIN_SPEED, Math.abs(getSpeed()));
        return Math.max(5, Mth.ceil(recipe.processingTicks() * (MIN_SPEED / speed)));
    }

    private void process(BasinBlockEntity basin, HighSpeedRecipe recipe) {
        SmartInventory inventory = basin.getInputInventory();
        int firstSlot = findSlot(inventory, recipe.first().get());
        int secondSlot = findSlot(inventory, recipe.second().get());
        if (firstSlot < 0 || secondSlot < 0 || firstSlot == secondSlot) {
            return;
        }

        inventory.extractItem(firstSlot, 1, false);
        inventory.extractItem(secondSlot, 1, false);
        ItemStack output = new ItemStack(recipe.output().get(), recipe.outputCount());
        if (!canInsertOutput(basin, output, false)) {
            ItemStack inputRemainder = ItemHandlerHelper.insertItemStacked(inventory, output.copy(), false);
            if (!inputRemainder.isEmpty()) {
                Block.popResource(level, basin.getBlockPos().above(), inputRemainder);
            }
        }
        basin.notifyChangeOfContents();
        basin.notifyUpdate();
    }

    private boolean canInsertOutput(BasinBlockEntity basin, ItemStack stack, boolean simulate) {
        SmartInventory output = basin.getOutputInventory();
        output.allowInsertion();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(output, stack.copy(), simulate);
        output.forbidInsertion();
        return remainder.isEmpty();
    }

    private boolean isSuperheated(BasinBlockEntity basin) {
        BlockPos burnerPos = basin.getBlockPos().below();
        return BasinBlockEntity.getHeatLevelOf(level.getBlockState(burnerPos)).isAtLeast(BlazeBurnerBlock.HeatLevel.SEETHING);
    }

    private BasinBlockEntity basin() {
        if (level == null) {
            return null;
        }
        if (level.getBlockEntity(worldPosition.below(2)) instanceof BasinBlockEntity basin) {
            return basin;
        }
        if (level.getBlockEntity(worldPosition.below()) instanceof BasinBlockEntity basin) {
            return basin;
        }

        for (int y = 1; y <= 3; y++) {
            BlockPos center = worldPosition.below(y);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (level.getBlockEntity(center.relative(direction)) instanceof BasinBlockEntity basin) {
                    return basin;
                }
            }
        }
        return null;
    }

    @Override
    protected Optional<BasinBlockEntity> getBasin() {
        return Optional.ofNullable(basin());
    }

    private HighSpeedRecipe matchingRecipe(BasinBlockEntity basin) {
        if (basin == null) {
            return null;
        }
        SmartInventory inventory = basin.getInputInventory();
        for (HighSpeedRecipe recipe : RECIPES) {
            if (findSlot(inventory, recipe.first().get()) >= 0 && findSlot(inventory, recipe.second().get()) >= 0) {
                return recipe;
            }
        }
        return null;
    }

    private static int findSlot(SmartInventory inventory, Item item) {
        if (item == Items.AIR) {
            return -1;
        }
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (inventory.getStackInSlot(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    private record HighSpeedRecipe(java.util.function.Supplier<Item> first, java.util.function.Supplier<Item> second, java.util.function.Supplier<Item> output, int outputCount, int processingTicks) {
    }
}
