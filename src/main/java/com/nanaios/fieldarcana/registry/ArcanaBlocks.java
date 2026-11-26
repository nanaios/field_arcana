package com.nanaios.fieldarcana.registry;

import com.nanaios.fieldarcana.FieldArcana;
import com.nanaios.fieldarcana.block.FieldEmitterBlock;
import com.nanaios.fieldarcana.block.FieldResonatorBlock;
import com.nanaios.fieldarcana.block.FieldSensorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ArcanaBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FieldArcana.MOD_ID);

    public static final DeferredBlock<FieldEmitterBlock> FIELD_EMITTER = BLOCKS.registerBlock("field_emitter", FieldEmitterBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(FieldEmitterBlock.ACTIVE) ? 9 : 0));

    public static final DeferredBlock<FieldResonatorBlock> FIELD_RESONATOR = BLOCKS.registerBlock("field_resonator", FieldResonatorBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(FieldResonatorBlock.ACTIVE) ? 7 : 0));

    public static final DeferredBlock<FieldSensorBlock> FIELD_SENSOR = BLOCKS.registerBlock("field_sensor", FieldSensorBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.COPPER));

    private ArcanaBlocks() {
    }
}
