package com.dp.toms_extensions.util;

import com.dp.toms_extensions.block.PaintedTrimSlabBlock;
import com.tom.storagemod.tile.PaintedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class FacadeRuntimeHelper {
    private FacadeRuntimeHelper() {
    }

    @Nullable
    public static BlockState resolveStoredFacade(BlockGetter level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PaintedBlockEntity painted) {
            return painted.getPaintedBlockState();
        }
        return null;
    }

    @Nullable
    public static BlockState resolveRuntimeFacade(BlockState hostState, BlockGetter level, BlockPos pos) {
        BlockState storedFacade = resolveStoredFacade(level, pos);
        boolean lampPowered = level instanceof Level l && pos != null && l.hasNeighborSignal(pos);
        return projectRuntimeFacade(hostState, storedFacade, lampPowered);
    }

    @Nullable
    public static BlockState projectRuntimeFacade(BlockState hostState, @Nullable BlockState storedFacade) {
        if (storedFacade == null) {
            return null;
        }

        boolean oreActive = hostState != null
                && hostState.hasProperty(PaintedTrimSlabBlock.FACADE_ACTIVE)
                && hostState.getValue(PaintedTrimSlabBlock.FACADE_ACTIVE);

        if (PassiveFacadeEffects.isRedstoneOreFacade(storedFacade)) {
            return PassiveFacadeEffects.projectRuntimeState(storedFacade, oreActive, false);
        }

        // Without world context, keep stored lamp state instead of forcing the unlit texture.
        return storedFacade;
    }

    @Nullable
    public static BlockState projectRuntimeFacade(BlockState hostState, @Nullable BlockState storedFacade, boolean lampPowered) {
        if (storedFacade == null) {
            return null;
        }

        boolean oreActive = hostState != null
                && hostState.hasProperty(PaintedTrimSlabBlock.FACADE_ACTIVE)
                && hostState.getValue(PaintedTrimSlabBlock.FACADE_ACTIVE);

        return PassiveFacadeEffects.projectRuntimeState(storedFacade, oreActive, lampPowered);
    }

    public static void refreshMirroredLight(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        sendRefresh(level, pos, state);
        level.setBlocksDirty(pos, state, state);
        level.blockEntityChanged(pos);

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            sendRefresh(level, neighborPos, neighborState);
            level.setBlocksDirty(neighborPos, neighborState, neighborState);
        }

        level.getLightEngine().checkBlock(pos);
        for (Direction direction : Direction.values()) {
            level.getLightEngine().checkBlock(pos.relative(direction));
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(pos);
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    public static boolean syncLampFacadeWithSignal(Level level, BlockPos pos, PaintedBlockEntity painted) {
        if (level == null || pos == null || painted == null) {
            return false;
        }

        BlockState currentFacade = painted.getPaintedBlockState();
        if (!PassiveFacadeEffects.isRedstoneLampFacade(currentFacade)) {
            return false;
        }

        BlockState projected = PassiveFacadeEffects.projectRuntimeState(
                currentFacade,
                false,
                level.hasNeighborSignal(pos)
        );
        if (projected == null || projected.equals(currentFacade)) {
            return false;
        }
        return painted.setPaintedBlockState(projected);
    }

    private static void sendRefresh(Level level, BlockPos pos, BlockState state) {
        level.sendBlockUpdated(pos, state, state, 3);
    }
}
