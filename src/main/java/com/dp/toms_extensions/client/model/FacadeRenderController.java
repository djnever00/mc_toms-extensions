package com.dp.toms_extensions.client.model;

import com.dp.toms_extensions.blockentity.PaintedTrimSlabBlockEntity;
import com.dp.toms_extensions.util.FacadeProfile;
import com.dp.toms_extensions.util.FacadeRuntimeHelper;
import com.dp.toms_extensions.util.FacadeStateHelper;
import com.tom.storagemod.tile.PaintedBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class FacadeRenderController {
    public enum TextureRoute {
        DIRECT_MODEL,
        SAMPLED_TEXTURE
    }

    private FacadeRenderController() {
    }

    @Nullable
    public static Plan resolve(BlockState hostState, ModelData data) {
        if (hostState == null || !FacadeStateHelper.isSlabLike(hostState)) {
            return null;
        }

        Supplier<BlockState> facadeSupplier = data.get(PaintedBlockEntity.FACADE_STATE);
        if (facadeSupplier == null) {
            return null;
        }

        BlockState storedFacade = facadeSupplier.get();
        if (storedFacade == null) {
            return null;
        }

        FacadeProfile profile = data.get(PaintedTrimSlabBlockEntity.FACADE_PROFILE);
        if (profile == null || profile.isEmpty()) {
            profile = FacadeProfile.fromBlockState(storedFacade);
        }

        BlockState runtimeFacade = FacadeRuntimeHelper.projectRuntimeFacade(hostState, storedFacade);
        if (runtimeFacade == null) {
            return null;
        }

        SlabType hostType = FacadeStateHelper.getSlabTypeOrDefault(hostState, SlabType.BOTTOM);

        BlockState directSlabState = resolveRenderableSlabState(runtimeFacade, profile, hostType);
        TextureRoute route = directSlabState != null || hostType == SlabType.DOUBLE
                ? TextureRoute.DIRECT_MODEL
                : TextureRoute.SAMPLED_TEXTURE;

        BlockState renderState = directSlabState != null
                ? directSlabState
                : hostType == SlabType.DOUBLE
                ? FacadeStateHelper.normalizeForDoubleSlabTarget(runtimeFacade)
                : FacadeStateHelper.normalizeForSlabTarget(hostState, runtimeFacade);

        BlockState samplingState = resolveSamplingState(runtimeFacade);

        return new Plan(
                hostState,
                storedFacade,
                runtimeFacade,
                profile,
                renderState,
                samplingState,
                hostType,
                route,
                mapSamplingProfile(profile),
                profile.preferUntintedSides()
        );
    }

    @Nullable
    private static BlockState resolveRenderableSlabState(BlockState runtimeFacade, FacadeProfile profile, SlabType hostType) {
        if (FacadeStateHelper.isSlabLike(runtimeFacade)) {
            return FacadeStateHelper.clearWaterlogged(FacadeStateHelper.setSlabType(runtimeFacade, hostType));
        }

        if (profile.hasVanillaSlab()) {
            return tryResolveVanillaSlab(profile, hostType);
        }

        return null;
    }

    private static BlockState resolveSamplingState(BlockState runtimeFacade) {
        if (FacadeStateHelper.isSlabLike(runtimeFacade)) {
            BlockState full = FacadeStateHelper.tryResolveFullBlockCounterpart(runtimeFacade);
            if (full != null) {
                return full;
            }
        }
        return runtimeFacade;
    }

    private static QuadUvHelper.SideSamplingProfile mapSamplingProfile(FacadeProfile profile) {
        if (profile == null) {
            return QuadUvHelper.SideSamplingProfile.DEFAULT;
        }
        return switch (profile.samplingMode()) {
            case TOP_HALF -> QuadUvHelper.SideSamplingProfile.TOP_HALF;
            case GRASS_TOP_HALF -> QuadUvHelper.SideSamplingProfile.GRASS_TOP_HALF;
            case SQUARE_BORDER -> QuadUvHelper.SideSamplingProfile.SQUARE_BORDER;
            case DEFAULT -> QuadUvHelper.SideSamplingProfile.DEFAULT;
        };
    }

    @Nullable
    private static BlockState tryResolveVanillaSlab(FacadeProfile profile, SlabType hostType) {
        ResourceLocation canonicalKey = profile.canonicalKey();
        if (canonicalKey == null || !"minecraft".equals(canonicalKey.getNamespace())) {
            return null;
        }

        for (String candidatePath : buildVanillaSlabCandidates(canonicalKey.getPath())) {
            Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft", candidatePath));
            if (block == null || !FacadeStateHelper.isSlabLike(block.defaultBlockState())) {
                continue;
            }
            return FacadeStateHelper.clearWaterlogged(FacadeStateHelper.setSlabType(block.defaultBlockState(), hostType));
        }
        return null;
    }

    private static String[] buildVanillaSlabCandidates(String fullPath) {
        if (fullPath.endsWith("_bricks")) {
            return new String[]{fullPath.substring(0, fullPath.length() - "_bricks".length()) + "_brick_slab", fullPath + "_slab"};
        }
        if (fullPath.endsWith("_tiles")) {
            return new String[]{fullPath.substring(0, fullPath.length() - "_tiles".length()) + "_tile_slab", fullPath + "_slab"};
        }
        if (fullPath.endsWith("_block")) {
            return new String[]{fullPath.substring(0, fullPath.length() - "_block".length()) + "_slab", fullPath + "_slab"};
        }
        return new String[]{fullPath + "_slab"};
    }

    public static final class Plan {
        private final BlockState hostState;
        private final BlockState storedFacadeState;
        private final BlockState runtimeFacadeState;
        private final FacadeProfile facadeProfile;
        private final BlockState renderState;
        private final BlockState samplingState;
        private final SlabType hostType;
        private final TextureRoute textureRoute;
        private final QuadUvHelper.SideSamplingProfile samplingProfile;
        private final boolean preferUntintedSides;

        private Plan(
                BlockState hostState,
                BlockState storedFacadeState,
                BlockState runtimeFacadeState,
                FacadeProfile facadeProfile,
                BlockState renderState,
                BlockState samplingState,
                SlabType hostType,
                TextureRoute textureRoute,
                QuadUvHelper.SideSamplingProfile samplingProfile,
                boolean preferUntintedSides
        ) {
            this.hostState = hostState;
            this.storedFacadeState = storedFacadeState;
            this.runtimeFacadeState = runtimeFacadeState;
            this.facadeProfile = facadeProfile;
            this.renderState = renderState;
            this.samplingState = samplingState;
            this.hostType = hostType;
            this.textureRoute = textureRoute;
            this.samplingProfile = samplingProfile;
            this.preferUntintedSides = preferUntintedSides;
        }

        public BlockState hostState() {
            return hostState;
        }

        public BlockState storedFacadeState() {
            return storedFacadeState;
        }

        public BlockState runtimeFacadeState() {
            return runtimeFacadeState;
        }

        public FacadeProfile facadeProfile() {
            return facadeProfile;
        }

        public BlockState renderState() {
            return renderState;
        }

        public BlockState samplingState() {
            return samplingState;
        }

        public SlabType hostType() {
            return hostType;
        }

        public TextureRoute textureRoute() {
            return textureRoute;
        }

        public QuadUvHelper.SideSamplingProfile samplingProfile() {
            return samplingProfile;
        }

        public boolean preferUntintedSides() {
            return preferUntintedSides;
        }

        public BlockState sourceRenderState() {
            return textureRoute == TextureRoute.DIRECT_MODEL ? renderState : samplingState;
        }
    }
}
