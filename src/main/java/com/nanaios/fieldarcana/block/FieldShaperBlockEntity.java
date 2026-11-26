package com.nanaios.fieldarcana.block;

import com.nanaios.fieldarcana.FieldArcana;
import com.nanaios.fieldarcana.registry.ArcanaBlockEntities;
import com.nanaios.fieldtheory.api.FieldRegion;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 場整形器 — a permanent spherical region around the block that shields, dampens, amplifies or
 * focuses a field (or every field). Its radius follows the redstone signal.
 */
public class FieldShaperBlockEntity extends BlockEntity {
    public static final ResourceLocation ORIGIN = FieldArcana.id("field_shaper");

    public enum Mode {
        SHIELD("shield", 0.0),
        DAMPEN("dampen", 0.5),
        AMPLIFY("amplify", 2.0),
        LENS("lens", 3.0);

        private final String key;
        private final double gain;

        Mode(String key, double gain) {
            this.key = key;
            this.gain = gain;
        }

        public double gain() {
            return gain;
        }

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    @Nullable
    private FieldType field;
    private Mode mode = Mode.SHIELD;
    @Nullable
    private FieldRegion applied;

    public FieldShaperBlockEntity(BlockPos pos, BlockState state) {
        super(ArcanaBlockEntities.FIELD_SHAPER.get(), pos, state);
    }

    public static UUID regionId(BlockPos pos) {
        return UUID.nameUUIDFromBytes(("field_arcana:shaper@" + pos.asLong()).getBytes(StandardCharsets.UTF_8));
    }

    /** Radius in blocks for a redstone signal (0 = off). */
    public static double radiusFor(int power) {
        return power <= 0 ? 0.0 : 1.5 + 0.5 * power;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FieldShaperBlockEntity shaper) {
        if (level.getGameTime() % 5 != 0) {
            return;
        }
        FieldWorld world = FieldTheoryAPI.get(level);
        double radius = radiusFor(level.getBestNeighborSignal(pos));
        boolean active = radius > 0;
        if (active) {
            FieldRegion wanted = new FieldRegion(regionId(pos), Optional.ofNullable(shaper.field), Vec3.atCenterOf(pos),
                    radius, shaper.mode.gain, -1L, ORIGIN);
            if (!wanted.equals(shaper.applied)) {
                world.putRegion(wanted);
                shaper.applied = wanted;
            }
        } else if (shaper.applied != null || world.regions().stream().anyMatch(r -> r.id().equals(regionId(pos)))) {
            world.removeRegion(regionId(pos));
            shaper.applied = null;
        }
        if (state.getValue(FieldShaperBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(FieldShaperBlock.ACTIVE, active), 3);
        }
    }

    public void attune(@Nullable FieldType type) {
        this.field = type;
        setChanged();
    }

    public void cycleMode() {
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
        Component fieldName = field == null ? Component.translatable("message.field_arcana.lens.all") : field.displayName();
        Component modeName = Component.translatable("message.field_arcana.shaper." + mode.key);
        return Component.translatable("message.field_arcana.shaper.status", fieldName, modeName, String.format(Locale.ROOT, "%.1f", mode.gain));
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
        mode = Mode.SHIELD;
        for (Mode m : Mode.values()) {
            if (m.key.equals(tag.getString("mode"))) {
                mode = m;
            }
        }
        applied = null;
    }
}
