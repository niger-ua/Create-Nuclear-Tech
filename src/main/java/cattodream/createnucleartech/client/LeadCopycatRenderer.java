package cattodream.createnucleartech.client;

import cattodream.createnucleartech.processing.LeadCopycatBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;

public class LeadCopycatRenderer implements BlockEntityRenderer<LeadCopycatBlockEntity> {
    private final BlockRenderDispatcher dispatcher;

    public LeadCopycatRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(LeadCopycatBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState copiedState = blockEntity.copiedState();
        if (!copiedState.isAir()) {
            poseStack.pushPose();
            float inset = 0.002F;
            poseStack.translate(inset, inset, inset);
            poseStack.scale(1.0F - inset * 2.0F, 1.0F - inset * 2.0F, 1.0F - inset * 2.0F);
            dispatcher.renderSingleBlock(copiedState, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }
}
