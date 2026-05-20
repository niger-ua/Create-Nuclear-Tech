package cattodream.createnucleartech.explosion;

import cattodream.createnucleartech.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HbmNukeExplosionRayBatched {
    private final Map<ChunkPos, List<BlockPos>> directPerChunk = new HashMap<>();
    private final List<ChunkPos> directOrderedChunks = new ArrayList<>();
    private final Map<ChunkPos, List<Vec3>> perChunk = new HashMap<>();
    private final List<ChunkPos> orderedChunks = new ArrayList<>();
    private final Level level;
    private final int posX;
    private final int posY;
    private final int posZ;
    private final int strength;
    private final int speed;
    private final int length;
    private final int rayCountMax;
    private final int volumeRadius;
    private final int jetHeight;
    private int volumeCursorX;
    private int volumeCursorY;
    private int volumeCursorZ;
    private int rayCount = 1;
    private int removedTotal;
    private double rayTheta = Math.PI;
    private double rayPhi;
    private boolean volumeCollectionComplete;
    private boolean rayCollectionComplete;

    public HbmNukeExplosionRayBatched(Level level, int x, int y, int z, int strength, int speed, int length) {
        this.level = level;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.strength = strength;
        this.speed = speed;
        this.length = length;
        this.rayCountMax = Math.max(1, (int) (Math.PI * 2.5D * strength * strength));
        this.volumeRadius = Math.max(16, Math.min(length, (int) Math.round(length * 0.68D)));
        this.jetHeight = Math.max(volumeRadius, length + volumeRadius / 2);
        this.volumeCursorX = -volumeRadius;
        this.volumeCursorY = -volumeRadius;
        this.volumeCursorZ = -volumeRadius;
    }

    public boolean isComplete() {
        return volumeCollectionComplete && directPerChunk.isEmpty() && rayCollectionComplete && perChunk.isEmpty();
    }

    public void cacheChunksTick(int workBudget) {
        if (!volumeCollectionComplete) {
            collectVolume(Math.max(1, workBudget) * 4096);
            return;
        }
        if (rayCollectionComplete) {
            return;
        }
        rayCollectionComplete = true;
    }

    public void destructionTick(long deadlineNanos, int maxChunks) {
        if (!volumeCollectionComplete) {
            return;
        }
        int chunks = 0;
        while (!directPerChunk.isEmpty() && !directOrderedChunks.isEmpty() && chunks < maxChunks && System.nanoTime() < deadlineNanos) {
            processDirectChunk(directOrderedChunks.removeFirst());
            chunks++;
        }
        while (rayCollectionComplete && !perChunk.isEmpty() && !orderedChunks.isEmpty() && chunks < maxChunks && System.nanoTime() < deadlineNanos) {
            processChunk(orderedChunks.removeFirst());
            chunks++;
        }
    }

    public void cancel() {
        volumeCollectionComplete = true;
        rayCollectionComplete = true;
        directPerChunk.clear();
        directOrderedChunks.clear();
        perChunk.clear();
        orderedChunks.clear();
    }

    private void collectVolume(int count) {
        int processed = 0;
        while (volumeCursorY <= jetHeight) {
            int dx = volumeCursorX;
            int dy = volumeCursorY;
            int dz = volumeCursorZ;

            int x = posX + dx;
            int y = posY + dy;
            int z = posZ + dz;
            if (isInsideMainSphere(dx, dy, dz) || isInsideUpwardJet(dx, dy, dz)) {
                if (y > level.getMinBuildHeight() && y < level.getMaxBuildHeight()) {
                    BlockPos pos = new BlockPos(x, y, z);
                    directPerChunk.computeIfAbsent(new ChunkPos(x >> 4, z >> 4), ignored -> new ArrayList<>()).add(pos);
                }
            }

            advanceVolumeCursor();
            if (++processed >= count) {
                return;
            }
        }

        directOrderedChunks.addAll(directPerChunk.keySet());
        directOrderedChunks.sort(Comparator.comparingInt(this::chunkDistance));
        volumeCollectionComplete = true;
        rayCollectionComplete = true;
    }

    private boolean isInsideMainSphere(int dx, int dy, int dz) {
        double radius = volumeRadius;
        double distance = dx * dx + dy * dy + dz * dz;
        return distance <= radius * radius;
    }

    private boolean isInsideUpwardJet(int dx, int dy, int dz) {
        if (dy <= 0 || dy > jetHeight) {
            return false;
        }
        double progress = dy / (double) jetHeight;
        double radius = Mth.lerp(progress, Math.max(7.0D, volumeRadius * 0.22D), Math.max(2.0D, volumeRadius * 0.055D));
        double wobble = 1.0D + 0.16D * Math.sin(dy * 0.35D);
        return dx * dx + dz * dz <= radius * radius * wobble;
    }

    private void advanceVolumeCursor() {
        volumeCursorX++;
        if (volumeCursorX > volumeRadius) {
            volumeCursorX = -volumeRadius;
            volumeCursorZ++;
        }
        if (volumeCursorZ > volumeRadius) {
            volumeCursorZ = -volumeRadius;
            volumeCursorY++;
        }
    }

    private void processDirectChunk(ChunkPos chunk) {
        List<BlockPos> list = directPerChunk.remove(chunk);
        if (list == null || list.isEmpty()) {
            return;
        }

        int removedThisTick = 0;
        int totalCap = Math.max(Config.nuclearBombMaxBlocksRemoved, volumeRadius * volumeRadius * volumeRadius * 5);
        int tickCap = Math.max(Config.nuclearBombMaxBlocksRemovedPerTick, volumeRadius * 180);
        for (BlockPos pos : list) {
            if (removedTotal >= totalCap || removedThisTick >= tickCap) {
                directPerChunk.put(chunk, list.subList(list.indexOf(pos), list.size()));
                directOrderedChunks.addFirst(chunk);
                break;
            }
            BlockState state = level.getBlockState(pos);
            if (canRemove(state, pos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                removedTotal++;
                removedThisTick++;
            }
        }
    }

    private void collectTips(int count) {
        int processed = 0;
        while (rayCountMax >= rayCount) {
            Vec3 vec = getSphericalCartesian();
            int maxLength = (int) Math.ceil(strength);
            float energy = strength;
            Vec3 lastSolid = null;
            Set<ChunkPos> touchedChunks = new HashSet<>();

            for (int i = 0; i < maxLength && i <= length; i++) {
                float x = (float) (posX + vec.x * i);
                float y = (float) (posY + vec.y * i);
                float z = (float) (posZ + vec.z * i);
                int blockX = Mth.floor(x);
                int blockY = Mth.floor(y);
                int blockZ = Mth.floor(z);
                if (blockY <= level.getMinBuildHeight() || blockY >= level.getMaxBuildHeight()) {
                    break;
                }

                BlockPos pos = new BlockPos(blockX, blockY, blockZ);
                BlockState state = level.getBlockState(pos);
                if (state.getFluidState().isEmpty()) {
                    energy -= Math.pow(masqueradeResistance(state, pos), 1.35D);
                }
                if (energy > 0.0F && !state.isAir()) {
                    lastSolid = new Vec3(x, y, z);
                    touchedChunks.add(new ChunkPos(blockX >> 4, blockZ >> 4));
                }
                if (energy <= 0.0F || i + 1 >= length || i == maxLength - 1) {
                    break;
                }
            }

            if (lastSolid != null) {
                for (ChunkPos chunk : touchedChunks) {
                    perChunk.computeIfAbsent(chunk, ignored -> new ArrayList<>()).add(lastSolid);
                }
            }

            generateNextRay();
            if (++processed >= count) {
                return;
            }
        }

        orderedChunks.addAll(perChunk.keySet());
        orderedChunks.sort(Comparator.comparingInt(this::chunkDistance));
        rayCollectionComplete = true;
    }

    private void processChunk(ChunkPos chunk) {
        List<Vec3> list = perChunk.remove(chunk);
        if (list == null || list.isEmpty()) {
            return;
        }

        Set<BlockPos> toRemove = new HashSet<>();
        Set<BlockPos> tipBlocks = new HashSet<>();
        int chunkX = chunk.x;
        int chunkZ = chunk.z;
        int enter = Math.min(Math.abs(posX - (chunkX << 4)), Math.abs(posZ - (chunkZ << 4))) - 16;
        enter = Math.max(enter, 0);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        rayLoop:
        for (Vec3 tip : list) {
            Vec3 ray = new Vec3(tip.x - posX, tip.y - posY, tip.z - posZ);
            double rayLength = ray.length();
            if (rayLength <= 0.0D) {
                continue;
            }
            Vec3 direction = ray.scale(1.0D / rayLength);
            int tipX = Mth.floor(tip.x);
            int tipY = Mth.floor(tip.y);
            int tipZ = Mth.floor(tip.z);
            boolean inChunk = false;

            for (int i = enter; i < rayLength; i++) {
                int x = Mth.floor(posX + direction.x * i);
                int y = Mth.floor(posY + direction.y * i);
                int z = Mth.floor(posZ + direction.z * i);
                if (x >> 4 != chunkX || z >> 4 != chunkZ) {
                    if (inChunk) {
                        continue rayLoop;
                    }
                    continue;
                }
                inChunk = true;
                mutable.set(x, y, z);
                BlockState state = level.getBlockState(mutable);
                if (canRemove(state, mutable)) {
                    BlockPos pos = mutable.immutable();
                    if (x == tipX && y == tipY && z == tipZ) {
                        tipBlocks.add(pos);
                    }
                    toRemove.add(pos);
                }
            }
        }

        int removedThisTick = 0;
        for (BlockPos pos : toRemove) {
            int totalCap = Math.max(Config.nuclearBombMaxBlocksRemoved, length * length * 120);
            int tickCap = Math.max(Config.nuclearBombMaxBlocksRemovedPerTick, length * 120);
            if (removedTotal >= totalCap || removedThisTick >= tickCap) {
                break;
            }
            if (tipBlocks.contains(pos)) {
                handleTip(pos);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            removedTotal++;
            removedThisTick++;
        }
    }

    private void handleTip(BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            NuclearAftermath.scorchTip(serverLevel, pos, level.random, 0.75D);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private float masqueradeResistance(BlockState state, BlockPos pos) {
        if (state.isAir()) {
            return 0.0F;
        }
        if (state.is(Blocks.SANDSTONE)) {
            return Blocks.STONE.defaultBlockState().getExplosionResistance(level, pos, null);
        }
        if (state.is(Blocks.OBSIDIAN)) {
            return Blocks.STONE.defaultBlockState().getExplosionResistance(level, pos, null) * 3.0F;
        }
        return state.getExplosionResistance(level, pos, null);
    }

    private boolean canRemove(BlockState state, BlockPos pos) {
        return !state.isAir() && !state.is(Blocks.BEDROCK) && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private void generateNextRay() {
        if (rayCount < rayCountMax) {
            int k = rayCount + 1;
            double hk = -1.0D + 2.0D * (k - 1.0D) / (rayCountMax - 1.0D);
            rayTheta = Math.acos(hk);
            rayPhi = (rayPhi + 3.6D / Math.sqrt(rayCountMax) / Math.sqrt(1.0D - hk * hk)) % (Math.PI * 2.0D);
        } else {
            rayTheta = 0.0D;
            rayPhi = 0.0D;
        }
        rayCount++;
    }

    private Vec3 getSphericalCartesian() {
        double x = Math.sin(rayTheta) * Math.cos(rayPhi);
        double z = Math.sin(rayTheta) * Math.sin(rayPhi);
        double y = Math.cos(rayTheta);
        return new Vec3(x, y, z);
    }

    private int chunkDistance(ChunkPos chunk) {
        int centerChunkX = posX >> 4;
        int centerChunkZ = posZ >> 4;
        return Math.abs(centerChunkX - chunk.x) + Math.abs(centerChunkZ - chunk.z);
    }
}
