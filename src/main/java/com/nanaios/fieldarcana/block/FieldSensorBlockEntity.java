package com.nanaios.fieldarcana.block;

import com.nanaios.fieldarcana.registry.ArcanaBlockEntities;
import com.nanaios.fieldtheory.api.FieldSample;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldType;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FieldSensorBlockEntity extends BlockEntity {
    /** Signal per unit of source potential |φ - φ_ambient|. */
    public static final double POTENTIAL_SCALE = 2.0;
    /** Signal per unit of field intensity |E|. */
    public static final double INTENSITY_SCALE = 12.0;

    @Nullable
    private FieldType field;
    private boolean intensityMode;
    private double lastValue;

    public FieldSensorBlockEntity(BlockPos pos, BlockState state) {
        super(ArcanaBlockEntities.FIELD_SENSOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FieldSensorBlockEntity sensor) {
        if (level.getGameTime() % 4 != 0) {
            return;
        }
        int power = 0;
        if (sensor.field != null) {
            FieldSample sample = FieldTheoryAPI.get(level).sample(sensor.field, Vec3.atCenterOf(pos));
            sensor.lastValue = sensor.intensityMode ? sample.intensity() : Math.abs(sample.sourcePotential());
            double scale = sensor.intensityMode ? INTENSITY_SCALE : POTENTIAL_SCALE;
            power = Mth.clamp((int) Math.floor(sensor.lastValue * scale), 0, 15);
        }
        if (state.getValue(FieldSensorBlock.POWER) != power) {
            level.setBlock(pos, state.setValue(FieldSensorBlock.POWER, power), 3);
        }
    }

    public void attune(FieldType type) {
        this.field = type;
        setChanged();
    }

    public void toggleMode() {
        this.intensityMode = !intensityMode;
        setChanged();
    }

    public Component describe() {
        Component fieldName = field == null ? Component.translatable("message.field_arcana.unattuned") : field.displayName();
        Component mode = Component.translatable(intensityMode ? "message.field_arcana.sensor.intensity" : "message.field_arcana.sensor.potential");
        return Component.translatable("message.field_arcana.sensor.status", fieldName, mode, String.format(Locale.ROOT, "%.2f", lastValue));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (field != null) {
            tag.putString("field", field.id().toString());
        }
        tag.putBoolean("intensity", intensityMode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ResourceLocation id = tag.contains("field") ? ResourceLocation.tryParse(tag.getString("field")) : null;
        field = id == null ? null : FieldTheoryAPI.fieldTypes().get(id);
        intensityMode = tag.getBoolean("intensity");
    }
}
