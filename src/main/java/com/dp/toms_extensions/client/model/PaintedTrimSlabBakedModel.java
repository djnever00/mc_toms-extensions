package com.dp.toms_extensions.client.model;

import com.tom.storagemod.tile.PaintedBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaintedTrimSlabBakedModel implements IDynamicBakedModel {
    private final BakedModel fallback;

    public PaintedTrimSlabBakedModel(BakedModel fallback) {
        this.fallback = fallback;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
        FacadeRenderController.Plan plan = FacadeRenderController.resolve(state, data);
        if (plan == null) {
            return fallback.getQuads(state, side, rand, data, renderType);
        }

        if (!supportsSourceRenderType(plan, rand, renderType)) {
            return Collections.emptyList();
        }

        List<BakedQuad> picked = plan.textureRoute() == FacadeRenderController.TextureRoute.DIRECT_MODEL
                ? paintDirect(plan, side, rand, renderType)
                : paintSampled(plan, side, rand, data, renderType);

        return picked;
    }

    private List<BakedQuad> paintDirect(
            FacadeRenderController.Plan plan,
            Direction side,
            RandomSource rand,
            RenderType renderType
    ) {
        BakedModel model = getSourceModel(plan);
        return model.getQuads(plan.renderState(), side, rand, ModelData.EMPTY, renderType);
    }

    private List<BakedQuad> paintSampled(
            FacadeRenderController.Plan plan,
            Direction side,
            RandomSource rand,
            ModelData data,
            RenderType renderType
    ) {
        List<BakedQuad> hostQuads = fallback.getQuads(plan.hostState(), side, rand, data, renderType);
        if (hostQuads.isEmpty() && renderType != null) {
            hostQuads = fallback.getQuads(plan.hostState(), side, rand, data, null);
        }
        if (hostQuads.isEmpty()) {
            return hostQuads;
        }

        BakedModel sourceModel = getSourceModel(plan);
        List<BakedQuad> out = new ArrayList<>(hostQuads.size());

        for (BakedQuad hostQuad : hostQuads) {
            List<BakedQuad> layered = tryPaintLayeredGrassSide(plan, hostQuad, sourceModel, rand, renderType);
            if (layered != null) {
                out.addAll(layered);
                continue;
            }

            boolean horizontal = hostQuad.getDirection().getAxis().isHorizontal();
            boolean preferUntinted = plan.preferUntintedSides() && horizontal;
            BakedQuad sourceQuad = resolveFaceQuad(
                    sourceModel,
                    plan.samplingState(),
                    hostQuad.getDirection(),
                    rand,
                    renderType,
                    preferUntinted
            );

            if (sourceQuad == null) {
                out.add(hostQuad);
                continue;
            }

            int tintIndex = preferUntinted ? -1 : sourceQuad.getTintIndex();
            out.add(QuadUvHelper.remapQuadToSprite(
                    hostQuad,
                    sourceQuad.getSprite(),
                    tintIndex,
                    hostQuad.getDirection(),
                    plan.hostType(),
                    false,
                    plan.samplingProfile()
            ));
        }

        return out;
    }

    private List<BakedQuad> tryPaintLayeredGrassSide(
            FacadeRenderController.Plan plan,
            BakedQuad hostQuad,
            BakedModel sourceModel,
            RandomSource rand,
            RenderType renderType
    ) {
        if (!isLayeredGrassFacade(plan) || !hostQuad.getDirection().getAxis().isHorizontal()) {
            return null;
        }

        List<BakedQuad> faceQuads = sourceModel.getQuads(
                plan.samplingState(),
                hostQuad.getDirection(),
                rand,
                ModelData.EMPTY,
                renderType
        );
        if (faceQuads == null || faceQuads.isEmpty()) {
            return null;
        }

        List<BakedQuad> out = new ArrayList<>(faceQuads.size());
        for (BakedQuad sourceQuad : faceQuads) {
            out.add(QuadUvHelper.remapQuadToSprite(
                    hostQuad,
                    sourceQuad.getSprite(),
                    sourceQuad.getTintIndex(),
                    hostQuad.getDirection(),
                    plan.hostType(),
                    false,
                    plan.samplingProfile()
            ));
        }
        return out;
    }

    private BakedModel getSourceModel(FacadeRenderController.Plan plan) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(plan.sourceRenderState());
    }

    private boolean isLayeredGrassFacade(FacadeRenderController.Plan plan) {
        if (plan == null || plan.facadeProfile() == null) {
            return false;
        }
        ResourceLocation key = plan.facadeProfile().canonicalKey();
        return key != null && key.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "grass_block"));
    }

    private BakedQuad resolveFaceQuad(
            BakedModel model,
            BlockState state,
            Direction face,
            RandomSource rand,
            RenderType renderType,
            boolean preferUntinted
    ) {
        List<BakedQuad> faceQuads = model.getQuads(state, face, rand, ModelData.EMPTY, renderType);
        if ((faceQuads == null || faceQuads.isEmpty()) && renderType != null) {
            faceQuads = model.getQuads(state, face, rand, ModelData.EMPTY, null);
        }
        BakedQuad bestFace = choosePreferredQuad(faceQuads, preferUntinted, face);
        if (bestFace != null) {
            return bestFace;
        }

        List<BakedQuad> general = model.getQuads(state, null, rand, ModelData.EMPTY, renderType);
        if (general.isEmpty() && renderType != null) {
            general = model.getQuads(state, null, rand, ModelData.EMPTY, null);
        }
        if (general.isEmpty()) {
            return null;
        }

        List<BakedQuad> matching = new ArrayList<>();
        for (BakedQuad quad : general) {
            if (quad.getDirection() == face) {
                matching.add(quad);
            }
        }

        BakedQuad bestGeneral = choosePreferredQuad(matching, preferUntinted, face);
        return bestGeneral != null ? bestGeneral : general.get(0);
    }

    private BakedQuad choosePreferredQuad(List<BakedQuad> quads, boolean preferUntinted, Direction face) {
        if (quads == null || quads.isEmpty()) {
            return null;
        }

        if (!preferUntinted) {
            return quads.get(0);
        }

        for (BakedQuad quad : quads) {
            if (quad.getTintIndex() < 0 && quad.getDirection() == face) {
                return quad;
            }
        }
        for (BakedQuad quad : quads) {
            if (quad.getTintIndex() < 0) {
                return quad;
            }
        }
        return quads.get(0);
    }

    private boolean supportsSourceRenderType(
            FacadeRenderController.Plan plan,
            RandomSource rand,
            RenderType renderType
    ) {
        if (renderType == null) {
            return true;
        }

        BakedModel sourceModel = getSourceModel(plan);
        ChunkRenderTypeSet set = sourceModel.getRenderTypes(plan.sourceRenderState(), rand, ModelData.EMPTY);
        return set == null || set.contains(renderType);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, net.minecraft.core.BlockPos pos, BlockState state, ModelData data) {
        if (level == null || pos == null) {
            return data;
        }
        if (level.getBlockEntity(pos) instanceof com.dp.toms_extensions.blockentity.PaintedTrimSlabBlockEntity painted) {
            return ModelData.builder()
                    .with(PaintedBlockEntity.FACADE_STATE, painted::getPaintedBlockState)
                    .with(com.dp.toms_extensions.blockentity.PaintedTrimSlabBlockEntity.FACADE_PROFILE, painted.getFacadeProfile())
                    .build();
        }
        if (level.getBlockEntity(pos) instanceof PaintedBlockEntity painted) {
            return painted.getModelData();
        }
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
        return fallback.getParticleIcon();
    }

    @Override
    public ItemOverrides getOverrides() {
        return fallback.getOverrides();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        ChunkRenderTypeSet base = fallback.getRenderTypes(state, rand, data);
        FacadeRenderController.Plan plan = FacadeRenderController.resolve(state, data);
        if (plan == null) {
            return base;
        }

        BakedModel sourceModel = getSourceModel(plan);
        ChunkRenderTypeSet source = sourceModel.getRenderTypes(plan.sourceRenderState(), rand, ModelData.EMPTY);
        return ChunkRenderTypeSet.union(base, source);
    }
}
