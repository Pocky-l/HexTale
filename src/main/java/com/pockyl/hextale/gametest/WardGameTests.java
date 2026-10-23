package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import com.pockyl.hextale.block.FieldWardBlockEntity;
import com.pockyl.hextale.registry.ModBlocks;

/** The Field Ward: natural monster spawns fail in its watch and pass outside it; spawners and animals are untouched. */
public final class WardGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private WardGameTests() {
    }

    private static MobSpawnEvent.SpawnPlacementCheck.Result check(GameTestHelper helper, BlockPos at, EntitySpawnReason reason,
                                                                  net.minecraft.world.entity.EntityType<?> type) {
        var event = new MobSpawnEvent.SpawnPlacementCheck(type, helper.getLevel(), reason, helper.absolutePos(at),
                helper.getLevel().getRandom(), true);
        NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }

    @HexTest(maxTicks = 20)
    public static void theWardForbidsNaturalMonsters(GameTestHelper helper) {
        helper.setBlock(BASE, ModBlocks.FIELD_WARD.get());
        // A freshly set ward joins its index on the next tick.
        helper.runAfterDelay(1, () -> {
            var fail = MobSpawnEvent.SpawnPlacementCheck.Result.FAIL;
            var keep = MobSpawnEvent.SpawnPlacementCheck.Result.DEFAULT;
            helper.assertValueEqual(check(helper, BASE.offset(10, 0, 5), EntitySpawnReason.NATURAL, EntityTypes.ZOMBIE), fail,
                    "a zombie spawning in the ward's watch");
            helper.assertValueEqual(check(helper, BASE.offset(FieldWardBlockEntity.RADIUS + 3, 0, 0), EntitySpawnReason.NATURAL,
                    EntityTypes.ZOMBIE), keep, "a zombie spawning outside the watch");
            helper.assertValueEqual(check(helper, BASE.offset(3, 0, 0), EntitySpawnReason.SPAWNER, EntityTypes.ZOMBIE), keep,
                    "a spawner's zombie");
            helper.assertValueEqual(check(helper, BASE.offset(3, 0, 0), EntitySpawnReason.NATURAL, EntityTypes.COW), keep,
                    "a cow spawning in the watch");
            var trample = new BlockEvent.FarmlandTrampleEvent(helper.getLevel(), helper.absolutePos(BASE.offset(2, 0, 0)),
                    Blocks.FARMLAND.defaultBlockState(), 1.5, helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL));
            NeoForge.EVENT_BUS.post(trample);
            helper.assertTrue(trample.isCanceled(), "farmland in the watch is not trampled");
            helper.succeed();
        });
    }
}
