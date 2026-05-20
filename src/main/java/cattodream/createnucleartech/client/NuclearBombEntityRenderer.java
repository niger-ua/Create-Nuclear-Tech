package cattodream.createnucleartech.client;

import cattodream.createnucleartech.ModRegistry;
import cattodream.createnucleartech.explosion.NuclearBombBlock;
import cattodream.createnucleartech.explosion.NuclearBombEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class NuclearBombEntityRenderer extends EntityRenderer<NuclearBombEntity> {
    public NuclearBombEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(NuclearBombEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        Direction facing = entity.getFacing();
        float downwardTilt = entity.getFallTilt(partialTick);
        poseStack.translate(0.0D, 0.5D, 0.0D);
        applyNoseDownTilt(poseStack, facing, downwardTilt);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        BlockState state = ModRegistry.NUCLEAR_BOMB.get()
                .defaultBlockState()
                .setValue(NuclearBombBlock.LIT, true)
                .setValue(NuclearBombBlock.FACING, facing);
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        dispatcher.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void applyNoseDownTilt(PoseStack poseStack, Direction facing, float degrees) {
        if (degrees <= 0.0F) {
            return;
        }
        switch (facing) {
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(degrees));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-degrees));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-degrees));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(degrees));
            default -> {
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(NuclearBombEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
