package cattodream.createnucleartech.processing;

import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.integration.crowns.CrownsFuelProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.function.Supplier;

public enum CntFuelType implements StringRepresentable {
    EMPTY("empty", null, false),
    NATURAL_URANIUM("natural_uranium", CrownsFuelProfile.NATURAL_URANIUM, true),
    ENRICHED_URANIUM("enriched_uranium", CrownsFuelProfile.ENRICHED_URANIUM, true),
    MILITARY_URANIUM("military_uranium", CrownsFuelProfile.MILITARY_URANIUM, true),
    MOX("mox", CrownsFuelProfile.MOX, true),
    PLUTONIUM_239("plutonium_239", CrownsFuelProfile.PLUTONIUM_239, true),
    REACTOR_GRADE_PLUTONIUM("reactor_grade_plutonium", CrownsFuelProfile.REACTOR_GRADE_PLUTONIUM, true),
    THORIUM("thorium", CrownsFuelProfile.THORIUM, true),
    SPENT("spent", CrownsFuelProfile.SPENT_FUEL, false);

    private final String id;
    private final CrownsFuelProfile profile;
    private final boolean freshFuel;

    CntFuelType(String id, CrownsFuelProfile profile, boolean freshFuel) {
        this.id = id;
        this.profile = profile;
        this.freshFuel = freshFuel;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public CrownsFuelProfile profile() {
        return profile == null ? CrownsFuelProfile.INERT : profile;
    }

    public boolean isFreshFuel() {
        return freshFuel;
    }

    public ItemStack newRodStack(int count) {
        Supplier<? extends Item> supplier = switch (this) {
            case NATURAL_URANIUM -> ModRegistry.NATURAL_URANIUM_FUEL_ROD;
            case ENRICHED_URANIUM -> ModRegistry.ENRICHED_URANIUM_FUEL_ROD;
            case MILITARY_URANIUM -> ModRegistry.MILITARY_URANIUM_FUEL_ROD;
            case MOX -> ModRegistry.MOX_FUEL_ROD;
            case PLUTONIUM_239 -> ModRegistry.PLUTONIUM_FUEL_ROD;
            case REACTOR_GRADE_PLUTONIUM -> ModRegistry.REACTOR_PLUTONIUM_FUEL_ROD;
            case THORIUM -> ModRegistry.THORIUM_FUEL_ROD;
            case SPENT -> ModRegistry.SPENT_FUEL_ROD;
            default -> null;
        };
        return supplier == null ? ItemStack.EMPTY : new ItemStack(supplier.get(), count);
    }

    public CompoundTag composition() {
        CompoundTag composition = new CompoundTag();
        if (profile != null) {
            composition.putString(CrownsFuelProfile.PROFILE_KEY, profile.id());
        }
        switch (this) {
            case NATURAL_URANIUM -> {
                composition.putDouble("crowns:u235", 0.0078125D);
                composition.putDouble("crowns:u238", 0.9921875D);
            }
            case ENRICHED_URANIUM -> {
                composition.putDouble("crowns:u235", 0.1875D);
                composition.putDouble("crowns:u238", 0.8125D);
            }
            case MILITARY_URANIUM -> {
                composition.putDouble("crowns:u235", 0.875D);
                composition.putDouble("crowns:u238", 0.125D);
            }
            case MOX -> {
                composition.putDouble("crowns:p239", 0.25D);
                composition.putDouble("crowns:u238", 0.75D);
            }
            case PLUTONIUM_239 -> composition.putDouble("crowns:p239", 1.0D);
            case REACTOR_GRADE_PLUTONIUM -> {
                composition.putDouble("crowns:p239", 0.72D);
                composition.putDouble(CrownsFuelProfile.PU240_KEY, 0.28D);
            }
            case THORIUM -> composition.putDouble(CrownsFuelProfile.TH232_KEY, 1.0D);
            case SPENT -> {
                composition.putDouble("crowns:u238", 0.72D);
                composition.putDouble("crowns:p239", 0.025D);
                composition.putDouble(CrownsFuelProfile.PU240_KEY, 0.10D);
            }
            default -> {
            }
        }
        return composition;
    }

    public static CntFuelType byName(String name) {
        if (name == null || name.isBlank()) {
            return EMPTY;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        for (CntFuelType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        return EMPTY;
    }
}
