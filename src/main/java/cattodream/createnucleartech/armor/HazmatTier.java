package cattodream.createnucleartech.armor;

import cattodream.createnucleartech.Config;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorMaterial;

public enum HazmatTier {
    /** HBM basic yellow hazmat — chemical protection. */
    BASIC(false, false, false, 11),
    /** HBM advanced red hazmat cloth tier. */
    ADVANCED(true, false, false, 20),
    /** HBM lead-reinforced grey hazmat tier. */
    REINFORCED(true, false, false, 27),
    /** HBM PaA battle hazmat — full sealed protection. */
    PAA(true, true, true, 38);

    private final boolean advanced;
    private final boolean reinforced;
    private final boolean sealed;
    private final int durabilityMultiplier;

    HazmatTier(boolean advanced, boolean reinforced, boolean sealed, int durabilityMultiplier) {
        this.advanced = advanced;
        this.reinforced = reinforced;
        this.sealed = sealed;
        this.durabilityMultiplier = durabilityMultiplier;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public boolean isReinforced() {
        return reinforced;
    }

    public int durabilityMultiplier() {
        return durabilityMultiplier;
    }

    public boolean isSealed() {
        return sealed;
    }

    public double fullSetProtection() {
        if (sealed) {
            return 0.0D;
        }
        if (reinforced) {
            return Config.hazmatReinforcedProtection;
        }
        return advanced ? Config.hazmatAdvancedProtection : Config.hazmatBasicProtection;
    }

    public Holder<ArmorMaterial> material() {
        if (sealed) {
            return ModArmorMaterials.PAA_HAZMAT;
        }
        if (reinforced) {
            return ModArmorMaterials.REINFORCED_HAZMAT;
        }
        return advanced ? ModArmorMaterials.ADVANCED_HAZMAT : ModArmorMaterials.BASIC_HAZMAT;
    }
}
