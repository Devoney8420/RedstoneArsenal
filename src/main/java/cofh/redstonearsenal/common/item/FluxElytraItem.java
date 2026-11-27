package cofh.redstonearsenal.common.item;

import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import cofh.core.common.config.CoreClientConfig;
import cofh.core.util.ProxyUtils;
import cofh.lib.util.Utils;
import cofh.lib.util.constants.NBTTags;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import cofh.lib.common.item.ArmorMaterialCoFH;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

import static cofh.lib.util.helpers.StringHelper.getTextComponent;

public class FluxElytraItem extends FluxArmorItem implements IMultiModeFluxItem {

    public float propelSpeed = 0.85F;
    public float brakeRate = 0.95F;
    public int boostTime = 32;
    public int energyUseInterval = 8;

    public FluxElytraItem(ArmorMaterialCoFH pMaterial, ArmorItem.Type pType, Properties pProperties, int maxEnergy, int maxTransfer) {

        super(pMaterial, pType, pProperties, maxEnergy, maxTransfer);

        ProxyUtils.registerItemModelProperty(this, ResourceLocation.parse("empowered"), this::getEmpoweredModelProperty);
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
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {

        return hasEnergy(stack, false);
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {

        boolean isCreative = Utils.isCreativePlayer(entity);
        boolean simulate = isCreative || flightTicks % energyUseInterval != 0;
        if (!useEnergy(stack, false, simulate)) {
            return false;
        }

        if (entity.isShiftKeyDown() && useEnergy(stack, true, simulate)) {
            clearBoostTime(stack);
            brake(entity);
        } else {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            long time = entity.level().getGameTime();
            if (tag.contains(NBTTags.TAG_TIME) && time - tag.getLong(NBTTags.TAG_TIME) <= boostTime) {
                propel(entity);
            } else if (isEmpowered(stack) && useEnergy(stack, true, isCreative)) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.putLong(NBTTags.TAG_TIME, time));
                propel(entity);
            }
        }

        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {

        if (entity instanceof LivingEntity && !((LivingEntity) entity).isFallFlying()) {
            clearBoostTime(stack);
        }
    }

    private void clearBoostTime(ItemStack stack) {

        if (stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(NBTTags.TAG_TIME)) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(NBTTags.TAG_TIME));
        }
    }

    //Used to boost a single time, similar to a rocket.
    public boolean boost(ItemStack stack, LivingEntity entity, int time) {

        if (!entity.getItemBySlot(EquipmentSlot.CHEST).canElytraFly(entity)) {
            return false;
        }
        boolean isPlayer = entity instanceof Player;
        boolean isCreative = isPlayer && ((Player) entity).getAbilities().instabuild;
        if (!useEnergy(stack, getEnergyPerUse(true) * time / energyUseInterval, isCreative)) {
            return false;
        }
        if (!entity.isFallFlying() && isPlayer) {
            ((Player) entity).startFallFlying();
        }
        propel(entity, propelSpeed);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.putLong(NBTTags.TAG_TIME, entity.level().getGameTime()));

        return true;
    }

    public boolean boost(ItemStack stack, LivingEntity entity) {

        return boost(stack, entity, boostTime);
    }

    public void propel(LivingEntity entity, double speed) {

        if (entity.isFallFlying()) {
            Vec3 look = entity.getLookAngle();
            Vec3 velocity = entity.getDeltaMovement();
            entity.setDeltaMovement(velocity.add(look.x * speed - velocity.x * 0.5, look.y * speed - velocity.y * 0.5, look.z * speed - velocity.z * 0.5));

            if (entity.level().isClientSide()) {
                entity.level().addParticle(DustParticleOptions.REDSTONE, entity.getX(), entity.getY(), entity.getZ(), 0, 0, 0);
            }
        }
    }

    public void propel(LivingEntity entity) {

        propel(entity, propelSpeed);
    }

    public void brake(LivingEntity entity, double rate) {

        if (entity.isFallFlying()) {
            Vec3 velocity = entity.getDeltaMovement();
            double horzBrake = velocity.x() * velocity.x() + velocity.z() * velocity.z() > 0.16 ? rate : 1;
            double vertBrake = velocity.y() * velocity.y() > 0.2 ? rate : 1;
            entity.setDeltaMovement(velocity.multiply(horzBrake, vertBrake, horzBrake));
        }
    }

    public void brake(LivingEntity entity) {

        brake(entity, brakeRate);
    }

}
