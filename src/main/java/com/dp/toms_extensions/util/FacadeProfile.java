package com.dp.toms_extensions.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public record FacadeProfile(
        String canonicalBlockId,
        boolean hasVanillaSlab,
        boolean grassTint,
        boolean preferUntintedSides,
        boolean transparent,
        boolean mirrorLight,
        boolean mirrorParticles,
        boolean delayedEffects,
        SamplingMode samplingMode,
        RuntimeMode runtimeMode
) {
    private static final FacadeProfile EMPTY = new FacadeProfile(
            "",
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            SamplingMode.DEFAULT,
            RuntimeMode.NONE
    );

    private static final Set<ResourceLocation> GRASS_BLOCKS = Set.of(
            rl("minecraft", "grass_block"),
            rl("minecraft", "podzol"),
            rl("minecraft", "mycelium"),
            rl("minecraft", "warped_nylium"),
            rl("minecraft", "crimson_nylium")
    );

    private static final Set<ResourceLocation> DELAYED_EFFECT_BLOCKS = Set.of(
            rl("minecraft", "crying_obsidian"),
            rl("minecraft", "redstone_ore"),
            rl("minecraft", "deepslate_redstone_ore"),
            rl("minecraft", "redstone_lamp"),
            rl("minecraft", "amethyst_block"),
            rl("minecraft", "budding_amethyst")
    );

    public enum SamplingMode {
        DEFAULT,
        TOP_HALF,
        GRASS_TOP_HALF,
        SQUARE_BORDER
    }

    public enum RuntimeMode {
        NONE,
        REDSTONE_ORE,
        REDSTONE_LAMP
    }

    public static FacadeProfile empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return canonicalBlockId == null || canonicalBlockId.isEmpty();
    }

    @Nullable
    public ResourceLocation canonicalKey() {
        if (isEmpty()) {
            return null;
        }
        try {
            return ResourceLocation.parse(canonicalBlockId);
        } catch (Exception ignored) {
            return null;
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("canonical", canonicalBlockId);
        tag.putBoolean("hasVanillaSlab", hasVanillaSlab);
        tag.putBoolean("grassTint", grassTint);
        tag.putBoolean("preferUntintedSides", preferUntintedSides);
        tag.putBoolean("transparent", transparent);
        tag.putBoolean("mirrorLight", mirrorLight);
        tag.putBoolean("mirrorParticles", mirrorParticles);
        tag.putBoolean("delayedEffects", delayedEffects);
        tag.putString("samplingMode", samplingMode.name());
        tag.putString("runtimeMode", runtimeMode.name());
        return tag;
    }

    public static FacadeProfile fromTag(@Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return EMPTY;
        }

        return new FacadeProfile(
                tag.getString("canonical"),
                tag.getBoolean("hasVanillaSlab"),
                tag.getBoolean("grassTint"),
                tag.getBoolean("preferUntintedSides"),
                tag.getBoolean("transparent"),
                tag.getBoolean("mirrorLight"),
                tag.getBoolean("mirrorParticles"),
                tag.getBoolean("delayedEffects"),
                parseSamplingMode(tag.getString("samplingMode")),
                parseRuntimeMode(tag.getString("runtimeMode"))
        );
    }

    public static FacadeProfile fromBlockState(@Nullable BlockState sourceState) {
        if (sourceState == null || sourceState.isAir()) {
            return EMPTY;
        }

        ResourceLocation canonical = resolveCanonicalKey(sourceState);
        if (canonical == null) {
            return EMPTY;
        }

        boolean grass = GRASS_BLOCKS.contains(canonical);
        return new FacadeProfile(
                canonical.toString(),
                hasVanillaSlabCandidate(canonical),
                grass,
                false,
                FacadeStateHelper.isTransparentFacade(sourceState),
                PassiveFacadeEffects.resolveEffectState(sourceState) != null,
                PassiveFacadeEffects.canMirror(sourceState),
                DELAYED_EFFECT_BLOCKS.contains(canonical),
                chooseSamplingMode(canonical, sourceState),
                chooseRuntimeMode(canonical)
        );
    }

    private static SamplingMode chooseSamplingMode(ResourceLocation canonical, BlockState sourceState) {
        if (usesHalfBlockSampling(canonical, sourceState)) {
            return SamplingMode.TOP_HALF;
        }
        if (usesTopHalfSampling(canonical)) {
            return SamplingMode.GRASS_TOP_HALF;
        }
        return SamplingMode.DEFAULT;
    }

    private static boolean usesTopHalfSampling(ResourceLocation canonical) {
        if (!"minecraft".equals(canonical.getNamespace())) {
            return false;
        }

        String path = canonical.getPath();
        if (path.equals("honeycomb_block")
                || path.equals("glowstone")
                || path.equals("redstone_ore")
                || path.equals("deepslate_redstone_ore")
                || path.equals("cut_copper")) {
            return true;
        }

        return path.endsWith("_cut_copper");
    }

    private static boolean usesHalfBlockSampling(ResourceLocation canonical, BlockState sourceState) {
        if (FacadeStateHelper.isLeavesLikeFacade(sourceState)) {
            return true;
        }

        String path = canonical.getPath();
        if (path.equals("polished_basalt")) {
            return true;
        }

        if (path.equals("budding_amethyst")) {
            return false;
        }

        if (path.contains("polished")) {
            return false;
        }

        return path.equals("obsidian")
                || path.equals("crying_obsidian")
                || path.equals("shroomlight")
                || path.equals("gilded_blackstone")
                || path.equals("amethyst_block")
                || path.equals("reinforced_deepslate")
                || path.equals("dried_kelp_block")
                || path.equals("bookshelf")
                || path.equals("chiseled_bookshelf")
                || path.contains("bookshelf")
                || path.endsWith("_planks")
                || path.endsWith("_log")
                || path.endsWith("_wood")
                || path.endsWith("_stem")
                || path.endsWith("_hyphae")
                || path.endsWith("_wool")
                || path.contains("purpur")
                || path.contains("brick")
                || path.contains("tile");
    }

    private static RuntimeMode chooseRuntimeMode(ResourceLocation canonical) {
        if (canonical.equals(rl("minecraft", "redstone_ore"))
                || canonical.equals(rl("minecraft", "deepslate_redstone_ore"))) {
            return RuntimeMode.REDSTONE_ORE;
        }
        if (canonical.equals(rl("minecraft", "redstone_lamp"))) {
            return RuntimeMode.REDSTONE_LAMP;
        }
        return RuntimeMode.NONE;
    }

    @Nullable
    private static ResourceLocation resolveCanonicalKey(BlockState sourceState) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(sourceState.getBlock());
        if (key == null) {
            return null;
        }

        if (!FacadeStateHelper.isSlabLike(sourceState)) {
            return key;
        }

        BlockState full = FacadeStateHelper.tryResolveFullBlockCounterpart(sourceState);
        if (full == null) {
            return key;
        }
        ResourceLocation fullKey = ForgeRegistries.BLOCKS.getKey(full.getBlock());
        return fullKey != null ? fullKey : key;
    }

    private static boolean hasVanillaSlabCandidate(ResourceLocation canonical) {
        if (!"minecraft".equals(canonical.getNamespace())) {
            return false;
        }

        for (String candidate : buildVanillaSlabCandidates(canonical.getPath())) {
            Block block = ForgeRegistries.BLOCKS.getValue(rl("minecraft", candidate));
            if (block != null && FacadeStateHelper.isSlabLike(block.defaultBlockState())) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> buildVanillaSlabCandidates(String fullPath) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(fullPath + "_slab");

        if (fullPath.endsWith("_bricks")) {
            candidates.add(fullPath.substring(0, fullPath.length() - "_bricks".length()) + "_brick_slab");
        }

        if (fullPath.endsWith("_tiles")) {
            candidates.add(fullPath.substring(0, fullPath.length() - "_tiles".length()) + "_tile_slab");
        }

        if (fullPath.endsWith("_block")) {
            candidates.add(fullPath.substring(0, fullPath.length() - "_block".length()) + "_slab");
        }

        return candidates;
    }

    private static SamplingMode parseSamplingMode(String raw) {
        try {
            return SamplingMode.valueOf(raw);
        } catch (Exception ignored) {
            return SamplingMode.DEFAULT;
        }
    }

    private static RuntimeMode parseRuntimeMode(String raw) {
        try {
            return RuntimeMode.valueOf(raw);
        } catch (Exception ignored) {
            return RuntimeMode.NONE;
        }
    }

    private static ResourceLocation rl(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
