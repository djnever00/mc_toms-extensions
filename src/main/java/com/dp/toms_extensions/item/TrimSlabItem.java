package com.dp.toms_extensions.item;

import com.dp.toms_extensions.config.ModConfigData;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public class TrimSlabItem extends BlockItem {
    public TrimSlabItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!ModConfigData.VALUES.enableTrimSlabs.get() || !ModConfigData.VALUES.allowTrimSlabPlacement.get()) {
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }
}