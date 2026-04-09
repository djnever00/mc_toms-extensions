package com.dp.toms_extensions.util;

import com.dp.toms_extensions.config.ModConfigData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FacadeCompatOverrides {
    private FacadeCompatOverrides() {
    }

    private static final Map<ResourceLocation, ResourceLocation> SLAB_TO_FULL = new HashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> FULL_TO_SLAB = new HashMap<>();
    private static boolean loaded = false;

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }

        FULL_TO_SLAB.clear();
        SLAB_TO_FULL.clear();

        List<? extends String> entries = ModConfigData.VALUES.facadeOverrides.get();
        for (String entry : entries) {
            parseAndAdd(entry);
        }

        loaded = true;
    }

    public static void reload() {
        loaded = false;
        ensureLoaded();
    }

    private static void parseAndAdd(String entry) {
        if (entry == null) {
            return;
        }

        String trimmed = entry.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        int split = trimmed.indexOf('=');
        if (split <= 0 || split >= trimmed.length() - 1) {
            return;
        }

        String fullId = trimmed.substring(0, split).trim();
        String slabId = trimmed.substring(split + 1).trim();

        try {
            ResourceLocation full = ResourceLocation.parse(fullId);
            ResourceLocation slab = ResourceLocation.parse(slabId);

            FULL_TO_SLAB.put(full, slab);
            SLAB_TO_FULL.put(slab, full);
        } catch (Exception ignored) {
        }
    }

    @Nullable
    public static BlockState tryResolveFullFromSlab(BlockState slabState) {
        ensureLoaded();

        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(slabState.getBlock());
        if (key == null) {
            return null;
        }

        ResourceLocation mapped = SLAB_TO_FULL.get(key);
        if (mapped == null) {
            return null;
        }

        Block block = ForgeRegistries.BLOCKS.getValue(mapped);
        return block != null ? block.defaultBlockState() : null;
    }

    @Nullable
    public static BlockState tryResolveSlabFromFull(BlockState fullState) {
        ensureLoaded();

        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(fullState.getBlock());
        if (key == null) {
            return null;
        }

        ResourceLocation mapped = FULL_TO_SLAB.get(key);
        if (mapped == null) {
            return null;
        }

        Block block = ForgeRegistries.BLOCKS.getValue(mapped);
        return block != null ? block.defaultBlockState() : null;
    }
}