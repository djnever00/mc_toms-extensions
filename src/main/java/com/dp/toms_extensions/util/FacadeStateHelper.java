package com.dp.toms_extensions.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class FacadeStateHelper {
    private static final String PROP_TYPE = "type";
    private static final String PROP_WATERLOGGED = "waterlogged";
    private static final BlockPos ZERO_POS = BlockPos.ZERO;

    private FacadeStateHelper() {
    }

    public static BlockState normalizeForSlabTarget(BlockState targetState, BlockState sourceState) {
        if (!isSlabLike(targetState)) {
            return sourceState;
        }

        SlabType targetType = getSlabTypeOrDefault(targetState, SlabType.BOTTOM);

        if (isSlabLike(sourceState)) {
            BlockState out = sourceState;
            out = setSlabType(out, targetType);
            out = clearWaterlogged(out);
            return out;
        }

        if (targetType != SlabType.DOUBLE) {
            BlockState slabCounterpart = tryResolveSlabCounterpart(sourceState, targetType);
            if (slabCounterpart != null) {
                return slabCounterpart;
            }
        }

        return sourceState;
    }

    public static BlockState normalizeForDoubleSlabTarget(BlockState sourceState) {
        if (isSlabLike(sourceState)) {
            BlockState out = sourceState;
            out = setSlabType(out, SlabType.DOUBLE);
            out = clearWaterlogged(out);
            return out;
        }

        return sourceState;
    }

    @Nullable
    public static BlockState tryResolveFullBlockCounterpart(BlockState slabState) {
        if (!isSlabLike(slabState)) {
            return null;
        }

        BlockState override = FacadeCompatOverrides.tryResolveFullFromSlab(slabState);
        if (override != null) {
            return override;
        }

        BlockState registryMatch = FacadeRegistryMapper.tryResolveFullFromSlab(slabState);
        if (registryMatch != null) {
            return registryMatch;
        }

        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(slabState.getBlock());
        if (key == null) {
            return null;
        }

        for (String candidatePath : buildFullCandidates(key.getPath())) {
            Block fullBlock = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(key.getNamespace(), candidatePath));
            if (fullBlock != null) {
                return fullBlock.defaultBlockState();
            }
        }

        return null;
    }

    @Nullable
    public static BlockState tryResolveSlabCounterpart(BlockState fullState, SlabType slabType) {
        BlockState override = FacadeCompatOverrides.tryResolveSlabFromFull(fullState);
        if (override != null && isSlabLike(override)) {
            return clearWaterlogged(setSlabType(override, slabType));
        }

        BlockState registryMatch = FacadeRegistryMapper.tryResolveSlabFromFull(fullState);
        if (registryMatch != null && isSlabLike(registryMatch)) {
            return clearWaterlogged(setSlabType(registryMatch, slabType));
        }

        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(fullState.getBlock());
        if (key == null) {
            return null;
        }

        for (String candidatePath : buildSlabCandidates(key.getPath())) {
            Block slabBlock = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(key.getNamespace(), candidatePath));
            if (slabBlock != null && isSlabLike(slabBlock.defaultBlockState())) {
                return clearWaterlogged(setSlabType(slabBlock.defaultBlockState(), slabType));
            }
        }

        return null;
    }

    private static List<String> buildSlabCandidates(String fullPath) {
        Set<String> candidates = new LinkedHashSet<>();

        // Exact/common
        candidates.add(fullPath + "_slab");

        // Common family transforms
        if (fullPath.endsWith("_bricks")) {
            candidates.add(fullPath.substring(0, fullPath.length() - "_bricks".length()) + "_brick_slab");
            candidates.add(fullPath + "_slab");
        }

        if (fullPath.endsWith("_tiles")) {
            candidates.add(fullPath.substring(0, fullPath.length() - "_tiles".length()) + "_tile_slab");
            candidates.add(fullPath + "_slab");
        }

        // Last-token singularization fallback
        String singularized = singularizeLastToken(fullPath);
        if (!singularized.equals(fullPath)) {
            candidates.add(singularized + "_slab");
        }

        return new ArrayList<>(candidates);
    }

    private static List<String> buildFullCandidates(String slabPath) {
        Set<String> candidates = new LinkedHashSet<>();

        if (slabPath.endsWith("_slab")) {
            String base = slabPath.substring(0, slabPath.length() - "_slab".length());

            // 1) exact stripped form
            candidates.add(base);

            String[] parts = base.split("_");
            String last = parts.length == 0 ? base : parts[parts.length - 1];

            // 2) explicit common family reversals on the last token
            if (last.equals("brick")) {
                parts[parts.length - 1] = "bricks";
                candidates.add(String.join("_", parts));
                parts[parts.length - 1] = last;
            }

            if (last.equals("tile")) {
                parts[parts.length - 1] = "tiles";
                candidates.add(String.join("_", parts));
                parts[parts.length - 1] = last;
            }

            if (last.equals("paver")) {
                parts[parts.length - 1] = "pavers";
                candidates.add(String.join("_", parts));
                parts[parts.length - 1] = last;
            }

            if (last.equals("plate")) {
                parts[parts.length - 1] = "plates";
                candidates.add(String.join("_", parts));
                parts[parts.length - 1] = last;
            }

            if (last.equals("shingle")) {
                parts[parts.length - 1] = "shingles";
                candidates.add(String.join("_", parts));
                parts[parts.length - 1] = last;
            }

            // 3) already-plural forms
            if (last.equals("bricks") || last.equals("tiles") || last.equals("pavers")
                    || last.equals("plates") || last.equals("shingles")) {
                candidates.add(base);
            }

            // 4) guarded plural fallback on the last token
            String pluralized = pluralizeLastToken(base);
            if (!pluralized.equals(base)) {
                candidates.add(pluralized);
            }
        }

        // Handle odd but seen-in-the-wild forms directly
        if (slabPath.endsWith("_bricks_slab")) {
            candidates.add(slabPath.substring(0, slabPath.length() - "_slab".length()));
        }

        if (slabPath.endsWith("_tiles_slab")) {
            candidates.add(slabPath.substring(0, slabPath.length() - "_slab".length()));
        }

        if (slabPath.endsWith("_pavers_slab")) {
            candidates.add(slabPath.substring(0, slabPath.length() - "_slab".length()));
        }

        if (slabPath.endsWith("_plates_slab")) {
            candidates.add(slabPath.substring(0, slabPath.length() - "_slab".length()));
        }

        if (slabPath.endsWith("_shingles_slab")) {
            candidates.add(slabPath.substring(0, slabPath.length() - "_slab".length()));
        }

        return new ArrayList<>(candidates);
    }

    private static String singularizeLastToken(String path) {
        String[] parts = path.split("_");
        if (parts.length == 0) {
            return path;
        }

        String last = parts[parts.length - 1];
        if (!shouldTrySingularize(last)) {
            return path;
        }

        parts[parts.length - 1] = last.substring(0, last.length() - 1);
        return String.join("_", parts);
    }

    private static String pluralizeLastToken(String path) {
        String[] parts = path.split("_");
        if (parts.length == 0) {
            return path;
        }

        String last = parts[parts.length - 1];
        if (shouldAvoidPluralization(last)) {
            return path;
        }

        if (!last.endsWith("s")) {
            parts[parts.length - 1] = last + "s";
            return String.join("_", parts);
        }

        return path;
    }

    private static boolean shouldAvoidPluralization(String token) {
        return token.equals("glass")
                || token.equals("grass")
                || token.equals("moss")
                || token.equals("boss")
                || token.equals("cross");
    }

    private static boolean shouldTrySingularize(String token) {
        if (token.length() <= 1 || !token.endsWith("s")) {
            return false;
        }

        // Avoid obvious bad guesses
        return !token.equals("glass")
                && !token.equals("grass")
                && !token.equals("moss")
                && !token.equals("boss")
                && !token.equals("cross");
    }

    public static boolean isSlabLike(BlockState state) {
        return findSlabTypeProperty(state) != null;
    }

    public static boolean isTransparentFacade(@Nullable BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }

        BlockState candidate = state;
        if (isSlabLike(candidate)) {
            BlockState full = tryResolveFullBlockCounterpart(candidate);
            if (full != null) {
                candidate = full;
            }
        }

        try {
            if (candidate.propagatesSkylightDown(EmptyBlockGetter.INSTANCE, ZERO_POS)
                    && candidate.getLightBlock(EmptyBlockGetter.INSTANCE, ZERO_POS) == 0) {
                return true;
            }

            return !candidate.canOcclude() && !candidate.isSolidRender(EmptyBlockGetter.INSTANCE, ZERO_POS);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isGlassLikeFacade(@Nullable BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }

        BlockState candidate = state;
        if (isSlabLike(candidate)) {
            BlockState full = tryResolveFullBlockCounterpart(candidate);
            if (full != null) {
                candidate = full;
            }
        }

        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(candidate.getBlock());
        if (key != null) {
            String path = key.getPath();
            if (path.contains("glass") || path.contains("pane") || path.contains("ice")) {
                return true;
            }
        }

        try {
            return candidate.propagatesSkylightDown(EmptyBlockGetter.INSTANCE, ZERO_POS)
                    && candidate.getLightBlock(EmptyBlockGetter.INSTANCE, ZERO_POS) == 0
                    && !candidate.canOcclude();
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isLeavesLikeFacade(@Nullable BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }

        BlockState candidate = state;
        if (isSlabLike(candidate)) {
            BlockState full = tryResolveFullBlockCounterpart(candidate);
            if (full != null) {
                candidate = full;
            }
        }

        if (candidate.getBlock() instanceof LeavesBlock || candidate.is(BlockTags.LEAVES)) {
            return true;
        }

        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(candidate.getBlock());
        return key != null && key.getPath().contains("leaves");
    }

    public static SlabType getSlabTypeOrDefault(BlockState state, SlabType fallback) {
        EnumProperty<SlabType> prop = findSlabTypeProperty(state);
        if (prop == null) {
            return fallback;
        }
        return state.getValue(prop);
    }

    public static BlockState setSlabType(BlockState state, SlabType slabType) {
        EnumProperty<SlabType> prop = findSlabTypeProperty(state);
        if (prop == null || !prop.getPossibleValues().contains(slabType)) {
            return state;
        }
        return state.setValue(prop, slabType);
    }

    public static BlockState clearWaterlogged(BlockState state) {
        BooleanProperty prop = findWaterloggedProperty(state);
        if (prop == null) {
            return state;
        }
        return state.setValue(prop, false);
    }

    @Nullable
    private static EnumProperty<SlabType> findSlabTypeProperty(BlockState state) {
        if (state == null) {
            return null;
        }
        if (state.hasProperty(SlabBlock.TYPE)) {
            return SlabBlock.TYPE;
        }

        for (Property<?> property : state.getProperties()) {
            if (property instanceof EnumProperty<?> enumProp
                    && PROP_TYPE.equals(property.getName())
                    && enumProp.getValueClass() == SlabType.class) {
                @SuppressWarnings("unchecked")
                EnumProperty<SlabType> typed = (EnumProperty<SlabType>) enumProp;
                return typed;
            }
        }
        return null;
    }

    @Nullable
    private static BooleanProperty findWaterloggedProperty(BlockState state) {
        if (state == null) {
            return null;
        }
        if (state.hasProperty(SlabBlock.WATERLOGGED)) {
            return SlabBlock.WATERLOGGED;
        }

        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty boolProp
                    && PROP_WATERLOGGED.equals(property.getName())) {
                return boolProp;
            }
        }
        return null;
    }
}
