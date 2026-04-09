package com.dp.toms_extensions.registry;

import com.dp.toms_extensions.TomsSimpleStorageExtensions;
import com.dp.toms_extensions.blockentity.PaintedTrimSlabBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TomsSimpleStorageExtensions.MOD_ID);

    public static final RegistryObject<BlockEntityType<PaintedTrimSlabBlockEntity>> PAINTED_TRIM_SLAB_BE =
            BLOCK_ENTITIES.register("painted_trim_slab",
                    () -> BlockEntityType.Builder.of(
                            PaintedTrimSlabBlockEntity::new,
                            ModBlocks.PAINTED_TRIM_SLAB.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
