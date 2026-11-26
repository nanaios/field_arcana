package com.nanaios.fieldarcana.item;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/**
 * The four ways of interfering with a field:
 * <ul>
 *   <li><b>Emit</b> — place a point source (needs a Field Core in the other hand);</li>
 *   <li><b>Imbue</b> — induce charge on a creature (or yourself), making it a source and a target;</li>
 *   <li><b>Lens</b> — amplify the field inside a sphere (gain ×3);</li>
 *   <li><b>Shield</b> — cancel the field around you (gain ×0).</li>
 * </ul>
 */
public enum StaffMode implements StringRepresentable {
    EMIT_POSITIVE("emit_positive", 1, 120, true),
    EMIT_NEGATIVE("emit_negative", -1, 120, true),
    IMBUE_POSITIVE("imbue_positive", 1, 100, true),
    IMBUE_NEGATIVE("imbue_negative", -1, 100, true),
    LENS("lens", 0, 80, false),
    SHIELD("shield", 0, 80, false);

    public static final Codec<StaffMode> CODEC = StringRepresentable.fromEnum(StaffMode::values);
    public static final StreamCodec<FriendlyByteBuf, StaffMode> STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(StaffMode.class);

    private final String name;
    private final int sign;
    private final int cost;
    private final boolean needsField;

    StaffMode(String name, int sign, int cost, boolean needsField) {
        this.name = name;
        this.sign = sign;
        this.cost = cost;
        this.needsField = needsField;
    }

    public int sign() {
        return sign;
    }

    public int cost() {
        return cost;
    }

    public boolean needsField() {
        return needsField;
    }

    public StaffMode next() {
        StaffMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public MutableComponent displayName() {
        return Component.translatable("staff_mode.field_arcana." + name);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
