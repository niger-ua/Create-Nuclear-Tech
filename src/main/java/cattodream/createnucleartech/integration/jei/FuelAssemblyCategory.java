package cattodream.createnucleartech.integration.jei;

import cattodream.createnucleartech.Createnucleartech;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class FuelAssemblyCategory implements IRecipeCategory<FuelAssemblyJeiRecipe> {
    public static final RecipeType<FuelAssemblyJeiRecipe> TYPE = RecipeType.create(
            Createnucleartech.MODID,
            "crowns_fuel_assembly",
            FuelAssemblyJeiRecipe.class
    );

    private final IDrawable background;
    private final IDrawable icon;

    public FuelAssemblyCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(170, 96);
        icon = guiHelper.createDrawableItemStack(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("crowns", "fuel_assembly"))));
    }

    @Override
    public RecipeType<FuelAssemblyJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.createnucleartech.fuel_assembly");
    }

    @Deprecated
    @Override
    public @Nullable IDrawable getBackground() {
        return background;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FuelAssemblyJeiRecipe recipe, IFocusGroup focuses) {
        int[][] rodSlots = {
                {24, 10}, {42, 10}, {60, 10},
                {24, 28},           {60, 28},
                {24, 46}, {42, 46}, {60, 46}
        };
        for (int[] slot : rodSlots) {
            builder.addInputSlot(slot[0], slot[1]).addIngredients(recipe.rod());
        }
        builder.addInputSlot(42, 28).addItemStack(recipe.core());
        builder.addOutputSlot(126, 28).addItemStack(recipe.output());
    }

    @Override
    public void draw(FuelAssemblyJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("jei.createnucleartech.fuel_assembly.info"),
                6,
                74,
                0xFF707070,
                false
        );
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(FuelAssemblyJeiRecipe recipe) {
        return ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, "jei/crowns_fuel_assembly");
    }
}
