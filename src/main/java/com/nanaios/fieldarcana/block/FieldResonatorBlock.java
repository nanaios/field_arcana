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
 * 共鳴器 — right-click with a Field Core to attune, with an empty hand to change the period,
 * sneak + empty hand to shift the phase by 90°. Redstone sets the amplitude.
 */
public class FieldResonatorBlock extends BaseEntityBlock {
    public static final MapCodec<FieldResonatorBlock> CODEC = simpleCodec(FieldResonatorBlock::new);
    public static final BooleanProperty ACTIVE = FieldEmitterBlock.ACTIVE;

    public FieldResonatorBlock(Properties properties) {
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
        return new FieldResonatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ArcanaBlockEntities.FIELD_RESONATOR.get(), FieldResonatorBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof FieldCoreItem core)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FieldResonatorBlockEntity resonator) {
            resonator.attune(core.field());
            player.displayClientMessage(resonator.describe(), true);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FieldResonatorBlockEntity resonator) {
            if (player.isShiftKeyDown()) {
                resonator.cyclePhase();
            } else {
                resonator.cyclePeriod();
            }
            player.displayClientMessage(resonator.describe(), true);
            float pitch = (float) Math.max(0.5, Math.min(2.0, 12.0 / resonator.period()));
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8F, pitch);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            FieldTheoryAPI.get(level).removeSource(FieldResonatorBlockEntity.sourceId(pos));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
