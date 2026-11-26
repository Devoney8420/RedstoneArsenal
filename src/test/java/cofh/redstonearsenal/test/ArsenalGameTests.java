package cofh.redstonearsenal.test;

import net.minecraft.world.InteractionHand;
import cofh.redstonearsenal.common.capability.CapabilityFluxShielding;
import cofh.redstonearsenal.common.item.FluxCrossbowItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@GameTestHolder("redstone_arsenal_tests")
@PrefixGameTestTemplate(false)
public final class ArsenalGameTests {

    @GameTest(template = "arsenal")
    public static void fluxGearEnchantsAndUsesEnergy(GameTestHelper test) {

        var enchantments = test.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var sword = stack("flux_sword");
        var hammer = stack("flux_hammer");
        var bow = stack("flux_bow");
        var helmet = stack("flux_helmet");
        test.assertTrue(sword.isPrimaryItemFor(enchantments.getHolderOrThrow(Enchantments.SHARPNESS)), "Sword missing Sharpness");
        test.assertTrue(hammer.isPrimaryItemFor(enchantments.getHolderOrThrow(Enchantments.EFFICIENCY)), "Hammer missing Efficiency");
        test.assertTrue(bow.isPrimaryItemFor(enchantments.getHolderOrThrow(Enchantments.POWER)), "Bow missing Power");
        test.assertTrue(helmet.isPrimaryItemFor(enchantments.getHolderOrThrow(Enchantments.RESPIRATION)), "Helmet missing Respiration");
        test.assertTrue(attackDamage(sword) == 0, "Uncharged sword should have no attack modifier");
        var energy = Objects.requireNonNull(sword.getCapability(Capabilities.EnergyStorage.ITEM));
        test.assertTrue(energy.receiveEnergy(1000, false) == 1000, "Sword did not accept energy");
        test.assertTrue(attackDamage(sword) > 0, "Charged sword lost attack damage");
        test.succeed();
    }

    @GameTest(template = "arsenal")
    public static void crossbowKeepsThreeRoundsInOrder(GameTestHelper test) {

        var crossbow = stack("flux_crossbow");
        var item = (FluxCrossbowItem) crossbow.getItem();
        var player = test.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        test.assertTrue(item.loadAmmo(player, crossbow, new ItemStack(Items.ARROW)), "First round rejected");
        test.assertTrue(item.loadAmmo(player, crossbow, new ItemStack(Items.SPECTRAL_ARROW)), "Second round rejected");
        test.assertTrue(item.loadAmmo(player, crossbow, new ItemStack(Items.FIREWORK_ROCKET)), "Third round rejected");
        test.assertTrue(!item.loadAmmo(player, crossbow, new ItemStack(Items.ARROW)), "Crossbow accepted a fourth round");
        var saved = ItemStack.parse(test.getLevel().registryAccess(), crossbow.save(test.getLevel().registryAccess())).orElseThrow();
        test.assertTrue(item.getLoadedAmmo(saved).is(Items.ARROW), "First round did not survive saving");
        var energy = Objects.requireNonNull(saved.getCapability(Capabilities.EnergyStorage.ITEM));
        energy.receiveEnergy(4000, false);
        int charged = energy.getEnergyStored();
        test.assertTrue(item.shootLoadedAmmo(test.getLevel(), player, InteractionHand.MAIN_HAND, saved), "First round did not fire");
        test.assertTrue(energy.getEnergyStored() < charged, "Firing did not consume energy");
        test.assertTrue(item.getLoadedAmmo(saved).is(Items.SPECTRAL_ARROW), "Round order changed");
        item.removeLoadedAmmo(saved);
        test.assertTrue(item.getLoadedAmmo(saved).is(Items.FIREWORK_ROCKET), "Third round was lost");
        item.removeLoadedAmmo(saved);
        test.assertTrue(item.getLoadedAmmoCount(saved) == 0, "Final round was not removed");
        test.succeed();
    }

    @GameTest(template = "arsenal")
    public static void armorShieldUsesEnergyAndPersistsCooldown(GameTestHelper test) {

        var armor = stack("flux_chestplate");
        var energy = Objects.requireNonNull(armor.getCapability(Capabilities.EnergyStorage.ITEM));
        test.assertTrue(energy.receiveEnergy(10000, false) > 2000, "Armor did not charge");
        int charged = energy.getEnergyStored();
        var target = test.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 1));
        var shielding = Objects.requireNonNull(armor.getCapability(CapabilityFluxShielding.ITEM));
        test.assertTrue(shielding.curCharges(target) == 1, "Charged armor should provide a shield");
        test.assertTrue(shielding.useCharge(target), "Shield did not absorb the hit");
        test.assertTrue(energy.getEnergyStored() < charged, "Shield use did not consume FE");
        var saved = ItemStack.parse(test.getLevel().registryAccess(), armor.save(test.getLevel().registryAccess())).orElseThrow();
        var restored = Objects.requireNonNull(saved.getCapability(CapabilityFluxShielding.ITEM));
        test.assertTrue(restored.curCharges(target) == 0, "Cooldown was lost when saving armor");
        test.succeed();
    }

    @GameTest(template = "arsenal")
    public static void recipesAndProjectilesLoad(GameTestHelper test) {

        for (String path : new String[]{"flux_sword", "flux_quiver", "materials/obsidian_rod"}) {
            test.assertTrue(test.getLevel().getRecipeManager().byKey(id(path)).isPresent(), "Missing recipe " + path);
        }
        var recipes = test.getLevel().getRecipeManager();
        boolean thermalLoaded = ModList.get().isLoaded("thermal");
        test.assertTrue(recipes.byKey(id("materials/flux_ingot_fire_charge")).isPresent() != thermalLoaded,
                "Gold ingot fallback should only load when Thermal is absent");
        test.assertTrue(recipes.byKey(id("materials/flux_dust")).isPresent() == thermalLoaded,
                "Gold dust recipe should load when Thermal supplies gold dust");
        for (String path : new String[]{"flux_slash", "flux_arrow", "flux_trident", "flux_wrench", "shockwave"}) {
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(id(path)).orElseThrow();
            test.assertTrue(type.create(test.getLevel()) != null, "Could not initialize " + path);
        }
        test.succeed();
    }

    private static double attackDamage(ItemStack stack) {

        var damage = new AtomicReference<>(0.0);
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) {
                damage.set(modifier.amount());
            }
        });
        return damage.get();
    }

    private static ItemStack stack(String path) {

        return new ItemStack(BuiltInRegistries.ITEM.getOptional(id(path)).orElseThrow());
    }

    private static ResourceLocation id(String path) {

        return ResourceLocation.fromNamespaceAndPath("redstone_arsenal", path);
    }

}
