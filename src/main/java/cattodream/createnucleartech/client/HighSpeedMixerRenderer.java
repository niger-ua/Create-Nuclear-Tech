package cattodream.createnucleartech.client;

import cattodream.createnucleartech.Createnucleartech;
import cattodream.createnucleartech.processing.HighSpeedMixerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class HighSpeedMixerRenderer extends KineticBlockEntityRenderer<HighSpeedMixerBlockEntity> {
    private static final PartialModel STEEL_COG = partial("cog");
    private static final PartialModel MIXER_POLE = partial("pole");
    private static final PartialModel MIXER_HEAD = partial("head");

    public HighSpeedMixerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRenderOffScreen(HighSpeedMixerBlockEntity be) {
        return true;
    }

    @Override
    protected void renderSafe(HighSpeedMixerBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        BlockState blockState = be.getBlockState();
        VertexConsumer solid = buffer.getBuffer(RenderType.solid());

        SuperByteBuffer cog = CachedBuffers.partial(STEEL_COG, blockState);
        standardKineticRotationTransform(cog, be, light).renderInto(poseStack, solid);

        float headOffset = be.getRenderedHeadOffset(partialTicks);
        float speed = be.getRenderedHeadRotationSpeed(partialTicks);
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float angle = ((time * speed * 6.0F / 10.0F) % 360.0F) / 180.0F * (float) Math.PI;

        SuperByteBuffer pole = CachedBuffers.partial(MIXER_POLE, blockState);
        pole.translate(0.0F, -headOffset, 0.0F)
                .light(light)
                .renderInto(poseStack, solid);

        VertexConsumer cutout = buffer.getBuffer(RenderType.cutoutMipped());
        SuperByteBuffer head = CachedBuffers.partial(MIXER_HEAD, blockState);
        head.rotateCentered(angle, Direction.UP)
                .translate(0.0F, -headOffset, 0.0F)
                .light(light)
                .renderInto(poseStack, cutout);
    }

    private static PartialModel partial(String name) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(
                Createnucleartech.MODID,
                "block/high_speed_mixer/" + name
        ));
    }
}
