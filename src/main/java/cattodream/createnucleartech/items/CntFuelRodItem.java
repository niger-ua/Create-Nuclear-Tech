package cattodream.createnucleartech.items;

import cattodream.createnucleartech.processing.CntFuelType;
import net.minecraft.world.item.Item;

public class CntFuelRodItem extends Item {
    private final CntFuelType fuelType;

    public CntFuelRodItem(CntFuelType fuelType, Properties properties) {
        super(properties);
        this.fuelType = fuelType;
    }

    public CntFuelType fuelType() {
        return fuelType;
    }
}
