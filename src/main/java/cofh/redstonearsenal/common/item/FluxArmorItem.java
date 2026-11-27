package cofh.redstonearsenal.common.item;

import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.Item;
import cofh.core.common.config.CoreClientConfig;
import cofh.core.common.item.ArmorItemCoFH;
import cofh.core.util.ProxyUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import cofh.lib.common.item.ArmorMaterialCoFH;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

import static cofh.lib.util.helpers.StringHelper.getTextComponent;

public class FluxArmorItem extends ArmorItemCoFH implements IFluxItem {

    protected int maxEnergy;
    protected int extract;
    protected int receive;

    public FluxArmorItem(ArmorMaterialCoFH pMaterial, ArmorItem.Type pType, Properties pProperties, int maxEnergy, int maxTransfer) {

        super(pMaterial.asMaterial(false), pType, pProperties);

        this.maxEnergy = maxEnergy;
        this.extract = maxTransfer;
        this.receive = maxTransfer;

        ProxyUtils.registerItemModelProperty(this, ResourceLocation.parse("charged"), this::getChargedModelProperty);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {

        if (Screen.hasShiftDown() || CoreClientConfig.alwaysShowDetails.get()) {
            tooltipDelegate(stack, context.level(), tooltip, flagIn);
        } else if (CoreClientConfig.holdShiftForDetails.get()) {
            tooltip.add(getTextComponent("info.cofh.hold_shift_for_details").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {

        return getEnchantmentValue(stack) > 0;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {

        return false;
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {

        // setEnergyStored(stack, getMaxEnergyStored(stack) - getEnergyPerUse(false) * damage);
    }

    @Override
    public int getDamage(ItemStack stack) {

        return 0;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {

        return hasEnergy(stack, false) ? super.getDefaultAttributeModifiers() : ItemAttributeModifiers.EMPTY;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {

        useEnergy(stack, Math.min(getEnergyStored(stack), amount * getEnergyPerUse(false)), entity);
        return 0;
    }

    @Override
    public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity wearer) {

        return true;
    }

    // region DURABILITY BAR
    @Override
    public boolean isBarVisible(ItemStack stack) {

        return IFluxItem.super.isBarVisible(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {

        return IFluxItem.super.getBarColor(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {

        return IFluxItem.super.getBarWidth(stack);
    }
    // endregion

    // region IEnergyContainerItem
    @Override
    public int getExtract(ItemStack container) {

        return extract;
    }

    @Override
    public int getReceive(ItemStack container) {

        return receive;
    }

    @Override
    public int getMaxEnergyStored(ItemStack container) {

        return getMaxStored(container, maxEnergy);
    }
    // endregion

}
