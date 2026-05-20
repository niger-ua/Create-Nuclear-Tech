package cattodream.createnucleartech.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class NuclearSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float startSize;
    private final float growMultiplier;
    private final boolean hot;
    private final float startR;
    private final float startG;
    private final float startB;
    private final float coolGrey;
    private final float maxAlpha;

    protected NuclearSmokeParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites, float baseSize, int baseLifetime, float growMultiplier, boolean hot) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.startSize = baseSize * (0.75F + random.nextFloat() * 0.55F);
        this.growMultiplier = growMultiplier;
        this.hot = hot;
        this.quadSize = startSize;
        this.lifetime = Math.max(30, baseLifetime + random.nextInt(Math.max(1, baseLifetime / 2)));
        this.friction = 0.94F;
        this.gravity = -0.012F;
        this.hasPhysics = false;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        this.coolGrey = 0.24F + random.nextFloat() * 0.16F;
        if (hot) {
            this.startR = 1.0F;
            this.startG = 0.18F + random.nextFloat() * 0.28F;
            this.startB = 0.0F + random.nextFloat() * 0.03F;
            this.maxAlpha = 0.56F;
        } else {
            this.startR = coolGrey * 1.04F;
            this.startG = coolGrey * 1.02F;
            this.startB = coolGrey;
            this.maxAlpha = 0.24F;
        }
        this.rCol = startR;
        this.gCol = startG;
        this.bCol = startB;
        this.alpha = maxAlpha;
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        float progress = age / (float) lifetime;
        this.quadSize = startSize * (1.0F + progress * growMultiplier);
        this.alpha = progress < 0.65F ? maxAlpha : Math.max(0.0F, maxAlpha * (1.0F - (progress - 0.65F) / 0.35F));
        if (hot) {
            float cool = Mth.clamp((progress - 0.68F) / 0.32F, 0.0F, 1.0F);
            float ember = (float) Math.sin((age + startSize) * 0.35F) * 0.08F;
            this.rCol = Mth.clamp(Mth.lerp(cool, startR + ember, 0.45F), 0.0F, 1.0F);
            this.gCol = Mth.clamp(Mth.lerp(cool, startG + ember * 0.4F, 0.18F + coolGrey * 0.6F), 0.0F, 1.0F);
            this.bCol = Mth.clamp(Mth.lerp(cool, startB, coolGrey * 0.5F), 0.0F, 1.0F);
        }
        this.yd += 0.0012D;
        setSpriteFromAge(sprites);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final float baseSize;
        private final int baseLifetime;
        private final float growMultiplier;
        private final boolean hot;

        private Provider(SpriteSet sprites, float baseSize, int baseLifetime, float growMultiplier, boolean hot) {
            this.sprites = sprites;
            this.baseSize = baseSize;
            this.baseLifetime = baseLifetime;
            this.growMultiplier = growMultiplier;
            this.hot = hot;
        }

        public static Provider grey(SpriteSet sprites, float baseSize, int baseLifetime, float growMultiplier) {
            return new Provider(sprites, baseSize, baseLifetime, growMultiplier, false);
        }

        public static Provider hot(SpriteSet sprites, float baseSize, int baseLifetime, float growMultiplier) {
            return new Provider(sprites, baseSize, baseLifetime, growMultiplier, true);
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new NuclearSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, baseSize, baseLifetime, growMultiplier, hot);
        }
    }
}
