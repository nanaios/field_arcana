package com.nanaios.fieldarcana.block;

import com.mojang.serialization.MapCodec;
import com.nanaios.fieldarcana.item.FieldCoreItem;
import com.nanaios.fieldarcana.registry.ArcanaBlockEntities;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 場整形器 — right-click with a Field Core to restrict it to that field, with an empty hand to
 * change the mode (shield ×0, dampen ×0.5, amplify ×2, lens ×3), sneak + empty hand to act on
 * every field again. Redstone sets the radius.
 */
public class FieldShaperBlock extends BaseEntityBlock {
    public static final MapCodec<FieldShaperBlock> CODEC = simpleCodec(FieldShaperBlock::new);
    public static final BooleanProperty ACTIVE = FieldEmitterBlock.ACTIVE;

    public FieldShaperBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FieldShaperBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ArcanaBlockEntities.FIELD_SHAPER.get(), FieldShaperBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof FieldCoreItem core)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FieldShaperBlockEntity shaper) {
            shaper.attune(core.field());
            player.displayClientMessage(shaper.describe(), true);
            level.playSound(null, pos, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FieldShaperBlockEntity shaper) {
            if (player.isShiftKeyDown()) {
                shaper.attune(null);
            } else {
                shaper.cycleMode();
            }
            player.displayClientMessage(shaper.describe(), true);
            level.playSound(null, pos, SoundEvents.COPPER_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.8F, 1.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            FieldTheoryAPI.get(level).removeRegion(FieldShaperBlockEntity.regionId(pos));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
