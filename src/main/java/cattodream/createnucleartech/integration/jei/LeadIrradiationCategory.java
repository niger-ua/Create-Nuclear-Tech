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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LeadIrradiationCategory implements IRecipeCategory<LeadIrradiationJeiRecipe> {
    public static final RecipeType<LeadIrradiationJeiRecipe> TYPE = RecipeType.create(
            Createnucleartech.MODID,
            "lead_irradiation",
            LeadIrradiationJeiRecipe.class
    );

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public LeadIrradiationCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(320, 148);
        icon = guiHelper.createDrawableItemStack(new ItemStack(ModRegistry.LEAD_IRRADIATION_BOX.get()));
        arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<LeadIrradiationJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.createnucleartech.lead_irradiation");
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
    public void setRecipe(IRecipeLayoutBuilder builder, LeadIrradiationJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(42, 62).addIngredients(recipe.input());
        builder.addInputSlot(146, 44).addItemStack(crownsFuelRod());
        builder.addInputSlot(146, 76).addItemStack(new ItemStack(ModRegistry.LEAD_IRRADIATION_BOX.get()));
        builder.addOutputSlot(260, 62).addItemStack(recipe.output());
    }

    @Override
    public void draw(LeadIrradiationJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 84, 62);
        arrow.draw(guiGraphics, 214, 62);
        guiGraphics.fill(132, 36, 180, 104, 0x552E2E2E);
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("jei.createnucleartech.lead_irradiation.source"),
                117,
                18,
                0xFF707070,
                false
        );
        var font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(Component.translatable("jei.createnucleartech.lead_irradiation.info"), 300);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            guiGraphics.drawString(font, lines.get(i), 10, 116 + i * 9, 0xFF707070, false);
        }
    }

    private static ItemStack crownsFuelRod() {
        return new ItemStack(ModRegistry.NATURAL_URANIUM_FUEL_ROD.get());
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(LeadIrradiationJeiRecipe recipe) {
        ResourceLocation outputId = BuiltInRegistries.ITEM.getKey(recipe.output().getItem());
        return ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, "lead_irradiation/" + outputId.getPath());
    }
}
