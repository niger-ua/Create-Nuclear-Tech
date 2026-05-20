package cattodream.createnucleartech.client;

import cattodream.createnucleartech.Createnucleartech;
import cattodream.createnucleartech.ModRegistry;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = Createnucleartech.MODID, value = Dist.CLIENT)
public final class FalloutClientEffects {
    private FalloutClientEffects() {
    }

    @SubscribeEvent
    public static void colorFalloutFog(ViewportEvent.ComputeFogColor event) {
        float intensity = falloutIntensity();
        if (intensity <= 0.0F) {
            return;
        }
        float grey = 0.42F;
        float sickGreen = 0.47F;
        float blueGrey = 0.43F;
        event.setRed(Mth.lerp(intensity, event.getRed(), grey));
        event.setGreen(Mth.lerp(intensity, event.getGreen(), sickGreen));
        event.setBlue(Mth.lerp(intensity, event.getBlue(), blueGrey));
    }

    @SubscribeEvent
    public static void thickenFalloutFog(ViewportEvent.RenderFog event) {
        float intensity = falloutIntensity();
        if (intensity <= 0.0F) {
            return;
        }
        event.setNearPlaneDistance(Mth.lerp(intensity, event.getNearPlaneDistance(), 2.0F));
        event.setFarPlaneDistance(Mth.lerp(intensity, event.getFarPlaneDistance(), 52.0F));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    private static float falloutIntensity() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return 0.0F;
        }
        double radiation = 0.0D;
        MobEffectInstance effect = minecraft.player.getEffect(ModRegistry.RADIATION);
        if (effect != null) {
            radiation = Math.max(radiation, 20.0D + effect.getAmplifier() * 70.0D);
        }
        return (float) Mth.clamp((radiation - 35.0D) / 260.0D, 0.0D, 0.78D);
    }
}
