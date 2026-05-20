package cattodream.createnucleartech.mixin;

import cattodream.createnucleartech.ModRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity", remap = false)
public abstract class MechanicalMixerBlockEntityMixin {
    @Unique
    private static final float CREATENUCLEARTECH_HIGH_SPEED_MIXING_RPM = 1024.0F;
    @Unique
    private static final float CREATENUCLEARTECH_NORMAL_MIXER_MAX_RPM = 256.0F;

    @Inject(method = "getMatchingRecipes", at = @At("RETURN"), cancellable = true)
    private void createnucleartech$requireHighSpeedForAlloyMixing(CallbackInfoReturnable<List<Recipe<?>>> cir) {
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) {
            return;
        }

        boolean highSpeedMixer = self.getBlockState().is(ModRegistry.HIGH_SPEED_MIXER.get());
        float speed = Math.abs(createnucleartech$getSpeed());

        if (!highSpeedMixer && speed > CREATENUCLEARTECH_NORMAL_MIXER_MAX_RPM) {
            cir.setReturnValue(List.of());
            return;
        }

        boolean validHighSpeedMixer = highSpeedMixer && speed >= CREATENUCLEARTECH_HIGH_SPEED_MIXING_RPM;
        if (validHighSpeedMixer) {
            return;
        }

        List<Recipe<?>> filtered = new ArrayList<>(cir.getReturnValue());
        filtered.removeIf(recipe -> createnucleartech$isHighSpeedAlloyRecipe(level, recipe));
        cir.setReturnValue(filtered);
    }

    @Unique
    private float createnucleartech$getSpeed() {
        try {
            Object value = getClass().getMethod("getSpeed").invoke(this);
            return value instanceof Number number ? number.floatValue() : 0.0F;
        } catch (ReflectiveOperationException exception) {
            return 0.0F;
        }
    }

    @Unique
    private static boolean createnucleartech$isHighSpeedAlloyRecipe(Level level, Recipe<?> recipe) {
        ItemStack result = recipe.getResultItem(level.registryAccess());
        return result.is(ModRegistry.RED_COPPER_INGOT.get()) || result.is(ModRegistry.ADVANCED_ALLOY_INGOT.get());
    }
}
