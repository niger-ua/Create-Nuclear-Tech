package cattodream.createnucleartech.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Visible player-facing radiation stage. Accumulation is stored separately on
 * the entity; this effect only turns that stored dose into symptoms over time.
 */
public class RadiationEffect extends MobEffect {
    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0x58D36B);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = Math.max(10, 120 - amplifier * 15);
        return duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        applySymptoms(entity, amplifier, true);
        return true;
    }

    public static void applySymptoms(LivingEntity entity, int amplifier, boolean damage) {
        Level level = entity.level();
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 180, Math.min(2, amplifier), false, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 180, Math.min(2, amplifier), false, false, false));

        if (amplifier >= 1) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, Math.min(1, amplifier - 1), false, false, false));
        }
        if (amplifier >= 2) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 180, 0, false, false, false));
            if (damage) {
                entity.hurt(level.damageSources().magic(), 1.0F + amplifier * 0.35F);
            }
        }
        if (amplifier >= 3) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, false, false));
        }
        if (amplifier >= 4) {
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0, false, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 0, false, false, false));
            if (damage) {
                entity.hurt(level.damageSources().magic(), 2.0F + amplifier * 0.6F);
            }
        }
        if (amplifier >= 5) {
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 2, false, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1, false, false, false));
        }
        if (amplifier >= 6 && damage) {
            entity.hurt(level.damageSources().magic(), 8.0F + amplifier);
        }
    }
}
