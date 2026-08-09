package cattodream.createnucleartech.recipe;

import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.items.CntFuelRodItem;
import cattodream.createnucleartech.integration.crowns.CrownsFuelProfile;
import cattodream.createnucleartech.processing.CntFuelType;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Complex fuel assembly recipe for the addon progression.
 * Crowns stores fuel isotope ratios in custom_data.composition; this recipe
 * copies the actual average composition from the eight rods into the block so
 * plutonium, MOX, natural, and enriched rods cannot collapse into the wrong
 * static Crowns result.
 */
public class CrownsFuelAssemblyRecipe extends CustomRecipe {
    private static final ResourceLocation FUEL_ASSEMBLY = ResourceLocation.fromNamespaceAndPath("crowns", "fuel_assembly");
    private static final ResourceLocation ASSEMBLY_CORE = ResourceLocation.fromNamespaceAndPath("chemica", "tungsten_ingot");

    public CrownsFuelAssemblyRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return collectComposition(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        CompoundTag composition = collectComposition(input);
        if (composition == null) {
            return ItemStack.EMPTY;
        }

        Item assembly = BuiltInRegistries.ITEM.get(FUEL_ASSEMBLY);
        ItemStack result = new ItemStack(assembly);
        CompoundTag root = new CompoundTag();
        root.put("composition", composition);
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(BuiltInRegistries.ITEM.get(FUEL_ASSEMBLY));
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRegistry.CROWNS_FUEL_ASSEMBLY_RECIPE.get();
    }

    private static CompoundTag collectComposition(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3) {
            return null;
        }
        if (!isAssemblyFrame(input.getItem(1, 1))) {
            return null;
        }

        Map<String, Double> totals = new HashMap<>();
        Map<String, Integer> profileCounts = new HashMap<>();
        int rods = 0;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                if (x == 1 && y == 1) {
                    continue;
                }
                CompoundTag composition = compositionForRod(input.getItem(x, y));
                if (composition == null) {
                    return null;
                }
                rods++;
                for (String key : composition.getAllKeys()) {
                    if (CrownsFuelProfile.PROFILE_KEY.equals(key)) {
                        String profile = composition.getString(key);
                        if (!profile.isBlank()) {
                            profileCounts.merge(profile, 1, Integer::sum);
                        }
                        continue;
                    }
                    double amount = composition.getDouble(key);
                    if (amount > 0.0D) {
                        totals.merge(key, amount, Double::sum);
                    }
                }
            }
        }

        if (rods != 8 || totals.isEmpty()) {
            return null;
        }

        CompoundTag averaged = new CompoundTag();
        for (Map.Entry<String, Double> entry : totals.entrySet()) {
            averaged.putDouble(entry.getKey(), entry.getValue() / rods);
        }
        int rodCount = rods;
        profileCounts.entrySet().stream()
                .filter(entry -> entry.getValue() == rodCount)
                .findFirst()
                .ifPresent(entry -> averaged.putString(CrownsFuelProfile.PROFILE_KEY, entry.getKey()));
        return averaged;
    }

    private static CompoundTag compositionForRod(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof CntFuelRodItem rodItem) {
            CntFuelType type = rodItem.fuelType();
            CompoundTag composition = type.composition();
            if (type == CntFuelType.SPENT) {
                return null;
            }
            return composition.isEmpty() ? null : composition;
        }
        return null;
    }

    private static boolean isAssemblyFrame(ItemStack stack) {
        return ASSEMBLY_CORE.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }
}
