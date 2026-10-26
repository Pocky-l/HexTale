package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.HextechPurifierBlockEntity;
import com.pockyl.hextale.block.NullstoneBlock;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.thevoid.Healing;
import com.pockyl.hextale.thevoid.TheVoid;
import com.pockyl.hextale.thevoid.Thinning;
import com.pockyl.hextale.thevoid.VoidShape;

/** The healing road: nullstone drinks sick land, the Purifier's crystal clouds as it draws, land heals, breaches starve. */
public final class HealingGameTests {
    private HealingGameTests() {
    }

    @HexTest
    public static void nullstoneDrinksSickLand(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.NULLSTONE.get());
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(at);
        LevelChunk chunk = level.getChunkAt(pos);
        Thinning.set(level, chunk, 1000);
        for (int i = 0; i < 40 && level.getBlockState(pos).getValue(NullstoneBlock.SATURATION) < NullstoneBlock.FULL; i++) {
            level.getBlockState(pos).randomTick(level, pos, level.getRandom());
        }
        float left = Thinning.get(chunk);
        helper.assertTrue(left < 1000 && left >= 1000 - 40 * NullstoneBlock.DRINK, "the stone drank the land, left " + left);
        helper.assertTrue(level.getBlockState(pos).getValue(NullstoneBlock.SATURATION) == NullstoneBlock.FULL, "and filled up drinking");
        Thinning.set(level, chunk, 0);
        BlockState full = level.getBlockState(pos);
        for (int i = 0; i < 60; i++) {
            level.getBlockState(pos).randomTick(level, pos, level.getRandom());
        }
        helper.assertTrue(level.getBlockState(pos).getValue(NullstoneBlock.SATURATION) < full.getValue(NullstoneBlock.SATURATION),
                "on well land what it drank seeps away");
        helper.succeed();
    }

    @HexTest
    public static void voidGroundHealsBackIntoWhatLayUnderIt(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sand = new BlockPos(0, 1, 0);
        BlockPos rock = new BlockPos(2, 1, 0);
        helper.setBlock(sand.below(), Blocks.SANDSTONE);
        helper.setBlock(sand, ModBlocks.VOID_SOIL.get());
        helper.setBlock(rock.below(), Blocks.ANDESITE);
        helper.setBlock(rock, ModBlocks.VOID_ROCK.get());
        helper.assertTrue(Healing.restored(level, helper.absolutePos(sand), helper.getBlockState(sand)).is(Blocks.SAND),
                "soil over sandstone was sand");
        helper.assertTrue(Healing.restored(level, helper.absolutePos(rock), helper.getBlockState(rock)).is(Blocks.ANDESITE),
                "rock over andesite was andesite");

        // A blighted patch on well land heals away, and the chunk is whole again.
        BlockPos ground = new BlockPos(1, 1, 2);
        helper.setBlock(ground.below(), Blocks.DIRT);
        helper.setBlock(ground, Blocks.GRASS_BLOCK);
        BlockPos abs = helper.absolutePos(ground);
        LevelChunk chunk = level.getChunkAt(abs);
        Thinning.set(level, chunk, 0);
        helper.assertTrue(Thinning.blightColumn(level, abs.getX(), abs.getZ(), level.getRandom()), "the blight takes the grass");
        helper.assertBlockPresent(ModBlocks.VOID_SOIL.get(), ground);
        helper.assertTrue(Healing.scarred(chunk), "the chunk is scarred");
        int rounds = 0;
        int healed = 0;
        for (; rounds < 400 && Healing.scarred(chunk); rounds++) {
            healed += Healing.healPatch(level, chunk, level.getRandom()) ? 1 : 0;
        }
        helper.assertTrue(!helper.getBlockState(ground).is(ModBlocks.VOID_SOIL.get()), "the soil healed, got " + helper.getBlockState(ground)
                + " after " + rounds + " rounds, " + healed + " healed, scarred " + Healing.scarred(chunk) + ", surface "
                + level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, abs.getX(), abs.getZ())
                + " ground y " + abs.getY() + ", value " + Thinning.get(chunk) + ", column " + column(level, abs));
        helper.assertTrue(!Healing.scarred(chunk), "with no Void ground left the chunk is whole");
        helper.succeed();
    }

    private static String column(ServerLevel level, BlockPos pos) {
        StringBuilder out = new StringBuilder();
        for (int dy = -2; dy <= 3; dy++) {
            out.append(pos.getY() + dy).append('=').append(level.getBlockState(pos.above(dy)).getBlock().getDescriptionId()).append(' ');
        }
        return out.toString();
    }

    @HexTest
    public static void thePurifierCloudsItsCrystal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = new BlockPos(1, 2, 1);
        BlockPos jarAt = new BlockPos(3, 2, 1);
        helper.setBlock(at, ModBlocks.HEXTECH_PURIFIER.get());
        HextechPurifierBlockEntity purifier = helper.getBlockEntity(at, HextechPurifierBlockEntity.class);
        LevelChunk chunk = level.getChunkAt(helper.absolutePos(at));
        Thinning.setAround(level, helper.absolutePos(at), 1, 0);
        Thinning.set(level, chunk, 1800);
        purifier.work(level);
        helper.assertTrue(purifier.mode() == HextechPurifierBlockEntity.Mode.EMPTY, "without a crystal it does nothing");
        helper.assertTrue(purifier.insert(new ItemStack(ModItems.HEXTECH_CRYSTAL.get())), "a hextech crystal goes in");
        purifier.work(level);
        helper.assertTrue(purifier.mode() == HextechPurifierBlockEntity.Mode.UNLIT, "without light it cannot draw, got " + purifier.mode());

        helper.setBlock(jarAt, ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity jar = helper.getBlockEntity(jarAt, EssenceTankBlockEntity.class);
        jar.tank().set(EssenceMix.pure(Essence.LIGHT, 200));
        purifier.work(level);
        float half = Thinning.get(chunk);
        helper.assertTrue(purifier.mode() == HextechPurifierBlockEntity.Mode.WORKING && half < 1800, "with light it draws the land");
        helper.assertTrue(purifier.getUpdateTag(level.registryAccess()).contains("drew_from"), "the jar it drew from reaches the client");
        ItemStack out = purifier.take();
        helper.assertTrue(out.is(ModItems.HEXTECH_CRYSTAL.get()) && Math.abs(Thinning.get(chunk) - 1800) < 0.01F,
                "a crystal taken out half full gives back what it held, got " + Thinning.get(chunk));

        purifier.insert(new ItemStack(ModItems.HEXTECH_CRYSTAL.get()));
        for (int i = 0; i < 1000 && !purifier.crystal().is(ModItems.CLOUDED_CRYSTAL.get()); i++) {
            purifier.work(level);
        }
        helper.assertTrue(purifier.crystal().is(ModItems.CLOUDED_CRYSTAL.get()), "the crystal clouds over as it fills");
        float drawn = 1800 - Thinning.get(chunk);
        helper.assertTrue(Math.abs(drawn - HextechPurifierBlockEntity.HOLDS) < 1, "a crystal holds what it drew, got " + drawn);
        int light = jar.tank().contents().get(Essence.LIGHT);
        helper.assertTrue(light < 200 && light >= 200 - HextechPurifierBlockEntity.HOLDS / HextechPurifierBlockEntity.PER_MB - 8,
                "the light it burned pays for it, left " + light);
        purifier.work(level);
        helper.assertTrue(purifier.mode() == HextechPurifierBlockEntity.Mode.CLOUDED, "a clouded crystal rests it");
        helper.assertTrue(purifier.canTakeItemThroughFace(0, purifier.crystal(), net.minecraft.core.Direction.DOWN)
                && !purifier.canTakeItemThroughFace(0, purifier.crystal(), net.minecraft.core.Direction.UP), "hoppers take it from below");
        Thinning.setAround(level, helper.absolutePos(at), 1, 0);
        helper.succeed();
    }

    @HexTest(maxTicks = 500)
    public static void aBreachOnHealedLandStarvesAndCloses(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 centre = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 4, 1)));
        LevelChunk chunk = level.getChunkAt(helper.absolutePos(new BlockPos(1, 4, 1)));
        Thinning.set(level, chunk, 0);
        TheVoid breach = TheVoid.rooted(level, centre, 0, VoidShape.EYE, 3.5F, 778);
        level.addFreshEntity(breach);
        breach.setStarved(TheVoid.STARVE_TICKS - TheVoid.FEED_TICKS);
        helper.succeedWhen(() -> helper.assertTrue(breach.closing() || breach.isRemoved(),
                "it starves and closes (starving " + breach.starving() + ", age " + breach.age(0) + ")"));
    }

    @HexTest(maxTicks = 400)
    public static void aBreachOnSickLandIsFed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 4, 1));
        LevelChunk chunk = level.getChunkAt(at);
        Thinning.set(level, chunk, TheVoid.FADES_BELOW + 50);
        TheVoid breach = TheVoid.rooted(level, Vec3.atCenterOf(at), 0, VoidShape.EYE, 3.5F, 779);
        level.addFreshEntity(breach);
        breach.setStarved(TheVoid.STARVE_TICKS / 2);
        helper.runAfterDelay(TheVoid.OMEN_TICKS + TheVoid.ROOTED_OPEN_TICKS + TheVoid.FEED_TICKS * 2, () -> {
            helper.assertTrue(!breach.closing() && breach.starving() < 0.5F, "on sick land it is fed back, starving " + breach.starving());
            breach.discard();
            Thinning.set(level, chunk, 0);
            helper.succeed();
        });
    }
}
