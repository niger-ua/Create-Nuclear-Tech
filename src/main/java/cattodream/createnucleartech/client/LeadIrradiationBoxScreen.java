package cattodream.createnucleartech.client;

import cattodream.createnucleartech.menu.LeadIrradiationBoxMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class LeadIrradiationBoxScreen extends AbstractContainerScreen<LeadIrradiationBoxMenu> {
    // Change these if you want to manually nudge only the drawn slot frames.
    // Actual item/hover slot coordinates are in LeadIrradiationBoxMenu.
    private static final int SLOT_FRAME_OFFSET_X = -1;
    private static final int SLOT_FRAME_OFFSET_Y = -1;

    public LeadIrradiationBoxScreen(LeadIrradiationBoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 220;
        imageHeight = 188;
        titleLabelX = 0;
        titleLabelY = -2000;
        inventoryLabelX = 0;
        inventoryLabelY = -2000;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        drawBox(guiGraphics, x, y);

        for (Slot slot : menu.slots) {
            drawSlot(guiGraphics, x + slot.x + SLOT_FRAME_OFFSET_X, y + slot.y + SLOT_FRAME_OFFSET_Y);
        }

        int fieldColor = menu.fieldStrength() >= 8.0D ? 0xFFE8C85A : 0xFF7E9B8D;
        guiGraphics.drawString(font, Component.literal(String.format("Field %.1f", menu.fieldStrength())), x + 146, y + 69, fieldColor, false);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 20, y + 20, 0xFF121817);
        guiGraphics.fill(x + 1, y + 1, x + 19, y + 19, 0xFF303B38);
        guiGraphics.fill(x + 2, y + 2, x + 18, y + 18, 0xFF26312F);
    }

    private void drawBox(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF111716);
        guiGraphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF202726);
        guiGraphics.fill(x + 5, y + 5, x + imageWidth - 5, y + imageHeight - 5, 0xFF313A38);
        drawBevel(guiGraphics, x + 5, y + 5, imageWidth - 10, imageHeight - 10, 0xFF43514D, 0xFF111817);

        guiGraphics.fill(x + 8, y + 20, x + imageWidth - 8, y + 86, 0xFF101817);
        guiGraphics.fill(x + 10, y + 22, x + imageWidth - 10, y + 84, 0xFF1B2322);
        drawBevel(guiGraphics, x + 8, y + 20, imageWidth - 16, 66, 0xFF2B3734, 0xFF070B0A);

        guiGraphics.fill(x + 27, y + 101, x + 191, y + 157, 0xFF101817);
        drawBevel(guiGraphics, x + 27, y + 101, 164, 56, 0xFF2B3734, 0xFF070B0A);
        guiGraphics.fill(x + 27, y + 159, x + 191, y + 179, 0xFF101817);
        drawBevel(guiGraphics, x + 27, y + 159, 164, 20, 0xFF2B3734, 0xFF070B0A);
    }

    private static void drawBevel(GuiGraphics guiGraphics, int x, int y, int width, int height, int light, int dark) {
        guiGraphics.fill(x, y, x + width, y + 1, light);
        guiGraphics.fill(x, y, x + 1, y + height, light);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, dark);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, dark);
    }
}
