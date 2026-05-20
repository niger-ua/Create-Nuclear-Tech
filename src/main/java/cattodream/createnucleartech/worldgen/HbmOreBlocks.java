package cattodream.createnucleartech.worldgen;

import cattodream.createnucleartech.Createnucleartech;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HbmOreBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Createnucleartech.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Createnucleartech.MODID);
    private static final List<HbmOreType> REGISTERED = new ArrayList<>();

    public static void register(IEventBus modEventBus) {
        for (HbmOreType type : HbmOreType.values()) {
            if (type.skip()) {
                continue;
            }
            type.register();
            REGISTERED.add(type);
        }
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    public static List<HbmOreType> registered() {
        return Collections.unmodifiableList(REGISTERED);
    }

    public enum HbmOreType {
        THORIUM("thorium", 6, 8, -60, 20, false, true, true),
        NITER("niter", 5, 7, -16, 64, false, false, false),
        BERYLLIUM("beryllium", 3, 5, -40, 8, false, true, true),
        CINNABAR("cinnabar", 4, 6, -32, 32, false, false, true);

        private final String path;
        private final int veinSize;
        private final int veinsPerChunk;
        private final int minY;
        private final int maxY;
        private final boolean nether;
        private final boolean registerIngot;
        private final boolean registerRawOre;

        public DeferredBlock<Block> stoneOre;
        public DeferredBlock<Block> deepslateOre;
        public DeferredItem<Item> rawOre;

        HbmOreType(String path, int veinSize, int veinsPerChunk, int minY, int maxY, boolean nether, boolean registerIngot, boolean registerRawOre) {
            this.path = path;
            this.veinSize = veinSize;
            this.veinsPerChunk = veinsPerChunk;
            this.minY = minY;
            this.maxY = maxY;
            this.nether = nether;
            this.registerIngot = registerIngot;
            this.registerRawOre = registerRawOre;
        }

        boolean skip() {
            return false;
        }

        void register() {
            BlockBehaviour.Properties stoneProperties = BlockBehaviour.Properties.of()
                    .mapColor(nether ? MapColor.NETHER : MapColor.STONE)
                    .strength(stoneStrength(), 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE);
            BlockBehaviour.Properties deepslateProperties = BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(stoneStrength() + 1.5F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE);
            stoneOre = BLOCKS.registerSimpleBlock(path + "_ore", stoneProperties);
            deepslateOre = BLOCKS.registerSimpleBlock("deepslate_" + path + "_ore", deepslateProperties);
            if (registerRawOre) {
                rawOre = ITEMS.register("raw_" + path, () -> new Item(new Item.Properties()));
            }
            if (registerIngot) {
                ITEMS.register(path + "_ingot", () -> new Item(new Item.Properties()));
            }
            ITEMS.register(path + "_ore", () -> new BlockItem(stoneOre.get(), new Item.Properties()));
            ITEMS.register("deepslate_" + path + "_ore", () -> new BlockItem(deepslateOre.get(), new Item.Properties()));
        }

        private float stoneStrength() {
            return switch (path) {
                case "niter" -> 1.8F;
                case "cinnabar" -> 2.4F;
                case "beryllium" -> 3.2F;
                case "thorium" -> 3.6F;
                default -> 3.0F;
            };
        }

        public String path() {
            return path;
        }

        public int veinSize() {
            return veinSize;
        }

        public int veinsPerChunk() {
            return veinsPerChunk;
        }

        public int minY() {
            return minY;
        }

        public int maxY() {
            return maxY;
        }

        public boolean nether() {
            return nether;
        }
    }

    private HbmOreBlocks() {
    }
}
