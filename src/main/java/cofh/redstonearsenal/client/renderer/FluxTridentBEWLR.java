package cofh.redstonearsenal.client.renderer;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.TridentModel;
import cofh.redstonearsenal.common.item.FluxTridentItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import static cofh.lib.util.constants.ModIds.ID_REDSTONE_ARSENAL;

public class FluxTridentBEWLR extends BlockEntityWithoutLevelRenderer {

    public static final FluxTridentBEWLR INSTANCE = new FluxTridentBEWLR(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    private static final ResourceLocation UNCHARGED = ResourceLocation.fromNamespaceAndPath(ID_REDSTONE_ARSENAL, "textures/entity/flux_trident.png");
    private static final ResourceLocation CHARGED = ResourceLocation.fromNamespaceAndPath(ID_REDSTONE_ARSENAL, "textures/entity/flux_trident.png");
    private static final ResourceLocation EMPOWERED = ResourceLocation.fromNamespaceAndPath(ID_REDSTONE_ARSENAL, "textures/entity/flux_trident.png");

    private final EntityModelSet modelSet;
    private TridentModel tridentModel;

    public FluxTridentBEWLR(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {

        super(dispatcher, modelSet);
        this.modelSet = modelSet;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        super.onResourceManagerReload(manager);
        this.tridentModel = new TridentModel(modelSet.bakeLayer(ModelLayers.TRIDENT));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, int overlayCoord) {

        if (stack.getItem() instanceof FluxTridentItem) {
            matrixStack.pushPose();
            matrixStack.scale(1.0F, -1.0F, -1.0F);
            VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(buffer, tridentModel.renderType(getTextureLocation(stack)), false, stack.hasFoil());
            tridentModel.renderToBuffer(matrixStack, consumer, packedLight, overlayCoord);
            matrixStack.popPose();
        } else {
            super.renderByItem(stack, displayContext, matrixStack, buffer, packedLight, overlayCoord);
        }
    }

    public ResourceLocation getTextureLocation(ItemStack stack) {

        FluxTridentItem trident = (FluxTridentItem) stack.getItem();
        if (trident.getEnergyStored(stack) > 0) {
            return trident.isEmpowered(stack) ? EMPOWERED : CHARGED;
        }
        return UNCHARGED;
    }

}
