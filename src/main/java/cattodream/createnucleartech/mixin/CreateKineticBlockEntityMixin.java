package cattodream.createnucleartech.mixin;

import cattodream.createnucleartech.integration.create.CreateKineticTiers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.simibubi.create.content.kinetics.base.KineticBlockEntity", remap = false)
public class CreateKineticBlockEntityMixin {
    @Shadow
    protected float stress;

    @Shadow
    protected float speed;

    @Inject(method = "tick", at = @At("TAIL"))
    private void createnucleartech$breakWeakWoodenCogwheels(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        CreateKineticTiers.breakCogwheelIfOverloaded(self.getBlockState(), self.getLevel(), self.getBlockPos(), speed, stress);
    }

    @Inject(method = "calculateStressApplied", at = @At("RETURN"), cancellable = true)
    private void createnucleartech$addSteelGearStress(CallbackInfoReturnable<Float> cir) {
        BlockState state = ((BlockEntity) (Object) this).getBlockState();
        float extraStress = CreateKineticTiers.additionalStressImpact(state);
        if (extraStress > 0.0F) {
            cir.setReturnValue(cir.getReturnValueF() + extraStress);
        }
    }
}
