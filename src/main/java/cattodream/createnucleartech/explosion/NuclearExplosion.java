package cattodream.createnucleartech.explosion;

import cattodream.createnucleartech.Config;
import cattodream.createnucleartech.radiation.RadiationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class NuclearExplosion {
    private NuclearExplosion() {
    }

    public static void detonate(ServerLevel level, BlockPos origin) {
        Vec3 center = Vec3.atCenterOf(origin);
        int craterRadius = Config.nuclearBombCraterRadius;

        HbmNukeExplosionEntity.spawn(level, craterRadius, center.x, center.y, center.z);
        contaminateFallout(level, origin, craterRadius);
    }

    private static void contaminateFallout(ServerLevel level, BlockPos origin, int craterRadius) {
        RadiationData data = RadiationData.get(level);
        int falloutRadius = Math.max(Config.nuclearBombFalloutRadiusChunks, 30);
        double centralFallout = Math.max(1.0D, Config.nuclearBombFalloutStrength) * 450.0D;
        data.addRadialFallout(origin, falloutRadius, centralFallout);
        data.registerSource(level, origin, centralFallout * 4.0D, Math.max(128.0D, craterRadius * 3.0D), 1.0D);
    }
}
