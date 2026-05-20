package cattodream.createnucleartech.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RadioactiveItem extends Item {
    private final double radiationStrength;

    public RadioactiveItem(Properties properties, double radiationStrength) {
        super(properties);
        this.radiationStrength = radiationStrength;
    }

    public double radiationStrength() {
        return radiationStrength;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.createnucleartech.radioactive", radiationStrength).withStyle(ChatFormatting.GREEN));
    }
}
