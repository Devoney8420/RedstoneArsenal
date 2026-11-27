package cofh.redstonearsenal.client.event;

import cofh.core.client.renderer.entity.ShockwaveRenderer;
import cofh.redstonearsenal.RedstoneArsenal;
import cofh.redstonearsenal.client.renderer.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.apache.logging.log4j.Level;

import static cofh.lib.util.constants.ModIds.ID_REDSTONE_ARSENAL;
import static cofh.redstonearsenal.init.registries.ModEntities.*;

@EventBusSubscriber(value = Dist.CLIENT, modid = ID_REDSTONE_ARSENAL)
public class RSAClientSetupEvents {

    @SubscribeEvent
    public static void entityLayerSetup(final EntityRenderersEvent.AddLayers event) {

        EntityModelSet models = event.getEntityModels();
        for (var skin : event.getSkins()) {
            LivingEntityRenderer<? extends Player, ? extends EntityModel<? extends Player>> renderer = event.getSkin(skin);
            if (renderer != null) {
                try {
                    castRenderer(renderer).addLayer(new FluxElytraLayer<>(castRenderer(renderer), models));
                } catch (ClassCastException e) {
                    RedstoneArsenal.LOG.log(Level.ERROR, "Failed render layer cast: " + renderer);
                }
            }
        }

        for (var type : event.getEntityTypes()) {
            EntityRenderer<?> renderer = event.getRenderer(type);
            if (renderer instanceof HumanoidMobRenderer || renderer instanceof ArmorStandRenderer) {
                LivingEntityRenderer<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> livingRenderer = (LivingEntityRenderer<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>>) renderer;
                try {
                    castRenderer(livingRenderer).addLayer(new FluxElytraLayer<>(castRenderer(livingRenderer), models));
                } catch (ClassCastException e) {
                    RedstoneArsenal.LOG.log(Level.ERROR, "Failed render layer cast: " + renderer);
                }
            }
        }
    }

    // pain
    private static <T extends LivingEntity, M extends EntityModel<T>> LivingEntityRenderer<T, M> castRenderer(LivingEntityRenderer<? extends LivingEntity, ? extends EntityModel<? extends LivingEntity>> renderer) throws ClassCastException {

        return (LivingEntityRenderer<T, M>) renderer;
    }

    @SubscribeEvent
    public static void entityRendererSetup(final EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(FLUX_SLASH.get(), FluxSlashRenderer::new);
        event.registerEntityRenderer(FLUX_ARROW.get(), FluxArrowRenderer::new);
        event.registerEntityRenderer(FLUX_TRIDENT.get(), ThrownFluxTridentRenderer::new);
        event.registerEntityRenderer(FLUX_WRENCH.get(), FluxWrenchRenderer::new);
        event.registerEntityRenderer(SHOCKWAVE_ENTITY.get(), ShockwaveRenderer::new);
    }

    @SubscribeEvent
    public static void overlaySetup(final RegisterGuiLayersEvent event) {

        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, ResourceLocation.fromNamespaceAndPath(ID_REDSTONE_ARSENAL, "flux_shielding"), new FluxShieldingOverlay());
    }

    // region RELOAD
    @SubscribeEvent
    public static void registerReloadListeners(final RegisterClientReloadListenersEvent event) {

        event.registerReloadListener(FluxTridentBEWLR.INSTANCE);
    }
    // endregion

}
