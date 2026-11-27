package cofh.redstonearsenal.common.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlotGroup;
import cofh.core.common.config.CoreClientConfig;
import cofh.core.common.item.ILeftClickHandlerItem;
import cofh.core.common.item.SickleItem;
import cofh.core.util.ProxyUtils;
import cofh.core.util.helpers.AreaEffectHelper;
import cofh.lib.api.capability.IAreaEffectHandler;
import cofh.lib.common.energy.EnergyContainerItemWrapper;
import cofh.lib.util.Utils;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static cofh.lib.util.helpers.StringHelper.getTextComponent;

public class FluxSickleItem extends SickleItem implements IMultiModeFluxItem, ILeftClickHandlerItem {

    protected static final Set<ResourceKey<Enchantment>> VALID_ENCHANTS = new ObjectOpenHashSet<>();
    protected final float damage;
    protected final float attackSpeed;

    protected final int maxEnergy;
    protected final int extract;
    protected final int receive;

    public FluxSickleItem(Tier tier, float attackDamageIn, float attackSpeedIn, Properties builder, int energy, int xfer) {

        super(tier, attackDamageIn, attackSpeedIn, builder);

        this.damage = attackDamageIn + tier.getAttackDamageBonus();
        this.attackSpeed = attackSpeedIn;

        this.maxEnergy = energy;
        this.extract = xfer;
        this.receive = xfer;

        ProxyUtils.registerItemModelProperty(this, ResourceLocation.parse("charged"), this::getChargedModelProperty);
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

    public static void initEnchants() {

        VALID_ENCHANTS.add(Enchantments.SWEEPING_EDGE);
        VALID_ENCHANTS.add(Enchantments.LOOTING);
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.supportsEnchantment(stack, enchantment) || VALID_ENCHANTS.stream().anyMatch(enchantment::is);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {

        return super.isPrimaryItemFor(stack, enchantment) || VALID_ENCHANTS.stream().anyMatch(enchantment::is);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {

        return getEnchantmentValue(stack) > 0;
    }

    @Override
    public AABB getSweepHitBox(ItemStack stack, Player player, Entity target) {

        return target.getBoundingBox().inflate(radius + 0.25, height + 0.25, radius + 0.25);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {

        return isEmpowered(stack) && entity instanceof LivingEntity living && living.isBaby();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        useEnergy(stack, false, attacker);
        return true;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving) {

        if (Utils.isServerWorld(worldIn) && !state.is(BlockTags.FIRE)) {
            useEnergy(stack, false, entityLiving);
        }
        return true;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {

        return hasEnergy(stack, false) && (super.isCorrectToolForDrops(stack, state) || state.getBlock() instanceof HugeMushroomBlock);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {

        return isCorrectToolForDrops(stack, state) ? getEfficiency(stack) : 0.0F;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {

        ItemStack stack = player.getMainHandItem();
        return !player.isCreative() || isCorrectToolForDrops(stack, state) && (!isEmpowered(stack) || AreaEffectHelper.isMature(level, pos));
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {

        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, getAttackDamage(stack), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, getAttackSpeed(stack), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    protected float getAttackDamage(ItemStack stack) {

        return hasEnergy(stack, false) ? damage : 0.0F;
    }

    protected float getAttackSpeed(ItemStack stack) {

        return attackSpeed;
    }

    public float getEfficiency(ItemStack stack) {

        return getTier().getSpeed();
    }

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

    // region CAPABILITY WRAPPER
    public static class AreaWrapper extends EnergyContainerItemWrapper implements IAreaEffectHandler {

        protected final FluxSickleItem sickle;

        public AreaWrapper(ItemStack containerIn, FluxSickleItem itemIn) {

            super(containerIn, itemIn);
            sickle = itemIn;
        }

        @Override
        public ImmutableList<BlockPos> getAreaEffectBlocks(BlockPos pos, Player player) {

            if (sickle.hasEnergy(container, false)) {
                if (sickle.isEmpowered(container)) {
                    return AreaEffectHelper.getMatureBlocksCentered(container, pos, player, sickle.radius, sickle.height);
                }
                return AreaEffectHelper.getBlocksCentered(container, pos, player, sickle.radius, sickle.height);
            }
            return ImmutableList.of();
        }

    }
    // endregion

    // region ILeftClickHandlerItem
    @Override
    public void onLeftClick(Player player, ItemStack stack) {

        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float strength = player.getAttackStrengthScale(0.5F);
        if (strength < 0.9F || player.isSprinting() || player.isSecondaryUseActive() || !hasEnergy(stack, false)) {
            return;
        }
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (0.2F + strength * strength * 0.8F);
        float sweep = (float) player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO);
        boolean empowered = isEmpowered(stack);
        boolean hit = false;
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, stack.getSweepHitBox(player, player))) {
            if (target == player || player.isAlliedTo(target) || (empowered && target.isBaby()) ||
                    (target instanceof ArmorStand stand && stand.isMarker()) || !player.canInteractWithEntity(target, 0)) {
                continue;
            }
            hit = true;
            Vec3 disp = player.position().subtract(target.position());
            target.knockback(0.3F, disp.x, disp.z);
            var source = serverLevel.damageSources().playerAttack(player);
            float bonus = (EnchantmentHelper.modifyDamage(serverLevel, stack, target, source, damage) - damage) * strength;
            target.hurt(source, 1.0F + (damage + bonus) * sweep);
        }
        if (hit) {
            useEnergy(stack, false, player);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0F, 1.0F);
        player.sweepAttack();
    }
    // endregion

}
