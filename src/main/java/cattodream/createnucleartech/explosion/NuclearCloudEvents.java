package cattodream.createnucleartech.explosion;

import cattodream.createnucleartech.ModRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class NuclearCloudEvents {
    private NuclearCloudEvents() {
    }

    public static void addCloud(ServerLevel level, Vec3 center, int craterRadius) {
        forceParticles(level, ModRegistry.NUKE_MUSHROOM_CLOUD.get(), center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static <T extends ParticleOptions> void forceParticles(ServerLevel level, T particle, double x, double y, double z, int count, double xDist, double yDist, double zDist, double speed) {
        for (ServerPlayer player : level.players()) {
            level.sendParticles(player, particle, true, x, y, z, count, xDist, yDist, zDist, speed);
        }
    }
}
