package cattodream.createnucleartech.mixin;

import cattodream.createnucleartech.integration.create.CreateKineticTiers;
import net.createmod.catnip.config.ConfigBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.simibubi.create.content.kinetics.RotationPropagator", remap = false)
public class RotationPropagatorMixin {
    @Redirect(
            method = "propagateNewSource",
            at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/config/ConfigBase$ConfigInt;get()Ljava/lang/Object;")
    )
    private static Object createnucleartech$raiseCreateSpeedLimit(ConfigBase.ConfigInt config) {
        Object original = config.get();
        if (original instanceof Integer value) {
            return Math.max(value, CreateKineticTiers.maxRotationSpeed());
        }
        return original;
    }
}
