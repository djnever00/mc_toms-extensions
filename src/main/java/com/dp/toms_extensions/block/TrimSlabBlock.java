package com.dp.toms_extensions.block;

import com.dp.toms_extensions.blockentity.PaintedTrimSlabBlockEntity;
import com.dp.toms_extensions.config.ModConfigData;
import com.dp.toms_extensions.util.FacadeRuntimeHelper;
import com.dp.toms_extensions.util.FacadeStateHelper;
import com.dp.toms_extensions.util.PassiveFacadeEffects;
import com.tom.storagemod.block.IPaintable;
import com.tom.storagemod.block.ITrim;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TrimSlabBlock extends SlabBlock implements ITrim, IPaintable {
    public TrimSlabBlock() {
        super(TrimSlabProperties.create());
    }

    @Override
    public boolean paint(Level level, BlockPos pos, BlockState paintedState) {
        if (!ModConfigData.VALUES.allowTrimSlabPainting.get()) {
            return false;
        }

        BlockState current = level.getBlockState(pos);
        if (MagmaTrimSlabBlock.shouldUseMagmaVariant(paintedState)) {
            return level.setBlock(pos, MagmaTrimSlabBlock.replacementState(current), 3);
        }

        if (!level.setBlock(pos, PaintedTrimSlabBlock.replacementState(current), 3)) {
            return false;
        }

        if (level.getBlockEntity(pos) instanceof PaintedTrimSlabBlockEntity be) {
            BlockState normalized = FacadeStateHelper.isSlabLike(paintedState)
                    ? current.getValue(TYPE) == net.minecraft.world.level.block.state.properties.SlabType.DOUBLE
                    ? FacadeStateHelper.normalizeForDoubleSlabTarget(paintedState)
                    : FacadeStateHelper.normalizeForSlabTarget(current, paintedState)
                    : paintedState;
            normalized = PassiveFacadeEffects.sanitizeCapturedFacade(normalized);
            boolean ok = be.setPaintedBlockState(normalized);
            if (ok) {
                PaintedTrimSlabBlock.syncTransparentState(level, pos);
                FacadeRuntimeHelper.syncLampFacadeWithSignal(level, pos, be);
                PaintedTrimSlabBlock.syncMirroredState(level, pos);
                FacadeRuntimeHelper.refreshMirroredLight(level, pos);
            }
            return ok;
        }

        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.toms_storage.paintable"));
        tooltip.add(Component.translatable("tooltip.toms_storage.trim"));
    }
}
