package com.nanaios.fieldarcana.gametest;

import com.nanaios.fieldarcana.FieldArcana;
import com.nanaios.fieldarcana.block.FieldEmitterBlock;
import com.nanaios.fieldarcana.block.FieldEmitterBlockEntity;
import com.nanaios.fieldarcana.block.FieldResonatorBlockEntity;
import com.nanaios.fieldarcana.block.FieldSensorBlock;
import com.nanaios.fieldarcana.block.FieldSensorBlockEntity;
import com.nanaios.fieldarcana.item.StaffMode;
import com.nanaios.fieldarcana.phenomena.Resonance;
import com.nanaios.fieldarcana.phenomena.Transmutations;
import com.nanaios.fieldarcana.registry.ArcanaBlocks;
import com.nanaios.fieldarcana.spell.StaffCasting;
import com.nanaios.fieldtheory.api.FieldSource;
import com.nanaios.fieldtheory.api.FieldTheoryAPI;
import com.nanaios.fieldtheory.api.FieldType;
import com.nanaios.fieldtheory.api.FieldTypes;
import com.nanaios.fieldtheory.api.FieldWorld;
import com.nanaios.fieldtheory.api.charge.Charges;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In-world tests of Field Arcana's phenomena. One batch per test so that fields never leak
 * between tests. The structure's floor top is at relative y = 2.
 *
 * <p>Run with {@code ./gradlew runGameTestServer}.</p>
 */
