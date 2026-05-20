package cattodream.createnucleartech.explosion;

import cattodream.createnucleartech.Config;
import cattodream.createnucleartech.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class NuclearBombEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(NuclearBombEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ARMED = SynchedEntityData.defineId(NuclearBombEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_FACING_ID = SynchedEntityData.defineId(NuclearBombEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_WILL_DETONATE = SynchedEntityData.defineId(NuclearBombEntity.class, EntityDataSerializers.BOOLEAN);
    private double dudChance;
    private float fallTilt;
    private float fallTiltO;

    public NuclearBombEntity(EntityType<? extends NuclearBombEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public NuclearBombEntity(Level level, double x, double y, double z, @Nullable LivingEntity igniter) {
        this(ModRegistry.NUCLEAR_BOMB_ENTITY.get(), level);
        setPos(x, y, z);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        setFuse(Config.nuclearBombFuseTicks);
    }

    public static void spawnPrimed(ServerLevel level, BlockPos pos, @Nullable LivingEntity igniter) {
        spawnPrimed(level, pos, Direction.EAST, igniter);
    }

    public static void spawnPrimed(ServerLevel level, BlockPos pos, Direction facing, @Nullable LivingEntity igniter) {
        spawnPrimed(level, pos, facing, Config.nuclearBombFuseTicks, 0.0D, igniter);
    }

    public static void spawnPrimed(ServerLevel level, BlockPos pos, Direction facing, int fuseTicks, double dudChance, @Nullable LivingEntity igniter) {
        NuclearBombEntity bomb = new NuclearBombEntity(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, igniter);
        bomb.setFacing(facing);
        bomb.setFuse(fuseTicks);
        bomb.dudChance = Math.max(0.0D, Math.min(1.0D, dudChance));
        bomb.setWillDetonate(level.random.nextDouble() >= bomb.dudChance);
        level.removeBlock(pos, false);
        level.addFreshEntity(bomb);
        bomb.arm();
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FUSE_ID, Config.nuclearBombFuseTicks);
        builder.define(DATA_ARMED, false);
        builder.define(DATA_FACING_ID, Direction.EAST.get2DDataValue());
        builder.define(DATA_WILL_DETONATE, false);
    }

    public void arm() {
        if (isArmed()) {
            return;
        }
        entityData.set(DATA_ARMED, true);
        if (!level().isClientSide) {
            level().playSound(null, getX(), getY(), getZ(), ModRegistry.BOMB_FUSE_SOUND.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
        }
    }

    public boolean isArmed() {
        return entityData.get(DATA_ARMED);
    }

    public int getFuse() {
        return entityData.get(DATA_FUSE_ID);
    }

    public void setFuse(int fuse) {
        entityData.set(DATA_FUSE_ID, fuse);
    }

    public Direction getFacing() {
        return Direction.from2DDataValue(entityData.get(DATA_FACING_ID));
    }

    public void setFacing(Direction facing) {
        Direction horizontal = facing.getAxis().isHorizontal() ? facing : Direction.EAST;
        entityData.set(DATA_FACING_ID, horizontal.get2DDataValue());
        setYRot(horizontal.toYRot());
        yRotO = getYRot();
    }

    public boolean willDetonate() {
        return entityData.get(DATA_WILL_DETONATE);
    }

    private void setWillDetonate(boolean willDetonate) {
        entityData.set(DATA_WILL_DETONATE, willDetonate);
    }

    @Override
    public void tick() {
        fallTiltO = fallTilt;
        if (!isNoGravity()) {
            setDeltaMovement(getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }

        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.98D));
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
        }
        updateFallTilt();

        if (isArmed()) {
            int fuse = getFuse();
            setFuse(fuse - 1);
            if (fuse <= 0) {
                discard();
                if (!level().isClientSide) {
                    if (level() instanceof ServerLevel serverLevel) {
                        if (willDetonate()) {
                            NuclearExplosion.detonate(serverLevel, blockPosition());
                        } else {
                            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.2F, 0.65F);
                        }
                    }
                }
            } else {
                tickFuseEffects(fuse);
            }
        }

        super.tick();
    }

    private void updateFallTilt() {
        double fallingSpeed = Math.max(0.0D, -getDeltaMovement().y);
        float target = onGround() ? 0.0F : Math.min(75.0F, (float) (fallingSpeed * 120.0D));
        float response = onGround() ? 0.08F : 0.35F;
        fallTilt += (target - fallTilt) * response;
    }

    public float getFallTilt(float partialTick) {
        return fallTiltO + (fallTilt - fallTiltO) * partialTick;
    }

    private void tickFuseEffects(int fuse) {
        if (!level().isClientSide) {
            int interval;
            float pitch;
            if (fuse < 40) {
                interval = 2;
                pitch = 2.0F;
            } else if (fuse < 80) {
                interval = 5;
                pitch = 1.8F;
            } else if (fuse < 140) {
                interval = 10;
                pitch = 1.5F;
            } else {
                interval = 20;
                pitch = 1.2F;
            }
            if (fuse % interval == 0) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.NOTE_BLOCK_BIT, SoundSource.BLOCKS, 6.0F, pitch);
            }
            return;
        }

        if (random.nextInt(3) == 0) {
            level().addParticle(
                    ParticleTypes.SMOKE,
                    getX() + random.nextGaussian() * 0.1D,
                    getY() + 0.8D,
                    getZ() + random.nextGaussian() * 0.1D,
                    0.0D,
                    0.05D,
                    0.0D
            );
        }
        if (fuse < 60 && random.nextInt(2) == 0) {
            level().addParticle(
                    ParticleTypes.FLAME,
                    getX() + random.nextGaussian() * 0.3D,
                    getY() + 0.5D + random.nextDouble() * 0.5D,
                    getZ() + random.nextGaussian() * 0.3D,
                    random.nextGaussian() * 0.02D,
                    0.05D,
                    random.nextGaussian() * 0.02D
            );
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        setFuse(compound.contains("Fuse") ? compound.getInt("Fuse") : Config.nuclearBombFuseTicks);
        entityData.set(DATA_ARMED, compound.getBoolean("Armed"));
        setFacing(compound.contains("Facing") ? Direction.from2DDataValue(compound.getInt("Facing")) : Direction.EAST);
        dudChance = compound.getDouble("DudChance");
        setWillDetonate(compound.getBoolean("WillDetonate"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("Fuse", getFuse());
        compound.putBoolean("Armed", isArmed());
        compound.putInt("Facing", getFacing().get2DDataValue());
        compound.putDouble("DudChance", dudChance);
        compound.putBoolean("WillDetonate", willDetonate());
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04D;
    }
}
