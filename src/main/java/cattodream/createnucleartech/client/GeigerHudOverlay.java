package cattodream.createnucleartech.client;

import cattodream.createnucleartech.Createnucleartech;
import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.items.GeigerCounterItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class GeigerHudOverlay {
    private static final ResourceLocation GEIGER = ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, "textures/gui/geiger_hud.png");
    private static final int TEXTURE_SIZE = 128;
    private static final int PANEL_WIDTH = 105;
    private static final int PANEL_HEIGHT = 30;
    private static final int PANEL_RIGHT_OFFSET = 8;
    private static final int PANEL_TOP_OFFSET = 8;
    private static final int RADS_TEXT_X = 30;
    private static final int RADS_TEXT_Y = 6;
    private static final int RAD_BAR_DST_X = 29;
    private static final int RAD_BAR_DST_Y = 16;
    private static final int RAD_BAR_SRC_X = 0;
    private static final int RAD_BAR_SRC_Y = 33;
    private static final int RAD_BAR_WIDTH = 72;
    private static final int RAD_BAR_HEIGHT = 10;
    private static final int RAD_TEXT_COLOR = 0xFFA7DFD7;

    private GeigerHudOverlay() {
    }

    public static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        // Kept for future armor-integrated Geiger displays. The handheld counter now
        // renders its reading on the 3D item model instead of spawning a HUD panel.
        if (!hasIntegratedGeiger(player)) {
            return;
        }
        ItemStack geiger = geigerStack(player);
        if (geiger.isEmpty()) {
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int x = Math.max(8, screenWidth - PANEL_WIDTH - PANEL_RIGHT_OFFSET);
        int y = PANEL_TOP_OFFSET;

        CompoundTag tag = geiger.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        double radsPerSecond = tag.getDouble(GeigerCounterItem.FIELD_RADIATION_KEY);
        double bodyRadiation = tag.getDouble(GeigerCounterItem.BODY_RADIATION_KEY);

        drawPanel(guiGraphics, x, y, radsPerSecond, bodyRadiation);
    }

    private static ItemStack geigerStack(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (isGeiger(mainHand)) {
            return mainHand;
        }
        ItemStack offhand = player.getOffhandItem();
        return isGeiger(offhand) ? offhand : ItemStack.EMPTY;
    }

    private static boolean hasIntegratedGeiger(Player player) {
        return false;
    }

    private static boolean isGeiger(ItemStack stack) {
        return stack.is(ModRegistry.GEIGER_COUNTER.get());
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, double radsPerSecond, double bodyRadiation) {
        guiGraphics.blit(GEIGER, x, y, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        drawRadiationBar(guiGraphics, x + RAD_BAR_DST_X, y + RAD_BAR_DST_Y, bodyRadiation);
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.drawString(minecraft.font, format(radsPerSecond), x + RADS_TEXT_X, y + RADS_TEXT_Y, RAD_TEXT_COLOR, false);
    }

    private static void drawRadiationBar(GuiGraphics guiGraphics, int x, int y, double bodyRadiation) {
        double clamped = Mth.clamp(bodyRadiation / 1200.0D, 0.0D, 1.0D);
        int fill = clamped <= 0.0D ? 0 : Math.max(1, (int) Math.round(RAD_BAR_WIDTH * clamped));
        if (fill > 0) {
            guiGraphics.blit(GEIGER, x, y, RAD_BAR_SRC_X, RAD_BAR_SRC_Y, fill, RAD_BAR_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }

    private static String format(double value) {
        return value >= 100.0D ? String.format("%.0f", value) : String.format("%.1f", value);
    }
}
