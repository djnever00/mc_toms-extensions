package com.dp.toms_extensions.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class PassiveFacadeEffects {
    private static final Set<ResourceLocation> ALLOW = Set.of(
            rl("minecraft", "glowstone"),
            rl("minecraft", "sea_lantern"),
            rl("minecraft", "shroomlight"),
            rl("minecraft", "jack_o_lantern"),
            rl("minecraft", "ochre_froglight"),
            rl("minecraft", "pearlescent_froglight"),
            rl("minecraft", "verdant_froglight"),
            rl("minecraft", "crying_obsidian"),
            rl("minecraft", "redstone_ore"),
            rl("minecraft", "deepslate_redstone_ore"),
            rl("minecraft", "amethyst_block"),
            rl("minecraft", "budding_amethyst"),
            rl("minecraft", "mycelium"),
            rl("minecraft", "warped_nylium"),
            rl("minecraft", "crimson_nylium"),
            rl("minecraft", "soul_sand")
    );

    private static final Set<ResourceLocation> DENY = Set.of(
            rl("minecraft", "end_rod"),
            rl("minecraft", "beacon"),
            rl("minecraft", "respawn_anchor"),
            rl("minecraft", "redstone_torch"),
            rl("minecraft", "redstone_wall_torch"),
            rl("minecraft", "campfire"),
            rl("minecraft", "soul_campfire"),
            rl("minecraft", "sculk"),
            rl("minecraft", "sculk_sensor"),
            rl("minecraft", "calibrated_sculk_sensor"),
            rl("minecraft", "sculk_catalyst"),
            rl("minecraft", "end_portal_frame"),
            rl("minecraft", "nether_portal")
    );

    private PassiveFacadeEffects() {
    }

    public static boolean canMirror(BlockState state) {
        return resolveEffectState(state) != null;
    }

    @Nullable
    public static BlockState resolveEffectState(BlockState state) {
        if (state == null || state.isAir()) {
            return null;
        }

        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (key == null || DENY.contains(key)) {
            return null;
        }

        if (ALLOW.contains(key)) {
            return state;
        }

        BlockState full = FacadeStateHelper.tryResolveFullBlockCounterpart(state);
        if (full == null) {
            return null;
        }
        full = copyLitIfPresent(state, full);

        ResourceLocation fullKey = ForgeRegistries.BLOCKS.getKey(full.getBlock());
        if (fullKey == null || DENY.contains(fullKey)) {
            return null;
        }

        return ALLOW.contains(fullKey) ? full : null;
    }

    public static boolean isRedstoneOreFacade(BlockState state) {
        ResourceLocation key = resolveCanonicalKey(state);
        return key != null
                && (key.equals(rl("minecraft", "redstone_ore"))
                || key.equals(rl("minecraft", "deepslate_redstone_ore")));
    }

    public static boolean isRedstoneLampFacade(BlockState state) {
        ResourceLocation key = resolveCanonicalKey(state);
        return key != null && key.equals(rl("minecraft", "redstone_lamp"));
    }

    public static boolean isMagmaFacade(BlockState state) {
        ResourceLocation key = resolveCanonicalKey(state);
        return key != null && key.equals(rl("minecraft", "magma_block"));
    }

    public static BlockState sanitizeCapturedFacade(BlockState state) {
        if (state == null) {
            return null;
        }
        if (isRedstoneOreFacade(state)) {
            BooleanProperty lit = findLitProperty(state);
            if (lit != null) {
                return state.setValue(lit, false);
            }
        }
        if (isRedstoneLampFacade(state)) {
            BooleanProperty lit = findLitProperty(state);
            if (lit != null) {
                return state.setValue(lit, false);
            }
        }
        return state;
    }

    public static BlockState projectRuntimeState(BlockState state, boolean active) {
        return projectRuntimeState(state, active, false);
    }

    public static BlockState projectRuntimeState(BlockState state, boolean oreActive, boolean lampPowered) {
        if (state == null) {
            return null;
        }
        if (isRedstoneOreFacade(state)) {
            BooleanProperty lit = findLitProperty(state);
            if (lit != null) {
                return state.setValue(lit, oreActive);
            }
        }
        if (isRedstoneLampFacade(state)) {
            BooleanProperty lit = findLitProperty(state);
            if (lit != null) {
                return state.setValue(lit, lampPowered);
            }
        }
        return state;
    }

    public static int resolveMirroredLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        if (state == null || level == null || pos == null) {
            return 0;
        }

        int light = 0;

        BlockState effect = resolveEffectState(state);
        if (effect != null) {
            light = Math.max(light, effect.getLightEmission(level, pos));
        }

        BlockState full = FacadeStateHelper.tryResolveFullBlockCounterpart(state);
        if (full != null) {
            light = Math.max(light, full.getLightEmission(level, pos));
        }

        light = Math.max(light, state.getLightEmission(level, pos));
        return light;
    }

    @Nullable
    private static ResourceLocation resolveCanonicalKey(BlockState state) {
        if (state == null || state.isAir()) {
            return null;
        }

        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (key == null) {
            return null;
        }

        if (key.equals(rl("minecraft", "redstone_ore")) || key.equals(rl("minecraft", "deepslate_redstone_ore"))) {
            return key;
        }

        BlockState full = FacadeStateHelper.tryResolveFullBlockCounterpart(state);
        if (full == null) {
            return key;
        }
        ResourceLocation fullKey = ForgeRegistries.BLOCKS.getKey(full.getBlock());
        return fullKey != null ? fullKey : key;
    }

    @Nullable
    private static BooleanProperty findLitProperty(BlockState state) {
        if (state == null) {
            return null;
        }
        if (state.hasProperty(RedStoneOreBlock.LIT)) {
            return RedStoneOreBlock.LIT;
        }

        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty boolProp && "lit".equals(property.getName())) {
                return boolProp;
            }
        }
        return null;
    }

    private static BlockState copyLitIfPresent(BlockState from, BlockState to) {
        BooleanProperty fromLit = findLitProperty(from);
        BooleanProperty toLit = findLitProperty(to);
        if (fromLit == null || toLit == null) {
            return to;
        }
        return to.setValue(toLit, from.getValue(fromLit));
    }

    private static ResourceLocation rl(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
