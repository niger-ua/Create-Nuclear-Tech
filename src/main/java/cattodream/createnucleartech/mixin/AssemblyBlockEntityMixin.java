package cattodream.createnucleartech.mixin;

import cattodream.createnucleartech.integration.crowns.CrownsNeutronDiagnostics;
import cattodream.createnucleartech.integration.crowns.CrownsFuelProfile;
import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Couple;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;

@Mixin(value = AssemblyBlockEntity.class, remap = false)
public abstract class AssemblyBlockEntityMixin {
    @Shadow
    public float temperature;

    @Shadow
    public float nbrOfFission;

    @Shadow
    float power;

    @Shadow
    public LerpedFloat additionalNeutronsAbsorbed;

    @Shadow
    public HashMap<ResourceLocation, Double> radioactiveElements;

    @Shadow
    double fastAbsorptionChance;

    @Shadow
    double slowAbsorptionChance;

    @Shadow
    public abstract float getEffectiveK();

    @Unique
    private String createnucleartech$fuelProfileOverride = "";

    @Unique
    private double createnucleartech$thorium232 = 0.0D;

    @Unique
    private double createnucleartech$plutonium240 = 0.0D;

    @Inject(method = "setComposition", at = @At("HEAD"))
    private void createnucleartech$readFuelProfile(CompoundTag composition, CallbackInfo ci) {
        createnucleartech$fuelProfileOverride = "";
        createnucleartech$thorium232 = 0.0D;
        createnucleartech$plutonium240 = 0.0D;
        if (composition.contains(CrownsFuelProfile.PROFILE_KEY)) {
            createnucleartech$fuelProfileOverride = composition.getString(CrownsFuelProfile.PROFILE_KEY);
        }
        createnucleartech$thorium232 = composition.getDouble(CrownsFuelProfile.TH232_KEY);
        createnucleartech$plutonium240 = composition.getDouble(CrownsFuelProfile.PU240_KEY);
    }

    @Inject(method = "setComposition", at = @At("RETURN"))
    private void createnucleartech$clearEmptyComposition(CompoundTag composition, CallbackInfo ci) {
        if (composition.isEmpty()) {
            radioactiveElements.clear();
        }
    }

    @Inject(method = "saveComposition", at = @At("RETURN"), cancellable = true)
    private void createnucleartech$saveFuelProfile(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        if (createnucleartech$fuelProfileOverride != null && !createnucleartech$fuelProfileOverride.isBlank()) {
            tag.putString(CrownsFuelProfile.PROFILE_KEY, createnucleartech$fuelProfileOverride);
        }
        if (createnucleartech$thorium232 > 0.0D) {
            tag.putDouble(CrownsFuelProfile.TH232_KEY, createnucleartech$thorium232);
        }
        if (createnucleartech$plutonium240 > 0.0D) {
            tag.putDouble(CrownsFuelProfile.PU240_KEY, createnucleartech$plutonium240);
        }
        cir.setReturnValue(tag);
    }

    @Inject(method = "getRadioactiveActivity", at = @At("RETURN"), cancellable = true)
    private void createnucleartech$boostActivityWithReflectors(CallbackInfoReturnable<Float> cir) {
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) {
            return;
        }

