package cattodream.createnucleartech.explosion;

import cattodream.createnucleartech.Createnucleartech;
import cattodream.createnucleartech.ModRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public final class NuclearAftermath {
    private static final int BOMB_CHARRED_LOG_RADIUS = 400;
    private static final int AFTERMATH_CHUNKS_PER_TICK = 2;
    private static final Queue<BombAftermathTask> BOMB_AFTERMATH_QUEUE = new ArrayDeque<>();
    private static final ResourceKey<Biome> FALLOUT_BIOME = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, "fallout")
    );

    private NuclearAftermath() {
    }

    public static boolean contaminateChunkBiome(ServerLevel level, ChunkPos chunkPos) {
        Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> fallout = biomeRegistry.getHolder(FALLOUT_BIOME).orElse(null);
        if (fallout == null) {
            return false;
        }

        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        boolean changed = false;
        for (LevelChunkSection section : chunk.getSections()) {
            if (section.getNoiseBiome(0, 0, 0).is(FALLOUT_BIOME)) {
                continue;
            }
            PalettedContainerRO<Holder<Biome>> biomes = section.getBiomes();
            if (biomes instanceof PalettedContainer<Holder<Biome>> writableBiomes) {
                writableBiomes.acquire();
                try {
                    for (int y = 0; y < 4; y++) {
                        for (int z = 0; z < 4; z++) {
                            for (int x = 0; x < 4; x++) {
                                writableBiomes.getAndSetUnchecked(x, y, z, fallout);
                            }
                        }
                    }
                    changed = true;
                } finally {
                    writableBiomes.release();
                }
            }
        }

        if (changed) {
            chunk.setUnsaved(true);
            level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
            killFalloutBiomeVegetation(level, chunkPos);
        }
        return changed;
    }

    public static boolean blackenChunk(ServerLevel level, ChunkPos chunk, double radiation, int samples) {
        if (radiation < 18.0D || samples <= 0) {
            return false;
        }
        boolean changed = false;
        RandomSource random = level.random;
        double severity = Mth.clamp(radiation / 220.0D, 0.08D, 1.0D);
        for (int i = 0; i < samples; i++) {
            int x = chunk.getMinBlockX() + random.nextInt(16);
            int z = chunk.getMinBlockZ() + random.nextInt(16);
            BlockPos surface = surfacePos(level, x, z);
            if (surface != null && convertColumn(level, surface, severity, random)) {
                changed = true;
            }
        }
        if (radiation >= 160.0D) {
            changed |= killContaminatedCanopy(level, chunk, Math.min(Math.max(samples * 4, 12), 48), severity);
        }
        return changed;
    }

    public static void applyBombFalloutTerrain(ServerLevel level, BlockPos center, int craterRadius, int deadRadius, int biomeRadius) {
        int maxAftermathRadius = Math.max(biomeRadius, BOMB_CHARRED_LOG_RADIUS);
        int minChunkX = (center.getX() - maxAftermathRadius) >> 4;
        int maxChunkX = (center.getX() + maxAftermathRadius) >> 4;
        int minChunkZ = (center.getZ() - maxAftermathRadius) >> 4;
        int maxChunkZ = (center.getZ() + maxAftermathRadius) >> 4;
        int craterRadiusSqr = craterRadius * craterRadius;
        int deadRadiusSqr = deadRadius * deadRadius;
        int biomeRadiusSqr = biomeRadius * biomeRadius;
        int charredLogRadiusSqr = BOMB_CHARRED_LOG_RADIUS * BOMB_CHARRED_LOG_RADIUS;

        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
                boolean falloutChunk = chunkIntersectsCircle(chunk, center, biomeRadiusSqr);
                boolean charredChunk = chunkIntersectsCircle(chunk, center, charredLogRadiusSqr);
                if (falloutChunk || charredChunk) {
                    BOMB_AFTERMATH_QUEUE.add(new BombAftermathTask(
                            level.dimension(),
                            center.immutable(),
                            chunk,
                            craterRadiusSqr,
                            deadRadiusSqr,
                            biomeRadiusSqr,
                            charredLogRadiusSqr,
                            falloutChunk,
                            charredChunk
                    ));
                }
            }
        }
    }

    public static void tick(ServerLevel level) {
        if (BOMB_AFTERMATH_QUEUE.isEmpty()) {
            return;
        }
        int processed = 0;
        int checked = BOMB_AFTERMATH_QUEUE.size();
        while (processed < AFTERMATH_CHUNKS_PER_TICK && checked-- > 0) {
            BombAftermathTask task = BOMB_AFTERMATH_QUEUE.poll();
            if (task == null) {
                return;
            }
            if (!task.dimension.equals(level.dimension())) {
                BOMB_AFTERMATH_QUEUE.add(task);
                continue;
            }
            processBombAftermathChunk(level, task);
            processed++;
        }
    }

    private static void processBombAftermathChunk(ServerLevel level, BombAftermathTask task) {
        if (!level.hasChunk(task.chunk.x, task.chunk.z)) {
            return;
        }
        if (task.falloutChunk) {
            contaminateChunkBiome(level, task.chunk);
            if (chunkIntersectsCircle(task.chunk, task.center, task.deadRadiusSqr)) {
                ruinBombChunk(level, task.chunk, task.center, task.craterRadiusSqr, task.deadRadiusSqr);
            }
        }
        if (task.charredChunk) {
            charBombLogs(level, task.chunk, task.center, task.charredLogRadiusSqr);
        }
    }

    public static void drainCraterWater(ServerLevel level, BlockPos center, int radius) {
        int radiusSqr = radius * radius;
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - radius - 24);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + radius + 48);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dz * dz > radiusSqr) {
                    continue;
                }
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                for (int y = minY; y <= maxY; y++) {
                    mutable.set(x, y, z);
                    if (!level.getFluidState(mutable).isEmpty()) {
                        level.setBlock(mutable, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static boolean chunkIntersectsCircle(ChunkPos chunk, BlockPos center, int radiusSqr) {
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        int closestX = Mth.clamp(center.getX(), minX, maxX);
        int closestZ = Mth.clamp(center.getZ(), minZ, maxZ);
        int dx = closestX - center.getX();
        int dz = closestZ - center.getZ();
        return dx * dx + dz * dz <= radiusSqr;
    }

    private static void ruinBombChunk(ServerLevel level, ChunkPos chunk, BlockPos center, int craterRadiusSqr, int deadRadiusSqr) {
        RandomSource random = level.random;
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int x = chunk.getMinBlockX() + localX;
                int z = chunk.getMinBlockZ() + localZ;
                int dx = x - center.getX();
                int dz = z - center.getZ();
                int distSqr = dx * dx + dz * dz;
                if (distSqr > deadRadiusSqr) {
                    continue;
                }

                BlockPos surface = surfacePos(level, x, z);
                if (surface != null) {
                    if (distSqr <= craterRadiusSqr) {
                        convertCraterStone(level, surface, 1.0D, random);
                    } else {
                        convertDeadSurface(level, surface);
                    }
                    killColumnVegetation(level, x, z, distSqr < deadRadiusSqr / 2);
                }
            }
        }
    }

    private static void convertDeadSurface(ServerLevel level, BlockPos surface) {
        for (int dy = 0; dy >= -2; dy--) {
            BlockPos pos = surface.offset(0, dy, 0);
            BlockState state = level.getBlockState(pos);
            if (isEarth(state)) {
                level.setBlock(pos, ModRegistry.WASTE_EARTH.get().defaultBlockState(), 2);
                return;
            }
        }
    }

    private static void killColumnVegetation(ServerLevel level, int x, int z, boolean charLogs) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = top; y >= Math.max(level.getMinBuildHeight(), top - 48); y--) {
            mutable.set(x, y, z);
            BlockState state = level.getBlockState(mutable);
            if (state.isAir()) {
                continue;
            }
            if (state.is(BlockTags.LEAVES)) {
                level.setBlock(mutable, ModRegistry.DEAD_LEAVES.get().defaultBlockState(), 2);
            } else if (isSmallPlant(state)) {
                level.setBlock(mutable, Blocks.AIR.defaultBlockState(), 2);
            } else if (state.is(BlockTags.LOGS) && charLogs) {
                level.setBlock(mutable, copyAxis(state, ModRegistry.CHARRED_LOG.get().defaultBlockState()), 2);
            }
        }
    }

    private static void charBombLogs(ServerLevel level, ChunkPos chunk, BlockPos center, int radiusSqr) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int x = chunk.getMinBlockX() + localX;
                int z = chunk.getMinBlockZ() + localZ;
                int dx = x - center.getX();
                int dz = z - center.getZ();
                if (dx * dx + dz * dz > radiusSqr) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = top; y >= Math.max(level.getMinBuildHeight(), top - 96); y--) {
                    mutable.set(x, y, z);
                    BlockState state = level.getBlockState(mutable);
                    if (state.is(BlockTags.LOGS)) {
                        level.setBlock(mutable, copyAxis(state, ModRegistry.CHARRED_LOG.get().defaultBlockState()), 2);
                    }
                }
            }
        }
    }

    private static void killFalloutBiomeVegetation(ServerLevel level, ChunkPos chunk) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int x = chunk.getMinBlockX() + localX;
                int z = chunk.getMinBlockZ() + localZ;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = top; y >= Math.max(level.getMinBuildHeight(), top - 32); y--) {
                    mutable.set(x, y, z);
                    BlockState state = level.getBlockState(mutable);
                    if (state.isAir()) {
                        continue;
                    }
                    if (state.is(BlockTags.LEAVES)) {
                        level.setBlock(mutable, ModRegistry.DEAD_LEAVES.get().defaultBlockState(), 2);
                    } else if (isSmallPlant(state)) {
                        level.setBlock(mutable, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    public static void scatterBombAftermath(ServerLevel level, BlockPos center, int radius, int samples) {
        if (radius <= 0 || samples <= 0) {
            return;
        }
        RandomSource random = level.random;
        for (int i = 0; i < samples; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            int x = center.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = center.getZ() + Mth.floor(Math.sin(angle) * distance);
            BlockPos surface = surfacePos(level, x, z);
            if (surface == null) {
                continue;
            }
            double falloff = 1.0D - Math.min(1.0D, distance / Math.max(1.0D, radius));
            double severity = Mth.clamp(0.18D + falloff * falloff, 0.08D, 1.0D);
            convertCraterStone(level, surface, severity, random);
        }
    }

    private static void convertCraterStone(ServerLevel level, BlockPos surface, double severity, RandomSource random) {
        for (int dy = 1; dy >= -3; dy--) {
            BlockPos pos = surface.offset(0, dy, 0);
            BlockState replacement = craterAftermathState(level.getBlockState(pos), random, severity);
            if (replacement != null) {
                level.setBlock(pos, replacement, 3);
                return;
            }
        }
    }

    public static void scorchTip(ServerLevel level, BlockPos pos, RandomSource random, double severity) {
        BlockState state = level.getBlockState(pos);
        BlockState replacement = craterAshState(state, random, severity);
        if (replacement != null) {
            level.setBlock(pos, replacement, 3);
            return;
        }

        BlockPos floor = findFloor(level, pos);
        if (floor != null) {
            convertCraterStone(level, floor, severity, random);
        }
    }

    public static boolean clearBlastCanopy(ServerLevel level, BlockPos center, int radius, int samples) {
        if (radius <= 0 || samples <= 0) {
            return false;
        }
        boolean changed = false;
        RandomSource random = level.random;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int i = 0; i < samples; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            int x = center.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = center.getZ() + Mth.floor(Math.sin(angle) * distance);
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            double falloff = 1.0D - Math.min(1.0D, distance / Math.max(1.0D, radius));
            for (int y = top; y >= Math.max(level.getMinBuildHeight(), top - 44); y--) {
                mutable.set(x, y, z);
                BlockState state = level.getBlockState(mutable);
                if (state.isAir()) {
                    continue;
                }
                if (state.is(BlockTags.LEAVES)) {
                    level.setBlock(mutable, falloff > 0.34D ? Blocks.AIR.defaultBlockState() : ModRegistry.DEAD_LEAVES.get().defaultBlockState(), 3);
                    changed = true;
                } else if (state.is(BlockTags.LOGS)) {
                    if (falloff > 0.72D) {
                        level.setBlock(mutable, Blocks.AIR.defaultBlockState(), 3);
                    } else {
                        level.setBlock(mutable, copyAxis(state, ModRegistry.CHARRED_LOG.get().defaultBlockState()), 3);
                    }
                    changed = true;
                    break;
                } else if (!state.isAir()) {
                    break;
                }
            }
        }
        return changed;
    }

    private static boolean convertColumn(ServerLevel level, BlockPos surface, double severity, RandomSource random) {
        boolean changed = false;
        for (int dy = 0; dy >= -2; dy--) {
            BlockPos pos = surface.offset(0, dy, 0);
            BlockState replacement = aftermathState(level.getBlockState(pos), random, severity);
            if (replacement != null) {
                level.setBlock(pos, replacement, 3);
                changed = true;
                break;
            }
        }
        scorchOrganicNeighbors(level, surface, random, severity);
        return changed;
    }

    private static void scorchOrganicNeighbors(ServerLevel level, BlockPos origin, RandomSource random, double severity) {
        int radius = severity > 0.45D ? 3 : 2;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 8 + (int) (severity * 14.0D); i++) {
            mutable.set(
                    origin.getX() + random.nextInt(radius * 2 + 1) - radius,
                    origin.getY() + random.nextInt(8) - 1,
                    origin.getZ() + random.nextInt(radius * 2 + 1) - radius
            );
            BlockState state = level.getBlockState(mutable);
            BlockState replacement = organicAftermathState(state);
            if (replacement != null && random.nextDouble() < 0.35D + severity * 0.55D) {
                level.setBlock(mutable, replacement, 3);
            }
        }
    }

    private static boolean killContaminatedCanopy(ServerLevel level, ChunkPos chunk, int samples, double severity) {
        boolean changed = false;
        RandomSource random = level.random;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int i = 0; i < samples; i++) {
            int x = chunk.getMinBlockX() + random.nextInt(16);
            int z = chunk.getMinBlockZ() + random.nextInt(16);
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            int depth = severity > 0.6D ? 36 : 22;
            for (int y = top; y >= Math.max(level.getMinBuildHeight(), top - depth); y--) {
                mutable.set(x, y, z);
                BlockState state = level.getBlockState(mutable);
                if (state.is(BlockTags.LEAVES)) {
                    level.setBlock(mutable, ModRegistry.DEAD_LEAVES.get().defaultBlockState(), 3);
                    changed = true;
                } else if (state.is(BlockTags.LOGS) && random.nextDouble() < 0.15D + severity * 0.65D) {
                    level.setBlock(mutable, copyAxis(state, ModRegistry.CHARRED_LOG.get().defaultBlockState()), 3);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static BlockState aftermathState(BlockState state, RandomSource random, double severity) {
        if (state.isAir() || state.is(Blocks.BEDROCK)) {
            return null;
        }
        if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GLASS)) {
            return ModRegistry.ASH_BLOCK.get().defaultBlockState();
        }
        if (isEarth(state)) {
            if (severity > 0.78D && random.nextDouble() < 0.28D) {
                return ModRegistry.ASH_BLOCK.get().defaultBlockState();
            }
            return ModRegistry.WASTE_EARTH.get().defaultBlockState();
        }
        if (isStone(state) && severity > 0.72D && random.nextDouble() < severity * 0.65D) {
            return ModRegistry.ASH_BLOCK.get().defaultBlockState();
        }
        return null;
    }

    private static BlockState craterAftermathState(BlockState state, RandomSource random, double severity) {
        if (state.isAir() || state.is(Blocks.BEDROCK)) {
            return null;
        }
        if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GLASS) || isEarth(state)) {
            return ModRegistry.ASH_BLOCK.get().defaultBlockState();
        }
        if (isStone(state) && random.nextDouble() < 0.2D + severity * 0.6D) {
            return ModRegistry.ASH_BLOCK.get().defaultBlockState();
        }
        return null;
    }

    private static BlockState craterAshState(BlockState state, RandomSource random, double severity) {
        if (state.isAir() || state.is(Blocks.BEDROCK)) {
            return null;
        }
        if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GLASS) || isEarth(state) || isStone(state)) {
            return ModRegistry.ASH_BLOCK.get().defaultBlockState();
        }
        return random.nextDouble() < severity * 0.18D ? ModRegistry.ASH_BLOCK.get().defaultBlockState() : null;
    }

    private static BlockState organicAftermathState(BlockState state) {
        if (state.is(BlockTags.LEAVES)) {
            return ModRegistry.DEAD_LEAVES.get().defaultBlockState();
        }
        if (state.is(BlockTags.LOGS)) {
            return copyAxis(state, ModRegistry.CHARRED_LOG.get().defaultBlockState());
        }
        if (state.is(BlockTags.PLANKS)) {
            return ModRegistry.CHARRED_PLANKS.get().defaultBlockState();
        }
        return null;
    }

    private static boolean isSmallPlant(BlockState state) {
        return state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CROPS)
                || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.SEAGRASS)
                || state.is(Blocks.TALL_SEAGRASS)
                || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.BAMBOO)
                || state.is(Blocks.BAMBOO_SAPLING)
                || state.is(Blocks.VINE)
                || state.is(Blocks.GLOW_LICHEN)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.TORCHFLOWER)
                || state.is(Blocks.PITCHER_PLANT);
    }

    private static BlockState copyAxis(BlockState source, BlockState replacement) {
        if (source.hasProperty(RotatedPillarBlock.AXIS) && replacement.hasProperty(RotatedPillarBlock.AXIS)) {
            return replacement.setValue(RotatedPillarBlock.AXIS, source.getValue(RotatedPillarBlock.AXIS));
        }
        return replacement;
    }

    private record BombAftermathTask(
            ResourceKey<Level> dimension,
            BlockPos center,
            ChunkPos chunk,
            int craterRadiusSqr,
            int deadRadiusSqr,
            int biomeRadiusSqr,
            int charredLogRadiusSqr,
            boolean falloutChunk,
            boolean charredChunk
    ) {
    }

    private static boolean isEarth(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.FARMLAND)
                || state.is(Blocks.DIRT_PATH)
                || state.is(Blocks.MUD)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.GRAVEL);
    }

    private static boolean isStone(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.CALCITE);
    }

    private static BlockPos surfacePos(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        if (y <= level.getMinBuildHeight()) {
            return null;
        }
        return new BlockPos(x, y, z);
    }

    private static BlockPos findFloor(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < 8 && mutable.getY() > level.getMinBuildHeight(); i++) {
            if (!level.getBlockState(mutable).isAir()) {
                return mutable.immutable();
            }
            mutable.move(0, -1, 0);
        }
        return null;
    }
}
