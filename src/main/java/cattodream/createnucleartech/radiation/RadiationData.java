package cattodream.createnucleartech.radiation;

import cattodream.createnucleartech.Config;
import cattodream.createnucleartech.Createnucleartech;
import cattodream.createnucleartech.explosion.NuclearAftermath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RadiationData extends SavedData {
    private static final String DATA_NAME = Createnucleartech.MODID + "_radiation";
    private static final double MIN_STORED_RADIATION = 0.01D;
    private static final SavedData.Factory<RadiationData> FACTORY = new SavedData.Factory<>(RadiationData::new, RadiationData::load);

    private final Map<Long, Double> chunkRadiation = new HashMap<>();
    private final Map<Long, RadiationVolume> volumes = new HashMap<>();
    private int terrainCursor;

    public static RadiationData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static RadiationData load(CompoundTag tag, HolderLookup.Provider registries) {
        RadiationData data = new RadiationData();
        ListTag chunks = tag.getList("Chunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < chunks.size(); i++) {
            CompoundTag chunk = chunks.getCompound(i);
            double radiation = chunk.getDouble("Radiation");
            if (radiation > MIN_STORED_RADIATION) {
                data.chunkRadiation.put(chunk.getLong("Chunk"), radiation);
            }
        }
        ListTag volumeTags = tag.getList("Volumes", Tag.TAG_COMPOUND);
        for (int i = 0; i < volumeTags.size(); i++) {
            CompoundTag volumeTag = volumeTags.getCompound(i);
            RadiationVolume volume = RadiationVolume.load(volumeTag);
            if (volume.energy > MIN_STORED_RADIATION) {
                data.volumes.put(volume.pos.asLong(), volume);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag chunks = new ListTag();
        chunkRadiation.forEach((chunkPos, radiation) -> {
            CompoundTag chunk = new CompoundTag();
            chunk.putLong("Chunk", chunkPos);
            chunk.putDouble("Radiation", radiation);
            chunks.add(chunk);
        });
        tag.put("Chunks", chunks);
        ListTag volumeTags = new ListTag();
        volumes.values().forEach(volume -> volumeTags.add(volume.save()));
        tag.put("Volumes", volumeTags);
        return tag;
    }

    public double radiationAt(BlockPos pos) {
        return radiationAt(new ChunkPos(pos)) + volumeRadiationAt(pos);
    }

    public double chunkRadiationAt(BlockPos pos) {
        return radiationAt(new ChunkPos(pos));
    }

    public double radiationAt(ServerLevel level, BlockPos pos) {
        return radiationAt(new ChunkPos(pos)) + volumeRadiationAt(level, pos);
    }

    public double radiationAt(ChunkPos pos) {
        return chunkRadiation.getOrDefault(pos.toLong(), 0.0D);
    }

    public void addRadiation(BlockPos pos, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        addRadiation(new ChunkPos(pos), amount);
    }

    public void leakRadiation(BlockPos pos, float amount) {
        addRadiation(pos, amount);
    }

    public void leakRadiation(BlockPos pos, double amount) {
        addRadiation(pos, amount);
    }

    public ContainmentResult registerSource(ServerLevel level, BlockPos pos, double strength, double radius, double containmentSensitivity) {
        if (strength <= 0.0D || radius <= 0.0D) {
            return new ContainmentResult(ContainmentStatus.LEAKING, 0, 0.0D, 0, 0);
        }

        int scanRadius = Math.min(Config.radiationContainmentRadius, Math.max(4, (int) Math.ceil(radius)));
        ContainmentResult containment = ContainmentScanner.scan(level, pos, scanRadius, strength * Math.max(0.25D, containmentSensitivity));
        RadiationVolume volume = volumes.computeIfAbsent(pos.asLong(), ignored -> new RadiationVolume(pos.immutable()));
        volume.radius = Math.max(2, containment.volumeRadius());
        volume.status = containment.status();
        volume.leakFactor = containment.leakFactor();
        volume.energy = Math.min(100000.0D, volume.energy + strength * (containment.fullyContained() ? Config.radiationContainedBuildup : 0.65D));
        volume.lastUpdatedGameTime = level.getGameTime();

        if (!containment.fullyContained()) {
            addRadiation(pos, strength * containment.leakFactor() * Config.radiationChunkLeakScale);
        } else {
            setDirty();
        }
        return containment;
    }

    public List<SourceView> nearbySources(BlockPos observer, int maxDistance, int limit) {
        int maxDistanceSqr = maxDistance * maxDistance;
        return volumes.values().stream()
                .filter(volume -> volume.energy > MIN_STORED_RADIATION)
                .filter(volume -> volume.pos.distSqr(observer) <= maxDistanceSqr)
                .map(volume -> new SourceView(volume.pos, contribution(volume, observer), volume.radius, volume.status, volume.leakFactor))
                .filter(view -> view.intensity() > MIN_STORED_RADIATION)
                .sorted(Comparator.comparingDouble(SourceView::intensity).reversed())
                .limit(limit)
                .toList();
    }

    public void addRadiation(ChunkPos pos, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        chunkRadiation.merge(pos.toLong(), amount, Double::sum);
        setDirty();
    }

    public void addRadialFallout(BlockPos center, int radiusChunks, double centralStrength) {
        ChunkPos centerChunk = new ChunkPos(center);
        for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
            for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance <= radiusChunks) {
                    double falloff = 1.0D - distance / Math.max(1.0D, radiusChunks);
                    addRadiation(new ChunkPos(centerChunk.x + dx, centerChunk.z + dz), centralStrength * falloff * falloff);
                }
            }
        }
    }

    public void simulate() {
        simulate(null);
    }

    public void simulate(ServerLevel level) {
        boolean changed = simulateVolumes();
        if (level != null) {
            changed |= blackenIrradiatedTerrain(level);
        }

        Map<Long, Double> additions = new HashMap<>();
        Iterator<Map.Entry<Long, Double>> iterator = chunkRadiation.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Double> entry = iterator.next();
            long packed = entry.getKey();
            ChunkPos pos = new ChunkPos(packed);
            double current = entry.getValue();
            double spreadEach = current * Config.radiationSpreadFactor * 0.25D;
            double remaining = (current - spreadEach * 4.0D) * (1.0D - Config.radiationDecayRate);

            if (remaining <= MIN_STORED_RADIATION) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }

            if (spreadEach > MIN_STORED_RADIATION) {
                additions.merge(ChunkPos.asLong(pos.x + 1, pos.z), spreadEach, Double::sum);
                additions.merge(ChunkPos.asLong(pos.x - 1, pos.z), spreadEach, Double::sum);
                additions.merge(ChunkPos.asLong(pos.x, pos.z + 1), spreadEach, Double::sum);
                additions.merge(ChunkPos.asLong(pos.x, pos.z - 1), spreadEach, Double::sum);
            }
        }

        additions.forEach((chunk, amount) -> chunkRadiation.merge(chunk, amount, Double::sum));
        if (!additions.isEmpty() || changed || !chunkRadiation.isEmpty()) {
            setDirty();
        }
    }

    private boolean blackenIrradiatedTerrain(ServerLevel level) {
        if (chunkRadiation.isEmpty()) {
            terrainCursor = 0;
            return false;
        }

        List<Map.Entry<Long, Double>> entries = new ArrayList<>(chunkRadiation.entrySet());
        int processed = 0;
        boolean changed = false;
        terrainCursor = Math.floorMod(terrainCursor, entries.size());
        for (int offset = 0; offset < entries.size() && processed < 3; offset++) {
            Map.Entry<Long, Double> entry = entries.get((terrainCursor + offset) % entries.size());
            double radiation = entry.getValue();
            if (radiation < 18.0D) {
                continue;
            }
            ChunkPos chunk = new ChunkPos(entry.getKey());
            changed |= NuclearAftermath.contaminateChunkBiome(level, chunk);
            if (radiation >= 160.0D) {
                int samples = radiation >= 400.0D ? 6 : 2;
                changed |= NuclearAftermath.blackenChunk(level, chunk, radiation, samples);
            }
            processed++;
        }
        terrainCursor = (terrainCursor + Math.max(1, processed)) % entries.size();
        return changed;
    }

    private boolean simulateVolumes() {
        if (volumes.isEmpty()) {
            return false;
        }

        boolean changed = false;
        Iterator<Map.Entry<Long, RadiationVolume>> iterator = volumes.entrySet().iterator();
        while (iterator.hasNext()) {
            RadiationVolume volume = iterator.next().getValue();
            volume.energy *= 1.0D - Math.min(0.5D, Config.radiationDecayRate * 0.35D);
            if (volume.energy <= MIN_STORED_RADIATION) {
                iterator.remove();
                changed = true;
                continue;
            }
            if (volume.leakFactor > 0.0D) {
                addRadiation(volume.pos, volume.energy * volume.leakFactor * Config.radiationChunkLeakScale * 0.025D);
            }
            changed = true;
        }
        return changed;
    }

    private double volumeRadiationAt(BlockPos pos) {
        double total = 0.0D;
        for (RadiationVolume volume : volumes.values()) {
            total += contribution(volume, pos);
        }
        return total;
    }

    private double volumeRadiationAt(ServerLevel level, BlockPos pos) {
        double total = 0.0D;
        for (RadiationVolume volume : volumes.values()) {
            double contribution = contribution(volume, pos);
            if (contribution <= 0.0D) {
                continue;
            }
            double transmission = ContainmentScanner.lineTransmission(level, volume.pos, pos, volume.energy);
            if (transmission <= 0.0D) {
                continue;
            }
            total += contribution * transmission;
        }
        return total;
    }

    private static double contribution(RadiationVolume volume, BlockPos pos) {
        double distanceSqr = volume.pos.distSqr(pos);
        int radius = Math.max(1, volume.radius);
        if (distanceSqr > radius * radius) {
            return 0.0D;
        }
        double distance = Math.max(1.0D, Math.sqrt(distanceSqr));
        double falloff = 1.0D - distance / radius;
        double containmentBoost = volume.status == ContainmentStatus.LEAKING ? 0.45D : 1.0D;
        return volume.energy * falloff * falloff * containmentBoost / Math.sqrt(distance);
    }

    public record SourceView(BlockPos pos, double intensity, int radius, ContainmentStatus status, double leakFactor) {
    }

    private static final class RadiationVolume {
        private final BlockPos pos;
        private double energy;
        private int radius = 4;
        private double leakFactor;
        private long lastUpdatedGameTime;
        private ContainmentStatus status = ContainmentStatus.LEAKING;

        private RadiationVolume(BlockPos pos) {
            this.pos = pos;
        }

        private static RadiationVolume load(CompoundTag tag) {
            RadiationVolume volume = new RadiationVolume(BlockPos.of(tag.getLong("Pos")));
            volume.energy = tag.getDouble("Energy");
            volume.radius = tag.getInt("Radius");
            volume.leakFactor = tag.getDouble("LeakFactor");
            volume.lastUpdatedGameTime = tag.getLong("LastUpdated");
            try {
                volume.status = ContainmentStatus.valueOf(tag.getString("Status"));
            } catch (IllegalArgumentException ignored) {
                volume.status = ContainmentStatus.LEAKING;
            }
            return volume;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("Pos", pos.asLong());
            tag.putDouble("Energy", energy);
            tag.putInt("Radius", radius);
            tag.putDouble("LeakFactor", leakFactor);
            tag.putLong("LastUpdated", lastUpdatedGameTime);
            tag.putString("Status", status.name());
            return tag;
        }
    }
}
