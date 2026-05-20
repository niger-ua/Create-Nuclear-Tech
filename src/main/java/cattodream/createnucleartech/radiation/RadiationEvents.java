package cattodream.createnucleartech.radiation;

import cattodream.createnucleartech.CNTTags;
import cattodream.createnucleartech.Config;
import cattodream.createnucleartech.Createnucleartech;
import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.armor.HazmatArmorItem;
import cattodream.createnucleartech.effects.RadiationEffect;
import cattodream.createnucleartech.explosion.NuclearAftermath;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = Createnucleartech.MODID)
public final class RadiationEvents {
    public static final String RADIATION_LEVEL_KEY = "CreateNuclearTechRadiationLevel";
    public static final String LEGACY_DOSE_KEY = "CreateNuclearTechRadiationDose";
    public static final String ANTIRADIN_GRACE_UNTIL_KEY = "CreateNuclearTechAntiradinGraceUntil";
    public static final String TOXICITY_LEVEL_KEY = "CreateNuclearTechToxicityLevel";
    private static final String LAST_RADIATION_SICKNESS_EVENT_KEY = "CreateNuclearTechLastRadiationSicknessEvent";
    private static final int EXPOSURE_INTERVAL = 40;

    private RadiationEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            NuclearAftermath.tick(serverLevel);
            if (serverLevel.getGameTime() % Math.max(1, Config.radiationTickInterval) == 0) {
                RadiationData.get(serverLevel).simulate(serverLevel);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity tickingEntity = event.getEntity();
        if (tickingEntity.level().isClientSide()) {
            return;
        }
        if (tickingEntity instanceof ItemEntity itemEntity) {
            contaminateFromDroppedItem(itemEntity);
            return;
        }
        if (!(tickingEntity instanceof LivingEntity entity) || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (isRadiationImmune(entity)) {
            clearAccumulatedRadiation(entity);
            clearAccumulatedToxicity(entity);
            clearRadiationSymptoms(entity);
            return;
        }
        if (entity.tickCount % 10 == 0) {
            enforceRadioactiveHandling(entity);
        }
        if (entity.tickCount % EXPOSURE_INTERVAL == 0) {
            updateAccumulatedRadiation(serverLevel, entity);
            updateAccumulatedToxicity(entity);
        }
    }

    private static void contaminateFromDroppedItem(ItemEntity itemEntity) {
        if (itemEntity.tickCount % EXPOSURE_INTERVAL != 0 || !(itemEntity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ItemStack droppedStack = itemEntity.getItem();
        double radiation = RadiationMaterials.radiationFor(droppedStack);
        if (radiation > 0.0D) {
            RadiationData.get(serverLevel).registerSource(serverLevel, itemEntity.blockPosition(), radiation * 0.15D, 4.0D, 0.75D);
        }
        // Activate inactive plutonium core when thrown: spawn screwdriver, convert to active core, and mark the dropped entity so it doesn't repeat
        if (droppedStack.is(ModRegistry.PLUTONIUM_CORE_INACTIVE.asItem())) {
            net.minecraft.nbt.CompoundTag entTag = itemEntity.getPersistentData();
            if (!entTag.getBoolean("cn_activated")) {
                entTag.putBoolean("cn_activated", true);
                try {
                    var screwItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tfmg", "screwdriver"));
                    if (screwItem != null) {
                        itemEntity.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(itemEntity.level(), itemEntity.getX(), itemEntity.getY() + 0.5, itemEntity.getZ(), new ItemStack(screwItem)));
                    }
                } catch (Exception ignored) {
                }
                // replace the inactive core with the active core
                itemEntity.setItem(new ItemStack(ModRegistry.PLUTONIUM_CORE.get()));
            }
        }
    }

    private static void updateAccumulatedRadiation(ServerLevel level, LivingEntity entity) {
        double environmental = RadiationData.get(level).radiationAt(level, entity.blockPosition());
        environmental += nearbyRadioactiveBlocks(level, entity.blockPosition());
        environmental *= shieldingMultiplier(level, entity);

        double carried = carriedRadiation(entity);
        double rawExposure = environmental + carried;

        double protection;
        double effectiveExposure;
        if (RadiationProtection.hasFullEliteSet(entity)) {
            // Elite sealed suit: does not remove accumulated radiation, but prevents new accumulation from weaker sources
            protection = 0.0D;
            if (rawExposure < 2000.0D) {
                effectiveExposure = 0.0D;
            } else {
                effectiveExposure = rawExposure; // full exposure for very strong fields
            }
        } else {
            protection = RadiationProtection.protectionFor(entity);
            effectiveExposure = Math.max(0.0D, rawExposure * (1.0D - protection));
        }

        if (entity.getPersistentData().getLong(ANTIRADIN_GRACE_UNTIL_KEY) > level.getGameTime()) {
            effectiveExposure *= 0.15D;
        }

        double radiationLevel = entity.getPersistentData().getDouble(RADIATION_LEVEL_KEY);
        boolean fullySealed = protection >= 1.0D;
        double decay = fullySealed ? Config.playerRadiationDecayPerUpdate * 8.0D : Config.playerRadiationDecayPerUpdate;
        radiationLevel = Math.max(0.0D, radiationLevel + effectiveExposure - decay);
        entity.getPersistentData().putDouble(RADIATION_LEVEL_KEY, radiationLevel);
        entity.getPersistentData().putDouble(LEGACY_DOSE_KEY, radiationLevel);

        if (fullySealed) {
            clearRadiationSymptoms(entity);
            return;
        }

        if (radiationLevel >= Config.radiationEffectLethalThreshold) {
            applyLethalDose(entity);
            return;
        }

        applyHbmStyleRadiationPressure(entity, radiationLevel, effectiveExposure);

        if (effectiveExposure > Config.radiationEntityDamageThreshold * 0.5D) {
            degradeHazmat(entity);
        }

        int amplifier = radiationAmplifier(radiationLevel);
        if (amplifier >= 0) {
            int duration = EXPOSURE_INTERVAL + 180 + amplifier * 80;
            entity.addEffect(new MobEffectInstance(ModRegistry.RADIATION, duration, amplifier, false, false, true));
            RadiationEffect.applySymptoms(entity, amplifier, false);
        }
    }

    private static int radiationAmplifier(double radiationLevel) {
        if (radiationLevel >= Config.radiationEffectCriticalThreshold) {
            return 5;
        }
        if (radiationLevel >= Config.radiationEffectSevereThreshold) {
            return 4;
        }
        if (radiationLevel >= Config.radiationEffectExtremeThreshold) {
            return 3;
        }
        if (radiationLevel >= Config.radiationEffectHighThreshold) {
            return 2;
        }
        if (radiationLevel >= Config.radiationEffectMediumThreshold) {
            return 1;
        }
        if (radiationLevel >= Config.radiationEffectLowThreshold) {
            return 0;
        }
        return -1;
    }

    private static void applyHbmStyleRadiationPressure(LivingEntity entity, double radiationLevel, double exposure) {
        if (radiationLevel >= Config.radiationEffectCriticalThreshold) {
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 180, 1, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0, false, false, true));
            triggerRadiationSicknessEvent(entity, true);
            entity.hurt(entity.damageSources().magic(), 2.0F);
        } else if (radiationLevel >= Config.radiationEffectSevereThreshold) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 180, 1, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 220, 1, false, false, true));
            triggerRadiationSicknessEvent(entity, true);
        } else if (radiationLevel >= Config.radiationEffectExtremeThreshold) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 0, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 180, 0, false, false, true));
            triggerRadiationSicknessEvent(entity, false);
        } else if (radiationLevel >= Config.radiationEffectMediumThreshold) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false, true));
            triggerRadiationSicknessEvent(entity, false);
        }
        if (exposure >= Config.radiationEntityDamageThreshold * 1.5D) {
            entity.hurt(entity.damageSources().magic(), (float) Math.min(4.0D, exposure / Math.max(1.0D, Config.radiationEntityDamageThreshold)));
        }
    }

    private static void triggerRadiationSicknessEvent(LivingEntity entity, boolean blood) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        long gameTime = level.getGameTime();
        long cooldown = blood ? 90L : 150L;
        long last = entity.getPersistentData().getLong(LAST_RADIATION_SICKNESS_EVENT_KEY);
        if (gameTime - last < cooldown) {
            return;
        }
        entity.getPersistentData().putLong(LAST_RADIATION_SICKNESS_EVENT_KEY, gameTime);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModRegistry.VOMIT_SOUND.get(), SoundSource.PLAYERS, blood ? 0.9F : 0.7F, blood ? 0.75F : 1.0F);
        ItemStack particleStack = blood ? new ItemStack(Items.RED_DYE) : new ItemStack(Items.SLIME_BALL);
        double lookX = entity.getLookAngle().x;
        double lookZ = entity.getLookAngle().z;
        level.sendParticles(
                new ItemParticleOption(ParticleTypes.ITEM, particleStack),
                entity.getX() + lookX * 0.45D,
                entity.getEyeY() - 0.15D,
                entity.getZ() + lookZ * 0.45D,
                blood ? 36 : 24,
                0.18D,
                0.08D,
                0.18D,
                blood ? 0.18D : 0.12D
        );
        if (blood) {
            entity.hurt(entity.damageSources().magic(), 1.5F);
        } else {
            entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 220, 1, false, false, true));
        }
    }

    public static double reduceAccumulatedRadiation(LivingEntity entity, double amount) {
        double current = storedRadiation(entity);
        double reduced = Math.max(0.0D, current - Math.max(0.0D, amount));
        entity.getPersistentData().putDouble(RADIATION_LEVEL_KEY, reduced);
        entity.getPersistentData().putDouble(LEGACY_DOSE_KEY, reduced);
        if (reduced < Config.radiationEffectLowThreshold) {
            clearRadiationSymptoms(entity);
        } else {
            int amplifier = radiationAmplifier(reduced);
            entity.addEffect(new MobEffectInstance(ModRegistry.RADIATION, EXPOSURE_INTERVAL + 180 + amplifier * 80, amplifier, false, false, true));
        }
        return current - reduced;
    }

    public static double applyAntiradin(LivingEntity entity, double amount, int graceTicks) {
        double current = storedRadiation(entity);
        double clearVisibleDose = Math.max(0.0D, current - Math.max(0.0D, Config.radiationEffectLowThreshold - 1.0D));
        double removed = reduceAccumulatedRadiation(entity, Math.max(amount, clearVisibleDose));
        clearRadiationSymptoms(entity);
        if (!entity.level().isClientSide()) {
            long until = entity.level().getGameTime() + Math.max(EXPOSURE_INTERVAL * 2L, graceTicks);
            entity.getPersistentData().putLong(ANTIRADIN_GRACE_UNTIL_KEY, until);
        }
        return removed;
    }

    private static double storedRadiation(LivingEntity entity) {
        return Math.max(
                entity.getPersistentData().getDouble(RADIATION_LEVEL_KEY),
                entity.getPersistentData().getDouble(LEGACY_DOSE_KEY)
        );
    }

    public static void clearAccumulatedRadiation(LivingEntity entity) {
        entity.getPersistentData().putDouble(RADIATION_LEVEL_KEY, 0.0D);
        entity.getPersistentData().putDouble(LEGACY_DOSE_KEY, 0.0D);
        entity.getPersistentData().putLong(ANTIRADIN_GRACE_UNTIL_KEY, 0L);
    }

    public static void clearAccumulatedToxicity(LivingEntity entity) {
        entity.getPersistentData().putDouble(TOXICITY_LEVEL_KEY, 0.0D);
    }

    public static boolean isRadiationImmune(LivingEntity entity) {
        return entity instanceof Player player
                && (player.isCreative() || player.isSpectator() || player.getAbilities().invulnerable);
    }

    public static void clearRadiationSymptoms(LivingEntity entity) {
        removeEffect(entity, ModRegistry.RADIATION);
        removeEffect(entity, ModRegistry.RADIATION_SICKNESS);
        removeEffect(entity, MobEffects.WEAKNESS);
        removeEffect(entity, MobEffects.HUNGER);
        removeEffect(entity, MobEffects.MOVEMENT_SLOWDOWN);
        removeEffect(entity, MobEffects.CONFUSION);
        removeEffect(entity, MobEffects.POISON);
        removeEffect(entity, MobEffects.WITHER);
        removeEffect(entity, MobEffects.DIG_SLOWDOWN);
        removeEffect(entity, MobEffects.BLINDNESS);
    }

    private static void removeEffect(LivingEntity entity, net.minecraft.core.Holder<MobEffect> effect) {
        if (entity.hasEffect(effect)) {
            entity.removeEffect(effect);
        }
    }

    private static void applyLethalDose(LivingEntity entity) {
        if (isRadiationImmune(entity)) {
            return;
        }
        entity.hurt(entity.damageSources().magic(), 10000.0F);
        if (entity.isAlive()) {
            entity.setHealth(0.0F);
        }
    }

    private static double carriedRadiation(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return 0.0D;
        }
        double total = 0.0D;
        for (ItemStack stack : player.getInventory().items) {
            total += RadiationMaterials.radiationFor(stack);
        }
        for (ItemStack stack : player.getInventory().armor) {
            total += RadiationMaterials.radiationFor(stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            total += RadiationMaterials.radiationFor(stack);
        }
        return total;
    }

    private static void updateAccumulatedToxicity(LivingEntity entity) {
        double exposure = carriedToxicity(entity);
        if (exposure > 0.0D) {
            double protection = RadiationProtection.protectionFor(entity);
            exposure *= Math.max(0.15D, 1.0D - protection * 0.65D);
        }

        double toxicity = entity.getPersistentData().getDouble(TOXICITY_LEVEL_KEY);
        toxicity = Math.max(0.0D, toxicity + exposure - (exposure > 0.0D ? 0.03D : 0.45D));
        entity.getPersistentData().putDouble(TOXICITY_LEVEL_KEY, toxicity);

        if (toxicity >= 220.0D) {
            entity.hurt(entity.damageSources().magic(), 10000.0F);
        } else if (toxicity >= 180.0D) {
            entity.hurt(entity.damageSources().magic(), 3.0F);
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 260, 2, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 240, 0, false, false, true));
        } else if (toxicity >= 90.0D) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 220, 1, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 180, 0, false, false, true));
        } else if (toxicity >= 35.0D) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0, false, false, true));
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false, true));
        }
    }

    private static double carriedToxicity(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return 0.0D;
        }
        double total = 0.0D;
        for (ItemStack stack : player.getInventory().items) {
            total += toxicityFor(stack);
        }
        for (ItemStack stack : player.getInventory().armor) {
            total += toxicityFor(stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            total += toxicityFor(stack);
        }
        return total;
    }

    private static double toxicityFor(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(CNTTags.Items.TOXIC_LEAD)) {
            return 0.0D;
        }
        return stack.getCount() * 0.12D;
    }

    private static void enforceRadioactiveHandling(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return;
        }
        if (isRadiationImmune(player)) {
            return;
        }
        boolean mainUnsafe = RadiationMaterials.perItemRadiationFor(player.getMainHandItem()) > 0.0D;
        boolean offhandUnsafe = RadiationMaterials.perItemRadiationFor(player.getOffhandItem()) > 0.0D;
        if (!mainUnsafe && !offhandUnsafe) {
            return;
        }
        if (RadiationProtection.protectionFor(player) >= Config.hazmatHandlingProtection) {
            return;
        }
        if (RadiationProtection.hasHandlingTool(player)) {
            RadiationProtection.damageHandlingTool(player);
            return;
        }
        dropUnsafeHandStack(player, EquipmentSlot.MAINHAND);
        dropUnsafeHandStack(player, EquipmentSlot.OFFHAND);
    }

    private static void dropUnsafeHandStack(Player player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        double perItemRadiation = RadiationMaterials.perItemRadiationFor(stack);
        if (perItemRadiation <= 0.0D) {
            return;
        }

        ItemStack dropped = stack.copy();
        player.setItemSlot(slot, ItemStack.EMPTY);
        player.drop(dropped, false);
        player.hurt(player.damageSources().magic(), (float) Math.min(7.0D, 1.0D + perItemRadiation));
        player.addEffect(new MobEffectInstance(ModRegistry.RADIATION, 260, Math.min(2, Math.max(0, (int) perItemRadiation)), false, false, true));
        player.displayClientMessage(Component.translatable("message.createnucleartech.radioactive_hand").withStyle(ChatFormatting.RED), true);
    }

    private static double nearbyRadioactiveBlocks(ServerLevel level, BlockPos center) {
        double total = 0.0D;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = -2; y <= 2; y++) {
            for (int z = -4; z <= 4; z++) {
                for (int x = -4; x <= 4; x++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState state = level.getBlockState(cursor);
                    FluidState fluidState = state.getFluidState();
                    double source = RadiationMaterials.radiationForBlock(state) + RadiationMaterials.radiationForFluid(fluidState);
                    if (source > 0.0D) {
                        double distanceSqr = Math.max(1.0D, x * x + y * y + z * z);
                        double transmission = ContainmentScanner.lineTransmission(level, cursor.immutable(), center, source);
                        total += source * transmission / distanceSqr;
                    }
                }
            }
        }
        return total;
    }

    private static double shieldingMultiplier(ServerLevel level, LivingEntity entity) {
        int shieldingBlocks = 0;
        BlockPos center = entity.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = -2; y <= 2; y++) {
            for (int z = -2; z <= 2; z++) {
                for (int x = -2; x <= 2; x++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (level.getBlockState(cursor).is(CNTTags.Blocks.RADIATION_SHIELDING)) {
                        shieldingBlocks++;
                    }
                }
            }
        }
        return Math.max(0.25D, 1.0D - shieldingBlocks * 0.04D);
    }

    private static void degradeHazmat(LivingEntity entity) {
        if (!Config.hazmatSuitsDegrade) {
            return;
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.getItem() instanceof HazmatArmorItem) {
                stack.hurtAndBreak(1, entity, slot);
            }
        }
    }
}
