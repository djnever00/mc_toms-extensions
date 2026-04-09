package com.dp.toms_extensions.registry;

import com.dp.toms_extensions.block.MagmaTrimSlabBlock;
import com.dp.toms_extensions.TomsSimpleStorageExtensions;
import com.dp.toms_extensions.block.PaintedTrimSlabBlock;
import com.dp.toms_extensions.block.TrimSlabBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TomsSimpleStorageExtensions.MOD_ID);

    public static final RegistryObject<Block> TRIM_SLAB =
            BLOCKS.register("trim_slab", TrimSlabBlock::new);

    public static final RegistryObject<Block> PAINTED_TRIM_SLAB =
            BLOCKS.register("painted_trim_slab", PaintedTrimSlabBlock::new);

    public static final RegistryObject<Block> MAGMA_TRIM_SLAB =
            BLOCKS.register("magma_trim_slab", MagmaTrimSlabBlock::new);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
