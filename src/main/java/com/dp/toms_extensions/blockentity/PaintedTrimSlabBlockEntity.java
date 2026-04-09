package com.dp.toms_extensions.blockentity;

import com.dp.toms_extensions.block.MagmaTrimSlabBlock;
import com.dp.toms_extensions.block.PaintedTrimSlabBlock;
import com.dp.toms_extensions.registry.ModBlockEntities;
import com.dp.toms_extensions.util.FacadeProfile;
import com.dp.toms_extensions.util.FacadeRuntimeHelper;
import com.dp.toms_extensions.util.PassiveFacadeEffects;
import com.tom.storagemod.tile.PaintedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public class PaintedTrimSlabBlockEntity extends PaintedBlockEntity {
    public static final ModelProperty<FacadeProfile> FACADE_PROFILE = new ModelProperty<>();
    private static final String TAG_PROFILE = "facade_profile";
    private static final int DEFAULT_SERVER_RELIGHT_DELAY = 1;
    private static final long MIRRORED_STATE_RECHECK_INTERVAL = 20L;

    private FacadeProfile facadeProfile = FacadeProfile.empty();
    private int pendingServerRelightTicks = -1;

    public PaintedTrimSlabBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PAINTED_TRIM_SLAB_BE.get(), pos, state);
    }

    @Override
    public boolean setPaintedBlockState(BlockState blockState) {
        boolean changed = super.setPaintedBlockState(blockState);
        boolean profileChanged = setFacadeProfileInternal(FacadeProfile.fromBlockState(blockState), false);
        if (changed || profileChanged) {
            markFacadeProfileDirty();
        }
        return changed || profileChanged;
    }

    public FacadeProfile getFacadeProfile() {
        if (facadeProfile == null || facadeProfile.isEmpty()) {
            facadeProfile = FacadeProfile.fromBlockState(getPaintedBlockState());
        }
        return facadeProfile;
    }

    public boolean setFacadeProfile(FacadeProfile profile) {
        return setFacadeProfileInternal(profile, true);
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        FacadeProfile stored = compound.contains(TAG_PROFILE)
                ? FacadeProfile.fromTag(compound.getCompound(TAG_PROFILE))
                : FacadeProfile.empty();
        FacadeProfile derived = FacadeProfile.fromBlockState(getPaintedBlockState());
        facadeProfile = derived.isEmpty() ? stored : derived;
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        compound.put(TAG_PROFILE, getFacadeProfile().toTag());
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(PaintedBlockEntity.FACADE_STATE, this::getPaintedBlockState)
                .with(FACADE_PROFILE, getFacadeProfile())
                .build();
    }

    @Override
    public void onLoad() {
        super.onLoad();

        Level level = getLevel();
        if (level == null) {
            return;
        }

        if (!level.isClientSide) {
            if (PassiveFacadeEffects.isMagmaFacade(getPaintedBlockState())) {
                level.setBlock(getBlockPos(), MagmaTrimSlabBlock.replacementState(getBlockState()), 3);
                return;
            }
            PaintedTrimSlabBlock.syncTransparentState(level, getBlockPos());
            FacadeRuntimeHelper.syncLampFacadeWithSignal(level, getBlockPos(), this);
            PaintedTrimSlabBlock.syncMirroredState(level, getBlockPos());
            queueServerRelight(DEFAULT_SERVER_RELIGHT_DELAY);
        } else {
            refreshClientModelAndRender();
        }
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        super.onDataPacket(net, packet);
        refreshClientModelAndRender();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        refreshClientModelAndRender();
    }

    public void queueServerRelight(int delayTicks) {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        int nextDelay = Math.max(0, delayTicks);
        if (pendingServerRelightTicks < 0 || nextDelay < pendingServerRelightTicks) {
            pendingServerRelightTicks = nextDelay;
            setChanged();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PaintedTrimSlabBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        if (PassiveFacadeEffects.isMagmaFacade(blockEntity.getPaintedBlockState())) {
            level.setBlock(pos, MagmaTrimSlabBlock.replacementState(state), 3);
            return;
        }

        if (blockEntity.pendingServerRelightTicks < 0 && PaintedTrimSlabBlock.syncTransparentState(level, pos)) {
            blockEntity.queueServerRelight(0);
        }
        long phase = (level.getGameTime() + pos.asLong()) % MIRRORED_STATE_RECHECK_INTERVAL;
        if (phase == 0L && PaintedTrimSlabBlock.syncMirroredState(level, pos)) {
            blockEntity.queueServerRelight(0);
        }

        if (blockEntity.pendingServerRelightTicks < 0) {
            return;
        }

        if (blockEntity.pendingServerRelightTicks > 0) {
            blockEntity.pendingServerRelightTicks--;
            return;
        }

        blockEntity.pendingServerRelightTicks = -1;
        FacadeRuntimeHelper.refreshMirroredLight(level, pos);
    }

    private boolean setFacadeProfileInternal(FacadeProfile profile, boolean markDirty) {
        FacadeProfile next = profile == null ? FacadeProfile.empty() : profile;
        if (next.equals(facadeProfile)) {
            return false;
        }

        facadeProfile = next;
        if (markDirty) {
            markFacadeProfileDirty();
        }
        return true;
    }

    private void markFacadeProfileDirty() {
        setChanged();
        requestModelDataUpdate();

        Level level = getLevel();
        if (level == null) {
            return;
        }

        BlockState state = level.getBlockState(getBlockPos());
        level.sendBlockUpdated(getBlockPos(), state, state, 3);
        level.blockEntityChanged(getBlockPos());
        if (!level.isClientSide) {
            PaintedTrimSlabBlock.syncTransparentState(level, getBlockPos());
            PaintedTrimSlabBlock.syncMirroredState(level, getBlockPos());
            queueServerRelight(DEFAULT_SERVER_RELIGHT_DELAY);
        }
    }

    private void refreshClientModelAndRender() {
        Level level = getLevel();
        if (level == null || !level.isClientSide) {
            return;
        }

        requestModelDataUpdate();
        BlockState state = level.getBlockState(getBlockPos());
        level.sendBlockUpdated(getBlockPos(), state, state, 3);
    }
}
