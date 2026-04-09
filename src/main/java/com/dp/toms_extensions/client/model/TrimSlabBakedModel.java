

package com.dp.toms_extensions.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;

public class TrimSlabBakedModel implements IDynamicBakedModel {
    private final BakedModel fallback;

    public TrimSlabBakedModel(BakedModel fallback) {
        this.fallback = fallback;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
        if (state == null || !state.hasProperty(SlabBlock.TYPE)) {
            return fallback.getQuads(state, side, rand, data, renderType);
        }

        List<BakedQuad> hostQuads = fallback.getQuads(state, side, rand, data, renderType);
        if (hostQuads.isEmpty()) {
            return hostQuads;
        }

        SlabType slabType = state.getValue(SlabBlock.TYPE);

        TextureAtlasSprite topBottomSprite = getSprite(TrimTextureResolver.resolveTopBottom());
        TextureAtlasSprite doubleSlabSprite = getSprite(TrimTextureResolver.resolveDoubleSlab());
        TextureAtlasSprite sideSprite = getSprite(TrimTextureResolver.resolveSide());

        List<BakedQuad> out = new ArrayList<>(hostQuads.size());

        for (BakedQuad hostQuad : hostQuads) {
            Direction face = hostQuad.getDirection();

            if (slabType == SlabType.DOUBLE) {
                out.add(QuadUvHelper.remapQuadToSprite(
                        hostQuad,
                        doubleSlabSprite,
                        hostQuad.getTintIndex(),
                        face,
                        slabType,
                        true,
                        QuadUvHelper.SideSamplingProfile.DEFAULT
                ));
                continue;
            }

            if (face == Direction.UP || face == Direction.DOWN) {
                out.add(QuadUvHelper.remapQuadToSprite(
                        hostQuad,
                        topBottomSprite,
                        hostQuad.getTintIndex(),
                        face,
                        slabType,
                        true,
                        QuadUvHelper.SideSamplingProfile.DEFAULT
                ));
            } else {
                out.add(QuadUvHelper.remapQuadToSprite(
                        hostQuad,
                        sideSprite,
                        hostQuad.getTintIndex(),
                        face,
                        slabType,
                        false,
                        QuadUvHelper.SideSamplingProfile.TOP_HALF
                ));
            }
        }

        return out;
    }

    private TextureAtlasSprite getSprite(ResourceLocation texture) {
        return Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(texture);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, net.minecraft.core.BlockPos pos, BlockState state, ModelData data) {
        return data;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return fallback.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return fallback.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return fallback.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return fallback.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return getSprite(TrimTextureResolver.resolveTopBottom());
    }

    @Override
    public ItemOverrides getOverrides() {
        return fallback.getOverrides();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return fallback.getRenderTypes(state, rand, data);
    }
}