@GameTestHolder(FieldArcana.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArcanaGameTests {
    private static final String LAB = "field_lab";
    private static final ResourceLocation ORIGIN = FieldArcana.id("gametest");

    private ArcanaGameTests() {
    }

    private static FieldWorld world(GameTestHelper helper) {
        return FieldTheoryAPI.get(helper.getLevel());
    }

    private static void put(GameTestHelper helper, FieldType type, Vec3 relative, double charge) {
        world(helper).putSource(FieldSource.temporary(helper.getLevel(), type, helper.absoluteVec(relative), charge, 400, ORIGIN));
    }

    private static void cleanup(GameTestHelper helper) {
        world(helper).removeSources(s -> s.origin().equals(ORIGIN) || s.origin().equals(StaffCasting.ORIGIN));
        world(helper).removeRegions(r -> r.origin().equals(StaffCasting.ORIGIN));
    }

    /** Mutatio alone does nothing to sand; with a Fulgur field it becomes glass (fulgurite). */
    @GameTest(template = LAB, batch = "fa_fulgurite")
    public static void fulguriteNeedsBothFields(GameTestHelper helper) {
        BlockPos sand = new BlockPos(7, 2, 7);
        helper.setBlock(sand, Blocks.SAND);
        put(helper, FieldTypes.MUTATIO.get(), new Vec3(7.5, 2.5, 7.5), 8.0);
        helper.assertTrue(Transmutations.tryTransmute(helper.getLevel(), world(helper), helper.absolutePos(sand)) == null,
                "sand must not transmute without Fulgur");
        helper.assertBlockPresent(Blocks.SAND, sand);

        put(helper, FieldTypes.FULGUR.get(), new Vec3(7.5, 2.5, 9.5), 8.0);
        ResourceLocation fired = Transmutations.tryTransmute(helper.getLevel(), world(helper), helper.absolutePos(sand));
        helper.assertTrue(FieldArcana.id("fulgurite").equals(fired), "sand should transmute by the fulgurite recipe, got " + fired);
        helper.assertBlockPresent(Blocks.GLASS, sand);
        cleanup(helper);
        helper.succeed();
    }

    /** The same input gives different products depending on the other fields present. */
    @GameTest(template = LAB, batch = "fa_specificity")
    public static void otherFieldsChooseTheProduct(GameTestHelper helper) {
        BlockPos first = new BlockPos(5, 2, 7);
        BlockPos second = new BlockPos(9, 2, 7);
        helper.setBlock(first, Blocks.COBBLESTONE);
        helper.setBlock(second, Blocks.COBBLESTONE);
        put(helper, FieldTypes.MUTATIO.get(), new Vec3(5.5, 2.5, 7.5), 8.0);
        Transmutations.tryTransmute(helper.getLevel(), world(helper), helper.absolutePos(first));
        helper.assertBlockPresent(Blocks.GRAVEL, first);

        put(helper, FieldTypes.MUTATIO.get(), new Vec3(9.5, 2.5, 7.5), 8.0);
        put(helper, FieldTypes.GRAVIS.get(), new Vec3(9.5, 3.5, 8.5), 24.0);
        Transmutations.tryTransmute(helper.getLevel(), world(helper), helper.absolutePos(second));
        helper.assertBlockPresent(Blocks.COBBLED_DEEPSLATE, second);
        cleanup(helper);
        helper.succeed();
    }

    /** A powered emitter is read by a sensor four blocks away: wireless redstone through a field. */
    @GameTest(template = LAB, batch = "fa_emitter_sensor", timeoutTicks = 120)
    public static void emitterDrivesSensor(GameTestHelper helper) {
        BlockPos emitterPos = new BlockPos(5, 2, 7);
        BlockPos powerPos = new BlockPos(5, 2, 6);
        BlockPos sensorPos = new BlockPos(9, 2, 7);
        helper.setBlock(emitterPos, ArcanaBlocks.FIELD_EMITTER.get());
        helper.setBlock(sensorPos, ArcanaBlocks.FIELD_SENSOR.get());
        FieldEmitterBlockEntity emitter = helper.getBlockEntity(emitterPos);
        FieldSensorBlockEntity sensor = helper.getBlockEntity(sensorPos);
        emitter.attune(FieldTypes.FULGUR.get());
        sensor.attune(FieldTypes.FULGUR.get());
        helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertBlockProperty(emitterPos, FieldEmitterBlock.ACTIVE, true);
                    helper.assertTrue(helper.getBlockState(sensorPos).getValue(FieldSensorBlock.POWER) > 0, "sensor should read the emitter");
                })
                .thenExecute(() -> helper.setBlock(powerPos, Blocks.AIR))
                .thenWaitUntil(() -> {
                    helper.assertBlockProperty(emitterPos, FieldEmitterBlock.ACTIVE, false);
                    helper.assertTrue(helper.getBlockState(sensorPos).getValue(FieldSensorBlock.POWER) == 0, "sensor should fall silent");
                })
                .thenExecute(() -> {
                    helper.setBlock(powerPos, Blocks.REDSTONE_BLOCK);
                })
                .thenWaitUntil(() -> helper.assertTrue(world(helper).source(FieldEmitterBlockEntity.sourceId(helper.absolutePos(emitterPos))).isPresent(), "source should exist again"))
                .thenExecute(() -> helper.destroyBlock(emitterPos))
                .thenExecute(() -> helper.assertTrue(world(helper).source(FieldEmitterBlockEntity.sourceId(helper.absolutePos(emitterPos))).isEmpty(),
                        "breaking the emitter must remove its source"))
                .thenSucceed();
    }

    /** Inside a Nexus field a falling body is slowed to a crawl. */
    @GameTest(template = LAB, batch = "fa_binding", timeoutTicks = 100)
    public static void nexusBindingSlowsFalls(GameTestHelper helper) {
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(7.5, 6.0, 7.5));
        double startY = stand.getY();
        put(helper, FieldTypes.NEXUS.get(), new Vec3(7.5, 5.0, 7.5), 16.0);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(startY - stand.getY() < 2.0, "bound stand should fall slowly, fell " + (startY - stand.getY()));
            helper.assertTrue(stand.getY() < startY, "bound stand should still fall a little");
            cleanup(helper);
            helper.succeed();
        });
    }

    /**
     * An oppositely charged creature is pulled toward a strong Fulgur source until the field breaks
     * down through it: it is hurt and discharged (a spark gap). A like charge would simply be
     * repelled out of danger.
     */
    @GameTest(template = LAB, batch = "fa_breakdown", timeoutTicks = 100)
    public static void chargedBodiesBreakDown(GameTestHelper helper) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(7.5, 2.0, 7.5));
        Charges.induce(pig, FieldTypes.FULGUR.get(), -4.0, 400);
        put(helper, FieldTypes.FULGUR.get(), new Vec3(7.5, 3.5, 9.5), 12.0);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "pig should be hurt by breakdown");
            helper.assertTrue(!Charges.induced(pig).has(FieldTypes.FULGUR.get()), "breakdown should discharge the pig");
            cleanup(helper);
        });
    }

    /** The staff writes into the field layer: Emit places a source, Imbue (no target) charges the caster. */
    @GameTest(template = LAB, batch = "fa_staff")
    public static void staffWritesIntoFields(GameTestHelper helper) {
        FakePlayer caster = FakePlayerFactory.getMinecraft(helper.getLevel());
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 2.0, 7.5));
        caster.moveTo(at.x, at.y, at.z, -90.0F, 0.0F);
        FieldType gravis = FieldTypes.GRAVIS.get();

        helper.assertTrue(StaffCasting.cast(helper.getLevel(), caster, StaffMode.EMIT_POSITIVE, gravis), "emit should succeed");
        helper.assertTrue(world(helper).sources().stream().anyMatch(s -> s.origin().equals(StaffCasting.ORIGIN) && s.type() == gravis && s.charge() > 0),
                "emit should create a positive Gravis source");

        caster.setXRot(-90.0F);
        helper.assertTrue(StaffCasting.cast(helper.getLevel(), caster, StaffMode.IMBUE_NEGATIVE, FieldTypes.FULGUR.get()), "imbue should succeed");
        helper.assertTrue(Charges.induced(caster).value(FieldTypes.FULGUR.get()) < 0, "imbue without a target should charge the caster");

        Charges.clearInduced(caster, null);
        cleanup(helper);
        helper.succeed();
    }

    /**
     * Two coherent resonators: in phase, a sensor in amplitude mode on their mid-plane reads a
     * strong signal; shifting one by 180° cancels the wave there and the sensor falls silent.
     */
    @GameTest(template = LAB, batch = "fa_interference", timeoutTicks = 160)
    public static void resonatorsInterfere(GameTestHelper helper) {
        BlockPos left = new BlockPos(4, 2, 7);
        BlockPos right = new BlockPos(10, 2, 7);
        BlockPos sensorPos = new BlockPos(7, 2, 7);
        for (BlockPos pos : new BlockPos[]{left, right}) {
            helper.setBlock(pos, ArcanaBlocks.FIELD_RESONATOR.get());
            helper.setBlock(pos.north(), Blocks.REDSTONE_BLOCK);
            FieldResonatorBlockEntity resonator = helper.getBlockEntity(pos);
            resonator.attune(FieldTypes.FULGUR.get());
            resonator.setPeriodIndex(1);
            resonator.setPhaseIndex(0);
        }
        helper.setBlock(sensorPos, ArcanaBlocks.FIELD_SENSOR.get());
        FieldSensorBlockEntity sensor = helper.getBlockEntity(sensorPos);
        sensor.attune(FieldTypes.FULGUR.get());
        sensor.setMode(FieldSensorBlockEntity.Mode.AMPLITUDE);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(helper.getBlockState(sensorPos).getValue(FieldSensorBlock.POWER) >= 12,
                        "in-phase resonators should reinforce on the mid-plane, power " + helper.getBlockState(sensorPos).getValue(FieldSensorBlock.POWER)))
                .thenExecute(() -> ((FieldResonatorBlockEntity) helper.getBlockEntity(right)).setPhaseIndex(2))
                .thenWaitUntil(() -> helper.assertTrue(helper.getBlockState(sensorPos).getValue(FieldSensorBlock.POWER) == 0,
                        "antiphase resonators should cancel on the mid-plane, power " + helper.getBlockState(sensorPos).getValue(FieldSensorBlock.POWER)))
                .thenSucceed();
    }

    /** Amethyst sings inside a strong oscillating field; ordinary stone does not. */
    @GameTest(template = LAB, batch = "fa_singing")
    public static void amethystSings(GameTestHelper helper) {
        BlockPos crystal = new BlockPos(8, 2, 7);
        BlockPos stone = new BlockPos(6, 2, 7);
        helper.setBlock(crystal, Blocks.AMETHYST_BLOCK);
        helper.setBlock(stone, Blocks.STONE);
        FieldSource wave = new FieldSource(java.util.UUID.randomUUID(), FieldTypes.FULGUR.get(), helper.absoluteVec(new Vec3(7.5, 3.5, 7.5)),
                15.0, helper.getLevel().getGameTime() + 200, ORIGIN, FieldSource.frequencyForPeriod(12), 0.0);
        world(helper).putSource(wave);
        helper.assertTrue(Resonance.trySing(helper.getLevel(), world(helper), helper.absolutePos(crystal), wave), "amethyst should sing");
        helper.assertTrue(!Resonance.trySing(helper.getLevel(), world(helper), helper.absolutePos(stone), wave), "stone must not sing");
        cleanup(helper);
        helper.succeed();
    }

    /** Every advancement loads, including the custom trigger conditions. */
    @GameTest(template = LAB, batch = "fa_advancements")
    public static void advancementsLoad(GameTestHelper helper) {
        for (String name : new String[]{"root", "crystallised_field", "observer", "interference", "gravity_well", "negative_mass",
                "faraday_cage", "spark_gap", "field_telegraph", "philosophers_field", "fulgurite", "carbon_under_pressure", "singing_crystal"}) {
            helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(FieldArcana.id(name)) != null, "advancement " + name + " did not load");
        }
        helper.succeed();
    }
}