        CrownsFuelProfile profile = createnucleartech$fuelProfile();
        if (profile == CrownsFuelProfile.INERT || radioactiveElements.isEmpty() && createnucleartech$thorium232 <= 0.0D && createnucleartech$plutonium240 <= 0.0D) {
            cir.setReturnValue(0.0F);
            return;
        }
        float startup = profile.startupMultiplier(level, self.getBlockPos());
        float multiplier = CrownsNeutronDiagnostics.reflectorMultiplier(level, self.getBlockPos());
        float baseActivity = cir.getReturnValueF();
        if (baseActivity <= 0.0F && createnucleartech$thorium232 > 0.0D) {
            baseActivity = (float) (18.0D * createnucleartech$thorium232);
        }
        cir.setReturnValue(baseActivity * profile.activityMultiplier() * startup * multiplier);
    }

    @Inject(method = "absorbNeutrons", at = @At("RETURN"), cancellable = true)
    private void createnucleartech$addMediumNeutronBand(Couple<Float> incoming, CallbackInfoReturnable<Couple<Float>> cir) {
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) {
            return;
        }

        CrownsFuelProfile profile = createnucleartech$fuelProfile();
        boolean started = profile.isStarted(level, self.getBlockPos());
        float inputFast = incoming.getFirst();
        float inputSlow = incoming.getSecond();
        float mediumFactor = CrownsNeutronDiagnostics.mediumFactor(level, self.getBlockPos());
        float absorberDamping = CrownsNeutronDiagnostics.absorberDamping(level, self.getBlockPos());
        float reflectorFeedback = CrownsNeutronDiagnostics.reflectedNeutronFeedback(level, self.getBlockPos());
        float fast = inputFast * profile.fastYield() * absorberDamping;
        float medium = inputFast * mediumFactor * profile.mediumYield() * absorberDamping;
        float slow = inputSlow * profile.slowYield() * absorberDamping;
        if (started && medium > 0.0001F) {
            float temperatureFeedback = 1.0F / Math.max(1.0F, (temperature - 200.0F) * 0.0075F);
            double customFertileBonus = createnucleartech$thorium232 * 0.24D + createnucleartech$plutonium240 * 0.10D;
            float mediumAbsorption = (float) (medium * (Math.sqrt(Math.max(0.0D, fastAbsorptionChance * slowAbsorptionChance)) + customFertileBonus) * temperatureFeedback * 0.35D);
            float reflectedAbsorption = (fast * 0.018F + medium * 0.070F + slow * 0.035F) * reflectorFeedback * temperatureFeedback;
            mediumAbsorption += reflectedAbsorption;
            if (Float.isFinite(mediumAbsorption) && mediumAbsorption > 0.0F) {
                additionalNeutronsAbsorbed.chaseTimed(additionalNeutronsAbsorbed.getChaseTarget() + mediumAbsorption, 5);
            }
        } else if (!started && !profile.selfStarting()) {
            additionalNeutronsAbsorbed.chaseTimed(additionalNeutronsAbsorbed.getChaseTarget() * 0.20F, 10);
        }

        float slowedMedium = medium * 0.45F;
        CrownsNeutronDiagnostics.record(
                level,
                self.getBlockPos(),
                fast,
                medium,
                slow + slowedMedium,
                getEffectiveK(),
                temperature,
                nbrOfFission,
                profile.id(),
                profile.displayName(),
                started
        );
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void createnucleartech$makeCustomFuelHotterAndLessStable(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        CrownsFuelProfile profile = createnucleartech$fuelProfile();
        if (profile == CrownsFuelProfile.INERT) {
            return;
        }

        float startup = profile.startupMultiplier(level, self.getBlockPos());
        if (startup <= 0.01F) {
            return;
        }

        float effectiveK = getEffectiveK();
        float supercritical = Math.max(0.0F, effectiveK - 1.0F);
        float fission = Math.max(0.0F, nbrOfFission);
        float fissionHeat = Math.min(4.0F, fission * 0.000020F);
        float instabilityHeat = supercritical * profile.activityMultiplier() * 0.32F;
        float heatBonus = (fissionHeat + instabilityHeat) * startup;

        if (Float.isFinite(heatBonus) && heatBonus > 0.0F) {
            temperature += heatBonus;
            power += heatBonus * 2400.0F;
        }

        if (supercritical > 0.08F) {
            additionalNeutronsAbsorbed.chaseTimed(
                    additionalNeutronsAbsorbed.getChaseTarget() + supercritical * profile.activityMultiplier() * 0.55F,
                    4
            );
        }

        if (!Float.isFinite(temperature)) {
            temperature = 300.0F;
        }
        if (!Float.isFinite(nbrOfFission)) {
            nbrOfFission = 0.0F;
        }
    }

    @Unique
    private CrownsFuelProfile createnucleartech$fuelProfile() {
        return CrownsFuelProfile.from(
                radioactiveElements,
                createnucleartech$fuelProfileOverride,
                createnucleartech$thorium232,
                createnucleartech$plutonium240
        );
    }
}
