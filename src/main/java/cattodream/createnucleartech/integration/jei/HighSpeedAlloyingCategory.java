package cattodream.createnucleartech.integration.jei;

import cattodream.createnucleartech.Createnucleartech;
import cattodream.createnucleartech.ModRegistry;
import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.compat.jei.category.animations.AnimatedMixer;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class HighSpeedAlloyingCategory implements IRecipeCategory<HighSpeedAlloyingJeiRecipe> {
    public static final RecipeType<HighSpeedAlloyingJeiRecipe> TYPE = RecipeType.create(
            Createnucleartech.MODID,
            "high_speed_alloying",
            HighSpeedAlloyingJeiRecipe.class
    );

    private final IDrawable background;
    private final IDrawable icon;
    private final AnimatedMixer mixer;
    private final AnimatedBlazeBurner heater;

    public HighSpeedAlloyingCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(177, 103);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.HIGH_SPEED_MIXER.get()));
        mixer = new AnimatedMixer();
        heater = new AnimatedBlazeBurner();
    }

    @Override
    public RecipeType<HighSpeedAlloyingJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.createnucleartech.high_speed_alloying");
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
    public void setRecipe(IRecipeLayoutBuilder builder, HighSpeedAlloyingJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(27, 52).addIngredients(recipe.firstInput());
        builder.addInputSlot(46, 52).addIngredients(recipe.secondInput());
        builder.addOutputSlot(143, 52).addItemStack(recipe.output());
    }

    @Override
    public void draw(HighSpeedAlloyingJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        AllGuiTextures.JEI_SLOT.render(guiGraphics, 26, 51);
        AllGuiTextures.JEI_SLOT.render(guiGraphics, 45, 51);
        AllGuiTextures.JEI_SLOT.render(guiGraphics, 142, 51);
        AllGuiTextures.JEI_DOWN_ARROW.render(guiGraphics, 136, 32);
        AllGuiTextures.JEI_LIGHT.render(guiGraphics, 81, 88);
        AllGuiTextures.JEI_HEAT_BAR.render(guiGraphics, 4, 80);
        heater.withHeat(BlazeBurnerBlock.HeatLevel.SEETHING).draw(guiGraphics, 91, 55);
        mixer.draw(guiGraphics, 91, 34);
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(HighSpeedAlloyingJeiRecipe recipe) {
        ResourceLocation outputId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(recipe.output().getItem());
        return ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, "jei/high_speed_alloying/" + outputId.getPath());
    }
}
