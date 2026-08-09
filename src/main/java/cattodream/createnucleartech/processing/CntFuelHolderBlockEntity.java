package cattodream.createnucleartech.processing;

import cattodream.createnucleartech.Config;
import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.integration.crowns.CrownsFuelProfile;
import cattodream.createnucleartech.radiation.RadiationData;
import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CntFuelHolderBlockEntity extends AssemblyBlockEntity {
    public static final int RODS_PER_HOLDER = 1;
    private static final float BURNUP_LIMIT = 1.0F;
    private static final float BURNUP_RATE_SCALE = 0.0001F;

    private CntFuelType fuelType = CntFuelType.EMPTY;
    private float burnup;
    private String spentFrom = "";

    public CntFuelHolderBlockEntity(BlockPos pos, BlockState state) {
        this(ModRegistry.FUEL_HOLDER_ENTITY.get(), pos, state);
    }

    public CntFuelHolderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        forceEmptyComposition();
    }

    @Override
    public void tick() {
        if (fuelType == CntFuelType.EMPTY) {
            forceEmptyComposition();
            return;
        }

        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        emitFuelRadiation();
        if (!fuelType.isFreshFuel()) {
            return;
        }

        float heat = Math.max(0.0F, temperature - 300.0F);
        float fissionLoad = Math.max(0.0F, nbrOfFission);
        float activeBurn = fissionLoad * 0.000000025F + heat * 0.000000000035F;
        float passiveBurn = fissionLoad > 0.0F || heat > 50.0F ? 0.000000004F : 0.00000000025F;
        burnup += Math.max(passiveBurn, activeBurn) * BURNUP_RATE_SCALE;
        if (burnup >= BURNUP_LIMIT) {
            spentFrom = fuelType.getSerializedName();
            fuelType = CntFuelType.SPENT;
            burnup = BURNUP_LIMIT;
            setComposition(fuelType.composition());
            BlockState state = getBlockState();
            if (state.hasProperty(CntFuelHolderBlock.FUEL)) {
                level.setBlock(worldPosition, state.setValue(CntFuelHolderBlock.FUEL, CntFuelType.SPENT), 3);
            }
            setChanged();
            sendData();
        }
    }

    private void emitFuelRadiation() {
        if (fuelType == CntFuelType.EMPTY || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int interval = Math.max(1, Config.radiationTickInterval);
        if ((serverLevel.getGameTime() + worldPosition.asLong()) % interval != 0) {
            return;
        }

        double strength = fuelRadiationStrength();
        if (strength <= 0.0D) {
            return;
        }
        double radius = fuelType == CntFuelType.SPENT ? 10.0D : 7.0D + Math.min(7.0D, strength * 0.12D);
        RadiationData.get(serverLevel).registerSource(serverLevel, worldPosition, strength, radius, 1.0D + burnup);
    }

    private double fuelRadiationStrength() {
        if (fuelType == CntFuelType.SPENT) {
            return Config.nuclearWasteRadiation * Config.radioactiveItemStrength * (3.0D + burnup * 2.5D);
        }

        CrownsFuelProfile profile = fuelType.profile();
        double heat = Math.max(0.0D, temperature - 300.0D);
        double fission = Math.max(0.0D, nbrOfFission);
        double base = Config.uraniumDustRadiation * Config.radioactiveItemStrength * (1.75D + profile.activityMultiplier() * 2.75D);
        double active = fission * Config.crownsActivityLeakScale * 0.45D;
        double hot = heat > Config.crownsHotTemperatureThreshold
                ? (heat - Config.crownsHotTemperatureThreshold) * Config.crownsHeatLeakScale * 0.006D
                : 0.0D;
        return base * (1.0D + burnup * 1.65D) + active + hot;
    }

    public CntFuelType fuelType() {
        return fuelType;
    }

    public float burnup() {
        return burnup;
    }

    public String spentFrom() {
        return spentFrom;
    }

    public void loadFuel(CntFuelType type) {
        loadFuel(type, 0.0F);
    }

    public void loadFuel(CntFuelType type, float initialBurnup) {
        fuelType = type;
        burnup = Mth.clamp(initialBurnup, 0.0F, BURNUP_LIMIT - 0.000001F);
        spentFrom = "";
        setComposition(type.composition());
        updateBlockFuel(type);
        forceReactorRecheck();
    }

    public void clearFuel() {
        forceEmptyComposition();
        updateBlockFuel(CntFuelType.EMPTY);
    }

    public void restoreFuel(CntFuelType type, float restoredBurnup, CompoundTag spentData) {
        fuelType = type;
        burnup = Mth.clamp(restoredBurnup, 0.0F, BURNUP_LIMIT);
        spentFrom = "";
        if (type == CntFuelType.SPENT && spentData != null) {
            spentFrom = spentData.getString("spent_from");
            burnup = Mth.clamp(spentData.getFloat("burnup"), 0.0F, BURNUP_LIMIT);
        }
        setComposition(type.composition());
        updateBlockFuel(type);
        forceReactorRecheck();
    }

    public void forceReactorRecheck() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (fuelType == CntFuelType.EMPTY) {
            forceEmptyComposition();
            return;
        }

        setComposition(fuelType.composition());
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        sendData();
    }

    public CompoundTag spentRodData() {
        CompoundTag tag = new CompoundTag();
        if (!spentFrom.isBlank()) {
            tag.putString("spent_from", spentFrom);
        }
        tag.putFloat("burnup", burnup);
        return tag;
    }

    private void updateBlockFuel(CntFuelType type) {
        if (level != null) {
            BlockState state = getBlockState();
            if (state.hasProperty(CntFuelHolderBlock.FUEL)) {
                level.setBlock(worldPosition, state.setValue(CntFuelHolderBlock.FUEL, type), 3);
            }
        }
        setChanged();
        sendData();
    }

    private void forceEmptyComposition() {
        fuelType = CntFuelType.EMPTY;
        burnup = 0.0F;
        spentFrom = "";
        setComposition(new CompoundTag());
    }

    @Override
    public void setComposition(CompoundTag composition) {
        super.setComposition(composition);
        if (composition.isEmpty()) {
            fuelType = CntFuelType.EMPTY;
            burnup = 0.0F;
            spentFrom = "";
            return;
        }

        String profile = composition.getString(CrownsFuelProfile.PROFILE_KEY);
        CntFuelType type = CntFuelType.byName(profile);
        if (type != CntFuelType.EMPTY) {
            fuelType = type;
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("FuelType", fuelType.getSerializedName());
        tag.putFloat("Burnup", burnup);
        if (!spentFrom.isBlank()) {
            tag.putString("SpentFrom", spentFrom);
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        fuelType = CntFuelType.byName(tag.getString("FuelType"));
        burnup = tag.getFloat("Burnup");
        spentFrom = tag.getString("SpentFrom");
        if (fuelType == CntFuelType.EMPTY) {
            forceEmptyComposition();
        } else {
            setComposition(fuelType.composition());
        }
    }
}
