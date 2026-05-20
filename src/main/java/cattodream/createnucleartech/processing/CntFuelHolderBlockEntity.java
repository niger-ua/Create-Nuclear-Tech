package cattodream.createnucleartech.processing;

import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.integration.crowns.CrownsFuelProfile;
import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CntFuelHolderBlockEntity extends AssemblyBlockEntity {
    public static final int RODS_PER_HOLDER = 4;
    private static final float BURNUP_LIMIT = 1.0F;

    private CntFuelType fuelType = CntFuelType.EMPTY;
    private float burnup;
    private String spentFrom = "";

    public CntFuelHolderBlockEntity(BlockPos pos, BlockState state) {
        this(ModRegistry.FUEL_HOLDER_ENTITY.get(), pos, state);
    }

    public CntFuelHolderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || !fuelType.isFreshFuel()) {
            return;
        }

        float heat = Math.max(0.0F, temperature - 300.0F);
        burnup += Math.max(0.00000008F, nbrOfFission * 0.00000055F + heat * 0.00000000075F);
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
        fuelType = type;
        burnup = 0.0F;
        spentFrom = "";
        setComposition(type.composition());
        updateBlockFuel(type);
    }

    public void clearFuel() {
        fuelType = CntFuelType.EMPTY;
        burnup = 0.0F;
        spentFrom = "";
        setComposition(new CompoundTag());
        updateBlockFuel(CntFuelType.EMPTY);
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

    @Override
    public void setComposition(CompoundTag composition) {
        super.setComposition(composition);
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
    }
}
