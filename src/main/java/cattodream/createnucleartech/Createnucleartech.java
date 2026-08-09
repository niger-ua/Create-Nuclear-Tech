package cattodream.createnucleartech;

import com.mojang.logging.LogUtils;
import cattodream.createnucleartech.client.BlastFurnaceScreen;
import cattodream.createnucleartech.client.GeigerCounterItemRenderer;
import cattodream.createnucleartech.client.GeigerHudOverlay;
import cattodream.createnucleartech.client.HighSpeedMixerRenderer;
import cattodream.createnucleartech.client.LeadCopycatRenderer;
import cattodream.createnucleartech.client.LeadIrradiationBoxScreen;
import cattodream.createnucleartech.client.NuclearFlashOverlay;
import cattodream.createnucleartech.client.NoopEntityRenderer;
import cattodream.createnucleartech.client.NuclearBombEntityRenderer;
import cattodream.createnucleartech.client.NuclearBombScreen;
import cattodream.createnucleartech.client.particle.NuclearMushroomCloudParticle;
import cattodream.createnucleartech.client.particle.NuclearSmokeParticle;
import cattodream.createnucleartech.integration.crowns.CrownsIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.slf4j.Logger;

@Mod(Createnucleartech.MODID)
public class Createnucleartech {
    public static final String MODID = "createnucleartech";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Createnucleartech(IEventBus modEventBus, ModContainer modContainer) {
        ModRegistry.init(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::hideCreativeEntries);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        CrownsIntegration.assertRequiredDependencyLoaded();
        LOGGER.info("CreateNuclearTech initialized");
    }

    private void hideCreativeEntries(BuildCreativeModeTabContentsEvent event) {
        hideFromCreative(event, new ItemStack(ModRegistry.REFLECTOR_TIER_1.get()));
        hideFromCreative(event, new ItemStack(ModRegistry.REFLECTOR_TIER_2.get()));
        hideFromCreative(event, new ItemStack(ModRegistry.REFLECTOR_TIER_3.get()));
        hideFromCreative(event, new ItemStack(ModRegistry.EARLY_NEUTRON_REFLECTOR_ITEM.get()));
        hideFromCreative(event, new ItemStack(ModRegistry.ADVANCED_NEUTRON_REFLECTOR_ITEM.get()));
        hideFromCreative(event, new ItemStack(ModRegistry.ELITE_NEUTRON_REFLECTOR_ITEM.get()));
        hideExternalItemFromCreative(event, "crowns", "fuel_assembly");
    }

    private static void hideExternalItemFromCreative(BuildCreativeModeTabContentsEvent event, String namespace, String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
        if (item != Items.AIR) {
            hideFromCreative(event, new ItemStack(item));
        }
    }

    private static void hideFromCreative(BuildCreativeModeTabContentsEvent event, ItemStack stack) {
        event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("CreateNuclearTech client setup");
            LOGGER.info("PLAYER >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void registerMenuScreens(RegisterMenuScreensEvent event) {
            event.register(ModRegistry.LEAD_IRRADIATION_BOX_MENU.get(), LeadIrradiationBoxScreen::new);
            event.register(ModRegistry.NUCLEAR_BOMB_MENU.get(), NuclearBombScreen::new);
            event.register(ModRegistry.BLAST_FURNACE_MENU.get(), BlastFurnaceScreen::new);
        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModRegistry.NUCLEAR_BOMB_ENTITY.get(), NuclearBombEntityRenderer::new);
            event.registerEntityRenderer(ModRegistry.HBM_NUKE_EXPLOSION_ENTITY.get(), NoopEntityRenderer::new);
            event.registerBlockEntityRenderer(ModRegistry.HIGH_SPEED_MIXER_ENTITY.get(), HighSpeedMixerRenderer::new);
            event.registerBlockEntityRenderer(ModRegistry.LEAD_COPYCAT_ENTITY.get(), LeadCopycatRenderer::new);
        }

        @SubscribeEvent
        public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
            event.register(ModelResourceLocation.standalone(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "block/high_speed_mixer/cog")));
            event.register(ModelResourceLocation.standalone(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "block/high_speed_mixer/head")));
            event.register(ModelResourceLocation.standalone(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "block/high_speed_mixer/pole")));
            event.register(ModelResourceLocation.standalone(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "item/geiger_counter_3d_base")));
        }

        @SubscribeEvent
        public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
            GeigerCounterItemRenderer geigerRenderer = new GeigerCounterItemRenderer();
            event.registerItem(new IClientItemExtensions() {
                @Override
                public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return geigerRenderer;
                }
            }, ModRegistry.GEIGER_COUNTER.get());
        }

        @SubscribeEvent
        public static void registerGuiLayers(RegisterGuiLayersEvent event) {
            event.registerAboveAll(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "nuclear_flash"),
                    NuclearFlashOverlay::renderFlash
            );
            event.registerAboveAll(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "geiger_hud"),
                    GeigerHudOverlay::render
            );
        }

        @SubscribeEvent
        public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
            event.registerSpecial(ModRegistry.NUKE_MUSHROOM_CLOUD.get(), NuclearMushroomCloudParticle::createLarge);
            event.registerSpriteSet(ModRegistry.NUCLEAR_SMOKE.get(), sprites -> NuclearSmokeParticle.Provider.grey(sprites, 2.0F, 170, 1.65F));
            event.registerSpriteSet(ModRegistry.NUCLEAR_SMOKE_LARGE.get(), sprites -> NuclearSmokeParticle.Provider.grey(sprites, 4.0F, 240, 1.85F));
            event.registerSpriteSet(ModRegistry.NUCLEAR_SMOKE_HUGE.get(), sprites -> NuclearSmokeParticle.Provider.grey(sprites, 6.0F, 340, 2.05F));
            event.registerSpriteSet(ModRegistry.NUCLEAR_HOT_SMOKE.get(), sprites -> NuclearSmokeParticle.Provider.hot(sprites, 2.4F, 120, 1.25F));
            event.registerSpriteSet(ModRegistry.NUCLEAR_HOT_SMOKE_LARGE.get(), sprites -> NuclearSmokeParticle.Provider.hot(sprites, 4.5F, 170, 1.45F));
            event.registerSpriteSet(ModRegistry.NUCLEAR_HOT_SMOKE_HUGE.get(), sprites -> NuclearSmokeParticle.Provider.hot(sprites, 6.8F, 230, 1.65F));
        }
    }
}
