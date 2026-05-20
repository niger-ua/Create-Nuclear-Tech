package cattodream.createnucleartech.radiation;

import cattodream.createnucleartech.armor.HazmatArmorItem;
import cattodream.createnucleartech.armor.HazmatTier;
import cattodream.createnucleartech.items.RadiationTongsItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class RadiationProtection {
    private RadiationProtection() {
    }

    public static double protectionFor(LivingEntity entity) {
        int basicPieces = 0;
        int advancedPieces = 0;
        int reinforcedPieces = 0;
        int paaPieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.getItem() instanceof HazmatArmorItem armor) {
                if (armor.tier().isSealed()) {
                    paaPieces++;
                } else if (armor.tier().isReinforced()) {
                    reinforcedPieces++;
                } else if (armor.tier().isAdvanced()) {
                    advancedPieces++;
                } else {
                    basicPieces++;
                }
            }
        }

        if (paaPieces == 4) {
            return 0.0D;
        }
        double basic = HazmatTier.BASIC.fullSetProtection() * (basicPieces / 4.0D);
        double advanced = HazmatTier.ADVANCED.fullSetProtection() * (advancedPieces / 4.0D);
        double reinforced = HazmatTier.REINFORCED.fullSetProtection() * (reinforcedPieces / 4.0D);
        double paa = HazmatTier.PAA.fullSetProtection() * (paaPieces / 4.0D);
        return Math.min(0.99D, basic + advanced + reinforced + paa);
    }

    public static boolean hasHandlingTool(LivingEntity entity) {
        return entity.getOffhandItem().getItem() instanceof RadiationTongsItem;
    }

    public static void damageHandlingTool(LivingEntity entity) {
        damageHandlingTool(entity, EquipmentSlot.OFFHAND);
    }

    private static void damageHandlingTool(LivingEntity entity, EquipmentSlot slot) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.getItem() instanceof RadiationTongsItem) {
            stack.hurtAndBreak(1, entity, slot);
        }
    }

    public static boolean hasFullPaaSet(LivingEntity entity) {
        int paaPieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.getItem() instanceof HazmatArmorItem armor && armor.tier().isSealed()) {
                paaPieces++;
            }
        }
        return paaPieces == 4;
    }

    /** @deprecated use {@link #hasFullPaaSet(LivingEntity)} */
    @Deprecated
    public static boolean hasFullEliteSet(LivingEntity entity) {
        return hasFullPaaSet(entity);
    }
}
