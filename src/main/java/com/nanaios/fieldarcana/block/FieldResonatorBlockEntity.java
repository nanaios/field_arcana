package com.nanaios.fieldarcana.block;

import com.nanaios.fieldarcana.FieldArcana;
import com.nanaios.fieldarcana.registry.ArcanaBlockEntities;
import com.nanaios.fieldarcana.spell.ArcanaTuning;
import com.nanaios.fieldtheory.api.FieldSource;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldWorld;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
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

/**
 * 共鳴器 — an oscillating source. Its amplitude follows the redstone signal; period and phase are
 * tuned by hand. Two resonators with the same period are coherent and interfere.
 */
public class FieldResonatorBlockEntity extends BlockEntity {
    public static final ResourceLocation ORIGIN = FieldArcana.id("field_resonator");
    /** Selectable periods in ticks. With the default wave speed 0.5 the wavelengths are 4–24 blocks. */
    public static final int[] PERIODS = {8, 12, 16, 24, 32, 48};
    public static final int PHASE_STEPS = 4;

    @Nullable
    private FieldType field;
    private int periodIndex = 1;
    private int phaseIndex;

    public FieldResonatorBlockEntity(BlockPos pos, BlockState state) {
        super(ArcanaBlockEntities.FIELD_RESONATOR.get(), pos, state);
    }

    public static UUID sourceId(BlockPos pos) {
        return UUID.nameUUIDFromBytes(("field_arcana:resonator@" + pos.asLong()).getBytes(StandardCharsets.UTF_8));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FieldResonatorBlockEntity resonator) {
        if (level.getGameTime() % 5 != 0) {
            return;
        }
        FieldWorld world = FieldTheoryAPI.get(level);
        UUID id = sourceId(pos);
        int power = level.getBestNeighborSignal(pos);
        boolean active = resonator.field != null && power > 0;
        if (active) {
            FieldSource wanted = new FieldSource(id, resonator.field, Vec3.atCenterOf(pos),
                    power * ArcanaTuning.emitterChargePerSignal(resonator.field), -1L, ORIGIN,
                    FieldSource.frequencyForPeriod(resonator.period()), resonator.phaseRadians());
            Optional<FieldSource> current = world.source(id);
            if (current.isEmpty() || !current.get().equals(wanted)) {
                active = world.putSource(wanted);
            }
        } else if (world.source(id).isPresent()) {
            world.removeSource(id);
        }
        if (state.getValue(FieldResonatorBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(FieldResonatorBlock.ACTIVE, active), 3);
        }
    }

    public void attune(FieldType type) {
        this.field = type;
        setChanged();
    }

    public void cyclePeriod() {
        periodIndex = (periodIndex + 1) % PERIODS.length;
        setChanged();
    }

    public void cyclePhase() {
        phaseIndex = (phaseIndex + 1) % PHASE_STEPS;
        setChanged();
    }

    public void setPeriodIndex(int index) {
        periodIndex = Mth.clamp(index, 0, PERIODS.length - 1);
        setChanged();
    }

    public void setPhaseIndex(int index) {
        phaseIndex = Math.floorMod(index, PHASE_STEPS);
        setChanged();
    }

    public int period() {
        return PERIODS[periodIndex];
    }

    public int phaseDegrees() {
        return phaseIndex * 360 / PHASE_STEPS;
    }

    public double phaseRadians() {
        return Math.toRadians(phaseDegrees());
    }

    @Nullable
    public FieldType field() {
        return field;
    }

    public Component describe() {
        Component fieldName = field == null ? Component.translatable("message.field_arcana.unattuned") : field.displayName();
        String wavelength = field == null ? "?" : String.format(Locale.ROOT, "%.1f", field.waveSpeed() * period());
        return Component.translatable("message.field_arcana.resonator.status", fieldName, period(), wavelength, phaseDegrees());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (field != null) {
            tag.putString("field", field.id().toString());
        }
        tag.putInt("period", periodIndex);
        tag.putInt("phase", phaseIndex);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ResourceLocation id = tag.contains("field") ? ResourceLocation.tryParse(tag.getString("field")) : null;
        field = id == null ? null : FieldTheoryAPI.fieldTypes().get(id);
        periodIndex = tag.contains("period") ? Mth.clamp(tag.getInt("period"), 0, PERIODS.length - 1) : 1;
        phaseIndex = Math.floorMod(tag.getInt("phase"), PHASE_STEPS);
    }
}
