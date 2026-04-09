package com.dp.toms_extensions.event;

import com.dp.toms_extensions.TomsSimpleStorageExtensions;
import com.dp.toms_extensions.blockentity.PaintedTrimSlabBlockEntity;
import com.dp.toms_extensions.config.ModConfigData;
import com.dp.toms_extensions.util.FacadeProfile;
import com.dp.toms_extensions.util.PaintKitHelper;
import com.tom.storagemod.block.IPaintable;
import com.tom.storagemod.item.PaintKitItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TomsSimpleStorageExtensions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PaintKitHooks {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof PaintKitItem)) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState targetState = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (level.isClientSide) {
            if (event.getEntity().isSecondaryUseActive()) {
                if (PaintKitHelper.canCapture(targetState, level, pos, blockEntity)) {
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
                return;
            }

            if (PaintKitHelper.hasCapturedBlock(stack) && targetState.getBlock() instanceof IPaintable) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }

        if (event.getEntity().isSecondaryUseActive()) {
            if (PaintKitHelper.canCapture(targetState, level, pos, blockEntity)) {
                PaintKitHelper.writeCapturedBlock(stack, targetState);
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
            return;
        }

        if (!ModConfigData.VALUES.allowTrimSlabPainting.get()) {
            return;
        }

        if (PaintKitHelper.hasCapturedBlock(stack) && targetState.getBlock() instanceof IPaintable paintable) {
            BlockState sourceState = PaintKitHelper.readCapturedBlock(level, stack);
            FacadeProfile capturedProfile = PaintKitHelper.readCapturedProfile(stack);
            BlockState normalizedSource = PaintKitHelper.normalizeSourceForTarget(targetState, sourceState);

            if (paintable.paint(level, pos, normalizedSource)) {
                if (level.getBlockEntity(pos) instanceof PaintedTrimSlabBlockEntity painted) {
                    painted.setFacadeProfile(capturedProfile);
                }

                level.playSound(event.getEntity(), pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);

                InteractionHand hand = event.getHand();
                stack.hurtAndBreak(1, event.getEntity(), player -> player.broadcastBreakEvent(hand));
                if (stack.isEmpty()) {
                    event.getEntity().setItemInHand(hand, new ItemStack(Items.BUCKET));
                }

                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }
}
