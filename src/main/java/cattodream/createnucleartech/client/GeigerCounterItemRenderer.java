package cattodream.createnucleartech.client;

import cattodream.createnucleartech.Createnucleartech;
import cattodream.createnucleartech.items.GeigerCounterItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class GeigerCounterItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation BASE_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, "item/geiger_counter_3d_base")
    );
    private static final int SCREEN_TEXT_COLOR = 0xFF07180D;
    private static final float TEXT_SCALE = 0.0083F;

    public GeigerCounterItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        BakedModel baseModel = minecraft.getModelManager().getModel(BASE_MODEL);
        itemRenderer.renderModelLists(
                baseModel,
                stack,
                light,
                overlay,
                poseStack,
                ItemRenderer.getFoilBufferDirect(buffer, Sheets.translucentItemSheet(), true, stack.hasFoil())
        );

        if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND) {
            return;
        }
        renderDisplayText(stack, poseStack, buffer);
    }

    private static void renderDisplayText(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        String text = displayText(stack, font);

        drawScreenText(font, text, poseStack, buffer, 0.626D, 0.325D, 0.665D, 90.0F);
        drawScreenText(font, text, poseStack, buffer, 0.439D, 0.325D, 0.455D, -90.0F);
    }

    private static void drawScreenText(Font font, String text, PoseStack poseStack, MultiBufferSource buffer, double x, double y, double z, float yaw) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        float textX = Math.max(0.0F, (16.0F - font.width(text)) * 0.5F);
        font.drawInBatch(text, textX, 0.0F, SCREEN_TEXT_COLOR, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    private static String displayText(ItemStack stack, Font font) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        double radiation = tag.getDouble(GeigerCounterItem.RADS_PER_SECOND_KEY);
        return format(radiation);
    }

    private static String format(double radiation) {
        if (radiation > 999.0D) {
            return ">999";
        }
        if (radiation >= 1.0D) {
            return String.format("%.0f", radiation);
        }
        return String.format("%.1f", radiation);
    }
}
