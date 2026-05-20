package cattodream.createnucleartech.explosion;

import cattodream.createnucleartech.Config;
import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.radiation.RadiationEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class HbmNukeExplosionEntity extends Entity {
    private static final long DESTRUCTION_BUDGET_NANOS = 2_500_000L;
    private static final int CHUNKS_PER_TICK = 1;
    private static final double INSTANT_BLAST_RADIUS = 300.0D;
    private static final double INSTANT_KILL_RADIUS = 100.0D;

    private int strength;
    private int speed;
    private int length;
    private long explosionStart;
    private boolean fallout = true;
    private boolean craterWaterDrained;
    private HbmNukeExplosionRayBatched explosion;

    public HbmNukeExplosionEntity(EntityType<? extends HbmNukeExplosionEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static HbmNukeExplosionEntity spawn(ServerLevel level, int strength, double x, double y, double z) {
        if (strength <= 0) {
            strength = 25;
        }
        HbmNukeExplosionEntity entity = new HbmNukeExplosionEntity(ModRegistry.HBM_NUKE_EXPLOSION_ENTITY.get(), level);
        entity.strength = strength * 3;
        entity.speed = (int) Math.ceil(100000.0D / entity.strength);
        entity.length = Math.max(strength * 2, entity.strength / 2);
        entity.setPos(x, y, z);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    public void tick() {
        super.tick();
        if (strength == 0) {
            discard();
            return;
        }

        if (!level().isClientSide && fallout && explosion != null && tickCount < 10 && strength >= 75) {
            radiate(2_500_000.0F / (tickCount * 5.0F + 1.0F), length * 2.0D);
        }
        dealDamage(length * 2.0D);

        if (explosion == null) {
            explosionStart = System.currentTimeMillis();
            level().playSound(null, getX(), getY(), getZ(), ModRegistry.NUCLEAR_EXPLOSION_SOUND.get(), SoundSource.BLOCKS, 10000.0F, 1.0F);
            dealInstantBlastDamage(INSTANT_BLAST_RADIUS);
            if (level() instanceof ServerLevel serverLevel) {
                NuclearCloudEvents.addCloud(serverLevel, position(), length);
                NuclearAftermath.applyBombFalloutTerrain(
                        serverLevel,
                        blockPosition(),
                        Math.max(12, length / 2),
                        100,
                        Math.max(128, Math.min(Config.nuclearBombFalloutRadiusChunks * 16, 256))
                );
            }
            explosion = new HbmNukeExplosionRayBatched(level(), blockPosition().getX(), blockPosition().getY(), blockPosition().getZ(), strength, speed, length);
        }

        if (!explosion.isComplete()) {
            explosion.cacheChunksTick(Math.max(1, Config.nuclearBombMaxPositionsCheckedPerTick / 1000));
            explosion.destructionTick(System.nanoTime() + DESTRUCTION_BUDGET_NANOS, CHUNKS_PER_TICK);
        } else {
            if (!craterWaterDrained && level() instanceof ServerLevel serverLevel) {
                NuclearAftermath.drainCraterWater(serverLevel, blockPosition(), Math.max(length + 32, Config.nuclearBombCraterRadius + 48));
                craterWaterDrained = true;
            }
            discard();
        }
    }

    private void dealInstantBlastDamage(double range) {
        if (level().isClientSide) {
            return;
        }
        AABB box = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(range);
        List<Entity> entities = level().getEntities(null, box);
        for (Entity entity : entities) {
            if (isExplosionExempt(entity)) {
                continue;
            }
            double distanceSq = entity.distanceToSqr(getX(), getY(), getZ());
            if (distanceSq > range * range) {
                continue;
            }

            double distance = Math.max(1.0D, Math.sqrt(distanceSq));
            float damage = distance <= INSTANT_KILL_RADIUS
                    ? Float.MAX_VALUE
                    : (float) (100.0D / (distance / 4.0D));
            entity.hurt(level().damageSources().explosion(null, null), damage);
            double falloff = 1.0D - distance / range;
            entity.setRemainingFireTicks((int) (60 + 180 * falloff));

            Vec3 knockback = new Vec3(entity.getX() - getX(), entity.getY() + entity.getEyeHeight() * 0.5D - getY(), entity.getZ() - getZ());
            if (knockback.lengthSqr() > 1.0E-4D) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(knockback.normalize().scale(1.2D + falloff * 4.5D)));
                entity.hurtMarked = true;
            }
        }
    }

    private void radiate(float rads, double range) {
        if (level().isClientSide) {
            return;
        }
        AABB box = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(range);
        List<LivingEntity> entities = level().getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity entity : entities) {
            if (RadiationEvents.isRadiationImmune(entity)) {
                continue;
            }
            Vec3 vec = new Vec3(entity.getX() - getX(), entity.getEyeY() - getY(), entity.getZ() - getZ());
            double len = Math.max(1.0D, vec.length());
            vec = vec.normalize();
            float resistance = 0.0F;
            for (int i = 1; i < len; i++) {
                BlockPos pos = new BlockPos(
                        (int) Math.floor(getX() + vec.x * i),
                        (int) Math.floor(getY() + vec.y * i),
                        (int) Math.floor(getZ() + vec.z * i)
                );
                resistance += level().getBlockState(pos).getExplosionResistance(level(), pos, null);
            }
            if (resistance < 1.0F) {
                resistance = 1.0F;
            }
            double dose = rads / resistance / (len * len);
            double currentDose = entity.getPersistentData().getDouble(RadiationEvents.RADIATION_LEVEL_KEY);
            entity.getPersistentData().putDouble(RadiationEvents.RADIATION_LEVEL_KEY, currentDose + dose);
            entity.addEffect(new MobEffectInstance(ModRegistry.RADIATION, 400, dose > 400.0D ? 5 : 3));
        }
    }

    private void dealDamage(double range) {
        if (level().isClientSide) {
            return;
        }
        AABB box = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(range);
        List<Entity> entities = level().getEntities(null, box);
        for (Entity entity : entities) {
            if (isExplosionExempt(entity)) {
                continue;
            }
            double distanceSq = entity.distanceToSqr(getX(), getY(), getZ());
            if (distanceSq > range * range) {
                continue;
            }
            double distance = Math.sqrt(distanceSq);
            float damage = (float) (250.0D * (range - distance) / range);
            if (damage <= 0.0F) {
                continue;
            }
            entity.hurt(level().damageSources().explosion(null, null), damage);
            entity.setRemainingFireTicks(100);
            Vec3 knockback = new Vec3(entity.getX() - getX(), entity.getY() + entity.getEyeHeight() - getY(), entity.getZ() - getZ())
                    .normalize()
                    .scale(0.2D);
            entity.setDeltaMovement(entity.getDeltaMovement().add(knockback));
            entity.hurtMarked = true;
        }
    }

    private static boolean isExplosionExempt(Entity entity) {
        return entity instanceof Player player && player.isCreative();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (explosion != null) {
            explosion.cancel();
        }
        super.remove(reason);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        tickCount = tag.getInt("age");
        strength = tag.getInt("strength");
        speed = tag.getInt("speed");
        length = tag.getInt("length");
        fallout = !tag.contains("fallout") || tag.getBoolean("fallout");
        craterWaterDrained = tag.getBoolean("craterWaterDrained");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("age", tickCount);
        tag.putInt("strength", strength);
        tag.putInt("speed", speed);
        tag.putInt("length", length);
        tag.putBoolean("fallout", fallout);
        tag.putBoolean("craterWaterDrained", craterWaterDrained);
    }
}
