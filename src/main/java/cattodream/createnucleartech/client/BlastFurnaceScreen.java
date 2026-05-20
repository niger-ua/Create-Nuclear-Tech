package cattodream.createnucleartech.client;

import cattodream.createnucleartech.Createnucleartech;
import cattodream.createnucleartech.menu.BlastFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BlastFurnaceScreen extends AbstractContainerScreen<BlastFurnaceMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            Createnucleartech.MODID,
            "textures/gui/blast_furnace.png"
    );

    public BlastFurnaceScreen(BlastFurnaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = BlastFurnaceMenu.slotX(BlastFurnaceMenu.PLAYER_INVENTORY_X);
        inventoryLabelY = BlastFurnaceMenu.slotY(BlastFurnaceMenu.PLAYER_INVENTORY_Y) - 12;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int progressWidth = Math.min(24, menu.progress() * 24 / 400);
        int fuelHeight = Math.min(52, menu.fuel() * 52 / 12800);
        if (fuelHeight > 0) {
            guiGraphics.blit(BACKGROUND, leftPos + 44, topPos + 70 - fuelHeight, 201, 53 - fuelHeight, 16, fuelHeight, 256, 256);
        }
        if (progressWidth > 0) {
            guiGraphics.blit(BACKGROUND, leftPos + 101, topPos + 35, 176, 14, progressWidth + 1, 17, 256, 256);
        }
        if (menu.fuel() > 0 && menu.canProcess()) {
            guiGraphics.blit(BACKGROUND, leftPos + 63, topPos + 37, 176, 0, 14, 14, 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFFFFF, false);
    }
}
