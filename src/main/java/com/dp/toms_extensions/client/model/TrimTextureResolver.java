package com.dp.toms_extensions.client.model;

import com.dp.toms_extensions.TomsSimpleStorageExtensions;
import com.dp.toms_extensions.config.ModConfigData;
import com.dp.toms_extensions.config.TextureSource;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

public final class TrimTextureResolver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<ResourceLocation> REPORTED_MISSING = new HashSet<>();

    private static final ResourceLocation TOMS_TRIM =
            ResourceLocation.fromNamespaceAndPath("toms_storage", "block/trim");
    private static final ResourceLocation ADDON_TRIM =
            ResourceLocation.fromNamespaceAndPath("toms_extensions", "block/trim");
    private static final ResourceLocation ADDON_TRIM_SLAB_1 =
            ResourceLocation.fromNamespaceAndPath("toms_extensions", "block/trim_slab_1");
    private static final ResourceLocation ADDON_TRIM_SLAB_2 =
            ResourceLocation.fromNamespaceAndPath("toms_extensions", "block/trim_slab_2");

    private TrimTextureResolver() {
    }

    public static ResourceLocation resolveTopBottom() {
        return resolveWithFallback(
                ModConfigData.VALUES.trimTopBottomSource.get(),
                TOMS_TRIM,
                ADDON_TRIM
        );
    }

    public static ResourceLocation resolveDoubleSlab() {
        return resolveWithFallback(
                ModConfigData.VALUES.trimDoubleSlabSource.get(),
                TOMS_TRIM,
                ADDON_TRIM
        );
    }

    public static ResourceLocation resolveSide() {
        ResourceLocation requested = ModConfigData.VALUES.trimSlabSideVariant.get() == 2
                ? ADDON_TRIM_SLAB_2
                : ADDON_TRIM_SLAB_1;

        if (textureExists(requested)) {
            return requested;
        }

        ResourceLocation fallback = requested.equals(ADDON_TRIM_SLAB_2) ? ADDON_TRIM_SLAB_1 : ADDON_TRIM_SLAB_2;
        if (textureExists(fallback)) {
            logFallback(requested, fallback);
            return fallback;
        }

        logFallback(requested, ADDON_TRIM);
        return ADDON_TRIM;
    }

    private static ResourceLocation resolveWithFallback(TextureSource source, ResourceLocation toms, ResourceLocation addon) {
        ResourceLocation requested = source == TextureSource.TOMS ? toms : addon;
        ResourceLocation fallback = source == TextureSource.TOMS ? addon : toms;

        if (textureExists(requested)) {
            return requested;
        }

        if (textureExists(fallback)) {
            logFallback(requested, fallback);
            return fallback;
        }

        logFallback(requested, addon);
        return addon;
    }

    private static boolean textureExists(ResourceLocation texture) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(texture);

        return sprite != null && !sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation());
    }

    private static void logFallback(ResourceLocation requested, ResourceLocation fallback) {
        if (REPORTED_MISSING.add(requested)) {
            LOGGER.warn("[{}] Requested texture {} was not found in the block atlas. Falling back to {}.",
                    TomsSimpleStorageExtensions.MOD_ID, requested, fallback);
        }
    }
}