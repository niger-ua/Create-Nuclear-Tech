package cattodream.createnucleartech.integration.crowns;

import cattodream.createnucleartech.Createnucleartech;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = Createnucleartech.MODID)
public final class CrownsFuelTooltipEvents {
    private static final ResourceLocation FUEL_ROD = ResourceLocation.fromNamespaceAndPath("crowns", "fuel_rod");
    private static final ResourceLocation FUEL_ASSEMBLY = ResourceLocation.fromNamespaceAndPath("crowns", "fuel_assembly");

    private CrownsFuelTooltipEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!FUEL_ROD.equals(id) && !FUEL_ASSEMBLY.equals(id)) {
            return;
        }

        CompoundTag composition = composition(stack);
        if (composition == null) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        String profileId = composition.getString(CrownsFuelProfile.PROFILE_KEY);
        CrownsFuelProfile profile = CrownsFuelProfile.byIdOrInert(profileId);
        if (!profile.id().equals("inert")) {
            tooltip.add(Component.literal("CNT Fuel: " + profile.displayName()).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Spectrum F/M/S: "
                    + fmt(profile.fastYield()) + " / "
                    + fmt(profile.mediumYield()) + " / "
                    + fmt(profile.slowYield())).withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.literal(profile.selfStarting()
                    ? "Startup: self-starting"
                    : "Startup: needs driver flux >= " + fmt(profile.requiredStarterFlux())).withStyle(profile.selfStarting() ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        }

        double th232 = composition.getDouble(CrownsFuelProfile.TH232_KEY);
        double pu240 = composition.getDouble(CrownsFuelProfile.PU240_KEY);
        if (th232 > 0.0D) {
            tooltip.removeIf(component -> {
                String line = component.getString();
                return line.contains("Uranium 238") && line.contains("0.00");
            });
            tooltip.add(Component.literal("Thorium 232: " + percent(th232)).withStyle(ChatFormatting.YELLOW));
        }
        if (pu240 > 0.0D) {
            tooltip.add(Component.literal("Plutonium 240: " + percent(pu240)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static CompoundTag composition(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || data.isEmpty()) {
            return null;
        }
        CompoundTag root = data.copyTag();
        if (!root.contains("composition")) {
            return null;
        }
        CompoundTag composition = root.getCompound("composition");
        return composition.isEmpty() ? null : composition;
    }

    private static String percent(double value) {
        return String.format("%.2f %%", value * 100.0D);
    }

    private static String fmt(float value) {
        return String.format("%.2f", value);
    }
}
