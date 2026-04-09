package com.dp.toms_extensions.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class ModConfigData {
    public static final ForgeConfigSpec SPEC;
    public static final Values VALUES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        VALUES = new Values(builder);
        SPEC = builder.build();
    }

    private ModConfigData() {
    }

    public static final class Values {
        public final ForgeConfigSpec.BooleanValue enableTrimSlabs;
        public final ForgeConfigSpec.BooleanValue allowTrimSlabPlacement;
        public final ForgeConfigSpec.BooleanValue allowTrimSlabRecipes;
        public final ForgeConfigSpec.BooleanValue allowTrimSlabPainting;
        public final ForgeConfigSpec.BooleanValue mirrorPaintedBlockLightEmission;
        public final ForgeConfigSpec.BooleanValue mirrorPaintedBlockParticleEmission;

        public final ForgeConfigSpec.EnumValue<TextureSource> trimTopBottomSource;
        public final ForgeConfigSpec.EnumValue<TextureSource> trimDoubleSlabSource;
        public final ForgeConfigSpec.IntValue trimSlabSideVariant;

        public final ForgeConfigSpec.ConfigValue<List<? extends String>> facadeOverrides;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> facadeNamespacePriority;

        Values(ForgeConfigSpec.Builder builder) {
            builder.push("gameplay");

            enableTrimSlabs = builder
                    .comment("Enable Inventory Trim Slabs from this addon.")
                    .define("enableTrimSlabs", true);

            allowTrimSlabPlacement = builder
                    .comment("Allow Inventory Trim Slab placement.")
                    .define("allowTrimSlabPlacement", true);

            allowTrimSlabRecipes = builder
                    .comment("Allow Inventory Trim Slab crafting recipes.")
                    .define("allowTrimSlabRecipes", true);

            allowTrimSlabPainting = builder
                    .comment("Allow Inventory Trim Slabs from this addon to be painted with the Paint Kit.")
                    .define("allowTrimSlabPainting", true);

            mirrorPaintedBlockLightEmission = builder
                    .comment(
                            "Mirror light emission from the copied paint source onto painted trim slabs.",
                            "When disabled, painted trim slabs keep the copied texture/state but emit no copied block light."
                    )
                    .define("mirrorPaintedBlockLightEmission", true);

            mirrorPaintedBlockParticleEmission = builder
                    .comment(
                            "Mirror passive particle and animateTick effects from the copied paint source onto painted trim slabs.",
                            "When disabled, painted trim slabs keep the copied texture/state but do not emit copied particles."
                    )
                    .define("mirrorPaintedBlockParticleEmission", true);

            builder.pop();

            builder.push("textures");

            trimTopBottomSource = builder
                    .comment(
                            "Requested texture source for trim slab top/bottom faces.",
                            "TOMS = prefer Tom's Simple Storage inventory trim texture.",
                            "EXT = prefer the addon trim texture.",
                            "If the requested texture is missing, the resolver falls back to the local addon texture and logs a warning."
                    )
                    .defineEnum("trimTopBottomSource", TextureSource.TOMS);

            trimDoubleSlabSource = builder
                    .comment(
                            "Requested texture source for the double slab / full block trim model.",
                            "TOMS = prefer Tom's Simple Storage inventory trim texture.",
                            "EXT = prefer the addon trim texture.",
                            "If the requested texture is missing, the resolver falls back to the local addon texture and logs a warning."
                    )
                    .defineEnum("trimDoubleSlabSource", TextureSource.TOMS);

            trimSlabSideVariant = builder
                    .comment(
                            "Selects the slab side texture variant.",
                            "1 = trim_slab_1.png",
                            "2 = trim_slab_2.png"
                    )
                    .defineInRange("trimSlabSideVariant", 2, 1, 2);

            builder.pop();

            builder.push("facade_overrides");

            facadeOverrides = builder
                    .comment(
                            "User-facing facade override mappings.",
                            "Format: full_block_id=slab_block_id",
                            "Examples:",
                            "minecraft:bricks=minecraft:brick_slab",
                            "minecraft:quartz_block=minecraft:quartz_slab",
                            "some_mod:pavers=some_mod:paver_slab",
                            "",
                            "These mappings are used before heuristic lookup.",
                            "Both directions are derived automatically:",
                            "full -> slab and slab -> full."
                    )
                    .defineListAllowEmpty(
                            List.of("mappings"),
                            List.of(
                                    "minecraft:bricks=minecraft:brick_slab",
                                    "minecraft:quartz_block=minecraft:quartz_slab",
                                    "minecraft:smooth_stone=minecraft:smooth_stone_slab"
                            ),
                            o -> o instanceof String s
                                    && s.contains("=")
                                    && s.indexOf('=') > 0
                                    && s.indexOf('=') < s.length() - 1
                    );

            facadeNamespacePriority = builder
                    .comment(
                            "Namespace priority for auto-discovered full<->slab mappings.",
                            "Earlier entries win when multiple slab/full candidates exist.",
                            "Examples: minecraft, another_mod, custom_building"
                    )
                    .defineListAllowEmpty(
                            List.of("namespacePriority"),
                            List.of(
                                    "minecraft"
                            ),
                            o -> o instanceof String s && !s.trim().isEmpty()
                    );

            builder.pop();
        }
    }
}
