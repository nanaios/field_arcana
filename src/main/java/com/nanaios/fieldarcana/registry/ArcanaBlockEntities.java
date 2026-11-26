package com.nanaios.fieldarcana.registry;

import com.nanaios.fieldarcana.FieldArcana;
import com.nanaios.fieldarcana.block.FieldEmitterBlockEntity;
import com.nanaios.fieldarcana.block.FieldSensorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ArcanaBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FieldArcana.MOD_ID);

    @SuppressWarnings("DataFlowIssue") // null data fixer type is the documented way for modded block entities
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FieldEmitterBlockEntity>> FIELD_EMITTER = BLOCK_ENTITIES.register("field_emitter",
            () -> BlockEntityType.Builder.of(FieldEmitterBlockEntity::new, ArcanaBlocks.FIELD_EMITTER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FieldSensorBlockEntity>> FIELD_SENSOR = BLOCK_ENTITIES.register("field_sensor",
            () -> BlockEntityType.Builder.of(FieldSensorBlockEntity::new, ArcanaBlocks.FIELD_SENSOR.get()).build(null));

    private ArcanaBlockEntities() {
    }
}
