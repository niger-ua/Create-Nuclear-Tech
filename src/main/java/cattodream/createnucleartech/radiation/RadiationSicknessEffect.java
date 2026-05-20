package cattodream.createnucleartech.radiation;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class RadiationSicknessEffect extends MobEffect {
    public RadiationSicknessEffect() {
        super(MobEffectCategory.HARMFUL, 0x66D56A);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = Math.max(20, 100 - amplifier * 20);
        return duration % interval == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, Math.min(3, amplifier), false, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 160, Math.min(3, amplifier), false, false, false));

        if (amplifier >= 1) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 180, 0, false, false, false));
        }
        if (amplifier >= 2) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, Math.min(2, amplifier - 2), false, false, false));
            entity.hurt(level.damageSources().magic(), 1.0F + amplifier * 0.5F);
        }
        if (amplifier >= 3) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, false, false));
        }
        if (amplifier >= 4) {
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, Math.min(1, amplifier - 4), false, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, false, false, false));
        }
        return true;
    }
}
