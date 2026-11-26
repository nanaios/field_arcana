package com.nanaios.fieldarcana.item;

import com.nanaios.fieldarcana.ArcanaConfig;
import com.nanaios.fieldarcana.registry.ArcanaComponents;
import com.nanaios.fieldarcana.registry.ArcanaTriggers;
import com.nanaios.fieldarcana.spell.ArcanaTuning;
import com.nanaios.fieldarcana.spell.StaffCasting;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldWorld;
import com.nanaios.fieldtheory.api.charge.Charges;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 干渉杖 — the casting tool. Right-click to cast the current mode with the field of the Field Core
 * held in the other hand; sneak + right-click to switch mode. Casting spends <em>resonance</em>,
 * which the staff slowly absorbs from the ambient fields around its holder.
 */
public class InterferenceStaffItem extends Item {

    public InterferenceStaffItem(Properties properties) {
        super(properties
                .component(ArcanaComponents.STAFF_MODE.get(), StaffMode.EMIT_POSITIVE)
                .component(ArcanaComponents.RESONANCE.get(), ArcanaTuning.MAX_RESONANCE));
    }

    public static StaffMode mode(ItemStack stack) {
        return stack.getOrDefault(ArcanaComponents.STAFF_MODE.get(), StaffMode.EMIT_POSITIVE);
    }

    public static int resonance(ItemStack stack) {
        return stack.getOrDefault(ArcanaComponents.RESONANCE.get(), 0);
    }

    /** Resonance a cast of {@code mode} costs, after the server's cost multiplier. */
    public static int cost(StaffMode mode) {
        return (int) Math.ceil(mode.cost() * ArcanaConfig.costMultiplier());
    }

    @Nullable
    public static FieldType heldField(Player player, InteractionHand staffHand) {
        ItemStack other = player.getItemInHand(staffHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        return other.getItem() instanceof FieldCoreItem core ? core.field() : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack staff = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                StaffMode next = mode(staff).next();
                staff.set(ArcanaComponents.STAFF_MODE.get(), next);
                player.displayClientMessage(Component.translatable("message.field_arcana.staff.mode", next.displayName()), true);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.6F);
            }
            return InteractionResultHolder.sidedSuccess(staff, level.isClientSide);
        }

        StaffMode mode = mode(staff);
        FieldType field = heldField(player, hand);
        if (mode.needsField() && field == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.field_arcana.staff.no_core").withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(staff);
        }
        boolean free = player.getAbilities().instabuild;
        int resonance = resonance(staff);
        int cost = cost(mode);
        if (!free && resonance < cost) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.field_arcana.staff.no_resonance", resonance, cost).withStyle(ChatFormatting.GRAY), true);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.6F, 0.6F);
            }
            return InteractionResultHolder.fail(staff);
        }
        if (level instanceof ServerLevel serverLevel && StaffCasting.cast(serverLevel, player, mode, field)) {
            if (!free) {
                staff.set(ArcanaComponents.RESONANCE.get(), resonance - cost);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                ArcanaTriggers.CAST_SPELL.get().trigger(serverPlayer, mode, field);
            }
            player.getCooldowns().addCooldown(this, 10);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(staff, level.isClientSide);
    }

    /** Absorb resonance from the ambient ("vacuum") value of every field once per second. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || level.getGameTime() % 20 != 0) {
            return;
        }
        int resonance = resonance(stack);
        if (resonance >= ArcanaTuning.MAX_RESONANCE) {
            return;
        }
        FieldWorld world = FieldTheoryAPI.get(level);
        Vec3 pos = Charges.center(entity);
        double ambient = 0.0;
        for (FieldType type : FieldTheoryAPI.fieldTypes()) {
            ambient += Math.abs(world.sample(type, pos, entity).ambient());
        }
        int gained = Math.max(0, (int) Math.round(ambient * ArcanaTuning.REGEN_PER_AMBIENT * ArcanaConfig.regenMultiplier()));
        if (gained > 0) {
            stack.set(ArcanaComponents.RESONANCE.get(), Math.min(ArcanaTuning.MAX_RESONANCE, resonance + gained));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return resonance(stack) < ArcanaTuning.MAX_RESONANCE;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13.0F * resonance(stack) / ArcanaTuning.MAX_RESONANCE), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xB58CFF;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack))
                .append(Component.literal(" — ").withStyle(ChatFormatting.DARK_GRAY))
                .append(mode(stack).displayName().withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.field_arcana.staff.resonance", resonance(stack), ArcanaTuning.MAX_RESONANCE).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.field_arcana.staff.mode_hint." + mode(stack).getSerializedName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.field_arcana.staff.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
