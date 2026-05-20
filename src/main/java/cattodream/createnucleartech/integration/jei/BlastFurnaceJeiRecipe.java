package cattodream.createnucleartech.integration.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public record BlastFurnaceJeiRecipe(Ingredient upperInput, Ingredient lowerInput, Ingredient fuel, ItemStack output) {
}
