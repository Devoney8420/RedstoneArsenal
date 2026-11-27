package cofh.redstonearsenal.client.renderer;

import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.DeltaTracker;
import cofh.redstonearsenal.util.FluxShieldingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import static cofh.lib.util.constants.ModIds.ID_REDSTONE_ARSENAL;

public class FluxShieldingOverlay implements LayeredDraw.Layer {

    public static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(ID_REDSTONE_ARSENAL, "textures/gui/flux_shielding_icons.png");

    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {

        if (FluxShieldingHelper.maximumCharges <= 0) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || (minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer())) {
            return;
        }

        var gui = minecraft.gui;
        int x = guiGraphics.guiWidth() / 2 + 10;
        int y = guiGraphics.guiHeight() - gui.rightHeight;
        gui.rightHeight += 10;

        for (int i = FluxShieldingHelper.currentCharges; i > 0; --i) {
            guiGraphics.blit(ICONS, x, y, 0, 0, 9, 9, 27, 27);
            x += 8;
        }
        for (int i = FluxShieldingHelper.maximumCharges - FluxShieldingHelper.currentCharges; i > 0; --i) {
            guiGraphics.blit(ICONS, x, y, 9, 0, 9, 9, 27, 27);
            x += 8;
        }
    }

}
