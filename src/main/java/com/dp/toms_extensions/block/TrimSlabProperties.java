package com.dp.toms_extensions.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.ForgeRegistries;

public final class TrimSlabProperties {
    private static final ResourceLocation TOMS_TRIM_ID =
            ResourceLocation.fromNamespaceAndPath("toms_storage", "ts.trim");

    private TrimSlabProperties() {
    }

    public static BlockBehaviour.Properties create() {
        Block tomsTrim = ForgeRegistries.BLOCKS.getValue(TOMS_TRIM_ID);
        if (tomsTrim != null) {
            return BlockBehaviour.Properties.copy(tomsTrim);
        }

        return fallback();
    }

    private static BlockBehaviour.Properties fallback() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0F)
                .sound(SoundType.METAL);
    }
}
