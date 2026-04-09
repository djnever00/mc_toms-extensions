package com.dp.toms_extensions.client;

import com.dp.toms_extensions.TomsSimpleStorageExtensions;
import com.dp.toms_extensions.blockentity.PaintedTrimSlabBlockEntity;
import com.dp.toms_extensions.client.model.PaintedTrimSlabBakedModel;
import com.dp.toms_extensions.client.model.TrimSlabBakedModel;
import com.dp.toms_extensions.registry.ModBlocks;
import com.dp.toms_extensions.util.FacadeRuntimeHelper;
import com.dp.toms_extensions.util.FacadeStateHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TomsSimpleStorageExtensions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TomsExtensionsClient {

    @SubscribeEvent
    public static void bakeModels(ModelEvent.ModifyBakingResult event) {
        for (BlockState state : ModBlocks.TRIM_SLAB.get().getStateDefinition().getPossibleStates()) {
            ModelResourceLocation loc = BlockModelShaper.stateToModelLocation(state);
            BakedModel existing = event.getModels().get(loc);
            if (existing != null) {
                event.getModels().put(loc, new TrimSlabBakedModel(existing));
            }
        }

        for (BlockState state : ModBlocks.PAINTED_TRIM_SLAB.get().getStateDefinition().getPossibleStates()) {
            ModelResourceLocation loc = BlockModelShaper.stateToModelLocation(state);
            BakedModel existing = event.getModels().get(loc);
            if (existing != null) {
                event.getModels().put(loc, new PaintedTrimSlabBakedModel(existing));
            }
        }
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(TomsExtensionsClient::getPaintedColor, ModBlocks.PAINTED_TRIM_SLAB.get());
    }

    private static int getPaintedColor(BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
        if (level == null || pos == null) {
            return -1;
        }

        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PaintedTrimSlabBlockEntity painted) {
                BlockState facade = FacadeRuntimeHelper.resolveRuntimeFacade(state, level, pos);
                if (facade != null) {
                    BlockColors colors = Minecraft.getInstance().getBlockColors();
                    int color = colors.getColor(facade, level, pos, tintIndex);
                    if (color != -1) {
                        return color;
                    }

                    BlockState full = FacadeStateHelper.tryResolveFullBlockCounterpart(facade);
                    if (full != null) {
                        return colors.getColor(full, level, pos, tintIndex);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return -1;
    }
}
