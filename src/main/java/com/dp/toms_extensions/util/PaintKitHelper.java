package com.dp.toms_extensions.util;

import com.tom.storagemod.block.IPaintable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PaintKitHelper {
    private static final String TAG_BLOCK = "block";
    private static final String TAG_PROFILE = "facade_profile";

    private PaintKitHelper() {
    }

    public static boolean canCapture(BlockState state, Level level, BlockPos pos, BlockEntity blockEntity) {
        if (state == null || state.isAir()) {
            return false;
        }
        if (blockEntity != null) {
            return false;
        }

        // Keep simple/full blocks, but explicitly allow slabs too.
        return Block.isShapeFullBlock(state.getShape(level, pos)) || FacadeStateHelper.isSlabLike(state);
    }

    public static void writeCapturedBlock(ItemStack stack, BlockState state) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.put(TAG_BLOCK, NbtUtils.writeBlockState(state));
        tag.put(TAG_PROFILE, FacadeProfile.fromBlockState(state).toTag());
    }

    public static boolean hasCapturedBlock(ItemStack stack) {
        return stack.hasTag() && stack.getTag() != null && stack.getTag().contains(TAG_BLOCK);
    }

    public static BlockState readCapturedBlock(Level level, ItemStack stack) {
        return IPaintable.readBlockState(level, stack.getTag().getCompound(TAG_BLOCK));
    }

    public static FacadeProfile readCapturedProfile(ItemStack stack) {
        if (stack == null || !stack.hasTag() || stack.getTag() == null) {
            return FacadeProfile.empty();
        }

        CompoundTag tag = stack.getTag();
        if (tag.contains(TAG_PROFILE)) {
            return FacadeProfile.fromTag(tag.getCompound(TAG_PROFILE));
        }
        return FacadeProfile.empty();
    }

    public static BlockState normalizeSourceForTarget(BlockState targetState, BlockState sourceState) {
        if (targetState == null || sourceState == null) {
            return sourceState;
        }

        // Slab targets already normalize themselves inside the slab paint path.
        if (FacadeStateHelper.isSlabLike(targetState)) {
            return sourceState;
        }

        // Full block target + slab source -> prefer a full block counterpart.
        if (FacadeStateHelper.isSlabLike(sourceState)) {
            BlockState full = FacadeStateHelper.tryResolveFullBlockCounterpart(sourceState);
            if (full != null) {
                return full;
            }
        }

        return sourceState;
    }
}
