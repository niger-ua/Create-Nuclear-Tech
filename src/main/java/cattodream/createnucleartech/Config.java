package cattodream.createnucleartech;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Createnucleartech.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.DoubleValue RADIATION_DECAY_RATE = BUILDER
            .comment("Fraction of radiation lost from each contaminated chunk per simulation step.")
            .defineInRange("radiation.decayRate", 0.035D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue RADIATION_SPREAD_FACTOR = BUILDER
            .comment("Fraction of a chunk's radiation pressure sent to direct neighbor chunks per simulation step.")
            .defineInRange("radiation.spreadFactor", 0.06D, 0.0D, 0.5D);
    private static final ModConfigSpec.IntValue RADIATION_TICK_INTERVAL = BUILDER
            .comment("Server ticks between chunk radiation diffusion/decay updates.")
            .defineInRange("radiation.tickInterval", 20, 1, 200);
    private static final ModConfigSpec.DoubleValue RADIATION_ENTITY_DAMAGE_THRESHOLD = BUILDER
            .comment("Effective radiation level above which living entities begin taking damage.")
            .defineInRange("radiation.entityDamageThreshold", 1.5D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue RADIOACTIVE_ITEM_STRENGTH = BUILDER
            .comment("Base radiation emitted by radioactive material items carried by entities.")
            .defineInRange("radiation.radioactiveItemStrength", 0.45D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue RADIATION_CHUNK_LEAK_SCALE = BUILDER
            .comment("How much local uncontained source radiation becomes weak chunk contamination.")
            .defineInRange("radiation.chunkLeakScale", 0.04D, 0.0D, 10.0D);
    private static final ModConfigSpec.DoubleValue PLAYER_RADIATION_DECAY = BUILDER
            .comment("Radiation level removed from each living entity every exposure update when exposure is lower than decay.")
            .defineInRange("radiation.playerDecayPerUpdate", 0.12D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue URANIUM_RADIATION = BUILDER
            .comment("Per-item local radiation strength for external items tagged createnucleartech:uranium.")
            .defineInRange("radiation.material.uranium", 0.35D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue CRUSHED_URANIUM_RADIATION = BUILDER
            .comment("Per-item local radiation strength for external items tagged createnucleartech:uranium_crushed.")
            .defineInRange("radiation.material.crushedUranium", 0.6D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue URANIUM_DUST_RADIATION = BUILDER
            .comment("Base radiation strength for nuclear fuel made from uranium dust.")
            .defineInRange("radiation.material.uraniumDust", 2.2D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue NUCLEAR_WASTE_RADIATION = BUILDER
            .comment("Per-item radiation strength for items tagged createnucleartech:nuclear_waste.")
            .defineInRange("radiation.material.nuclearWaste", 8.0D, 0.0D, 2000.0D);
    private static final ModConfigSpec.DoubleValue RADIATION_EFFECT_LOW = BUILDER
            .comment("Accumulated radiation level where the Radiation mob effect starts.")
            .defineInRange("radiation.effect.lowThreshold", 12.0D, 0.0D, 100000.0D);
    private static final ModConfigSpec.DoubleValue RADIATION_EFFECT_MEDIUM = BUILDER
            .comment("Accumulated radiation level where Radiation reaches amplifier 1.")
            .defineInRange("radiation.effect.mediumThreshold", 45.0D, 0.0D, 100000.0D);
    private static final ModConfigSpec.DoubleValue RADIATION_EFFECT_HIGH = BUILDER
            .comment("Accumulated radiation level where Radiation reaches amplifier 2.")
            .defineInRange("radiation.effect.highThreshold", 140.0D, 0.0D, 100000.0D);
    private static final ModConfigSpec.DoubleValue RADIATION_EFFECT_EXTREME = BUILDER
            .comment("Accumulated radiation level where Radiation reaches amplifier 3 or above.")
            .defineInRange("radiation.effect.extremeThreshold", 320.0D, 0.0D, 100000.0D);
    private static final ModConfigSpec.DoubleValue RADIATION_EFFECT_SEVERE = BUILDER
            .comment("Accumulated radiation level where Radiation reaches amplifier 4.")
            .defineInRange("radiation.effect.severeThreshold", 700.0D, 0.0D, 1000000.0D);
    private static final ModConfigSpec.DoubleValue RADIATION_EFFECT_CRITICAL = BUILDER
            .comment("Accumulated radiation level where Radiation reaches amplifier 5.")
            .defineInRange("radiation.effect.criticalThreshold", 1200.0D, 0.0D, 1000000.0D);
    private static final ModConfigSpec.DoubleValue RADIATION_EFFECT_LETHAL = BUILDER
            .comment("Accumulated radiation level where non-invulnerable living entities are killed immediately.")
            .defineInRange("radiation.effect.lethalThreshold", 2400.0D, 0.0D, 10000000.0D);

    private static final ModConfigSpec.IntValue CREATE_RADIATION_SCAN_INTERVAL = BUILDER
            .comment("Server ticks between Create machine radiation scans near players.")
            .defineInRange("createIntegration.scanInterval", 40, 10, 200);
    private static final ModConfigSpec.IntValue CREATE_RADIATION_SCAN_RADIUS = BUILDER
            .comment("Block radius around each player scanned for Create machines with radioactive inventories.")
            .defineInRange("createIntegration.machineScanRadius", 32, 2, 96);
    private static final ModConfigSpec.DoubleValue CREATE_PIPE_LEAK = BUILDER
            .comment("Chunk radiation leaked by Create pipe collisions/spills involving radioactive fluids.")
            .defineInRange("createIntegration.pipeLeak", 1.2D, 0.0D, 1000.0D);
    private static final ModConfigSpec.IntValue CREATE_MAX_ROTATION_SPEED = BUILDER
            .comment("Maximum Create kinetic speed allowed by Create Nuclear Tech before steel-tier engineering is required.")
            .defineInRange("createIntegration.kinetics.maxRotationSpeed", 8192, 256, 32768);
    private static final ModConfigSpec.DoubleValue CREATE_WOODEN_COG_MAX_RPM = BUILDER
            .comment("Wooden Create cogwheels break above this RPM. Steel TFMG cogwheels are intended for higher speeds.")
            .defineInRange("createIntegration.kinetics.woodenCogwheelMaxRpm", 256.0D, 16.0D, 8192.0D);
    private static final ModConfigSpec.DoubleValue CREATE_WOODEN_COG_MAX_STRESS = BUILDER
            .comment("Wooden Create cogwheels break when their kinetic network exceeds this stress load.")
            .defineInRange("createIntegration.kinetics.woodenCogwheelMaxStress", 16384.0D, 0.0D, 100000000.0D);
    private static final ModConfigSpec.DoubleValue CREATE_STEEL_COG_STRESS = BUILDER
            .comment("Base stress impact consumed by one steel TFMG cogwheel.")
            .defineInRange("createIntegration.kinetics.steelCogwheelStressImpact", 2.0D, 0.0D, 1024.0D);
    private static final ModConfigSpec.DoubleValue CREATE_LARGE_STEEL_COG_STRESS = BUILDER
            .comment("Base stress impact consumed by one large steel TFMG cogwheel.")
            .defineInRange("createIntegration.kinetics.largeSteelCogwheelStressImpact", 4.0D, 0.0D, 1024.0D);
    private static final ModConfigSpec.DoubleValue CROWNS_ACTIVITY_LEAK_SCALE = BUILDER
            .comment("Multiplier applied to Create: Crowns radioactive source activity before leaking into this mod's chunk radiation field.")
            .defineInRange("crownsIntegration.activityLeakScale", 0.015D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue CROWNS_HOT_TEMPERATURE = BUILDER
            .comment("Crowns temperature in Kelvin above which hot nuclear machinery adds extra contamination pressure.")
            .defineInRange("crownsIntegration.hotTemperatureThreshold", 900.0D, 273.0D, 10000.0D);
    private static final ModConfigSpec.DoubleValue CROWNS_HEAT_LEAK_SCALE = BUILDER
            .comment("Chunk radiation added by overheated Crowns nuclear machinery per exposure scan.")
            .defineInRange("crownsIntegration.heatLeakScale", 0.25D, 0.0D, 1000.0D);
    private static final ModConfigSpec.IntValue RADIATION_CONTAINMENT_RADIUS = BUILDER
            .comment("Maximum block radius used for cached containment scans around Crowns radiation sources.")
            .defineInRange("radiation.containment.scanRadius", 18, 4, 48);
    private static final ModConfigSpec.DoubleValue RADIATION_CONTAINED_BUILDUP = BUILDER
            .comment("Multiplier for radiation buildup inside sealed containment volumes.")
            .defineInRange("radiation.containment.containedBuildup", 1.35D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue PLUTONIUM_EXPOSURE_THRESHOLD = BUILDER
            .comment("Accumulated irradiation required before tagged uranium materials transform into plutonium.")
            .defineInRange("radiation.irradiation.plutoniumExposureThreshold", 900.0D, 1.0D, 1000000.0D);
    private static final ModConfigSpec.DoubleValue IRRADIATION_COST_MULTIPLIER = BUILDER
            .comment("Global multiplier for plutonium breeding exposure requirements. Set high to make weapon plutonium an endgame process.")
            .defineInRange("radiation.irradiation.costMultiplier", 500.0D, 1.0D, 1000000.0D);
    private static final ModConfigSpec.DoubleValue PLUTONIUM_MIN_FIELD = BUILDER
            .comment("Minimum local radiation field needed for uranium-to-plutonium irradiation progress.")
            .defineInRange("radiation.irradiation.minimumFieldStrength", 8.0D, 0.0D, 100000.0D);
    private static final ModConfigSpec.IntValue RADIATION_SCANNER_RANGE = BUILDER
            .comment("Block radius used by Radiation Scanner Goggles.")
            .defineInRange("radiation.scanner.range", 96, 8, 192);

    private static final ModConfigSpec.DoubleValue HAZMAT_BASIC_PROTECTION = BUILDER
            .comment("Radiation reduction from a complete basic hazmat suit.")
            .defineInRange("hazmat.basicProtection", 0.55D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue HAZMAT_ADVANCED_PROTECTION = BUILDER
            .comment("Radiation reduction from a complete advanced (red cloth) hazmat suit.")
            .defineInRange("hazmat.advancedProtection", 0.78D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue HAZMAT_REINFORCED_PROTECTION = BUILDER
            .comment("Radiation reduction from a complete lead-reinforced (grey) hazmat suit.")
            .defineInRange("hazmat.reinforcedProtection", 0.92D, 0.0D, 1.0D);
    private static final ModConfigSpec.BooleanValue HAZMAT_SUITS_DEGRADE = BUILDER
            .comment("Hazmat armor loses durability while absorbing dangerous radiation.")
            .define("hazmat.suitsDegrade", true);
    private static final ModConfigSpec.DoubleValue HAZMAT_HANDLING_PROTECTION = BUILDER
            .comment("Minimum total radiation protection required to safely hold radioactive materials in hand.")
            .defineInRange("hazmat.minimumHandlingProtection", 0.80D, 0.0D, 1.0D);

    private static final ModConfigSpec.DoubleValue ANTIRADIN_DOSE_REDUCTION = BUILDER
            .comment("Accumulated body radiation removed by one antiradin dose.")
            .defineInRange("antiradin.doseReduction", 30.0D, 0.0D, 10000000.0D);
    private static final ModConfigSpec.IntValue ANTIRADIN_COOLDOWN = BUILDER
            .comment("Cooldown in ticks after using antiradin.")
            .defineInRange("antiradin.cooldownTicks", 200, 0, 72000);

    private static final ModConfigSpec.DoubleValue LEAD_BOX_MIN_FIELD = BUILDER
            .comment("Minimum radiation field required for the lead irradiation box to breed U-238 into weapon plutonium.")
            .defineInRange("leadIrradiationBox.minimumFieldStrength", 8.0D, 0.0D, 100000.0D);
    private static final ModConfigSpec.DoubleValue LEAD_BOX_REQUIRED_EXPOSURE = BUILDER
            .comment("Accumulated exposure required for one U-238 item to become a plutonium core.")
            .defineInRange("leadIrradiationBox.requiredExposure", 1200.0D, 1.0D, 10000000.0D);
    private static final ModConfigSpec.DoubleValue LEAD_BOX_GAIN_MULTIPLIER = BUILDER
            .comment("Multiplier applied to radiation field when adding exposure in the lead irradiation box.")
            .defineInRange("leadIrradiationBox.exposureGainMultiplier", 1.0D, 0.0D, 10000.0D);
    private static final ModConfigSpec.IntValue LEAD_BOX_TICK_INTERVAL = BUILDER
            .comment("Server ticks between lead irradiation box processing updates.")
            .defineInRange("leadIrradiationBox.tickInterval", 20, 1, 200);

    private static final ModConfigSpec.IntValue NUCLEAR_BOMB_FUSE_TICKS = BUILDER
            .comment("Fuse length for the nuclear bomb after redstone activation.")
            .defineInRange("nuclearBomb.fuseTicks", 100, 20, 1200);
    private static final ModConfigSpec.IntValue NUCLEAR_BOMB_CRATER_RADIUS = BUILDER
            .comment("Approximate block radius cleared by the optimized nuclear crater pass.")
            .defineInRange("nuclearBomb.craterRadius", 48, 8, 128);
    private static final ModConfigSpec.IntValue NUCLEAR_BOMB_SHOCKWAVE_RADIUS = BUILDER
            .comment("Entity damage and knockback radius for the nuclear shockwave.")
            .defineInRange("nuclearBomb.shockwaveRadius", 112, 16, 256);
    private static final ModConfigSpec.IntValue NUCLEAR_BOMB_THERMAL_RADIUS = BUILDER
            .comment("Thermal flash and ignition radius for exposed entities and surfaces.")
            .defineInRange("nuclearBomb.thermalRadius", 160, 16, 384);
    private static final ModConfigSpec.IntValue NUCLEAR_BOMB_MAX_BLOCKS = BUILDER
            .comment("Safety cap for block removals in one nuclear explosion.")
            .defineInRange("nuclearBomb.maxBlocksRemoved", 140000, 1000, 1000000);
    private static final ModConfigSpec.IntValue NUCLEAR_BOMB_MAX_POSITIONS_PER_TICK = BUILDER
            .comment("Maximum block positions checked per tick by a staged nuclear terrain pass.")
            .defineInRange("nuclearBomb.maxPositionsCheckedPerTick", 70000, 1000, 500000);
    private static final ModConfigSpec.IntValue NUCLEAR_BOMB_MAX_BLOCKS_PER_TICK = BUILDER
            .comment("Maximum blocks removed per tick by a staged nuclear terrain pass.")
            .defineInRange("nuclearBomb.maxBlocksRemovedPerTick", 7000, 100, 100000);
    private static final ModConfigSpec.IntValue NUCLEAR_BOMB_SHOCKWAVE_EXPLOSIONS = BUILDER
            .comment("Maximum small visual explosions spawned per shockwave ring step.")
            .defineInRange("nuclearBomb.shockwaveExplosionsPerStep", 18, 0, 64);
    private static final ModConfigSpec.IntValue NUCLEAR_BOMB_FALLOUT_RADIUS = BUILDER
            .comment("Chunk radius contaminated by nuclear fallout.")
            .defineInRange("nuclearBomb.falloutRadiusChunks", 24, 0, 96);
    private static final ModConfigSpec.DoubleValue NUCLEAR_BOMB_FALLOUT_STRENGTH = BUILDER
            .comment("Central weak chunk radiation added by nuclear fallout.")
            .defineInRange("nuclearBomb.falloutStrength", 180.0D, 0.0D, 10000.0D);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static double radiationDecayRate;
    public static double radiationSpreadFactor;
    public static int radiationTickInterval;
    public static double radiationEntityDamageThreshold;
    public static double radioactiveItemStrength;
    public static double radiationChunkLeakScale;
    public static double playerRadiationDecayPerUpdate;
    public static double uraniumRadiation;
    public static double crushedUraniumRadiation;
    public static double uraniumDustRadiation;
    public static double nuclearWasteRadiation;
    public static double radiationEffectLowThreshold;
    public static double radiationEffectMediumThreshold;
    public static double radiationEffectHighThreshold;
    public static double radiationEffectExtremeThreshold;
    public static double radiationEffectSevereThreshold;
    public static double radiationEffectCriticalThreshold;
    public static double radiationEffectLethalThreshold;
    public static int createRadiationScanInterval;
    public static int createRadiationScanRadius;
    public static double createPipeLeak;
    public static int createMaxRotationSpeed;
    public static double createWoodenCogwheelMaxRpm;
    public static double createWoodenCogwheelMaxStress;
    public static double createSteelCogwheelStressImpact;
    public static double createLargeSteelCogwheelStressImpact;
    public static double crownsActivityLeakScale;
    public static double crownsHotTemperatureThreshold;
    public static double crownsHeatLeakScale;
    public static int radiationContainmentRadius;
    public static double radiationContainedBuildup;
    public static double plutoniumExposureThreshold;
    public static double irradiationCostMultiplier;
    public static double plutoniumMinimumFieldStrength;
    public static int radiationScannerRange;
    public static double hazmatBasicProtection;
    public static double hazmatAdvancedProtection;
    public static double hazmatReinforcedProtection;
    public static boolean hazmatSuitsDegrade;
    public static double hazmatHandlingProtection;
    public static double antiradinDoseReduction;
    public static int antiradinCooldownTicks;
    public static double leadBoxMinimumFieldStrength;
    public static double leadBoxRequiredExposure;
    public static double leadBoxExposureGainMultiplier;
    public static int leadBoxTickInterval;
    public static int nuclearBombFuseTicks;
    public static int nuclearBombCraterRadius;
    public static int nuclearBombShockwaveRadius;
    public static int nuclearBombThermalRadius;
    public static int nuclearBombMaxBlocksRemoved;
    public static int nuclearBombMaxPositionsCheckedPerTick;
    public static int nuclearBombMaxBlocksRemovedPerTick;
    public static int nuclearBombShockwaveExplosionsPerStep;
    public static int nuclearBombFalloutRadiusChunks;
    public static double nuclearBombFalloutStrength;

    private Config() {
    }

    @SubscribeEvent
    static void onLoad(ModConfigEvent event) {
        radiationDecayRate = RADIATION_DECAY_RATE.get();
        radiationSpreadFactor = RADIATION_SPREAD_FACTOR.get();
        radiationTickInterval = RADIATION_TICK_INTERVAL.get();
        radiationEntityDamageThreshold = RADIATION_ENTITY_DAMAGE_THRESHOLD.get();
        radioactiveItemStrength = RADIOACTIVE_ITEM_STRENGTH.get();
        radiationChunkLeakScale = RADIATION_CHUNK_LEAK_SCALE.get();
        playerRadiationDecayPerUpdate = PLAYER_RADIATION_DECAY.get();
        uraniumRadiation = URANIUM_RADIATION.get();
        crushedUraniumRadiation = CRUSHED_URANIUM_RADIATION.get();
        uraniumDustRadiation = URANIUM_DUST_RADIATION.get();
        nuclearWasteRadiation = NUCLEAR_WASTE_RADIATION.get();
        radiationEffectLowThreshold = RADIATION_EFFECT_LOW.get();
        radiationEffectMediumThreshold = RADIATION_EFFECT_MEDIUM.get();
        radiationEffectHighThreshold = RADIATION_EFFECT_HIGH.get();
        radiationEffectExtremeThreshold = RADIATION_EFFECT_EXTREME.get();
        radiationEffectSevereThreshold = RADIATION_EFFECT_SEVERE.get();
        radiationEffectCriticalThreshold = RADIATION_EFFECT_CRITICAL.get();
        radiationEffectLethalThreshold = RADIATION_EFFECT_LETHAL.get();
        createRadiationScanInterval = CREATE_RADIATION_SCAN_INTERVAL.get();
        createRadiationScanRadius = CREATE_RADIATION_SCAN_RADIUS.get();
        createPipeLeak = CREATE_PIPE_LEAK.get();
        createMaxRotationSpeed = CREATE_MAX_ROTATION_SPEED.get();
        createWoodenCogwheelMaxRpm = CREATE_WOODEN_COG_MAX_RPM.get();
        createWoodenCogwheelMaxStress = CREATE_WOODEN_COG_MAX_STRESS.get();
        createSteelCogwheelStressImpact = CREATE_STEEL_COG_STRESS.get();
        createLargeSteelCogwheelStressImpact = CREATE_LARGE_STEEL_COG_STRESS.get();
        crownsActivityLeakScale = CROWNS_ACTIVITY_LEAK_SCALE.get();
        crownsHotTemperatureThreshold = CROWNS_HOT_TEMPERATURE.get();
        crownsHeatLeakScale = CROWNS_HEAT_LEAK_SCALE.get();
        radiationContainmentRadius = RADIATION_CONTAINMENT_RADIUS.get();
        radiationContainedBuildup = RADIATION_CONTAINED_BUILDUP.get();
        plutoniumExposureThreshold = PLUTONIUM_EXPOSURE_THRESHOLD.get();
        irradiationCostMultiplier = IRRADIATION_COST_MULTIPLIER.get();
        plutoniumMinimumFieldStrength = PLUTONIUM_MIN_FIELD.get();
        radiationScannerRange = RADIATION_SCANNER_RANGE.get();
        hazmatBasicProtection = HAZMAT_BASIC_PROTECTION.get();
        hazmatAdvancedProtection = HAZMAT_ADVANCED_PROTECTION.get();
        hazmatReinforcedProtection = HAZMAT_REINFORCED_PROTECTION.get();
        hazmatSuitsDegrade = HAZMAT_SUITS_DEGRADE.get();
        hazmatHandlingProtection = HAZMAT_HANDLING_PROTECTION.get();
        antiradinDoseReduction = ANTIRADIN_DOSE_REDUCTION.get();
        antiradinCooldownTicks = ANTIRADIN_COOLDOWN.get();
        leadBoxMinimumFieldStrength = LEAD_BOX_MIN_FIELD.get();
        leadBoxRequiredExposure = LEAD_BOX_REQUIRED_EXPOSURE.get();
        leadBoxExposureGainMultiplier = LEAD_BOX_GAIN_MULTIPLIER.get();
        leadBoxTickInterval = LEAD_BOX_TICK_INTERVAL.get();
        nuclearBombFuseTicks = NUCLEAR_BOMB_FUSE_TICKS.get();
        nuclearBombCraterRadius = NUCLEAR_BOMB_CRATER_RADIUS.get();
        nuclearBombShockwaveRadius = NUCLEAR_BOMB_SHOCKWAVE_RADIUS.get();
        nuclearBombThermalRadius = NUCLEAR_BOMB_THERMAL_RADIUS.get();
        nuclearBombMaxBlocksRemoved = NUCLEAR_BOMB_MAX_BLOCKS.get();
        nuclearBombMaxPositionsCheckedPerTick = NUCLEAR_BOMB_MAX_POSITIONS_PER_TICK.get();
        nuclearBombMaxBlocksRemovedPerTick = NUCLEAR_BOMB_MAX_BLOCKS_PER_TICK.get();
        nuclearBombShockwaveExplosionsPerStep = NUCLEAR_BOMB_SHOCKWAVE_EXPLOSIONS.get();
        nuclearBombFalloutRadiusChunks = NUCLEAR_BOMB_FALLOUT_RADIUS.get();
        nuclearBombFalloutStrength = NUCLEAR_BOMB_FALLOUT_STRENGTH.get();
    }
}
