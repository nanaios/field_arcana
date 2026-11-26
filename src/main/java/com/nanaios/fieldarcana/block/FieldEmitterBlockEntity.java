package com.nanaios.fieldarcana.block;

import com.nanaios.fieldarcana.FieldArcana;
import com.nanaios.fieldarcana.registry.ArcanaBlockEntities;
import com.nanaios.fieldarcana.spell.ArcanaTuning;
import com.nanaios.fieldtheory.api.FieldSource;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldWorld;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FieldEmitterBlockEntity extends BlockEntity {
    public static final ResourceLocation ORIGIN = FieldArcana.id("field_emitter");

    @Nullable
    private FieldType field;
    private int polarity = 1;

    public FieldEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ArcanaBlockEntities.FIELD_EMITTER.get(), pos, state);
    }

    /** Stable id of the source owned by the emitter at {@code pos}. */
    public static UUID sourceId(BlockPos pos) {
        return UUID.nameUUIDFromBytes(("field_arcana:emitter@" + pos.asLong()).getBytes(StandardCharsets.UTF_8));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FieldEmitterBlockEntity emitter) {
        if (level.getGameTime() % 5 != 0) {
            return;
        }
        FieldWorld world = FieldTheoryAPI.get(level);
        UUID id = sourceId(pos);
        int power = level.getBestNeighborSignal(pos);
        boolean active = emitter.field != null && power > 0;
        if (active) {
            double charge = emitter.polarity * power * ArcanaTuning.emitterChargePerSignal(emitter.field);
            Optional<FieldSource> current = world.source(id);
            if (current.isEmpty() || current.get().type() != emitter.field || current.get().charge() != charge) {
                active = world.putSource(FieldSource.permanent(id, emitter.field, Vec3.atCenterOf(pos), charge, ORIGIN));
            }
        } else if (world.source(id).isPresent()) {
            world.removeSource(id);
        }
        if (state.getValue(FieldEmitterBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(FieldEmitterBlock.ACTIVE, active), 3);
        }
    }

    public void attune(FieldType type) {
        this.field = type;
        setChanged();
    }

    public void togglePolarity() {
        this.polarity = -polarity;
        setChanged();
    }

    public int polarity() {
        return polarity;
    }

    @Nullable
    public FieldType field() {
        return field;
    }

    public Component describe() {
        Component fieldName = field == null ? Component.translatable("message.field_arcana.unattuned") : field.displayName();
        return Component.translatable("message.field_arcana.emitter.status", fieldName, polarity > 0 ? "+" : "−");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (field != null) {
            tag.putString("field", field.id().toString());
        }
        tag.putInt("polarity", polarity);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ResourceLocation id = tag.contains("field") ? ResourceLocation.tryParse(tag.getString("field")) : null;
        field = id == null ? null : FieldTheoryAPI.fieldTypes().get(id);
        polarity = tag.contains("polarity") && tag.getInt("polarity") < 0 ? -1 : 1;
    }
}
