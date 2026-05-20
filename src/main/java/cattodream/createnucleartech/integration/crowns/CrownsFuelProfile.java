package cattodream.createnucleartech.integration.crowns;

import com.rae.crowns.content.nuclear.fuel_assembly.AssemblyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Locale;
import java.util.Map;

/**
 * Gameplay layer for Crowns fuel assemblies.
 *
 * Crowns keeps the actual heat/fission engine. This class only classifies our
 * richer fuel recipes into neutron spectra and startup behavior, so fertile or
 * hard-to-start fuels need a neighboring driver assembly instead of acting like
 * every rod is the same isotope mix.
 */
public record CrownsFuelProfile(
        String id,
        String displayName,
        float fastYield,
        float mediumYield,
        float slowYield,
        float activityMultiplier,
        boolean selfStarting,
        float starterStrength,
        float requiredStarterFlux
) {
    public static final String PROFILE_KEY = "createnucleartech:profile";
    public static final String TH232_KEY = "createnucleartech:th232";
    public static final String PU240_KEY = "createnucleartech:pu240";

    private static final ResourceLocation U235 = ResourceLocation.fromNamespaceAndPath("crowns", "u235");
    private static final ResourceLocation U238 = ResourceLocation.fromNamespaceAndPath("crowns", "u238");
    private static final ResourceLocation P239 = ResourceLocation.fromNamespaceAndPath("crowns", "p239");

    public static final CrownsFuelProfile INERT = new CrownsFuelProfile("inert", "Inert", 0.0F, 0.0F, 0.0F, 0.0F, false, 0.0F, 1.0F);
    public static final CrownsFuelProfile NATURAL_URANIUM = new CrownsFuelProfile("natural_uranium", "Natural U", 0.45F, 0.85F, 1.35F, 0.82F, true, 0.35F, 0.0F);
    public static final CrownsFuelProfile ENRICHED_URANIUM = new CrownsFuelProfile("enriched_uranium", "Enriched U", 0.80F, 1.18F, 1.34F, 1.10F, true, 0.85F, 0.0F);
    public static final CrownsFuelProfile MILITARY_URANIUM = new CrownsFuelProfile("military_uranium", "HEU", 1.18F, 1.12F, 1.02F, 1.28F, true, 1.15F, 0.0F);
    public static final CrownsFuelProfile MOX = new CrownsFuelProfile("mox", "MOX", 1.02F, 1.30F, 1.08F, 1.18F, true, 0.65F, 0.0F);
    public static final CrownsFuelProfile PLUTONIUM_239 = new CrownsFuelProfile("plutonium_239", "Pu-239", 1.42F, 1.10F, 0.62F, 1.38F, false, 0.12F, 0.18F);
    public static final CrownsFuelProfile REACTOR_GRADE_PLUTONIUM = new CrownsFuelProfile("reactor_grade_plutonium", "Reactor Pu", 1.32F, 0.96F, 0.55F, 1.46F, false, 0.18F, 0.14F);
    public static final CrownsFuelProfile THORIUM = new CrownsFuelProfile("thorium", "Th-232", 0.20F, 0.75F, 1.48F, 0.62F, false, 0.0F, 0.28F);
    public static final CrownsFuelProfile SPENT_FUEL = new CrownsFuelProfile("spent", "Spent Fuel", 0.10F, 0.08F, 0.04F, 1.85F, false, 0.0F, 2.0F);

    public static CrownsFuelProfile from(Map<ResourceLocation, Double> composition, String override) {
        return from(composition, override, 0.0D, 0.0D);
    }

    public static CrownsFuelProfile from(Map<ResourceLocation, Double> composition, String override, double th232, double pu240) {
        CrownsFuelProfile overridden = byId(override);
        if (overridden != null) {
            return overridden;
        }
        if (th232 > 0.0D) {
            return THORIUM;
        }
        if (composition == null || composition.isEmpty()) {
            return INERT;
        }

        double u235 = amount(composition, U235);
        double u238 = amount(composition, U238);
        double p239 = amount(composition, P239);

        if (pu240 >= 0.18D && p239 > 0.0D) {
            return REACTOR_GRADE_PLUTONIUM;
        }
        if (p239 >= 0.90D) {
            return PLUTONIUM_239;
        }
        if (p239 >= 0.55D) {
            return REACTOR_GRADE_PLUTONIUM;
        }
        if (p239 >= 0.08D && u238 >= 0.35D) {
            return MOX;
        }
        if (u235 >= 0.70D) {
            return MILITARY_URANIUM;
        }
        if (u235 >= 0.08D) {
            return ENRICHED_URANIUM;
        }
        if (u235 > 0.0D || u238 > 0.0D) {
            return NATURAL_URANIUM;
        }
        return INERT;
    }

    public boolean isStarted(Level level, BlockPos pos) {
        return selfStarting || starterFlux(level, pos) >= requiredStarterFlux;
    }

    public float startupMultiplier(Level level, BlockPos pos) {
        if (selfStarting) {
            return 1.0F;
        }
        float flux = starterFlux(level, pos);
        if (flux <= 0.0F) {
            return 0.08F;
        }
        return Math.min(1.0F, 0.08F + (flux / Math.max(0.01F, requiredStarterFlux)) * 0.92F);
    }

    public static float starterFlux(Level level, BlockPos pos) {
        if (level == null) {
            return 0.0F;
        }
        float flux = 0.0F;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            BlockEntity blockEntity = level.getBlockEntity(neighbor);
            if (!(blockEntity instanceof AssemblyBlockEntity assembly)) {
                continue;
            }
            CrownsNeutronDiagnostics.Snapshot snapshot = CrownsNeutronDiagnostics.snapshotFor(assembly);
            CrownsFuelProfile profile = snapshot == null
                    ? ENRICHED_URANIUM
                    : byIdOrInert(snapshot.profileId());
            float activeBonus = snapshot == null ? 0.25F : Math.min(1.25F, snapshot.activity() * 0.002F + Math.max(0.0F, snapshot.k() - 1.0F));
            flux += profile.starterStrength * (0.35F + activeBonus);
        }
        return flux;
    }

    public static CrownsFuelProfile byIdOrInert(String id) {
        CrownsFuelProfile profile = byId(id);
        return profile == null ? INERT : profile;
    }

    public static CrownsFuelProfile byId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "natural_uranium" -> NATURAL_URANIUM;
            case "enriched_uranium", "mid_enriched_uranium" -> ENRICHED_URANIUM;
            case "military_uranium", "heu" -> MILITARY_URANIUM;
            case "mox" -> MOX;
            case "plutonium_239", "weapon_plutonium" -> PLUTONIUM_239;
            case "reactor_grade_plutonium" -> REACTOR_GRADE_PLUTONIUM;
            case "thorium", "thorium_232" -> THORIUM;
            case "spent", "spent_fuel" -> SPENT_FUEL;
            case "inert" -> INERT;
            default -> null;
        };
    }

    private static double amount(Map<ResourceLocation, Double> composition, ResourceLocation key) {
        return composition.getOrDefault(key, 0.0D);
    }
}
