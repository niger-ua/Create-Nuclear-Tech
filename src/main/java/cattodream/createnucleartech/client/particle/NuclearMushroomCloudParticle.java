package cattodream.createnucleartech.client.particle;

import cattodream.createnucleartech.Createnucleartech;
import cattodream.createnucleartech.client.NuclearFlashOverlay;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class NuclearMushroomCloudParticle extends NoRenderParticle {
    private static final ResourceLocation CLOUDLET_TEXTURE = ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, "textures/particle/hbm/base_particle.png");
    private static final ResourceLocation FLASH_TEXTURE = ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, "textures/particle/hbm/flare_alpha.png");
    private static final RenderType CLOUDLET_RENDER_TYPE = RenderType.entityTranslucent(CLOUDLET_TEXTURE);
    private static final RenderType FLASH_RENDER_TYPE = RenderType.entityTranslucent(FLASH_TEXTURE);
    private static final List<NuclearMushroomCloudParticle> ACTIVE = new ArrayList<>();
    private static final double SIMULATION_SPEED = 0.36D;
    private static final double SPAWN_MULTIPLIER = 1.65D;
    private static final float CLOUDLET_SIZE_MULTIPLIER = 1.85F;

    public double coreHeight = 3.0D;
    public double convectionHeight = 3.0D;
    public double torusWidth = 3.0D;
    public double rollerSize = 1.0D;
    public double heat = 1.0D;
    public double lastSpawnY = -1.0D;
    public final List<Cloudlet> cloudlets = new ArrayList<>();

    private int colorType;
    private float scale = 1.0F;
    private boolean didStartFlash;

    public NuclearMushroomCloudParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        this.hasPhysics = false;
        this.lifetime = getMaxAge();
        ACTIVE.add(this);
    }

    public static Particle create(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        return new NuclearMushroomCloudParticle(level, x, y, z).setScale(1.5F, true);
    }

    public static Particle createLarge(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        return new NuclearMushroomCloudParticle(level, x, y, z).setScale(1.5F, true);
    }

    @Override
    public void tick() {
        age++;
        double s = 1.5D;
        double cs = 1.5D;
        int maxAge = getMaxAge();
        if (age == 1) {
            setScale((float) s, false);
            if (!didStartFlash) {
                didStartFlash = true;
                NuclearFlashOverlay.triggerFlash(new Vec3(x, y, z), 850.0F);
            }
        }
        if (lastSpawnY == -1.0D) {
            lastSpawnY = y - 3.0D;
        }
        if (age < 100) {
            level.setSkyFlashTime(5);
        }

        int spawnTarget = level.getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(x), Mth.floor(z)) - 3;
        double moveSpeed = 0.5D;
        lastSpawnY = Math.abs(spawnTarget - lastSpawnY) < moveSpeed ? spawnTarget : lastSpawnY + moveSpeed * Math.signum(spawnTarget - lastSpawnY);

        double range = (torusWidth - rollerSize) * 0.25D;
        double simSpeed = getSimulationSpeed();
        int toSpawn = (int) Math.ceil(10.0D * simSpeed * simSpeed * SPAWN_MULTIPLIER);
        int cloudLife = Math.min(age * age + 200, maxAge - age + 200);
        for (int i = 0; i < toSpawn; i++) {
            double px = x + random.nextGaussian() * range;
            double pz = z + random.nextGaussian() * range;
            Cloudlet cloud = new Cloudlet(px, lastSpawnY, pz, (float) (random.nextDouble() * Math.PI * 2.0D), 0, cloudLife);
            cloud.setScale((1.0F + age * 0.005F * (float) cs) * CLOUDLET_SIZE_MULTIPLIER, 5.0F * (float) cs * CLOUDLET_SIZE_MULTIPLIER);
            cloudlets.add(cloud);
        }

        if (age < 200) {
            int cloudCount = (int) (age * 5 * SPAWN_MULTIPLIER);
            int shockLife = Math.max(300 - age * 20, 50);
            for (int i = 0; i < cloudCount; i++) {
                double radius = ((age * 1.5D + random.nextDouble()) * 1.5D);
                float rot = (float) (Math.PI * 2.0D * random.nextDouble());
                double px = Math.cos(rot) * radius;
                double pz = Math.sin(rot) * radius;
                Cloudlet shock = new Cloudlet(px + x, level.getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(px + x), Mth.floor(pz + z)), pz + z, rot, 0, shockLife, TorexType.SHOCK);
                shock.setScale(7.0F * CLOUDLET_SIZE_MULTIPLIER, 2.0F * CLOUDLET_SIZE_MULTIPLIER).setMotion(age > 15 ? 0.75D : 0.0D);
                cloudlets.add(shock);
            }
        }

        if (age < 130.0D * s) {
            int ringLife = (int) (cloudLife * s);
            for (int i = 0; i < 2; i++) {
                Cloudlet cloud = new Cloudlet(x, y + coreHeight, z, (float) (random.nextDouble() * Math.PI * 2.0D), 0, ringLife, TorexType.RING);
                cloud.setScale((1.0F + age * 0.0025F * (float) (cs * cs)) * CLOUDLET_SIZE_MULTIPLIER, 3.0F * (float) (cs * cs) * CLOUDLET_SIZE_MULTIPLIER);
                cloudlets.add(cloud);
            }
        }

        for (Cloudlet cloud : cloudlets) {
            cloud.update();
        }
        coreHeight += 0.15D / s * SIMULATION_SPEED;
        torusWidth += 0.05D / s * SIMULATION_SPEED;
        rollerSize = torusWidth * 0.35D;
        convectionHeight = coreHeight + rollerSize;
        int maxHeat = (int) (50.0D * cs);
        heat = maxHeat - Math.pow(maxHeat * age / (double) maxAge, 1.0D);
        cloudlets.removeIf(cloud -> cloud.isDead);

        if (age > maxAge) {
            remove();
        }
    }

    private void spawnCondensation(int count, double heightOffset, double rowStep, double cs, double widthBase, double widthRand, double ignored) {
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < 4; j++) {
                float angle = (float) (Math.PI * 2.0D * random.nextDouble());
                double localRadius = torusWidth + rollerSize * (widthBase + random.nextDouble() * widthRand);
                double roll = Math.toRadians(4.0D * j);
                double px = Math.cos(roll) * localRadius;
                px = Math.cos(angle) * px;
                double pz = Math.sin(angle) * Math.cos(roll) * localRadius;
                Cloudlet cloud = new Cloudlet(
                        x + px,
                        y + coreHeight + heightOffset + j * rowStep,
                        z + pz,
                        angle,
                        0,
                        (int) ((20 + age / 10) * (1.0D + random.nextDouble() * 0.1D)),
                        TorexType.CONDENSATION
                );
                cloud.setScale(0.125F * (float) cs * CLOUDLET_SIZE_MULTIPLIER, 3.0F * (float) cs * CLOUDLET_SIZE_MULTIPLIER)
                        .setMotion(0.35D);
                cloudlets.add(cloud);
            }
        }
    }

    public NuclearMushroomCloudParticle setScale(float scale, boolean changeScale) {
        if (changeScale) {
            this.scale = scale;
        }
        coreHeight = coreHeight / 1.5D * scale;
        convectionHeight = convectionHeight / 1.5D * scale;
        torusWidth = torusWidth / 1.5D * scale;
        rollerSize = rollerSize / 1.5D * scale;
        return this;
    }

    public NuclearMushroomCloudParticle setType(int type) {
        colorType = type;
        return this;
    }

    public double getSimulationSpeed() {
        int maxAge = getMaxAge();
        int simSlow = maxAge / 4;
        int simStop = maxAge / 2;
        if (age > simStop) {
            return 0.0D;
        }
        if (age > simSlow) {
            return 1.0D - (age - simSlow) / (double) (simStop - simSlow);
        }
        return 1.0D;
    }

    public double getScale() {
        return scale;
    }

    public double getGreying() {
        int maxAge = getMaxAge();
        int greying = maxAge * 3 / 4;
        if (age > greying) {
            return 1.0D + (age - greying) / (double) (maxAge - greying);
        }
        return 1.0D;
    }

    public float getCloudAlpha() {
        int maxAge = getMaxAge();
        int fadeOut = maxAge * 3 / 4;
        if (age > fadeOut) {
            float fac = (age - fadeOut) / (float) (maxAge - fadeOut);
            return 1.0F - fac;
        }
        return 1.0F;
    }

    public int getMaxAge() {
        return (int) (900.0D * getScale());
    }

    @Override
    public void remove() {
        ACTIVE.remove(this);
        super.remove();
    }

    private void renderTorex(PoseStack poseStack, Camera camera, float partialTicks, MultiBufferSource.BufferSource buffer) {
        poseStack.pushPose();
        Vec3 camPos = camera.getPosition();
        poseStack.translate(x - camPos.x, y - camPos.y, z - camPos.z);
        VertexConsumer cloudConsumer = buffer.getBuffer(CLOUDLET_RENDER_TYPE);
        Matrix4f matrix = poseStack.last().pose();
        for (Cloudlet cloudlet : cloudlets) {
            Vec3 vec = cloudlet.getInterpPos(partialTicks);
            renderCloudlet(matrix, cloudConsumer, (float) (vec.x - x), (float) (vec.y - y), (float) (vec.z - z), cloudlet, partialTicks, camera);
        }
        if (age < 101) {
            VertexConsumer flashConsumer = buffer.getBuffer(FLASH_RENDER_TYPE);
            double renderAge = Math.min(age + partialTicks, 100.0F);
            float alpha = (float) ((100.0D - renderAge) / 100.0D);
            Random flashRandom = new Random(hashCode());
            for (int i = 0; i < 3; i++) {
                float px = (float) (flashRandom.nextGaussian() * 0.5D * rollerSize);
                float py = (float) (flashRandom.nextGaussian() * 0.5D * rollerSize);
                float pz = (float) (flashRandom.nextGaussian() * 0.5D * rollerSize);
                renderFlash(matrix, flashConsumer, px, (float) (py + coreHeight), pz, (float) (25.0D * rollerSize), alpha, camera);
            }
        }
        poseStack.popPose();
    }

    private void renderCloudlet(Matrix4f matrix, VertexConsumer consumer, float px, float py, float pz, Cloudlet cloud, float partialTicks, Camera camera) {
        float alpha = cloud.getAlpha();
        float cloudScale = cloud.getScale();
        Vector3f left = new Vector3f(camera.getLeftVector()).mul(cloudScale);
        Vector3f up = new Vector3f(camera.getUpVector()).mul(cloudScale);
        float brightness = cloud.type == TorexType.CONDENSATION ? 0.9F : 0.75F * cloud.colorMod;
        Vec3 interpColor = cloud.getInterpColor(partialTicks);
        int red = Mth.clamp((int) (interpColor.x * brightness * 255.0D), 0, 255);
        int green = Mth.clamp((int) (interpColor.y * brightness * 255.0D), 0, 255);
        int blue = Mth.clamp((int) (interpColor.z * brightness * 255.0D), 0, 255);
        int alphaInt = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        renderBillboard(matrix, consumer, px, py, pz, left, up, red, green, blue, alphaInt);
    }

    private void renderFlash(Matrix4f matrix, VertexConsumer consumer, float px, float py, float pz, float flashScale, float alpha, Camera camera) {
        Vector3f left = new Vector3f(camera.getLeftVector()).mul(flashScale);
        Vector3f up = new Vector3f(camera.getUpVector()).mul(flashScale);
        renderBillboard(matrix, consumer, px, py, pz, left, up, 255, 255, 255, Mth.clamp((int) (alpha * 255.0F), 0, 255));
    }

    private void renderBillboard(Matrix4f matrix, VertexConsumer consumer, float px, float py, float pz, Vector3f left, Vector3f up, int red, int green, int blue, int alpha) {
        int overlay = OverlayTexture.NO_OVERLAY;
        consumer.addVertex(matrix, px - left.x() - up.x(), py - left.y() - up.y(), pz - left.z() - up.z()).setColor(red, green, blue, alpha).setUv(1.0F, 1.0F).setOverlay(overlay).setNormal(0.0F, 1.0F, 0.0F).setLight(240);
        consumer.addVertex(matrix, px - left.x() + up.x(), py - left.y() + up.y(), pz - left.z() + up.z()).setColor(red, green, blue, alpha).setUv(1.0F, 0.0F).setOverlay(overlay).setNormal(0.0F, 1.0F, 0.0F).setLight(240);
        consumer.addVertex(matrix, px + left.x() + up.x(), py + left.y() + up.y(), pz + left.z() + up.z()).setColor(red, green, blue, alpha).setUv(0.0F, 0.0F).setOverlay(overlay).setNormal(0.0F, 1.0F, 0.0F).setLight(240);
        consumer.addVertex(matrix, px + left.x() - up.x(), py + left.y() - up.y(), pz + left.z() - up.z()).setColor(red, green, blue, alpha).setUv(0.0F, 1.0F).setOverlay(overlay).setNormal(0.0F, 1.0F, 0.0F).setLight(240);
    }

    @EventBusSubscriber(modid = Createnucleartech.MODID, value = Dist.CLIENT)
    public static final class Renderer {
        private Renderer() {
        }

        @SubscribeEvent
        public static void render(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE.isEmpty()) {
                return;
            }
            MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
            float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            Iterator<NuclearMushroomCloudParticle> iterator = ACTIVE.iterator();
            while (iterator.hasNext()) {
                NuclearMushroomCloudParticle particle = iterator.next();
                if (particle.removed) {
                    iterator.remove();
                    continue;
                }
                particle.renderTorex(event.getPoseStack(), event.getCamera(), partialTicks, buffer);
            }
            buffer.endBatch(CLOUDLET_RENDER_TYPE);
            buffer.endBatch(FLASH_RENDER_TYPE);
        }
    }

    public enum TorexType {
        STANDARD,
        SHOCK,
        RING,
        CONDENSATION
    }

    public class Cloudlet {
        public double posX;
        public double posY;
        public double posZ;
        public double prevPosX;
        public double prevPosY;
        public double prevPosZ;
        public double motionX;
        public double motionY;
        public double motionZ;
        public int age;
        public int cloudletLife;
        public float angle;
        public boolean isDead;
        float rangeMod;
        public float colorMod;
        public Vec3 color;
        public Vec3 prevColor;
        public TorexType type;
        private float startingScale = 1.0F;
        private float growingScale = 5.0F;
        private double motionMult = 1.0D;

        public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge) {
            this(posX, posY, posZ, angle, age, maxAge, TorexType.STANDARD);
        }

        public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge, TorexType type) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.prevPosX = posX;
            this.prevPosY = posY;
            this.prevPosZ = posZ;
            this.age = age;
            this.cloudletLife = maxAge;
            this.angle = angle;
            this.rangeMod = 0.3F + random.nextFloat() * 0.7F;
            this.colorMod = 0.8F + random.nextFloat() * 0.2F;
            this.type = type;
            updateColor();
            this.prevColor = this.color;
        }

        private void update() {
            age++;
            if (age > cloudletLife) {
                isDead = true;
            }
            prevPosX = posX;
            prevPosY = posY;
            prevPosZ = posZ;
            Vec3 simPos = new Vec3(NuclearMushroomCloudParticle.this.x - posX, 0.0D, NuclearMushroomCloudParticle.this.z - posZ);
            double simPosX = NuclearMushroomCloudParticle.this.x + simPos.length();
            double simPosZ = NuclearMushroomCloudParticle.this.z;
            if (type == TorexType.STANDARD) {
                Vec3 convection = getConvectionMotion(simPosX, simPosZ);
                Vec3 lift = getLiftMotion(simPosX);
                double factor = Mth.clamp((posY - NuclearMushroomCloudParticle.this.y) / coreHeight, 0.0D, 1.0D);
                motionX = convection.x * factor + lift.x * (1.0D - factor);
                motionY = convection.y * factor + lift.y * (1.0D - factor);
                motionZ = convection.z * factor + lift.z * (1.0D - factor);
            } else if (type == TorexType.SHOCK) {
                double factor = Mth.clamp((posY - NuclearMushroomCloudParticle.this.y) / coreHeight, 0.0D, 1.0D);
                Vec3 motion = new Vec3(1.0D, 0.0D, 0.0D).yRot(angle);
                motionX = motion.x * factor;
                motionY = motion.y * factor;
                motionZ = motion.z * factor;
            } else if (type == TorexType.RING) {
                Vec3 motion = getRingMotion(simPosX, simPosZ);
                motionX = motion.x;
                motionY = motion.y;
                motionZ = motion.z;
            } else if (type == TorexType.CONDENSATION) {
                Vec3 motion = getCondensationMotion();
                motionX = motion.x;
                motionY = motion.y;
                motionZ = motion.z;
            }
            double mult = motionMult * getSimulationSpeed() * SIMULATION_SPEED;
            posX += motionX * mult;
            posY += motionY * mult;
            posZ += motionZ * mult;
            updateColor();
        }

        private Vec3 getCondensationMotion() {
            Vec3 delta = new Vec3(posX - NuclearMushroomCloudParticle.this.x, 0.0D, posZ - NuclearMushroomCloudParticle.this.z);
            double speed = Math.min(0.0022D, 4.0E-6D * NuclearMushroomCloudParticle.this.age);
            return new Vec3(delta.x * speed, 0.0D, delta.z * speed);
        }

        private Vec3 getRingMotion(double simPosX, double simPosZ) {
            if (simPosX > NuclearMushroomCloudParticle.this.x + torusWidth * 2.0D) {
                return Vec3.ZERO;
            }
            Vec3 torusPos = new Vec3(NuclearMushroomCloudParticle.this.x + torusWidth, NuclearMushroomCloudParticle.this.y + coreHeight * 0.5D, NuclearMushroomCloudParticle.this.z);
            Vec3 delta = new Vec3(torusPos.x - simPosX, torusPos.y - posY, torusPos.z - simPosZ);
            double roller = rollerSize * rangeMod * 0.25D;
            double dist = delta.length() / roller - 1.0D;
            if (Math.abs(dist) < 1.0E-4D) {
                dist = 1.0E-4D;
            }
            double func = 1.0D - Math.pow(Math.E, -dist);
            float rotAngle = (float) (func * Math.PI * 0.5D);
            Vec3 rot = new Vec3(-delta.x / dist, -delta.y / dist, -delta.z / dist).zRot(rotAngle);
            Vec3 motion = new Vec3(torusPos.x + rot.x - simPosX, torusPos.y + rot.y - posY, torusPos.z + rot.z - simPosZ);
            motion = new Vec3(motion.x * 0.001D, motion.y * 0.001D, motion.z * 0.001D).yRot(angle).normalize();
            return motion;
        }

        private Vec3 getConvectionMotion(double simPosX, double simPosZ) {
            Vec3 torusPos = new Vec3(NuclearMushroomCloudParticle.this.x + torusWidth, NuclearMushroomCloudParticle.this.y + coreHeight, NuclearMushroomCloudParticle.this.z);
            Vec3 delta = new Vec3(torusPos.x - simPosX, torusPos.y - posY, torusPos.z - simPosZ);
            double roller = rollerSize * rangeMod;
            double dist = delta.length() / roller - 1.0D;
            if (Math.abs(dist) < 1.0E-4D) {
                dist = 1.0E-4D;
            }
            double func = 1.0D - Math.pow(Math.E, -dist);
            float rotAngle = (float) (func * Math.PI * 0.5D);
            Vec3 rot = new Vec3(-delta.x / dist, -delta.y / dist, -delta.z / dist).zRot(rotAngle);
            return new Vec3(torusPos.x + rot.x - simPosX, torusPos.y + rot.y - posY, torusPos.z + rot.z - simPosZ).yRot(angle).normalize();
        }

        private Vec3 getLiftMotion(double simPosX) {
            double localScale = Mth.clamp(1.0D - (simPosX - (NuclearMushroomCloudParticle.this.x + torusWidth)), 0.0D, 1.0D);
            Vec3 motion = new Vec3(NuclearMushroomCloudParticle.this.x - posX, NuclearMushroomCloudParticle.this.y + convectionHeight - posY, NuclearMushroomCloudParticle.this.z - posZ).normalize();
            return new Vec3(motion.x * localScale, motion.y * localScale, motion.z * localScale);
        }

        private void updateColor() {
            prevColor = color;
            double exX = NuclearMushroomCloudParticle.this.x;
            double exY = NuclearMushroomCloudParticle.this.y + coreHeight;
            double exZ = NuclearMushroomCloudParticle.this.z;
            double distX = exX - posX;
            double distY = exY - posY;
            double distZ = exZ - posZ;
            double heatSafe = Math.max(heat, 0.001D);
            double dist = Math.sqrt((distX * distX + distY * distY + distZ * distZ) / heatSafe);
            dist = Math.max(dist, 1.0D);
            double col = 2.0D / dist;
            if (colorType == 1) {
                color = new Vec3(Math.max(col * 1.0D, 0.25D), Math.max(col * 2.0D, 0.25D), Math.max(col * 0.5D, 0.25D));
            } else if (colorType == 2) {
                Color colorHsb = Color.getHSBColor(angle / 2.0F / (float) Math.PI, 1.0F, 1.0F);
                color = type == TorexType.RING
                        ? new Vec3(Math.max(col, 0.25D), Math.max(col, 0.25D), Math.max(col, 0.25D))
                        : new Vec3(colorHsb.getRed() / 255.0D, colorHsb.getGreen() / 255.0D, colorHsb.getBlue() / 255.0D);
            } else {
                color = new Vec3(Math.max(col * 2.0D, 0.25D), Math.max(col * 1.5D, 0.25D), Math.max(col * 0.5D, 0.25D));
            }
            if (prevColor == null) {
                prevColor = color;
            }
        }

        public Vec3 getInterpPos(float partialTicks) {
            Vec3 base = new Vec3(prevPosX + (posX - prevPosX) * partialTicks, prevPosY + (posY - prevPosY) * partialTicks, prevPosZ + (posZ - prevPosZ) * partialTicks);
            if (type != TorexType.SHOCK) {
                double localScale = getScale();
                double px = (base.x - NuclearMushroomCloudParticle.this.x) * localScale + NuclearMushroomCloudParticle.this.x;
                double py = (base.y - NuclearMushroomCloudParticle.this.y) * localScale + NuclearMushroomCloudParticle.this.y;
                double pz = (base.z - NuclearMushroomCloudParticle.this.z) * localScale + NuclearMushroomCloudParticle.this.z;
                base = new Vec3(px, py, pz);
            }
            return base;
        }

        public Vec3 getInterpColor(float partialTicks) {
            if (type == TorexType.CONDENSATION) {
                return new Vec3(1.0D, 1.0D, 1.0D);
            }
            double greying = getGreying();
            if (type == TorexType.RING) {
                greying += 1.0D;
            }
            return new Vec3(
                    (prevColor.x + (color.x - prevColor.x) * partialTicks) * greying,
                    (prevColor.y + (color.y - prevColor.y) * partialTicks) * greying,
                    (prevColor.z + (color.z - prevColor.z) * partialTicks) * greying
            );
        }

        public float getAlpha() {
            float alpha = (1.0F - age / (float) cloudletLife) * getCloudAlpha();
            if (type == TorexType.CONDENSATION) {
                alpha *= 0.25F;
            }
            return alpha;
        }

        public float getScale() {
            float base = startingScale + age / (float) cloudletLife * growingScale;
            if (type != TorexType.SHOCK) {
                base *= (float) NuclearMushroomCloudParticle.this.getScale();
            }
            return base;
        }

        public Cloudlet setScale(float start, float grow) {
            startingScale = start;
            growingScale = grow;
            return this;
        }

        public Cloudlet setMotion(double mult) {
            motionMult = mult;
            return this;
        }
    }
}
