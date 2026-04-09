package com.dp.toms_extensions.client.model;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.SlabType;

public final class QuadUvHelper {
    private static final int STRIDE = DefaultVertexFormat.BLOCK.getIntegerSize();

    // BLOCK format layout: pos(3), color(1), uv0(2), uv1(2), normal(1)
    private static final int Y_INDEX = 1;
    private static final int U_INDEX = 4;
    private static final int V_INDEX = 5;

    private QuadUvHelper() {
    }

    public enum SideSamplingProfile {
        DEFAULT,
        TOP_HALF,
        GRASS_TOP_HALF,
        SQUARE_BORDER
    }

    public static BakedQuad remapQuadSprite(BakedQuad hostQuad, BakedQuad sourceQuad, SlabType slabType) {
        return remapQuadToSprite(
                hostQuad,
                sourceQuad.getSprite(),
                sourceQuad.getTintIndex(),
                hostQuad.getDirection(),
                slabType,
                false,
                SideSamplingProfile.DEFAULT
        );
    }

    public static BakedQuad remapQuadToSprite(BakedQuad hostQuad, TextureAtlasSprite targetSprite, SlabType slabType) {
        return remapQuadToSprite(
                hostQuad,
                targetSprite,
                hostQuad.getTintIndex(),
                hostQuad.getDirection(),
                slabType,
                false,
                SideSamplingProfile.DEFAULT
        );
    }

    public static BakedQuad remapQuadToSprite(
            BakedQuad hostQuad,
            TextureAtlasSprite targetSprite,
            int tintIndex,
            Direction face,
            SlabType slabType,
            boolean forceFullFaceV,
            SideSamplingProfile sideSamplingProfile
    ) {
        int[] src = hostQuad.getVertices().clone();
        int[] out = src.clone();

        float targetU0 = targetSprite.getU0();
        float targetU1 = targetSprite.getU1();
        float targetV0 = targetSprite.getV0();
        float targetV1 = targetSprite.getV1();

        float sourceU0 = hostQuad.getSprite().getU0();
        float sourceU1 = hostQuad.getSprite().getU1();
        float sourceV0 = hostQuad.getSprite().getV0();
        float sourceV1 = hostQuad.getSprite().getV1();

        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (int vertex = 0; vertex < 4; vertex++) {
            int base = vertex * STRIDE;
            float y = Float.intBitsToFloat(src[base + Y_INDEX]);
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }

        boolean sideFace = face == Direction.NORTH || face == Direction.SOUTH || face == Direction.WEST || face == Direction.EAST;

        for (int vertex = 0; vertex < 4; vertex++) {
            int base = vertex * STRIDE;

            float y = Float.intBitsToFloat(src[base + Y_INDEX]);
            float u = Float.intBitsToFloat(src[base + U_INDEX]);
            float v = Float.intBitsToFloat(src[base + V_INDEX]);

            float uNorm = normalize(u, sourceU0, sourceU1);
            float vNorm = normalize(v, sourceV0, sourceV1);

            float newU = lerp(uNorm, targetU0, targetU1);
            float newV;

            if (sideFace && !forceFullFaceV) {
                if (slabType == SlabType.DOUBLE) {
                    newV = lerp(vNorm, targetV0, targetV1);
                } else {
                    float yNorm = normalize(y, minY, maxY);
                    float topAnchored = 1.0f - yNorm;
                    float sampledVNorm = sampleSideV(topAnchored, sideSamplingProfile);
                    newV = lerp(sampledVNorm, targetV0, targetV1);
                }
            } else {
                newV = lerp(vNorm, targetV0, targetV1);
            }

            out[base + U_INDEX] = Float.floatToRawIntBits(newU);
            out[base + V_INDEX] = Float.floatToRawIntBits(newV);
        }

        return new BakedQuad(out, tintIndex, hostQuad.getDirection(), targetSprite, hostQuad.isShade());
    }

    private static float sampleSideV(float topAnchored, SideSamplingProfile profile) {
        float t = clamp01(topAnchored);
        return switch (profile) {
            case TOP_HALF -> t * 0.5f;
            case GRASS_TOP_HALF -> t * 0.5f;
            case SQUARE_BORDER -> sampleSquareBorder(t);
            case DEFAULT -> t;
        };
    }

    private static float sampleSquareBorder(float t) {
        // 8 slab side rows sampled from 16px source rows:
        // top 2 rows, then every other in the mid band, then bottom 2 rows.
        final float[] rowStartPx = {0f, 1f, 4f, 6f, 8f, 10f, 14f, 15f};
        final float scaled = t * 8f;
        final int idx = Math.min(7, Math.max(0, (int) Math.floor(scaled)));
        final float local = clamp01(scaled - idx);
        return (rowStartPx[idx] + local) / 16f;
    }

    private static float normalize(float value, float min, float max) {
        if (max - min == 0f) return 0f;
        return (value - min) / (max - min);
    }

    private static float lerp(float t, float min, float max) {
        return min + t * (max - min);
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        return Math.min(v, 1f);
    }

    public static BakedQuad remapQuadToSprite(
            BakedQuad hostQuad,
            TextureAtlasSprite targetSprite,
            Direction face,
            SlabType slabType,
            boolean forceFullFaceV
    ) {
        return remapQuadToSprite(
                hostQuad,
                targetSprite,
                hostQuad.getTintIndex(),
                face,
                slabType,
                forceFullFaceV,
            SideSamplingProfile.DEFAULT
        );
    }

}
