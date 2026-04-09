package com.dp.toms_extensions.util;

import com.dp.toms_extensions.config.ModConfigData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FacadeRegistryMapper {
    private static final Map<ResourceLocation, List<ResourceLocation>> FULL_TO_SLABS = new HashMap<>();
    private static final Map<ResourceLocation, List<ResourceLocation>> SLAB_TO_FULLS = new HashMap<>();
    private static boolean built = false;

    private FacadeRegistryMapper() {
    }

    public static void invalidate() {
        built = false;
        FULL_TO_SLABS.clear();
        SLAB_TO_FULLS.clear();
    }

    @Nullable
    public static BlockState tryResolveSlabFromFull(BlockState fullState) {
        ensureBuilt();
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(fullState.getBlock());
        if (key == null) {
            return null;
        }
        List<ResourceLocation> slabIds = FULL_TO_SLABS.get(key);
        if (slabIds == null || slabIds.isEmpty()) {
            return null;
        }
        for (ResourceLocation slabId : slabIds) {
            Block block = ForgeRegistries.BLOCKS.getValue(slabId);
            if (isSlabLike(block)) {
                return block.defaultBlockState();
            }
        }
        return null;
    }

    @Nullable
    public static BlockState tryResolveFullFromSlab(BlockState slabState) {
        ensureBuilt();
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(slabState.getBlock());
        if (key == null) {
            return null;
        }
        List<ResourceLocation> fullIds = SLAB_TO_FULLS.get(key);
        if (fullIds == null || fullIds.isEmpty()) {
            return null;
        }
        for (ResourceLocation fullId : fullIds) {
            Block block = ForgeRegistries.BLOCKS.getValue(fullId);
            if (block != null && !isSlabLike(block)) {
                return block.defaultBlockState();
            }
        }
        return null;
    }

    private static void ensureBuilt() {
        if (built) {
            return;
        }
        built = true;

        List<String> priority = readPriority();
        Set<ResourceLocation> ids = new HashSet<>(ForgeRegistries.BLOCKS.getKeys());
        Map<String, List<ResourceLocation>> slabIdsByPath = new HashMap<>();
        Map<String, List<ResourceLocation>> fullIdsByPath = new HashMap<>();

        for (ResourceLocation id : ids) {
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block == null) {
                continue;
            }
            if (isSlabLike(block)) {
                slabIdsByPath.computeIfAbsent(id.getPath(), k -> new ArrayList<>()).add(id);
            } else {
                fullIdsByPath.computeIfAbsent(id.getPath(), k -> new ArrayList<>()).add(id);
            }
        }

        for (ResourceLocation id : ids) {
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (!isSlabLike(block)) {
                continue;
            }

            for (String fullPath : buildFullCandidates(id.getPath())) {
                List<ResourceLocation> candidates = fullIdsByPath.get(fullPath);
                if (candidates == null) {
                    continue;
                }
                for (ResourceLocation fullId : candidates) {
                    addBidirectional(fullId, id);
                }
            }
        }

        for (ResourceLocation id : ids) {
            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block == null || isSlabLike(block)) {
                continue;
            }
            for (String slabPath : buildSlabCandidates(id.getPath())) {
                List<ResourceLocation> candidates = slabIdsByPath.get(slabPath);
                if (candidates == null) {
                    continue;
                }
                for (ResourceLocation slabId : candidates) {
                    addBidirectional(id, slabId);
                }
            }
        }

        Comparator<ResourceLocation> byPriority = Comparator
                .comparingInt((ResourceLocation rl) -> namespaceRank(rl.getNamespace(), priority))
                .thenComparing(ResourceLocation::toString);

        FULL_TO_SLABS.values().forEach(list -> list.sort(byPriority));
        SLAB_TO_FULLS.values().forEach(list -> list.sort(byPriority));
    }

    private static int namespaceRank(String namespace, List<String> priority) {
        int i = priority.indexOf(namespace);
        return i >= 0 ? i : Integer.MAX_VALUE;
    }

    private static List<String> readPriority() {
        List<? extends String> configured = ModConfigData.VALUES.facadeNamespacePriority.get();
        List<String> out = new ArrayList<>();
        for (String raw : configured) {
            if (raw == null) continue;
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        if (out.isEmpty()) {
            out.add("minecraft");
        }
        return out;
    }

    private static void addBidirectional(ResourceLocation fullId, ResourceLocation slabId) {
        FULL_TO_SLABS.computeIfAbsent(fullId, k -> new ArrayList<>());
        if (!FULL_TO_SLABS.get(fullId).contains(slabId)) {
            FULL_TO_SLABS.get(fullId).add(slabId);
        }

        SLAB_TO_FULLS.computeIfAbsent(slabId, k -> new ArrayList<>());
        if (!SLAB_TO_FULLS.get(slabId).contains(fullId)) {
            SLAB_TO_FULLS.get(slabId).add(fullId);
        }
    }

    private static List<String> buildSlabCandidates(String fullPath) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(fullPath + "_slab");

        if (fullPath.endsWith("_bricks")) {
            candidates.add(fullPath.substring(0, fullPath.length() - "_bricks".length()) + "_brick_slab");
            candidates.add(fullPath + "_slab");
        }

        if (fullPath.endsWith("_tiles")) {
            candidates.add(fullPath.substring(0, fullPath.length() - "_tiles".length()) + "_tile_slab");
            candidates.add(fullPath + "_slab");
        }

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
            candidates.add(base);

            String[] parts = base.split("_");
            String last = parts.length == 0 ? base : parts[parts.length - 1];

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

            String pluralized = pluralizeLastToken(base);
            if (!pluralized.equals(base)) {
                candidates.add(pluralized);
            }
        }

        if (slabPath.endsWith("_bricks_slab")
                || slabPath.endsWith("_tiles_slab")
                || slabPath.endsWith("_pavers_slab")
                || slabPath.endsWith("_plates_slab")
                || slabPath.endsWith("_shingles_slab")) {
            candidates.add(slabPath.substring(0, slabPath.length() - "_slab".length()));
        }

        return new ArrayList<>(candidates);
    }

    private static String singularizeLastToken(String path) {
        String[] parts = path.split("_");
        if (parts.length == 0) return path;
        String last = parts[parts.length - 1];
        if (last.length() <= 1 || !last.endsWith("s")) return path;
        if (last.equals("glass") || last.equals("grass") || last.equals("moss")
                || last.equals("boss") || last.equals("cross")) {
            return path;
        }
        parts[parts.length - 1] = last.substring(0, last.length() - 1);
        return String.join("_", parts);
    }

    private static String pluralizeLastToken(String path) {
        String[] parts = path.split("_");
        if (parts.length == 0) return path;
        String last = parts[parts.length - 1];
        if (last.endsWith("s")) return path;
        if (last.equals("glass") || last.equals("grass") || last.equals("moss")
                || last.equals("boss") || last.equals("cross")) {
            return path;
        }
        parts[parts.length - 1] = last + "s";
        return String.join("_", parts);
    }

    private static boolean isSlabLike(Block block) {
        return block != null && FacadeStateHelper.isSlabLike(block.defaultBlockState());
    }
}
