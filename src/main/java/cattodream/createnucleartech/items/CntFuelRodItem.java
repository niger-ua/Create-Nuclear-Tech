package cattodream.createnucleartech.items;

import cattodream.createnucleartech.integration.crowns.CrownsFuelProfile;
import cattodream.createnucleartech.processing.CntFuelType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.Locale;
import java.util.List;

public class CntFuelRodItem extends Item {
    private final CntFuelType fuelType;
    private final String spentSource;

    public CntFuelRodItem(CntFuelType fuelType, Properties properties) {
        this(fuelType, "", properties);
    }

    public CntFuelRodItem(CntFuelType fuelType, String spentSource, Properties properties) {
        super(properties);
        this.fuelType = fuelType;
        this.spentSource = spentSource;
    }

    public CntFuelType fuelType() {
        return fuelType;
    }

    public String spentSource() {
        return spentSource;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return fuelType != CntFuelType.EMPTY;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * integrity(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float integrity = integrity(stack);
        return Mth.hsvToRgb(integrity * 0.33F, 0.95F, 0.9F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CrownsFuelProfile profile = fuelType.profile();
        tooltip.add(Component.literal("CNT Fuel: " + profile.displayName()).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Fuel integrity: " + Math.round(integrity(stack) * 100.0F) + "%").withStyle(integrity(stack) > 0.25F ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (fuelType == CntFuelType.SPENT) {
            String source = spentSource.isBlank() ? spentSourceFromStack(stack) : spentSource;
            tooltip.add(Component.literal("Spent from: " + displaySource(source)).withStyle(ChatFormatting.DARK_GREEN));
        }
        tooltip.add(Component.literal("Spectrum F/M/S: "
                + fmt(profile.fastYield()) + " / "
                + fmt(profile.mediumYield()) + " / "
                + fmt(profile.slowYield())).withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal(profile.selfStarting()
                ? "Startup: self-starting"
                : "Startup: needs driver flux >= " + fmt(profile.requiredStarterFlux())).withStyle(profile.selfStarting() ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        tooltip.add(Component.literal("Use: right-click a vertical fuel holder column to fill one holder per rod stack item.").withStyle(ChatFormatting.GRAY));
    }

    private static String fmt(float value) {
        return String.format("%.2f", value);
    }

    private float integrity(ItemStack stack) {
        if (fuelType == CntFuelType.SPENT) {
            return 0.0F;
        }
        return Mth.clamp(1.0F - burnup(stack), 0.0F, 1.0F);
    }

    public static float burnup(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0.0F;
        }
        CompoundTag root = data.copyTag();
        if (root.contains("burnup")) {
            return root.getFloat("burnup");
        }
        if (root.contains("spent")) {
            CompoundTag spent = root.getCompound("spent");
            return spent.getFloat("burnup");
        }
        return 0.0F;
    }

    public static void setBurnup(ItemStack stack, float burnup) {
        if (stack.isEmpty()) {
            return;
        }
        float clamped = Mth.clamp(burnup, 0.0F, 1.0F);
        if (clamped <= 0.000001F) {
            return;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = data == null ? new CompoundTag() : data.copyTag();
        root.putFloat("burnup", clamped);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static String spentSourceFromStack(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return "";
        }
        CompoundTag root = data.copyTag();
        if (root.contains("spent")) {
            return root.getCompound("spent").getString("spent_from");
        }
        return root.getString("spent_from");
    }

    private static String displaySource(String source) {
        if (source == null || source.isBlank()) {
            return "unknown fuel";
        }
        return source.replace('_', ' ').toLowerCase(Locale.ROOT);
    }
}
