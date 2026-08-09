package cattodream.createnucleartech;

import cattodream.createnucleartech.armor.HazmatArmorItem;
import cattodream.createnucleartech.armor.HazmatTier;
import cattodream.createnucleartech.armor.ModArmorMaterials;
import cattodream.createnucleartech.effects.RadiationEffect;
import cattodream.createnucleartech.explosion.HbmNukeExplosionEntity;
import cattodream.createnucleartech.explosion.NuclearBombBlock;
import cattodream.createnucleartech.explosion.NuclearBombBlockEntity;
import cattodream.createnucleartech.explosion.NuclearBombEntity;
import cattodream.createnucleartech.items.AntiradinItem;
import cattodream.createnucleartech.items.BombDetonatorItem;
import cattodream.createnucleartech.items.CntFuelRodItem;
import cattodream.createnucleartech.items.HazmatKitItem;
import cattodream.createnucleartech.items.GeigerCounterItem;
import cattodream.createnucleartech.worldgen.HbmOreBlocks;
import cattodream.createnucleartech.items.RadioactiveItem;
import cattodream.createnucleartech.items.RadiationScannerGogglesItem;
import cattodream.createnucleartech.items.RadiationTongsItem;
import cattodream.createnucleartech.menu.BlastFurnaceMenu;
import cattodream.createnucleartech.menu.LeadIrradiationBoxMenu;
import cattodream.createnucleartech.menu.NuclearBombMenu;
import cattodream.createnucleartech.processing.BlastFurnaceBlock;
import cattodream.createnucleartech.processing.BlastFurnaceBlockEntity;
import cattodream.createnucleartech.processing.CntFuelHolderBlock;
import cattodream.createnucleartech.processing.CntFuelHolderBlockEntity;
import cattodream.createnucleartech.processing.CntFuelType;
import cattodream.createnucleartech.processing.HighSpeedMixerBlock;
import cattodream.createnucleartech.processing.HighSpeedMixerBlockEntity;
import cattodream.createnucleartech.processing.LeadCopycatBlock;
import cattodream.createnucleartech.processing.LeadCopycatBlockEntity;
import cattodream.createnucleartech.processing.LeadIrradiationBoxBlock;
import cattodream.createnucleartech.processing.LeadIrradiationBoxBlockEntity;
import cattodream.createnucleartech.radiation.RadiationSicknessEffect;
import cattodream.createnucleartech.recipe.CrownsFuelAssemblyRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public final class ModRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Createnucleartech.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Createnucleartech.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Createnucleartech.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Createnucleartech.MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, Createnucleartech.MODID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, Createnucleartech.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, Createnucleartech.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, Createnucleartech.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Createnucleartech.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Createnucleartech.MODID);

    public static final DeferredBlock<NuclearBombBlock> NUCLEAR_BOMB = BLOCKS.registerBlock(
            "nuclear_bomb",
            NuclearBombBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(18.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(NuclearBombBlock.LIT) ? 5 : 0)
    );
    public static final DeferredBlock<LeadIrradiationBoxBlock> LEAD_IRRADIATION_BOX = BLOCKS.registerBlock(
            "lead_irradiation_box",
            LeadIrradiationBoxBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(8.0F, 1800.0F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<BlastFurnaceBlock> BLAST_FURNACE = BLOCKS.registerBlock(
            "blast_furnace",
            BlastFurnaceBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(7.0F, 60.0F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<HighSpeedMixerBlock> HIGH_SPEED_MIXER = BLOCKS.registerBlock(
            "high_speed_mixer",
            HighSpeedMixerBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(6.0F, 24.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
    );
    public static final DeferredBlock<CntFuelHolderBlock> FUEL_HOLDER = BLOCKS.registerBlock(
            "fuel_holder",
            CntFuelHolderBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(5.0F, 18.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
    );
    public static final DeferredBlock<LeadCopycatBlock> LEAD_COPYCAT = BLOCKS.registerBlock(
            "lead_copycat",
            LeadCopycatBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(5.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
    );
    public static final DeferredBlock<Block> EARLY_NEUTRON_REFLECTOR = BLOCKS.registerSimpleBlock(
            "early_neutron_reflector",
            BlockBehaviour.Properties.of()
                    .strength(5.0F, 10.0F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<Block> ADVANCED_NEUTRON_REFLECTOR = BLOCKS.registerSimpleBlock(
            "advanced_neutron_reflector",
            BlockBehaviour.Properties.of()
                    .strength(8.0F, 18.0F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<Block> ELITE_NEUTRON_REFLECTOR = BLOCKS.registerSimpleBlock(
            "elite_neutron_reflector",
            BlockBehaviour.Properties.of()
                    .strength(12.0F, 30.0F)
                    .sound(SoundType.METAL)
    );
    public static final DeferredBlock<Block> WASTE_EARTH = BLOCKS.registerSimpleBlock(
            "waste_earth",
            BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)
                    .strength(0.55F)
                    .sound(SoundType.GRAVEL)
    );
    public static final DeferredBlock<Block> DEAD_LEAVES = BLOCKS.registerBlock(
            "dead_leaves",
            Block::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
                    .strength(0.2F)
                    .sound(SoundType.GRASS)
    );
    public static final DeferredBlock<RotatedPillarBlock> CHARRED_LOG = BLOCKS.registerBlock(
            "charred_log",
            RotatedPillarBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)
                    .strength(1.6F)
                    .sound(SoundType.WOOD)
    );
    public static final DeferredBlock<Block> CHARRED_PLANKS = BLOCKS.registerSimpleBlock(
            "charred_planks",
            BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
                    .strength(1.3F, 3.0F)
                    .sound(SoundType.WOOD)
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LeadIrradiationBoxBlockEntity>> LEAD_IRRADIATION_BOX_ENTITY = BLOCK_ENTITY_TYPES.register(
            "lead_irradiation_box",
            () -> new BlockEntityType<>(LeadIrradiationBoxBlockEntity::new, Set.of(LEAD_IRRADIATION_BOX.get()), null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NuclearBombBlockEntity>> NUCLEAR_BOMB_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "nuclear_bomb",
            () -> new BlockEntityType<>(NuclearBombBlockEntity::new, Set.of(NUCLEAR_BOMB.get()), null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlastFurnaceBlockEntity>> BLAST_FURNACE_ENTITY = BLOCK_ENTITY_TYPES.register(
            "blast_furnace",
            () -> new BlockEntityType<>(BlastFurnaceBlockEntity::new, Set.of(BLAST_FURNACE.get()), null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HighSpeedMixerBlockEntity>> HIGH_SPEED_MIXER_ENTITY = BLOCK_ENTITY_TYPES.register(
            "high_speed_mixer",
            () -> new BlockEntityType<>(HighSpeedMixerBlockEntity::new, Set.of(HIGH_SPEED_MIXER.get()), null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CntFuelHolderBlockEntity>> FUEL_HOLDER_ENTITY = BLOCK_ENTITY_TYPES.register(
            "fuel_holder",
            () -> new BlockEntityType<>(CntFuelHolderBlockEntity::new, Set.of(FUEL_HOLDER.get()), null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LeadCopycatBlockEntity>> LEAD_COPYCAT_ENTITY = BLOCK_ENTITY_TYPES.register(
            "lead_copycat",
            () -> new BlockEntityType<>(LeadCopycatBlockEntity::new, Set.of(LEAD_COPYCAT.get()), null)
    );
    public static final DeferredHolder<EntityType<?>, EntityType<NuclearBombEntity>> NUCLEAR_BOMB_ENTITY = ENTITY_TYPES.register(
            "nuclear_bomb",
            () -> EntityType.Builder.<NuclearBombEntity>of(NuclearBombEntity::new, MobCategory.MISC)
                    .sized(0.55F, 0.55F)
                    .clientTrackingRange(12)
                    .updateInterval(2)
                    .build("nuclear_bomb")
    );
    public static final DeferredHolder<EntityType<?>, EntityType<HbmNukeExplosionEntity>> HBM_NUKE_EXPLOSION_ENTITY = ENTITY_TYPES.register(
            "hbm_nuke_explosion",
            () -> EntityType.Builder.<HbmNukeExplosionEntity>of(HbmNukeExplosionEntity::new, MobCategory.MISC)
                    .sized(0.01F, 0.01F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("hbm_nuke_explosion")
    );
    public static final DeferredHolder<MenuType<?>, MenuType<LeadIrradiationBoxMenu>> LEAD_IRRADIATION_BOX_MENU = MENU_TYPES.register(
            "lead_irradiation_box",
            () -> IMenuTypeExtension.create(LeadIrradiationBoxMenu::new)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<NuclearBombMenu>> NUCLEAR_BOMB_MENU = MENU_TYPES.register(
            "nuclear_bomb",
            () -> IMenuTypeExtension.create(NuclearBombMenu::new)
    );
    public static final DeferredHolder<MenuType<?>, MenuType<BlastFurnaceMenu>> BLAST_FURNACE_MENU = MENU_TYPES.register(
            "blast_furnace",
            () -> IMenuTypeExtension.create(BlastFurnaceMenu::new)
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEAR_EXPLOSION_SOUND = SOUND_EVENTS.register(
            "nuclear_explosion",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, "nuclear_explosion"))
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> BOMB_FUSE_SOUND = SOUND_EVENTS.register(
            "bomb_fuse",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, "bomb_fuse"))
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> VOMIT_SOUND = SOUND_EVENTS.register(
            "vomit",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Createnucleartech.MODID, "vomit"))
    );

    public static final DeferredItem<RadioactiveItem> PLUTONIUM_CORE = ITEMS.register(
            "plutonium_core",
            () -> new RadioactiveItem(new Item.Properties().stacksTo(16), 8.0D)
    );
    public static final DeferredItem<RadioactiveItem> PLUTONIUM_CORE_INACTIVE = ITEMS.register(
            "plutonium_core_inactive",
            () -> new RadioactiveItem(new Item.Properties().stacksTo(16), 0.0D)
    );
    public static final DeferredItem<Item> REFLECTOR_TIER_1 = ITEMS.register(
            "reflector_tier_1",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredItem<Item> REFLECTOR_TIER_2 = ITEMS.register(
            "reflector_tier_2",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredItem<Item> REFLECTOR_TIER_3 = ITEMS.register(
            "reflector_tier_3",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredItem<RadioactiveItem> YELLOWCAKE = ITEMS.register(
            "yellowcake",
            () -> new RadioactiveItem(new Item.Properties().stacksTo(64), 0.35D)
    );
    public static final DeferredItem<RadioactiveItem> IMPURE_URANIUM_DUST = ITEMS.register(
            "impure_uranium_dust",
            () -> new RadioactiveItem(new Item.Properties().stacksTo(64), 1.0D)
    );
    public static final DeferredItem<Item> STEEL_MIX = ITEMS.register(
            "steel_mix",
            () -> new Item(new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<Item> REDSTONE_INGOT = ITEMS.register(
            "redstone_ingot",
            () -> new Item(new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<Item> RED_COPPER_INGOT = ITEMS.register(
            "red_copper_ingot",
            () -> new Item(new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<Item> ADVANCED_ALLOY_INGOT = ITEMS.register(
            "advanced_alloy_ingot",
            () -> new Item(new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<Item> ADVANCED_ALLOY_PLATE = ITEMS.register(
            "advanced_alloy_plate",
            () -> new Item(new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<Item> COBALT_PLATE = ITEMS.register(
            "cobalt_plate",
            () -> new Item(new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<Item> MIXED_PLATE = ITEMS.register(
            "mixed_plate",
            () -> new Item(new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<Item> PAA_ALLOY_PLATE = ITEMS.register(
            "paa_alloy_plate",
            () -> new Item(new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<Item> HAZMAT_CLOTH = ITEMS.register(
            "hazmat_cloth",
            () -> new Item(new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<Item> HAZMAT_CLOTH_RED = ITEMS.register(
            "hazmat_cloth_red",
            () -> new Item(new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<Item> HAZMAT_CLOTH_GREY = ITEMS.register(
            "hazmat_cloth_grey",
            () -> new Item(new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<Item> MERCURY_DROP = ITEMS.register(
            "mercury_drop",
            () -> new Item(new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<RadioactiveItem> NEPTUNIUM_239 = ITEMS.register(
            "neptunium_239",
            () -> new RadioactiveItem(new Item.Properties().stacksTo(64), 18.0D)
    );
    public static final DeferredItem<RadioactiveItem> PLUTONIUM_239_INGOT = ITEMS.register(
            "plutonium_239_ingot",
            () -> new RadioactiveItem(new Item.Properties().stacksTo(64), 7.5D)
    );
    public static final DeferredItem<RadioactiveItem> PLUTONIUM_240_INGOT = ITEMS.register(
            "plutonium_240_ingot",
            () -> new RadioactiveItem(new Item.Properties().stacksTo(64), 5.5D)
    );
    public static final DeferredItem<RadioactiveItem> COBALT_60_SOURCE = ITEMS.register(
            "cobalt_60_source",
            () -> new RadioactiveItem(new Item.Properties().stacksTo(16), 3.0D)
    );
    public static final DeferredItem<RadioactiveItem> IRIDIUM_192_SOURCE = ITEMS.register(
            "iridium_192_source",
            () -> new RadioactiveItem(new Item.Properties().stacksTo(16), 2.6D)
    );
    public static final DeferredItem<CntFuelRodItem> NATURAL_URANIUM_FUEL_ROD = ITEMS.register(
            "natural_uranium_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.NATURAL_URANIUM, new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<CntFuelRodItem> ENRICHED_URANIUM_FUEL_ROD = ITEMS.register(
            "enriched_uranium_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.ENRICHED_URANIUM, new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<CntFuelRodItem> MILITARY_URANIUM_FUEL_ROD = ITEMS.register(
            "military_uranium_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.MILITARY_URANIUM, new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<CntFuelRodItem> MOX_FUEL_ROD = ITEMS.register(
            "mox_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.MOX, new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<CntFuelRodItem> PLUTONIUM_FUEL_ROD = ITEMS.register(
            "plutonium_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.PLUTONIUM_239, new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<CntFuelRodItem> REACTOR_PLUTONIUM_FUEL_ROD = ITEMS.register(
            "reactor_plutonium_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.REACTOR_GRADE_PLUTONIUM, new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<CntFuelRodItem> THORIUM_FUEL_ROD = ITEMS.register(
            "thorium_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.THORIUM, new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<CntFuelRodItem> SPENT_FUEL_ROD = ITEMS.register(
            "spent_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.SPENT, new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<CntFuelRodItem> SPENT_NATURAL_URANIUM_FUEL_ROD = ITEMS.register(
            "spent_natural_uranium_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.SPENT, "natural_uranium", new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<CntFuelRodItem> SPENT_ENRICHED_URANIUM_FUEL_ROD = ITEMS.register(
            "spent_enriched_uranium_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.SPENT, "enriched_uranium", new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<CntFuelRodItem> SPENT_MILITARY_URANIUM_FUEL_ROD = ITEMS.register(
            "spent_military_uranium_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.SPENT, "military_uranium", new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<CntFuelRodItem> SPENT_MOX_FUEL_ROD = ITEMS.register(
            "spent_mox_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.SPENT, "mox", new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<CntFuelRodItem> SPENT_PLUTONIUM_FUEL_ROD = ITEMS.register(
            "spent_plutonium_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.SPENT, "plutonium_239", new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<CntFuelRodItem> SPENT_REACTOR_PLUTONIUM_FUEL_ROD = ITEMS.register(
            "spent_reactor_plutonium_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.SPENT, "reactor_grade_plutonium", new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<CntFuelRodItem> SPENT_THORIUM_FUEL_ROD = ITEMS.register(
            "spent_thorium_fuel_rod",
            () -> new CntFuelRodItem(CntFuelType.SPENT, "thorium", new Item.Properties().stacksTo(64))
    );
    public static final DeferredItem<?> NUCLEAR_BOMB_ITEM = ITEMS.registerSimpleBlockItem(
            NUCLEAR_BOMB,
            new Item.Properties().stacksTo(1)
    );
    public static final DeferredItem<?> LEAD_IRRADIATION_BOX_ITEM = ITEMS.registerSimpleBlockItem(
            LEAD_IRRADIATION_BOX,
            new Item.Properties().stacksTo(1)
    );
    public static final DeferredItem<?> BLAST_FURNACE_ITEM = ITEMS.registerSimpleBlockItem(
            BLAST_FURNACE,
            new Item.Properties().stacksTo(1)
    );
    public static final DeferredItem<?> HIGH_SPEED_MIXER_ITEM = ITEMS.registerSimpleBlockItem(
            HIGH_SPEED_MIXER,
            new Item.Properties().stacksTo(64)
    );
    public static final DeferredItem<?> FUEL_HOLDER_ITEM = ITEMS.registerSimpleBlockItem(
            FUEL_HOLDER,
            new Item.Properties().stacksTo(64)
    );
    public static final DeferredItem<?> LEAD_COPYCAT_ITEM = ITEMS.registerSimpleBlockItem(
            LEAD_COPYCAT,
            new Item.Properties().stacksTo(64)
    );
    public static final DeferredItem<?> EARLY_NEUTRON_REFLECTOR_ITEM = ITEMS.registerSimpleBlockItem(
            EARLY_NEUTRON_REFLECTOR,
            new Item.Properties().stacksTo(64)
    );
    public static final DeferredItem<?> ADVANCED_NEUTRON_REFLECTOR_ITEM = ITEMS.registerSimpleBlockItem(
            ADVANCED_NEUTRON_REFLECTOR,
            new Item.Properties().stacksTo(64)
    );
    public static final DeferredItem<?> ELITE_NEUTRON_REFLECTOR_ITEM = ITEMS.registerSimpleBlockItem(
            ELITE_NEUTRON_REFLECTOR,
            new Item.Properties().stacksTo(64)
    );
    public static final DeferredItem<?> WASTE_EARTH_ITEM = ITEMS.registerSimpleBlockItem(
            WASTE_EARTH,
            new Item.Properties().stacksTo(64)
    );
    public static final DeferredItem<?> DEAD_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(
            DEAD_LEAVES,
            new Item.Properties().stacksTo(64)
    );
    public static final DeferredItem<?> CHARRED_LOG_ITEM = ITEMS.registerSimpleBlockItem(
            CHARRED_LOG,
            new Item.Properties().stacksTo(64)
    );
    public static final DeferredItem<?> CHARRED_PLANKS_ITEM = ITEMS.registerSimpleBlockItem(
            CHARRED_PLANKS,
            new Item.Properties().stacksTo(64)
    );
    public static final DeferredItem<GeigerCounterItem> GEIGER_COUNTER = ITEMS.register(
            "geiger_counter",
            () -> new GeigerCounterItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<RadiationScannerGogglesItem> RADIATION_SCANNER_GOGGLES = ITEMS.register(
            "radiation_scanner_goggles",
            () -> new RadiationScannerGogglesItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<RadiationTongsItem> RADIATION_TONGS = ITEMS.register(
            "radiation_tongs",
            () -> new RadiationTongsItem(new Item.Properties().durability(512))
    );
    public static final DeferredItem<AntiradinItem> ANTIRADIN = ITEMS.register(
            "antiradin",
            () -> new AntiradinItem(new Item.Properties().stacksTo(16))
    );
    public static final DeferredItem<BombDetonatorItem> BOMB_DETONATOR = ITEMS.register(
            "bomb_detonator",
            () -> new BombDetonatorItem(new Item.Properties().stacksTo(1))
    );
    public static final DeferredItem<Item> EXPLOSIVE_LENS_SEGMENT = ITEMS.register(
            "explosive_lens_segment",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredItem<Item> SUPER_EXPLOSIVE_LENS_SEGMENT = ITEMS.register(
            "super_explosive_lens_segment",
            () -> new Item(new Item.Properties().stacksTo(16))
    );
    public static final DeferredItem<HazmatArmorItem> BASIC_HAZMAT_HELMET = hazmat("basic_hazmat_helmet", HazmatTier.BASIC, ArmorItem.Type.HELMET);
    public static final DeferredItem<HazmatArmorItem> BASIC_HAZMAT_CHESTPLATE = hazmat("basic_hazmat_chestplate", HazmatTier.BASIC, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<HazmatArmorItem> BASIC_HAZMAT_LEGGINGS = hazmat("basic_hazmat_leggings", HazmatTier.BASIC, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<HazmatArmorItem> BASIC_HAZMAT_BOOTS = hazmat("basic_hazmat_boots", HazmatTier.BASIC, ArmorItem.Type.BOOTS);
    public static final DeferredItem<HazmatArmorItem> ADVANCED_HAZMAT_HELMET = hazmat("advanced_hazmat_helmet", HazmatTier.ADVANCED, ArmorItem.Type.HELMET);
    public static final DeferredItem<HazmatArmorItem> ADVANCED_HAZMAT_CHESTPLATE = hazmat("advanced_hazmat_chestplate", HazmatTier.ADVANCED, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<HazmatArmorItem> ADVANCED_HAZMAT_LEGGINGS = hazmat("advanced_hazmat_leggings", HazmatTier.ADVANCED, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<HazmatArmorItem> ADVANCED_HAZMAT_BOOTS = hazmat("advanced_hazmat_boots", HazmatTier.ADVANCED, ArmorItem.Type.BOOTS);
    public static final DeferredItem<HazmatArmorItem> REINFORCED_HAZMAT_HELMET = hazmat("reinforced_hazmat_helmet", HazmatTier.REINFORCED, ArmorItem.Type.HELMET);
    public static final DeferredItem<HazmatArmorItem> REINFORCED_HAZMAT_CHESTPLATE = hazmat("reinforced_hazmat_chestplate", HazmatTier.REINFORCED, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<HazmatArmorItem> REINFORCED_HAZMAT_LEGGINGS = hazmat("reinforced_hazmat_leggings", HazmatTier.REINFORCED, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<HazmatArmorItem> REINFORCED_HAZMAT_BOOTS = hazmat("reinforced_hazmat_boots", HazmatTier.REINFORCED, ArmorItem.Type.BOOTS);
    public static final DeferredItem<HazmatArmorItem> ELITE_HAZMAT_HELMET = hazmat("elite_hazmat_helmet", HazmatTier.PAA, ArmorItem.Type.HELMET);
    public static final DeferredItem<HazmatArmorItem> ELITE_HAZMAT_CHESTPLATE = hazmat("elite_hazmat_chestplate", HazmatTier.PAA, ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<HazmatArmorItem> ELITE_HAZMAT_LEGGINGS = hazmat("elite_hazmat_leggings", HazmatTier.PAA, ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<HazmatArmorItem> ELITE_HAZMAT_BOOTS = hazmat("elite_hazmat_boots", HazmatTier.PAA, ArmorItem.Type.BOOTS);
    public static final DeferredItem<HazmatKitItem> HAZMAT_KIT = ITEMS.register(
            "hazmat_kit",
            () -> new HazmatKitItem(
                    HazmatTier.BASIC,
                    () -> new ItemStack(BASIC_HAZMAT_HELMET.get()),
                    () -> new ItemStack(BASIC_HAZMAT_CHESTPLATE.get()),
                    () -> new ItemStack(BASIC_HAZMAT_LEGGINGS.get()),
                    () -> new ItemStack(BASIC_HAZMAT_BOOTS.get()),
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final DeferredItem<HazmatKitItem> HAZMAT_RED_KIT = ITEMS.register(
            "hazmat_red_kit",
            () -> new HazmatKitItem(
                    HazmatTier.ADVANCED,
                    () -> new ItemStack(ADVANCED_HAZMAT_HELMET.get()),
                    () -> new ItemStack(ADVANCED_HAZMAT_CHESTPLATE.get()),
                    () -> new ItemStack(ADVANCED_HAZMAT_LEGGINGS.get()),
                    () -> new ItemStack(ADVANCED_HAZMAT_BOOTS.get()),
                    new Item.Properties().stacksTo(1)
            )
    );
    public static final DeferredItem<HazmatKitItem> HAZMAT_GREY_KIT = ITEMS.register(
            "hazmat_grey_kit",
            () -> new HazmatKitItem(
                    HazmatTier.REINFORCED,
                    () -> new ItemStack(REINFORCED_HAZMAT_HELMET.get()),
                    () -> new ItemStack(REINFORCED_HAZMAT_CHESTPLATE.get()),
                    () -> new ItemStack(REINFORCED_HAZMAT_LEGGINGS.get()),
                    () -> new ItemStack(REINFORCED_HAZMAT_BOOTS.get()),
                    new Item.Properties().stacksTo(1)
            )
    );

    public static final DeferredHolder<MobEffect, RadiationEffect> RADIATION = EFFECTS.register(
            "radiation",
            RadiationEffect::new
    );
    public static final DeferredHolder<MobEffect, RadiationSicknessEffect> RADIATION_SICKNESS = EFFECTS.register(
            "radiation_sickness",
            RadiationSicknessEffect::new
    );
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUKE_MUSHROOM_CLOUD = PARTICLE_TYPES.register(
            "nuke_mushroom_cloud",
            () -> new SimpleParticleType(true)
    );
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_SMOKE = PARTICLE_TYPES.register(
            "nuclear_smoke",
            () -> new SimpleParticleType(true)
    );
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_SMOKE_LARGE = PARTICLE_TYPES.register(
            "nuclear_smoke_large",
            () -> new SimpleParticleType(true)
    );
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_SMOKE_HUGE = PARTICLE_TYPES.register(
            "nuclear_smoke_huge",
            () -> new SimpleParticleType(true)
    );
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_HOT_SMOKE = PARTICLE_TYPES.register(
            "nuclear_hot_smoke",
            () -> new SimpleParticleType(true)
    );
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_HOT_SMOKE_LARGE = PARTICLE_TYPES.register(
            "nuclear_hot_smoke_large",
            () -> new SimpleParticleType(true)
    );
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> NUCLEAR_HOT_SMOKE_HUGE = PARTICLE_TYPES.register(
            "nuclear_hot_smoke_huge",
            () -> new SimpleParticleType(true)
    );
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<CrownsFuelAssemblyRecipe>> CROWNS_FUEL_ASSEMBLY_RECIPE = RECIPE_SERIALIZERS.register(
            "crowns_fuel_assembly",
            () -> new SimpleCraftingRecipeSerializer<>(CrownsFuelAssemblyRecipe::new)
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createnucleartech"))
                    .icon(() -> new ItemStack(GEIGER_COUNTER.get()))
                    .displayItems((parameters, output) -> {
                        ITEMS.getEntries().forEach(item -> {
                            Item value = item.get();
                            if (!isTemporarilyHidden(value)) {
                                output.accept(value);
                            }
                        });
                        HbmOreBlocks.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                    })
                    .build()
    );

    private ModRegistry() {
    }

    public static void init(IEventBus modEventBus) {
        HbmOreBlocks.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        EFFECTS.register(modEventBus);
        PARTICLE_TYPES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }

    private static DeferredItem<HazmatArmorItem> hazmat(String name, HazmatTier tier, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new HazmatArmorItem(
                tier.material(),
                type,
                tier,
                new Item.Properties().durability(type.getDurability(tier.durabilityMultiplier()))
        ));
    }

    private static boolean isTemporarilyHidden(Item item) {
        return item == HAZMAT_CLOTH_RED.get()
                || item == HAZMAT_CLOTH_GREY.get()
                || item == REFLECTOR_TIER_1.get()
                || item == REFLECTOR_TIER_2.get()
                || item == REFLECTOR_TIER_3.get()
                || item == EARLY_NEUTRON_REFLECTOR_ITEM.get()
                || item == ADVANCED_NEUTRON_REFLECTOR_ITEM.get()
                || item == ELITE_NEUTRON_REFLECTOR_ITEM.get()
                || item == ADVANCED_HAZMAT_HELMET.get()
                || item == ADVANCED_HAZMAT_CHESTPLATE.get()
                || item == ADVANCED_HAZMAT_LEGGINGS.get()
                || item == ADVANCED_HAZMAT_BOOTS.get()
                || item == REINFORCED_HAZMAT_HELMET.get()
                || item == REINFORCED_HAZMAT_CHESTPLATE.get()
                || item == REINFORCED_HAZMAT_LEGGINGS.get()
                || item == REINFORCED_HAZMAT_BOOTS.get()
                || item == ELITE_HAZMAT_HELMET.get()
                || item == ELITE_HAZMAT_CHESTPLATE.get()
                || item == ELITE_HAZMAT_LEGGINGS.get()
                || item == ELITE_HAZMAT_BOOTS.get()
                || item == HAZMAT_RED_KIT.get()
                || item == HAZMAT_GREY_KIT.get();
    }
}
