package cofh.redstonearsenal.init.data;

import cofh.redstonearsenal.init.data.providers.ModBlockStateProvider;
import cofh.redstonearsenal.init.data.providers.ModItemModelProvider;
import cofh.redstonearsenal.init.data.providers.ModLootTableProvider;
import cofh.redstonearsenal.init.data.providers.ModRecipeProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static cofh.lib.util.constants.ModIds.ID_REDSTONE_ARSENAL;

@EventBusSubscriber(modid = ID_REDSTONE_ARSENAL)
public class ModDataGen {

    @SubscribeEvent
    public static void gatherData(final GatherDataEvent event) {

        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        ExistingFileHelper exFileHelper = event.getExistingFileHelper();

        gen.addProvider(event.includeServer(), new ModLootTableProvider(output, event.getLookupProvider()));
        gen.addProvider(event.includeServer(), new ModRecipeProvider(output, event.getLookupProvider()));

        gen.addProvider(event.includeClient(), new ModBlockStateProvider(output, exFileHelper));
        gen.addProvider(event.includeClient(), new ModItemModelProvider(output, exFileHelper));
    }

}
