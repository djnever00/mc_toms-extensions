package com.dp.toms_extensions.block;

import com.dp.toms_extensions.blockentity.PaintedTrimSlabBlockEntity;
import com.dp.toms_extensions.config.ModConfigData;
import com.dp.toms_extensions.registry.ModBlocks;
import com.dp.toms_extensions.registry.ModItems;
import com.dp.toms_extensions.util.FacadeRuntimeHelper;
import com.dp.toms_extensions.util.FacadeStateHelper;
import com.dp.toms_extensions.util.PassiveFacadeEffects;
import com.tom.storagemod.block.IPaintable;
import com.tom.storagemod.block.ITrim;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.biome.Biome;

public class MagmaTrimSlabBlock extends SlabBlock implements ITrim, IPaintable {
    public MagmaTrimSlabBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.MAGMA_BLOCK));
    }

    public static boolean shouldUseMagmaVariant(BlockState sourceState) {
        return PassiveFacadeEffects.isMagmaFacade(sourceState);
    }

    public static BlockState replacementState(BlockState currentState) {
        return ModBlocks.MAGMA_TRIM_SLAB.get()
                .defaultBlockState()
                .setValue(TYPE, currentState.getValue(TYPE))
                .setValue(WATERLOGGED, currentState.getValue(WATERLOGGED));
    }

    @Override
    public boolean paint(Level level, BlockPos pos, BlockState paintedState) {
        if (!ModConfigData.VALUES.allowTrimSlabPainting.get()) {
            return false;
        }

        if (shouldUseMagmaVariant(paintedState)) {
            return false;
        }

        BlockState current = level.getBlockState(pos);
        BlockState replacement = ModBlocks.PAINTED_TRIM_SLAB.get()
                .defaultBlockState()
                .setValue(TYPE, current.getValue(TYPE))
                .setValue(WATERLOGGED, current.getValue(WATERLOGGED));

        if (!level.setBlock(pos, replacement, 3)) {
            return false;
        }

        if (level.getBlockEntity(pos) instanceof PaintedTrimSlabBlockEntity be) {
            BlockState normalized = FacadeStateHelper.isSlabLike(paintedState)
                    ? current.getValue(TYPE) == SlabType.DOUBLE
                    ? FacadeStateHelper.normalizeForDoubleSlabTarget(paintedState)
                    : FacadeStateHelper.normalizeForSlabTarget(current, paintedState)
                    : paintedState;
            normalized = PassiveFacadeEffects.sanitizeCapturedFacade(normalized);
            boolean ok = be.setPaintedBlockState(normalized);
            if (ok) {
                FacadeRuntimeHelper.syncLampFacadeWithSignal(level, pos, be);
                PaintedTrimSlabBlock.syncMirroredState(level, pos);
                FacadeRuntimeHelper.refreshMirroredLight(level, pos);
            }
            return ok;
        }

        return false;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.TRIM_SLAB_ITEM.get());
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return ModConfigData.VALUES.mirrorPaintedBlockLightEmission.get()
                ? super.getLightEmission(state, level, pos)
                : 0;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!ModConfigData.VALUES.mirrorPaintedBlockParticleEmission.get()) {
            return;
        }

        if (level.isRainingAt(pos.above()) && random.nextInt(4) == 0) {
            spawnMagmaRainSmoke(level, pos, state, random);
        }
    }

    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        if (!ModConfigData.VALUES.mirrorPaintedBlockParticleEmission.get()
                || precipitation != Biome.Precipitation.RAIN) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            double x = pos.getX() + 0.2D + level.random.nextDouble() * 0.6D;
            double y = pos.getY() + topSurfaceHeight(state) + 0.05D;
            double z = pos.getZ() + 0.2D + level.random.nextDouble() * 0.6D;
            serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.0D, 0.02D, 0.0D, 0.0D);

            if (level.random.nextInt(8) == 0) {
                level.playSound(
                        null,
                        pos,
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS,
                        0.15F,
                        1.8F + level.random.nextFloat() * 0.2F
                );
            }
        }
    }

    private static double topSurfaceHeight(BlockState state) {
        SlabType type = state.getValue(TYPE);
        return type == SlabType.BOTTOM ? 0.5D : 1.0D;
    }

    private static void spawnMagmaRainSmoke(Level level, BlockPos pos, BlockState state, RandomSource random) {
        double y = pos.getY() + topSurfaceHeight(state) + 0.05D;
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + 0.2D + random.nextDouble() * 0.6D;
            double z = pos.getZ() + 0.2D + random.nextDouble() * 0.6D;
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.02D, 0.0D);
        }

        if (random.nextInt(12) == 0) {
            level.playLocalSound(
                    pos.getX() + 0.5D,
                    pos.getY() + topSurfaceHeight(state),
                    pos.getZ() + 0.5D,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.15F,
                    1.8F + random.nextFloat() * 0.2F,
                    false
            );
        }
    }
}
