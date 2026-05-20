package cattodream.createnucleartech.integration.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public record HighSpeedAlloyingJeiRecipe(
        Ingredient firstInput,
        Ingredient secondInput,
        ItemStack output,
        int rpm,
        int processingTicks
) {
}
