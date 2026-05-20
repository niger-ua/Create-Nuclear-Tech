package cattodream.createnucleartech.integration.jei;

import cattodream.createnucleartech.Createnucleartech;
import cattodream.createnucleartech.ModRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class BlastFurnaceCategory implements IRecipeCategory<BlastFurnaceJeiRecipe> {
    private static final ResourceLocation GUI = ResourceLocation.fromNamespaceAndPath(
            Createnucleartech.MODID,
            "textures/gui/blast_furnace.png"
    );
    public static final RecipeType<BlastFurnaceJeiRecipe> TYPE = RecipeType.create(
            Createnucleartech.MODID,
            "blast_furnace",
            BlastFurnaceJeiRecipe.class
    );

    private final IDrawable background;
    private final IDrawable icon;

    public BlastFurnaceCategory(IGuiHelper guiHelper) {
        background = guiHelper.createDrawable(GUI, 6, 16, 150, 56);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.BLAST_FURNACE.get()));
    }

    @Override
    public RecipeType<BlastFurnaceJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.createnucleartech.blast_furnace");
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
    public void setRecipe(IRecipeLayoutBuilder builder, BlastFurnaceJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(73, 1).addIngredients(recipe.upperInput());
        builder.addInputSlot(73, 37).addIngredients(recipe.lowerInput());
        builder.addInputSlot(1, 19).addIngredients(recipe.fuel());
        builder.addOutputSlot(127, 19).addItemStack(recipe.output());
    }

    @Override
    public void draw(BlastFurnaceJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        long tick = Minecraft.getInstance().level == null ? System.currentTimeMillis() / 50L : Minecraft.getInstance().level.getGameTime();
        int progressWidth = (int) (tick % 25L);
        int fuelHeight = (int) (tick % 53L);
        if (fuelHeight > 0) {
            guiGraphics.blit(GUI, 38, 55 - fuelHeight, 201, 53 - fuelHeight, 17, fuelHeight, 256, 256);
        }
        guiGraphics.blit(GUI, 56, 21, 176, 0, 14, 14, 256, 256);
        if (progressWidth > 0) {
            guiGraphics.blit(GUI, 95, 19, 176, 14, progressWidth, 17, 256, 256);
        }
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(BlastFurnaceJeiRecipe recipe) {
        return ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, "jei/blast_furnace_steel_mix");
    }
}
