package com.nanaios.fieldarcana.spell;

import com.nanaios.fieldarcana.ArcanaConfig;
import com.nanaios.fieldarcana.FieldArcana;
import com.nanaios.fieldarcana.item.StaffMode;
import com.nanaios.fieldtheory.api.FieldRegion;
import com.nanaios.fieldtheory.api.FieldSource;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldWorld;
import com.nanaios.fieldtheory.api.charge.Charges;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * What the Interference Staff actually does. Note that every branch only writes into the field
 * layer — the consequences are left to the physics.
 */
public final class StaffCasting {
    public static final ResourceLocation ORIGIN = FieldArcana.id("staff");
    private static final int NEUTRAL_COLOR = 0xE6E0FF;

    private StaffCasting() {
    }

    /** @return {@code false} if nothing happened (e.g. another mod vetoed the source) */
    public static boolean cast(ServerLevel level, Player caster, StaffMode mode, @Nullable FieldType field) {
        FieldWorld world = FieldTheoryAPI.get(level);
        int color = field != null ? field.color() : NEUTRAL_COLOR;
        Vec3 eye = caster.getEyePosition();
        switch (mode) {
            case EMIT_POSITIVE, EMIT_NEGATIVE -> {
                if (field == null) {
                    return false;
                }
                Vec3 target = targetPoint(caster, ArcanaConfig.castRange());
                double charge = mode.sign() * ArcanaTuning.emitCharge(field);
                if (!world.putSource(FieldSource.temporary(level, field, target, charge, ArcanaTuning.EMIT_DURATION, ORIGIN))) {
                    return false;
                }
                ArcanaEffects.beam(level, eye, target, color);
                ArcanaEffects.burst(level, target, color, 30);
                level.playSound(null, target.x, target.y, target.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, mode.sign() > 0 ? 1.2F : 0.7F);
            }
            case IMBUE_POSITIVE, IMBUE_NEGATIVE -> {
                if (field == null) {
                    return false;
                }
                Entity target = targetEntity(caster, ArcanaTuning.IMBUE_RANGE);
                if (target == null) {
                    target = caster;
                }
                Charges.induce(target, field, mode.sign() * ArcanaTuning.imbueCharge(field), ArcanaTuning.IMBUE_DURATION);
                Vec3 center = Charges.center(target);
                if (target != caster) {
                    ArcanaEffects.beam(level, eye, center, color);
                }
                ArcanaEffects.burst(level, center, color, 20);
                level.playSound(null, center.x, center.y, center.z, SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.PLAYERS, 1.0F, mode.sign() > 0 ? 1.4F : 0.8F);
            }
            case LENS -> {
                Vec3 target = targetPoint(caster, ArcanaConfig.castRange());
                world.putRegion(FieldRegion.temporary(level, Optional.ofNullable(field), target, ArcanaTuning.LENS_RADIUS,
                        ArcanaTuning.LENS_GAIN, ArcanaTuning.REGION_DURATION, ORIGIN));
                ArcanaEffects.beam(level, eye, target, color);
                ArcanaEffects.ring(level, target, ArcanaTuning.LENS_RADIUS, 0xFFD24A);
                level.playSound(null, target.x, target.y, target.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8F, 1.5F);
            }
            case SHIELD -> {
                Vec3 center = Charges.center(caster);
                world.putRegion(FieldRegion.temporary(level, Optional.ofNullable(field), center, ArcanaTuning.SHIELD_RADIUS,
                        0.0, ArcanaTuning.REGION_DURATION, ORIGIN));
                ArcanaEffects.ring(level, center, ArcanaTuning.SHIELD_RADIUS, 0xB0B4C8);
                level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.4F);
            }
        }
        return true;
    }

    /** Centre of the free block in front of the face the caster looks at, or a point in the air. */
    public static Vec3 targetPoint(Player caster, double range) {
        HitResult hit = caster.pick(range, 1.0F, false);
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            return Vec3.atCenterOf(blockHit.getBlockPos().relative(blockHit.getDirection()));
        }
        return caster.getEyePosition().add(caster.getLookAngle().scale(8.0));
    }

    @Nullable
    public static Entity targetEntity(Player caster, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        HitResult blockHit = caster.pick(range, 1.0F, false);
        double maxDistanceSqr = blockHit.getType() == HitResult.Type.MISS ? range * range : blockHit.getLocation().distanceToSqr(eye);
        AABB box = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(caster, eye, end, box, e -> !e.isSpectator() && e.isPickable(), maxDistanceSqr);
        return hit == null ? null : hit.getEntity();
    }
}
