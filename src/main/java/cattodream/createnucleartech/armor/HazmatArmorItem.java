package cattodream.createnucleartech.armor;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

public class HazmatArmorItem extends ArmorItem {
    private final HazmatTier tier;

    public HazmatArmorItem(Holder<ArmorMaterial> material, Type type, HazmatTier tier, Properties properties) {
        super(material, type, properties);
        this.tier = tier;
    }

    public HazmatTier tier() {
        return tier;
    }
}
