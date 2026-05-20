package cattodream.createnucleartech.client;

import cattodream.createnucleartech.menu.NuclearBombMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class NuclearBombScreen extends AbstractContainerScreen<NuclearBombMenu> {
    // Change these two values if the custom slot frame needs manual pixel nudging.
    // The real Minecraft slot stays at slot.x / slot.y; this only moves our drawn frame.
    private static final int SLOT_FRAME_OFFSET_X = -2;
    private static final int SLOT_FRAME_OFFSET_Y = -2;
    private static final UiButton[] BUTTONS = {
            new UiButton(12, 49, 31, 16, "-60", 0),
            new UiButton(48, 49, 31, 16, "-10", 1),
            new UiButton(12, 70, 31, 16, "+10", 2),
            new UiButton(48, 70, 31, 16, "+60", 3),
            new UiButton(184, 76, 46, 18, "START", 4)
    };

    public NuclearBombScreen(NuclearBombMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 240;
        imageHeight = 204;
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (UiButton uiButton : BUTTONS) {
            if (uiButton.id() < 4 && !menu.hasTimerActivator()) {
                continue;
            }
            if (uiButton.contains(leftPos, topPos, mouseX, mouseY)) {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, uiButton.id());
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        drawPanel(guiGraphics, x, y);
        guiGraphics.fill(x + 8, y + 20, x + imageWidth - 8, y + 108, 0xFF141112);
        drawBevel(guiGraphics, x + 8, y + 20, imageWidth - 16, 88, 0xFF2A2225, 0xFF090707);
        guiGraphics.fill(x + 86, y + 28, x + 180, y + 106, 0xFF1D181A);
        drawBevel(guiGraphics, x + 86, y + 28, 94, 78, 0xFF342A2D, 0xFF100C0E);

        for (int index = 0; index < menu.slots.size(); index++) {
            Slot slot = menu.slots.get(index);
            drawSlot(guiGraphics, x + slot.x + SLOT_FRAME_OFFSET_X, y + slot.y + SLOT_FRAME_OFFSET_Y, index == 4);
        }

        for (UiButton button : BUTTONS) {
            if (button.id() < 4 && !menu.hasTimerActivator()) {
                continue;
            }
            drawButton(guiGraphics, x, y, button, button.contains(x, y, mouseX, mouseY));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.hasTimerActivator()) {
            guiGraphics.drawString(font, Component.literal("Timer " + formatTimer(menu.timerSeconds())), 12, 29, 0xFFFFC66D, false);
        }
        guiGraphics.drawString(font, Component.literal("Nuke " + menu.nuclearChancePercent() + "%"), 184, 57, 0xFFFFA35E, false);
    }

    private static String formatTimer(int seconds) {
        int minutes = seconds / 60;
        int rest = seconds % 60;
        return String.format("%02d:%02d", minutes, rest);
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y, boolean core) {
        guiGraphics.fill(x, y, x + 20, y + 20, core ? 0xFF5A3440 : 0xFF0D0A0B);
        guiGraphics.fill(x + 1, y + 1, x + 19, y + 19, core ? 0xFF3A2730 : 0xFF2D2729);
        guiGraphics.fill(x + 2, y + 2, x + 18, y + 18, core ? 0xFF46313A : 0xFF352E31);
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 240, y + 204, 0xFF221F21);
        guiGraphics.fill(x + 3, y + 3, x + 237, y + 201, 0xFF332D30);
        drawBevel(guiGraphics, x + 3, y + 3, 234, 198, 0xFF4A4044, 0xFF171315);
        guiGraphics.fill(x + 8, y + 112, x + 232, y + 196, 0xFF141112);
        drawBevel(guiGraphics, x + 8, y + 112, 224, 84, 0xFF2A2225, 0xFF090707);
    }

    private static void drawBevel(GuiGraphics guiGraphics, int x, int y, int width, int height, int light, int dark) {
        guiGraphics.fill(x, y, x + width, y + 1, light);
        guiGraphics.fill(x, y, x + 1, y + height, light);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, dark);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, dark);
    }

    private void drawButton(GuiGraphics guiGraphics, int x, int y, UiButton button, boolean hovered) {
        int bx = x + button.x();
        int by = y + button.y();
        int frame = hovered ? 0xFFFFC66D : 0xFF6A5558;
        int fill = hovered ? 0xFF4A3435 : 0xFF2A2224;
        guiGraphics.fill(bx, by, bx + button.width(), by + button.height(), frame);
        guiGraphics.fill(bx + 1, by + 1, bx + button.width() - 1, by + button.height() - 1, 0xFF120E10);
        guiGraphics.fill(bx + 2, by + 2, bx + button.width() - 2, by + button.height() - 2, fill);
        int color = button.id() == 4 ? 0xFFFF6D6D : 0xFFFFE0B0;
        int tx = bx + (button.width() - font.width(button.label())) / 2;
        guiGraphics.drawString(font, button.label(), tx, by + 5, color, false);
    }

    private record UiButton(int x, int y, int width, int height, String label, int id) {
        private boolean contains(int left, int top, double mouseX, double mouseY) {
            return mouseX >= left + x && mouseX < left + x + width && mouseY >= top + y && mouseY < top + y + height;
        }
    }
}
