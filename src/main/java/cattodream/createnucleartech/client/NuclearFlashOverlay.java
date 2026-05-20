package cattodream.createnucleartech.client;

import cattodream.createnucleartech.Createnucleartech;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = Createnucleartech.MODID, value = Dist.CLIENT)
public final class NuclearFlashOverlay {
    private static final float FLASH_POWER = 850.0F;
    private static final RandomSource SHAKE_RANDOM = RandomSource.create();
    private static float flashAlpha;
    private static float shakeIntensity;
    private static float shakeDecay = 0.95F;
    private static float rumbleIntensity;

    private NuclearFlashOverlay() {
    }

    public static void triggerFlash(Vec3 center, float power) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        double distance = minecraft.player.position().distanceTo(center);
        float range = power * 10.0F;
        float intensity = Math.max(0.0F, 1.0F - (float) (distance / range));
        if (intensity <= 0.0F) {
            return;
        }

        flashAlpha = Math.max(flashAlpha, intensity);
        shakeIntensity = Math.max(shakeIntensity, intensity * 8.0F);
        rumbleIntensity = Math.max(rumbleIntensity, intensity * 3.0F);
        shakeDecay = 0.9F + 0.04F * (1.0F - intensity);
    }

    public static void renderFlash(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (flashAlpha <= 0.003F) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        if (flashAlpha > 0.5F) {
            int alpha = (int) (Math.min(flashAlpha, 1.0F) * 255.0F);
            guiGraphics.fill(0, 0, width, height, alpha << 24 | 0xFFFFFF);
        }
        if (flashAlpha > 0.25F && flashAlpha <= 0.7F) {
            float warmth = (flashAlpha - 0.25F) / 0.45F;
            int red = 255;
            int green = (int) (255.0F * (0.5F + 0.5F * warmth));
            int blue = (int) (100.0F * warmth);
            int color = red << 16 | green << 8 | blue;
            int alpha = (int) (Math.min(flashAlpha * 0.85F, 0.7F) * 255.0F);
            guiGraphics.fill(0, 0, width, height, alpha << 24 | color);
        }
        if (flashAlpha > 0.1F && flashAlpha <= 0.4F) {
            int alpha = Math.min((int) (flashAlpha * 1.5F * 255.0F), 160);
            guiGraphics.fill(0, 0, width, height, alpha << 24 | 0xFF9900);
        }
        if (flashAlpha <= 0.2F && flashAlpha > 0.01F) {
            int alpha = Math.min((int) (flashAlpha * 3.0F * 255.0F), 80);
            guiGraphics.fill(0, 0, width, height, alpha << 24 | 0xCC2200);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (flashAlpha > 0.0F) {
            flashAlpha = flashAlpha * 0.92F - 0.001F;
            if (flashAlpha < 0.0F) {
                flashAlpha = 0.0F;
            }
        }
        if (shakeIntensity > 0.0F) {
            shakeIntensity *= shakeDecay;
            if (shakeIntensity < 0.003F) {
                shakeIntensity = 0.0F;
            }
        }
        if (rumbleIntensity > 0.0F) {
            rumbleIntensity = rumbleIntensity * 0.985F - 0.001F;
            if (rumbleIntensity < 0.003F) {
                rumbleIntensity = 0.0F;
            }
        }

    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        float total = shakeIntensity + rumbleIntensity;
        if (total <= 0.003F) {
            return;
        }

        float yawShake = (float) (SHAKE_RANDOM.nextGaussian() * shakeIntensity);
        float pitchShake = (float) (SHAKE_RANDOM.nextGaussian() * shakeIntensity);
        float rollShake = (float) (SHAKE_RANDOM.nextGaussian() * shakeIntensity * 0.7D);
        double time = System.nanoTime() / 1.0E9D;
        float yawRumble = (float) (Math.sin(time * 3.5D) * rumbleIntensity * 0.6D);
        float pitchRumble = (float) (Math.sin(time * 2.8D + 1.0D) * rumbleIntensity * 0.5D);
        float rollRumble = (float) (Math.sin(time * 4.1D + 2.0D) * rumbleIntensity * 0.3D);

        event.setYaw(event.getYaw() + yawShake + yawRumble);
        event.setPitch(event.getPitch() + pitchShake + pitchRumble);
        event.setRoll(event.getRoll() + rollShake + rollRumble);
    }
}
