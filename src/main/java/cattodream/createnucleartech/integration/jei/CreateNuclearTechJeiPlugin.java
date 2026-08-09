package cattodream.createnucleartech.integration.jei;

import cattodream.createnucleartech.CNTTags;
import cattodream.createnucleartech.Createnucleartech;
import cattodream.createnucleartech.ModRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRuntimeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class CreateNuclearTechJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new LeadIrradiationCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new BlastFurnaceCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new HighSpeedAlloyingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(LeadIrradiationCategory.TYPE, List.of(
                new LeadIrradiationJeiRecipe(
                        Ingredient.of(CNTTags.Items.URANIUM_238),
                        new ItemStack(ModRegistry.NEPTUNIUM_239.get())
                ),
                new LeadIrradiationJeiRecipe(
                        Ingredient.of(ModRegistry.NEPTUNIUM_239.get()),
                        new ItemStack(ModRegistry.PLUTONIUM_239_INGOT.get())
                ),
                new LeadIrradiationJeiRecipe(
                        Ingredient.of(ModRegistry.PLUTONIUM_239_INGOT.get()),
                        new ItemStack(ModRegistry.PLUTONIUM_240_INGOT.get())
                ),
                new LeadIrradiationJeiRecipe(
                        Ingredient.of(CNTTags.Items.COBALT_IRRADIATION_TARGET),
                        new ItemStack(ModRegistry.COBALT_60_SOURCE.get())
                ),
                new LeadIrradiationJeiRecipe(
                        Ingredient.of(CNTTags.Items.IRIDIUM_IRRADIATION_TARGET),
                        new ItemStack(ModRegistry.IRIDIUM_192_SOURCE.get())
                )
        ));
        registration.addRecipes(BlastFurnaceCategory.TYPE, List.of(
                new BlastFurnaceJeiRecipe(
                        Ingredient.of(ModRegistry.STEEL_MIX.get()),
                        Ingredient.of(Items.COAL),
                        Ingredient.of(Items.COAL, Items.COAL_BLOCK),
                        new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("tfmg", "steel_ingot")))
                ),
                new BlastFurnaceJeiRecipe(
                        Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "zinc_ingot"))),
                        Ingredient.of(Items.COPPER_INGOT),
                        Ingredient.of(Items.COAL, Items.COAL_BLOCK),
                        new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "brass_ingot")))
                ),
                new BlastFurnaceJeiRecipe(
                        Ingredient.of(ModRegistry.REDSTONE_INGOT.get()),
                        Ingredient.of(Items.COPPER_INGOT),
                        Ingredient.of(Items.COAL, Items.COAL_BLOCK),
                        new ItemStack(ModRegistry.RED_COPPER_INGOT.get())
                ),
                new BlastFurnaceJeiRecipe(
                        Ingredient.of(ModRegistry.RED_COPPER_INGOT.get()),
                        Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("tfmg", "steel_ingot"))),
                        Ingredient.of(Items.COAL, Items.COAL_BLOCK),
                        new ItemStack(ModRegistry.ADVANCED_ALLOY_INGOT.get())
                ),
                new BlastFurnaceJeiRecipe(
                        Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "golden_sheet"))),
                        Ingredient.of(ModRegistry.MIXED_PLATE.get()),
                        Ingredient.of(Items.COAL, Items.COAL_BLOCK),
                        new ItemStack(ModRegistry.PAA_ALLOY_PLATE.get())
                )
        ));
        registration.addRecipes(HighSpeedAlloyingCategory.TYPE, List.of(
                new HighSpeedAlloyingJeiRecipe(
                        Ingredient.of(ModRegistry.REDSTONE_INGOT.get()),
                        Ingredient.of(Items.COPPER_INGOT),
                        new ItemStack(ModRegistry.RED_COPPER_INGOT.get(), 2),
                        1024,
                        60
                ),
                new HighSpeedAlloyingJeiRecipe(
                        Ingredient.of(ModRegistry.RED_COPPER_INGOT.get()),
                        Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("tfmg", "steel_ingot"))),
                        new ItemStack(ModRegistry.ADVANCED_ALLOY_INGOT.get(), 2),
                        1024,
                        60
                )
        ));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(LeadIrradiationCategory.TYPE, ModRegistry.LEAD_IRRADIATION_BOX.get());
        registration.addRecipeCatalysts(BlastFurnaceCategory.TYPE, ModRegistry.BLAST_FURNACE.get());
        registration.addRecipeCatalysts(HighSpeedAlloyingCategory.TYPE, ModRegistry.HIGH_SPEED_MIXER.get());
    }

    @Override
    public void registerRuntime(IRuntimeRegistration registration) {
        registration.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hiddenItemStacks());
    }

    private static List<ItemStack> hiddenItemStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(ModRegistry.REFLECTOR_TIER_1.get()));
        stacks.add(new ItemStack(ModRegistry.REFLECTOR_TIER_2.get()));
        stacks.add(new ItemStack(ModRegistry.REFLECTOR_TIER_3.get()));
        stacks.add(new ItemStack(ModRegistry.EARLY_NEUTRON_REFLECTOR_ITEM.get()));
        stacks.add(new ItemStack(ModRegistry.ADVANCED_NEUTRON_REFLECTOR_ITEM.get()));
        stacks.add(new ItemStack(ModRegistry.ELITE_NEUTRON_REFLECTOR_ITEM.get()));
        addOptional(stacks, "crowns", "fuel_assembly");
        return stacks;
    }

    private static void addOptional(List<ItemStack> stacks, String namespace, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item != Items.AIR) {
            stacks.add(new ItemStack(item));
        }
    }
}
