package com.dp.toms_extensions.event;

import com.dp.toms_extensions.TomsSimpleStorageExtensions;
import com.dp.toms_extensions.registry.ModBlockEntities;
import com.dp.toms_extensions.registry.ModBlocks;
import com.dp.toms_extensions.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

@Mod.EventBusSubscriber(modid = TomsSimpleStorageExtensions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LegacyMissingMappings {
    private static final String[] OLD_NAMESPACES = {
            "tss_trim_slab",
            "toms_trim_slab"
    };

    private LegacyMissingMappings() {
    }

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        for (String oldNs : OLD_NAMESPACES) {
            remapBlocks(event, oldNs);
            remapItems(event, oldNs);
            remapBlockEntities(event, oldNs);
        }
    }

    private static void remapBlocks(MissingMappingsEvent event, String oldNs) {
        for (MissingMappingsEvent.Mapping<Block> mapping : event.getMappings(ForgeRegistries.Keys.BLOCKS, oldNs)) {
            ResourceLocation key = mapping.getKey();
            if (key == null) {
                continue;
            }

            switch (key.getPath()) {
                case "trim_slab" -> mapping.remap(ModBlocks.TRIM_SLAB.get());
                case "painted_trim_slab" -> mapping.remap(ModBlocks.PAINTED_TRIM_SLAB.get());
            }
        }
    }

    private static void remapItems(MissingMappingsEvent event, String oldNs) {
        for (MissingMappingsEvent.Mapping<Item> mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, oldNs)) {
            ResourceLocation key = mapping.getKey();
            if (key == null) {
                continue;
            }

            switch (key.getPath()) {
                case "trim_slab" -> mapping.remap(ModItems.TRIM_SLAB_ITEM.get());
                case "painted_trim_slab" -> mapping.remap(ModItems.PAINTED_TRIM_SLAB_ITEM.get());
            }
        }
    }

    private static void remapBlockEntities(MissingMappingsEvent event, String oldNs) {
        for (MissingMappingsEvent.Mapping<BlockEntityType<?>> mapping : event.getMappings(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES, oldNs)) {
            ResourceLocation key = mapping.getKey();
            if (key == null) {
                continue;
            }

            if ("painted_trim_slab".equals(key.getPath())) {
                mapping.remap(ModBlockEntities.PAINTED_TRIM_SLAB_BE.get());
            }
        }
    }
}
