package com.dp.toms_extensions.registry;

import com.dp.toms_extensions.TomsSimpleStorageExtensions;
import com.dp.toms_extensions.item.TrimSlabItem;
import com.tom.storagemod.item.PaintedBlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TomsSimpleStorageExtensions.MOD_ID);

    public static final RegistryObject<Item> TRIM_SLAB_ITEM =
            ITEMS.register("trim_slab", () -> new TrimSlabItem(ModBlocks.TRIM_SLAB.get(), new Item.Properties()));

    public static final RegistryObject<Item> PAINTED_TRIM_SLAB_ITEM =
            ITEMS.register("painted_trim_slab",
                    () -> PaintedBlockItem.makeHidden().apply(ModBlocks.PAINTED_TRIM_SLAB.get()));

    public static void register(IEventBus eventBus) {
            ITEMS.register(eventBus);
    }
}