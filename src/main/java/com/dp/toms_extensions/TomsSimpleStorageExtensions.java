package com.dp.toms_extensions;

import com.dp.toms_extensions.config.ModConfigData;
import com.dp.toms_extensions.data.condition.AllowTrimSlabRecipesCondition;
import com.dp.toms_extensions.registry.ModBlockEntities;
import com.dp.toms_extensions.registry.ModBlocks;
import com.dp.toms_extensions.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TomsSimpleStorageExtensions.MOD_ID)
public class TomsSimpleStorageExtensions {
    public static final String MOD_ID = "toms_extensions";
    private static final ResourceKey<CreativeModeTab> TOMS_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("toms_storage", "tab"));

    public TomsSimpleStorageExtensions() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ModConfigData.SPEC);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addCreative);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> CraftingHelper.register(AllowTrimSlabRecipesCondition.Serializer.INSTANCE));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (!ModConfigData.VALUES.enableTrimSlabs.get()) {
            return;
        }

        if (event.getTabKey().equals(TOMS_TAB_KEY)) {
            event.accept(ModItems.TRIM_SLAB_ITEM.get());
        }
    }
}
