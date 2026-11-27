package cofh.redstonearsenal.common.item;

import java.util.ArrayList;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.core.component.DataComponents;
import cofh.lib.util.helpers.StackData;
import cofh.core.common.config.CoreClientConfig;
import cofh.core.common.item.ILeftClickHandlerItem;
import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.ArcheryHelper;
import cofh.lib.common.item.CrossbowItemCoFH;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

import static cofh.lib.util.constants.NBTTags.TAG_AMMO;
import static cofh.lib.util.helpers.StringHelper.getTextComponent;
import static net.minecraft.nbt.Tag.TAG_COMPOUND;

public class FluxCrossbowItem extends CrossbowItemCoFH implements IMultiModeFluxItem, ILeftClickHandlerItem {

    protected final int maxCharges;

    protected final int maxEnergy;
    protected final int extract;
    protected final int receive;

    public FluxCrossbowItem(int enchantability, float accuracyModifier, float damageModifier, float velocityModifier, int maxCharges, Item.Properties builder, int energy, int xfer) {

        super(enchantability, accuracyModifier, damageModifier, velocityModifier, builder);

        this.maxEnergy = energy;
        this.extract = xfer;
        this.receive = xfer;
        this.maxCharges = maxCharges;

        ProxyUtils.registerItemModelProperty(this, ResourceLocation.parse("charged"), this::getChargedModelProperty);
        ProxyUtils.registerItemModelProperty(this, ResourceLocation.parse("empowered"), this::getEmpoweredModelProperty);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {

        if (getLoadedAmmoCount(stack) > 0) {
            tooltip.add(Component.translatable("info.cofh.crossbow_loaded"));
            for (ItemStack ammo : getAllLoadedAmmo(stack)) {
                tooltip.add((Component.literal("- ")).append(ammo.getHoverName()));
            }
        }
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
    public float getAmmoModelProperty(ItemStack stack, Level world, LivingEntity entity, int seed) {

        return getLoadedAmmoCount(stack) * 0.334F;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        if (hasEnergy(stack, false) && getLoadedAmmoCount(stack) < maxCharges && (player.getAbilities().instabuild || !ArcheryHelper.findAmmo(player, stack).isEmpty())) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity living) {

        if (!world.isClientSide && getLoadedAmmoCount(stack) < maxCharges && loadAmmo(living, stack)) {

            world.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.CROSSBOW_LOADING_END, living instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.0F, 1.0F / (world.random.nextFloat() * 0.5F + 1.0F) + 0.2F);
        }
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity living, int durationRemaining) {

        if (!world.isClientSide && durationRemaining < 0 && getLoadedAmmoCount(stack) < maxCharges && loadAmmo(living, stack)) {

            world.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.CROSSBOW_LOADING_END, living instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.0F, 1.0F / (world.random.nextFloat() * 0.5F + 1.0F) + 0.2F);
        }
    }

    @Override
    public boolean useOnRelease(ItemStack stack) {

        return false;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {

        return !player.isCreative();
    }

    @Override
    public void onCrossbowShot(Player shooter, InteractionHand hand, ItemStack crossbow, int damage) {

        useEnergy(crossbow, Math.min(getEnergyPerUse(false) * damage, getEnergyStored(crossbow)), shooter.getAbilities().instabuild);

        if (shooter instanceof ServerPlayer) {
            if (!shooter.level().isClientSide()) {
                CriteriaTriggers.SHOT_CROSSBOW.trigger((ServerPlayer) shooter, crossbow);
            }
            shooter.awardStat(Stats.ITEM_USED.get(crossbow.getItem()));
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {

        return !oldStack.equals(newStack) && (slotChanged || getEnergyStored(oldStack) > 0 != getEnergyStored(newStack) > 0 || getLoadedAmmoCount(oldStack) != getLoadedAmmoCount(newStack));
    }

    // region LOADING
    private List<ItemStack> getAmmoQueue(ItemStack crossbow) {

        var projectiles = crossbow.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        if (!projectiles.isEmpty()) {
            return projectiles.getItems();
        }
        var legacy = crossbow.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!legacy.contains(TAG_AMMO)) {
            return List.of();
        }
        var tags = legacy.copyTag().getList(TAG_AMMO, TAG_COMPOUND);
        var registries = StackData.builtInRegistries();
        List<ItemStack> loaded = new ArrayList<>(tags.size());
        for (int i = 0; i < tags.size(); i++) {
            var ammo = StackData.readItem(registries, tags.getCompound(i));
            if (!ammo.isEmpty()) {
                loaded.add(ammo);
            }
        }
        return loaded;
    }

    private void setAmmoQueue(ItemStack crossbow, List<ItemStack> queue) {

        if (crossbow.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(TAG_AMMO)) {
            CustomData.update(DataComponents.CUSTOM_DATA, crossbow, tag -> tag.remove(TAG_AMMO));
        }
        if (queue.isEmpty()) {
            crossbow.remove(DataComponents.CHARGED_PROJECTILES);
        } else {
            crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(queue));
        }
    }

    public List<ItemStack> getAllLoadedAmmo(ItemStack crossbow) {

        return getAmmoQueue(crossbow);
    }

    public int getLoadedAmmoCount(ItemStack crossbow) {

        return getAmmoQueue(crossbow).size();
    }

    @Override
    public boolean loadAmmo(Player player, ItemStack crossbow, ItemStack ammo) {

        var queue = new ArrayList<>(getAmmoQueue(crossbow));
        if (ammo.isEmpty() || queue.size() >= maxCharges) {
            return false;
        }
        queue.add(ammo.copyWithCount(1));
        setAmmoQueue(crossbow, queue);
        return true;
    }

    @Override
    public ItemStack getLoadedAmmo(ItemStack crossbow) {

        var queue = getAmmoQueue(crossbow);
        return queue.isEmpty() ? ItemStack.EMPTY : queue.getFirst();
    }

    @Override
    public void removeLoadedAmmo(ItemStack crossbow) {

        var queue = getAmmoQueue(crossbow);
        if (!queue.isEmpty()) {
            setAmmoQueue(crossbow, queue.subList(1, queue.size()));
        }
    }
    // endregion

    // region DURABILITY BAR
    @Override
    public boolean isBarVisible(ItemStack stack) {

        return IMultiModeFluxItem.super.isBarVisible(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {

        return IMultiModeFluxItem.super.getBarColor(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {

        return IMultiModeFluxItem.super.getBarWidth(stack);
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

    // region ILeftClickHandlerItem
    @Override
    public void onLeftClick(Player player, ItemStack stack) {

        if (getLoadedAmmoCount(stack) > 0) {
            if (!stack.has(DataComponents.CHARGED_PROJECTILES)) {
                setAmmoQueue(stack, getAmmoQueue(stack));
            }
            if (isEmpowered(stack)) {
                float xRot = player.getXRot();
                int count = getLoadedAmmoCount(stack);
                for (int i = (1 - count) / 2; i <= count / 2; ++i) {
                    player.setXRot(xRot - 10 * i);
                    shootLoadedAmmo(player.level(), player, InteractionHand.MAIN_HAND, stack);
                }
                player.setXRot(xRot);
            } else {
                shootLoadedAmmo(player.level(), player, InteractionHand.MAIN_HAND, stack);
            }
        }
    }
    // endregion

}
