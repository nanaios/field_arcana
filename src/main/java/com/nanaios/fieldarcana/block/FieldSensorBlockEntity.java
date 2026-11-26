package com.nanaios.fieldarcana.block;

import com.nanaios.fieldarcana.advancement.WitnessPhenomenonTrigger;
import com.nanaios.fieldarcana.registry.ArcanaBlockEntities;
import com.nanaios.fieldarcana.registry.ArcanaTriggers;
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
import net.minecraft.server.level.ServerLevel;
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
    /** Signal per unit of oscillation amplitude. */
    public static final double AMPLITUDE_SCALE = 2.0;

    /** What the sensor measures. */
    public enum Mode {
        POTENTIAL("potential", POTENTIAL_SCALE),
        INTENSITY("intensity", INTENSITY_SCALE),
        AMPLITUDE("amplitude", AMPLITUDE_SCALE);

        private final String key;
        private final double scale;

        Mode(String key, double scale) {
            this.key = key;
            this.scale = scale;
        }

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    @Nullable
    private FieldType field;
    private Mode mode = Mode.POTENTIAL;
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
            Vec3 at = Vec3.atCenterOf(pos);
            if (sensor.mode == Mode.AMPLITUDE) {
                sensor.lastValue = FieldTheoryAPI.get(level).amplitude(sensor.field, at);
            } else {
                FieldSample sample = FieldTheoryAPI.get(level).sample(sensor.field, at);
                sensor.lastValue = sensor.mode == Mode.INTENSITY ? sample.intensity() : Math.abs(sample.sourcePotential());
            }
            power = Mth.clamp((int) Math.floor(sensor.lastValue * sensor.mode.scale), 0, 15);
        }
        int previous = state.getValue(FieldSensorBlock.POWER);
        if (previous != power) {
            level.setBlock(pos, state.setValue(FieldSensorBlock.POWER, power), 3);
            if (previous == 0 && level instanceof ServerLevel serverLevel) {
                ArcanaTriggers.WITNESS.get().witness(serverLevel, Vec3.atCenterOf(pos), WitnessPhenomenonTrigger.Kind.TELEGRAPH, null);
            }
        }
    }

    public void attune(FieldType type) {
        this.field = type;
        setChanged();
    }

    public void toggleMode() {
        this.mode = mode.next();
        setChanged();
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        setChanged();
    }

    public Mode mode() {
        return mode;
    }

    public Component describe() {
        Component fieldName = field == null ? Component.translatable("message.field_arcana.unattuned") : field.displayName();
        Component modeName = Component.translatable("message.field_arcana.sensor." + mode.key);
        return Component.translatable("message.field_arcana.sensor.status", fieldName, modeName, String.format(Locale.ROOT, "%.2f", lastValue));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (field != null) {
            tag.putString("field", field.id().toString());
        }
        tag.putString("mode", mode.key);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ResourceLocation id = tag.contains("field") ? ResourceLocation.tryParse(tag.getString("field")) : null;
        field = id == null ? null : FieldTheoryAPI.fieldTypes().get(id);
        mode = Mode.POTENTIAL;
        if (tag.contains("mode")) {
            for (Mode m : Mode.values()) {
                if (m.key.equals(tag.getString("mode"))) {
                    mode = m;
                }
            }
        } else if (tag.getBoolean("intensity")) {
            mode = Mode.INTENSITY; // saved by 0.1/0.2
        }
    }
}
