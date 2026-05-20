package cattodream.createnucleartech.radiation;

import cattodream.createnucleartech.CNTTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class ContainmentScanner {
    private ContainmentScanner() {
    }

    public static ContainmentResult scan(ServerLevel level, BlockPos source, int radius, double radiationStrength) {
        int requiredConcrete = requiredConcreteThickness(radiationStrength);
        int leadFaces = 0;
        int shieldedFaces = 0;
        int closestWall = radius;
        double leak = 0.0D;

        for (Direction direction : Direction.values()) {
            RayResult ray = scanRay(level, source, direction, radius, requiredConcrete);
            if (ray.status == ContainmentStatus.BLOCKED) {
                leadFaces++;
                shieldedFaces++;
            } else if (ray.status == ContainmentStatus.CONTAINED) {
                shieldedFaces++;
            }
            closestWall = Math.min(closestWall, ray.distance);
            leak += ray.leakFactor;
        }

        ContainmentStatus status;
        if (leadFaces == Direction.values().length) {
            status = ContainmentStatus.BLOCKED;
        } else if (shieldedFaces == Direction.values().length) {
            status = ContainmentStatus.CONTAINED;
        } else {
            status = ContainmentStatus.LEAKING;
        }

        int volumeRadius = Math.max(2, closestWall);
        double leakFactor = status == ContainmentStatus.LEAKING ? Math.max(0.05D, leak / Direction.values().length) : 0.0D;
        return new ContainmentResult(status, volumeRadius, leakFactor, leadFaces, shieldedFaces);
    }

    public static ContainmentStatus evaluateLine(ServerLevel level, BlockPos from, BlockPos to, double radiationStrength) {
        int steps = Math.max(1, Math.max(Math.abs(to.getX() - from.getX()), Math.max(Math.abs(to.getY() - from.getY()), Math.abs(to.getZ() - from.getZ()))));
        int concrete = 0;
        int requiredConcrete = requiredConcreteThickness(radiationStrength);
        for (int step = 1; step <= steps; step++) {
            double t = step / (double) steps;
            BlockPos pos = BlockPos.containing(
                    from.getX() + (to.getX() - from.getX()) * t,
                    from.getY() + (to.getY() - from.getY()) * t,
                    from.getZ() + (to.getZ() - from.getZ()) * t
            );
            BlockState state = level.getBlockState(pos);
            if (isLead(state)) {
                return ContainmentStatus.BLOCKED;
            }
            if (isConcrete(state)) {
                concrete++;
                if (concrete >= requiredConcrete) {
                    return ContainmentStatus.CONTAINED;
                }
            } else if (!state.isAir()) {
                concrete = Math.max(0, concrete - 1);
            }
        }
        return ContainmentStatus.LEAKING;
    }

    public static double lineTransmission(ServerLevel level, BlockPos from, BlockPos to, double radiationStrength) {
        int steps = Math.max(1, Math.max(Math.abs(to.getX() - from.getX()), Math.max(Math.abs(to.getY() - from.getY()), Math.abs(to.getZ() - from.getZ()))));
        int concrete = 0;
        int requiredConcrete = requiredConcreteThickness(radiationStrength);
        double transmission = 1.0D;

        for (int step = 1; step < steps; step++) {
            double t = step / (double) steps;
            BlockPos pos = BlockPos.containing(
                    from.getX() + (to.getX() - from.getX()) * t,
                    from.getY() + (to.getY() - from.getY()) * t,
                    from.getZ() + (to.getZ() - from.getZ()) * t
            );
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                concrete = 0;
                continue;
            }
            if (isLead(state)) {
                return 0.0D;
            }
            if (isConcrete(state)) {
                concrete++;
                transmission *= 0.55D;
                if (concrete >= requiredConcrete) {
                    return 0.0D;
                }
            } else {
                concrete = 0;
                if (isPartialShield(state)) {
                    transmission *= 0.72D;
                } else {
                    transmission *= 0.88D;
                }
            }
            if (transmission <= 0.01D) {
                return 0.0D;
            }
        }
        return transmission;
    }

    private static RayResult scanRay(ServerLevel level, BlockPos source, Direction direction, int radius, int requiredConcrete) {
        int concrete = 0;
        double attenuation = 0.0D;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int distance = 1; distance <= radius; distance++) {
            cursor.setWithOffset(source, direction.getStepX() * distance, direction.getStepY() * distance, direction.getStepZ() * distance);
            BlockState state = level.getBlockState(cursor);
            if (isLead(state)) {
                return new RayResult(ContainmentStatus.BLOCKED, distance, 0.0D);
            }
            if (isConcrete(state)) {
                concrete++;
                if (concrete >= requiredConcrete) {
                    return new RayResult(ContainmentStatus.CONTAINED, distance, 0.0D);
                }
                continue;
            }
            if (isPartialShield(state)) {
                attenuation += 0.22D;
            } else if (!state.isAir() && state.getFluidState().isEmpty()) {
                attenuation += 0.08D;
            }
        }
        return new RayResult(ContainmentStatus.LEAKING, radius, Math.max(0.05D, 1.0D - attenuation));
    }

    private static int requiredConcreteThickness(double radiationStrength) {
        if (radiationStrength >= 80.0D) {
            return 5;
        }
        if (radiationStrength >= 30.0D) {
            return 4;
        }
        return 3;
    }

    private static boolean isLead(BlockState state) {
        return state.is(CNTTags.Blocks.LEAD_RADIATION_SHIELDING);
    }

    private static boolean isConcrete(BlockState state) {
        return state.is(CNTTags.Blocks.CONCRETE_RADIATION_SHIELDING);
    }

    private static boolean isPartialShield(BlockState state) {
        return state.is(CNTTags.Blocks.PARTIAL_RADIATION_SHIELDING) || state.is(CNTTags.Blocks.RADIATION_SHIELDING);
    }

    private record RayResult(ContainmentStatus status, int distance, double leakFactor) {
    }
}
