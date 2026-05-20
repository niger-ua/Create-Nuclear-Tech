package cattodream.createnucleartech.armor;

import cattodream.createnucleartech.CNTTags;
import cattodream.createnucleartech.Createnucleartech;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Map;

public final class ModArmorMaterials {
    public static final Holder<ArmorMaterial> BASIC_HAZMAT = Holder.direct(new ArmorMaterial(
            Map.of(
                    ArmorItem.Type.BOOTS, 1,
                    ArmorItem.Type.LEGGINGS, 2,
                    ArmorItem.Type.CHESTPLATE, 2,
                    ArmorItem.Type.HELMET, 1
            ),
            9,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(CNTTags.Items.RADIATION_SHIELDING_MATERIAL),
            List.of(new ArmorMaterial.Layer(id("basic_hazmat"))),
            0.0F,
            0.0F
    ));

    public static final Holder<ArmorMaterial> ADVANCED_HAZMAT = Holder.direct(new ArmorMaterial(
            Map.of(
                    ArmorItem.Type.BOOTS, 1,
                    ArmorItem.Type.LEGGINGS, 4,
                    ArmorItem.Type.CHESTPLATE, 5,
                    ArmorItem.Type.HELMET, 2
            ),
            15,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(CNTTags.Items.ADVANCED_ALLOY),
            List.of(new ArmorMaterial.Layer(id("advanced_hazmat"))),
            1.0F,
            0.0F
    ));

    public static final Holder<ArmorMaterial> REINFORCED_HAZMAT = Holder.direct(new ArmorMaterial(
            Map.of(
                    ArmorItem.Type.BOOTS, 2,
                    ArmorItem.Type.LEGGINGS, 4,
                    ArmorItem.Type.CHESTPLATE, 5,
                    ArmorItem.Type.HELMET, 2
            ),
            16,
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(CNTTags.Items.RADIATION_SHIELDING_MATERIAL),
            List.of(new ArmorMaterial.Layer(id("reinforced_hazmat"))),
            1.5F,
            0.02F
    ));

    public static final Holder<ArmorMaterial> PAA_HAZMAT = Holder.direct(new ArmorMaterial(
            Map.of(
                    ArmorItem.Type.BOOTS, 2,
                    ArmorItem.Type.LEGGINGS, 5,
                    ArmorItem.Type.CHESTPLATE, 7,
                    ArmorItem.Type.HELMET, 3
            ),
            18,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(CNTTags.Items.ADVANCED_ALLOY),
            List.of(new ArmorMaterial.Layer(id("elite_hazmat"))),
            2.0F,
            0.05F
    ));

    private ModArmorMaterials() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, path);
    }
}
