package com.dp.toms_extensions.block;

import com.dp.toms_extensions.blockentity.PaintedTrimSlabBlockEntity;
import com.dp.toms_extensions.config.ModConfigData;
import com.dp.toms_extensions.registry.ModItems;
import com.dp.toms_extensions.util.FacadeProfile;
import com.dp.toms_extensions.util.FacadeRuntimeHelper;
import com.dp.toms_extensions.util.FacadeStateHelper;
import com.dp.toms_extensions.util.PassiveFacadeEffects;
import com.tom.storagemod.block.IPaintable;
import com.tom.storagemod.block.ITrim;
import com.tom.storagemod.tile.PaintedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PaintedTrimSlabBlock extends SlabBlock implements EntityBlock, ITrim, IPaintable {
    public static final BooleanProperty FACADE_ACTIVE = BooleanProperty.create("facade_active");
    public static final BooleanProperty TRANSPARENT_FACADE = BooleanProperty.create("transparent_facade");
    public static final IntegerProperty LIGHT_LEVEL = IntegerProperty.create("light_level", 0, 15);
    private static final int REDSTONE_ACTIVE_TICKS = 80;

    public PaintedTrimSlabBlock() {
        this(TrimSlabProperties.create());
    }

    protected PaintedTrimSlabBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACADE_ACTIVE, false)
                .setValue(TRANSPARENT_FACADE, false)
                .setValue(LIGHT_LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACADE_ACTIVE, TRANSPARENT_FACADE, LIGHT_LEVEL);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PaintedTrimSlabBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide || blockEntityType != com.dp.toms_extensions.registry.ModBlockEntities.PAINTED_TRIM_SLAB_BE.get()) {
            return null;
        }
        return (tickerLevel, tickerPos, tickerState, blockEntity) -> {
            if (blockEntity instanceof PaintedTrimSlabBlockEntity painted) {
                PaintedTrimSlabBlockEntity.serverTick(tickerLevel, tickerPos, tickerState, painted);
            }
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PaintedTrimSlabBlockEntity painted) {
            BlockState normalized = FacadeStateHelper.isSlabLike(paintedState)
                    ? current.getValue(TYPE) == net.minecraft.world.level.block.state.properties.SlabType.DOUBLE
                    ? FacadeStateHelper.normalizeForDoubleSlabTarget(paintedState)
                    : FacadeStateHelper.normalizeForSlabTarget(current, paintedState)
                    : paintedState;
            normalized = PassiveFacadeEffects.sanitizeCapturedFacade(normalized);
            boolean ok = painted.setPaintedBlockState(normalized);
            if (ok) {
                syncTransparentState(level, pos);
                syncLampFacadeWithSignal(level, pos);
                syncMirroredState(level, pos);
                refreshMirroredLight(level, pos);
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
        if (!ModConfigData.VALUES.mirrorPaintedBlockLightEmission.get()) {
            return 0;
        }
        return state.hasProperty(LIGHT_LEVEL) ? state.getValue(LIGHT_LEVEL) : 0;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.hasProperty(TRANSPARENT_FACADE) && state.getValue(TRANSPARENT_FACADE);
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.hasProperty(TRANSPARENT_FACADE) && state.getValue(TRANSPARENT_FACADE)) {
            return 0;
        }
        if (state.getValue(TYPE) == SlabType.DOUBLE) {
            return level.getMaxLightLevel();
        }
        return super.getLightBlock(state, level, pos);
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.hasProperty(TRANSPARENT_FACADE) && state.getValue(TRANSPARENT_FACADE)) {
            return 1.0F;
        }
        return super.getShadeBrightness(state, level, pos);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return !(state.hasProperty(TRANSPARENT_FACADE) && state.getValue(TRANSPARENT_FACADE));
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.hasProperty(TRANSPARENT_FACADE) && state.getValue(TRANSPARENT_FACADE)) {
            return Shapes.empty();
        }
        return super.getOcclusionShape(state, level, pos);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!ModConfigData.VALUES.mirrorPaintedBlockParticleEmission.get()) {
            return;
        }

        BlockState facade = getFacadeState(state, level, pos);
        BlockState effectState = PassiveFacadeEffects.resolveEffectState(facade);
        if (effectState != null) {
            effectState.getBlock().animateTick(effectState, level, pos, random);
        }
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        activateRedstoneFacade(state, level, pos);
        super.attack(state, level, pos, player);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        activateRedstoneFacade(state, level, pos);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        activateRedstoneFacade(state, level, pos);
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(FACADE_ACTIVE)) {
            level.setBlock(pos, state.setValue(FACADE_ACTIVE, false), 3);
            syncLampFacadeWithSignal(level, pos);
            syncMirroredState(level, pos);
            refreshMirroredLight(level, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (level.isClientSide) {
            return;
        }

        if (syncLampFacadeWithSignal(level, pos)) {
            syncMirroredState(level, pos);
            refreshMirroredLight(level, pos);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.toms_storage.paintable"));
        tooltip.add(Component.translatable("tooltip.toms_storage.trim"));
    }

    @Nullable
    private static BlockState getFacadeState(BlockState hostState, BlockGetter level, BlockPos pos) {
        return FacadeRuntimeHelper.resolveRuntimeFacade(hostState, level, pos);
    }

    public static BlockState replacementState(BlockState currentState) {
        return com.dp.toms_extensions.registry.ModBlocks.PAINTED_TRIM_SLAB.get()
                .defaultBlockState()
                .setValue(TYPE, currentState.getValue(TYPE))
                .setValue(WATERLOGGED, currentState.getValue(WATERLOGGED));
    }

    public static boolean syncMirroredState(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }

        BlockState hostState = level.getBlockState(pos);
        if (!(hostState.getBlock() instanceof PaintedTrimSlabBlock) || !hostState.hasProperty(LIGHT_LEVEL)) {
            return false;
        }

        int targetLightLevel = resolveMirroredLightLevel(hostState, level, pos);
        if (hostState.getValue(LIGHT_LEVEL) == targetLightLevel) {
            return false;
        }

        return level.setBlock(pos, hostState.setValue(LIGHT_LEVEL, targetLightLevel), 3);
    }

    public static boolean syncTransparentState(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }

        BlockState hostState = level.getBlockState(pos);
        if (!(hostState.getBlock() instanceof PaintedTrimSlabBlock) || !hostState.hasProperty(TRANSPARENT_FACADE)) {
            return false;
        }

        boolean targetTransparency = resolveTransparentFacade(level, pos);
        if (hostState.getValue(TRANSPARENT_FACADE) == targetTransparency) {
            return false;
        }

        return level.setBlock(pos, hostState.setValue(TRANSPARENT_FACADE, targetTransparency), 3);
    }

    private static void activateRedstoneFacade(BlockState hostState, Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }

        BlockState facade = getFacadeState(hostState, level, pos);
        if (!PassiveFacadeEffects.isRedstoneOreFacade(facade)) {
            return;
        }

        if (!hostState.getValue(FACADE_ACTIVE)) {
            level.setBlock(pos, hostState.setValue(FACADE_ACTIVE, true), 3);
            syncMirroredState(level, pos);
            refreshMirroredLight(level, pos);
        }
        level.scheduleTick(pos, hostState.getBlock(), REDSTONE_ACTIVE_TICKS);
    }

    private static void refreshMirroredLight(Level level, BlockPos pos) {
        FacadeRuntimeHelper.refreshMirroredLight(level, pos);
    }

    private static boolean syncLampFacadeWithSignal(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PaintedBlockEntity painted)) {
            return false;
        }
        return FacadeRuntimeHelper.syncLampFacadeWithSignal(level, pos, painted);
    }

    private static int resolveMirroredLightLevel(BlockState hostState, BlockGetter level, BlockPos pos) {
        if (!ModConfigData.VALUES.mirrorPaintedBlockLightEmission.get()) {
            return 0;
        }

        BlockState facade = getFacadeState(hostState, level, pos);
        return PassiveFacadeEffects.resolveMirroredLightEmission(facade, level, pos);
    }

    private static boolean resolveTransparentFacade(BlockGetter level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PaintedTrimSlabBlockEntity painted) {
            FacadeProfile profile = painted.getFacadeProfile();
            if (profile != null && !profile.isEmpty()) {
                return profile.transparent();
            }
            BlockState stored = painted.getPaintedBlockState();
            return stored != null && FacadeProfile.fromBlockState(stored).transparent();
        }

        if (be instanceof PaintedBlockEntity painted) {
            BlockState stored = painted.getPaintedBlockState();
            return stored != null && FacadeProfile.fromBlockState(stored).transparent();
        }

        return false;
    }
}
