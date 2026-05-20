package cattodream.createnucleartech.items;

import cattodream.createnucleartech.armor.HazmatArmorItem;
import cattodream.createnucleartech.armor.HazmatTier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

public class HazmatKitItem extends Item {
    private final HazmatTier tier;
    private final Supplier<ItemStack> helmet;
    private final Supplier<ItemStack> chestplate;
    private final Supplier<ItemStack> leggings;
    private final Supplier<ItemStack> boots;

    public HazmatKitItem(
            HazmatTier tier,
            Supplier<ItemStack> helmet,
            Supplier<ItemStack> chestplate,
            Supplier<ItemStack> leggings,
            Supplier<ItemStack> boots,
            Properties properties
    ) {
        super(properties);
        this.tier = tier;
        this.helmet = helmet;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            equip(player, EquipmentSlot.HEAD, helmet.get());
            equip(player, EquipmentSlot.CHEST, chestplate.get());
            equip(player, EquipmentSlot.LEGS, leggings.get());
            equip(player, EquipmentSlot.FEET, boots.get());

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static void equip(Player player, EquipmentSlot slot, ItemStack armor) {
        ItemStack current = player.getItemBySlot(slot);
        if (!current.isEmpty()) {
            player.drop(current, false);
        }
        player.setItemSlot(slot, armor.copy());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.createnucleartech.hazmat_kit.tooltip", tier.name().toLowerCase()));
    }
}
