package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import com.pockyl.hextale.block.ChemBoilerBlock;
import com.pockyl.hextale.block.ChemBoilerBlockEntity;
import com.pockyl.hextale.block.ChemBurnerBlock;
import com.pockyl.hextale.block.ChemBurnerBlockEntity;
import com.pockyl.hextale.block.DeviceCrystals;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.HextechPurifierBlockEntity;
import com.pockyl.hextale.block.VoidAnchorBlockEntity;
import com.pockyl.hextale.block.HextechLamppostBlock;
import com.pockyl.hextale.block.HextechLamppostBlockEntity;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.chem.ShimmerVeins;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.BoilerRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.thevoid.Thinning;
import com.pockyl.hextale.thevoid.Wards;

import org.jspecify.annotations.Nullable;

/**
 * Stage 5, hextech against chemtech: the Chem Boiler cracks a mixture into all its essences at once and pours them into
 * stickered jars - quickly, but part of every batch goes up as fumes and part burns down to unknown essence that feeds
 * its burner; it leaves pure jars alone and lets its burner rest when there is nothing to crack.
 */
public final class DualityGameTests {
    private DualityGameTests() {
    }

    /**
     * Takes away everything a test of this class set up: jars, boilers and lampposts reach 5-16 blocks, and blocks outside
     * the 1x1x1 spot outlive a test - left there, they would draw from (or ward) the tests that come after.
     */
    private static void wipe(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(-1, 1, -1, 9, 4, 9)) {
            helper.setBlock(pos.immutable(), net.minecraft.world.level.block.Blocks.AIR);
        }
    }

    /** A boiler on a burner with one vial of fuel, at {@code at}; returns it. */
    private static ChemBoilerBlockEntity boiler(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at.below(), ModBlocks.CHEM_BURNER.get());
        helper.setBlock(at, ModBlocks.CHEM_BOILER.get().defaultBlockState().setValue(ChemBoilerBlock.FACING, Direction.NORTH));
        helper.getBlockEntity(at.below(), ChemBurnerBlockEntity.class).feed(ChemVialItem.of(Brew.FUEL, 1));
        return helper.getBlockEntity(at, ChemBoilerBlockEntity.class);
    }

    /** A jar at {@code at} with an optional sticker and contents. */
    private static EssenceTankBlockEntity jar(GameTestHelper helper, BlockPos at, @Nullable Essence sticker, EssenceMix contents) {
        helper.setBlock(at, ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity jar = helper.getBlockEntity(at, EssenceTankBlockEntity.class);
        jar.setSticker(sticker);
        jar.tank().set(contents);
        return jar;
    }

    @HexTest(maxTicks = 600, padding = 9)
    public static void aBoilerCracksAMixtureIntoStickeredJars(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 2, 2);
        ChemBoilerBlockEntity boiler = boiler(helper, at);
        EssenceMix mixture = EssenceMix.pure(Essence.FIRE, 48).plus(EssenceMix.pure(Essence.WATER, 48));
        EssenceTankBlockEntity source = jar(helper, new BlockPos(0, 2, 2), null, mixture);
        EssenceTankBlockEntity fire = jar(helper, new BlockPos(4, 2, 2), Essence.FIRE, EssenceMix.EMPTY);
        EssenceTankBlockEntity water = jar(helper, new BlockPos(2, 2, 4), Essence.WATER, EssenceMix.EMPTY);
        LevelChunk chunk = helper.getLevel().getChunkAt(helper.absolutePos(at));
        float before = chunk.getData(ModAttachments.THINNING_SOURCES).getOrDefault(Thinning.Source.FUMES.name(), 0F);
        ChemBurnerBlockEntity burner = helper.getBlockEntity(at.below(), ChemBurnerBlockEntity.class);
        boolean[] lit = {false};
        helper.succeedWhen(() -> {
            if (ChemBurnerBlock.isLit(helper.getBlockState(at.below()))) {
                lit[0] = true;
            }
            helper.assertTrue(source.tank().isEmpty(), "the mixture was drawn out of the plain jar");
            helper.assertTrue(boiler.mixture().isEmpty() && boiler.held().isEmpty(), "and all of it cracked and poured");
            int firePoured = fire.tank().contents().get(Essence.FIRE);
            int waterPoured = water.tank().contents().get(Essence.WATER);
            helper.assertTrue(firePoured > 20 && waterPoured > 20, "both essences reached their jars");
            helper.assertTrue(firePoured + waterPoured < mixture.total(), "some was lost on the way");
            helper.assertTrue(lit[0], "the burner below heated it");
            float fumes = chunk.getData(ModAttachments.THINNING_SOURCES).getOrDefault(Thinning.Source.FUMES.name(), 0F);
            helper.assertTrue(fumes > before, "the fumes wore the land");
            helper.assertTrue(burner.tank().unknown() > 0 || burner.fuelTicks() > 0, "the burnt part fed the flame");
            wipe(helper);
        });
    }

    @HexTest(maxTicks = 120, padding = 9)
    public static void aBoilerLeavesPureJarsAloneAndItsBurnerRests(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 2, 2);
        ChemBoilerBlockEntity boiler = boiler(helper, at);
        EssenceTankBlockEntity pure = jar(helper, new BlockPos(0, 2, 2), null, EssenceMix.pure(Essence.LIGHT, 64));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(pure.tank().contents().get(Essence.LIGHT) == 64, "a pure jar is not drawn from");
            helper.assertTrue(boiler.mixture().isEmpty(), "nothing went in");
            helper.assertFalse(boiler.wantsHeat(), "there is nothing to crack");
            helper.assertBlockProperty(at.below(), ChemBurnerBlock.LIT, false);
            wipe(helper);
            helper.succeed();
        });
    }

    @HexTest(maxTicks = 600, padding = 9)
    public static void boilerRunesMakeTheCrackingCleaner(GameTestHelper helper) {
        ChemBoilerBlockEntity plain = boiler(helper, new BlockPos(1, 2, 1));
        ChemBoilerBlockEntity runed = boiler(helper, new BlockPos(5, 2, 1));
        runed.setRunes(PickaxeRunes.EMPTY.with(BoilerRune.DAM.rune(), 3).with(BoilerRune.PURIFICATION.rune(), 3));
        helper.assertTrue(runed.fumesShare() < plain.fumesShare() && runed.unknownShare() < plain.unknownShare(),
                "Dam and Purification keep more of each batch");
        EssenceMix mixture = EssenceMix.pure(Essence.EARTH, 64).plus(EssenceMix.pure(Essence.METAL, 64));
        plain.pourIn(mixture);
        runed.pourIn(mixture);
        helper.succeedWhen(() -> {
            helper.assertTrue(plain.mixture().isEmpty() && runed.mixture().isEmpty(), "both cracked everything");
            // No stickered jar is near: the cracked essences wait in the boilers, the unknown went down into the burners.
            int crudeKept = plain.held().get(Essence.EARTH) + plain.held().get(Essence.METAL);
            int refinedKept = runed.held().get(Essence.EARTH) + runed.held().get(Essence.METAL);
            helper.assertTrue(refinedKept > crudeKept, "the runed boiler kept more (" + refinedKept + " vs " + crudeKept + ")");
            wipe(helper);
        });
    }

    /** A lamppost (both halves) at {@code at} with a jar of light essence beside it. */
    private static HextechLamppostBlockEntity lamppost(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at, ModBlocks.HEXTECH_LAMPPOST.get().defaultBlockState());
        helper.setBlock(at.above(), ModBlocks.HEXTECH_LAMPPOST.get().defaultBlockState().setValue(HextechLamppostBlock.HALF, DoubleBlockHalf.UPPER));
        jar(helper, at.east(), null, EssenceMix.pure(Essence.LIGHT, 256));
        return helper.getBlockEntity(at, HextechLamppostBlockEntity.class);
    }

    @HexTest(maxTicks = 300, padding = 9)
    public static void aLitLamppostBurnsOffTheThinningOfTheWorkAround(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 2, 2);
        HextechLamppostBlockEntity post = lamppost(helper, at);
        ServerLevel level = helper.getLevel();
        helper.succeedWhen(() -> {
            helper.assertTrue(post.lit(), "it draws light essence from the jar and lights up");
            helper.assertBlockProperty(at.above(), HextechLamppostBlock.LIT, true);
            BlockPos work = helper.absolutePos(at.west(2));
            LevelChunk chunk = level.getChunkAt(work);
            helper.assertTrue(Wards.lightAt(level, work) == post, "the spot lies in its light");
            float before = Thinning.get(chunk);
            float store = post.store();
            Thinning.add(level, work, 10, Thinning.Source.BOILED);
            float gained = Thinning.get(chunk) - before;
            float expected = 10 * (1 - post.share());
            helper.assertTrue(Math.abs(gained - expected) < 0.01F, "only " + expected + " reached the land, not " + gained);
            helper.assertTrue(post.store() < store, "the burn cost light");
            float beforeBreach = Thinning.get(chunk);
            Thinning.add(level, work, 1, Thinning.Source.BREACH);
            helper.assertTrue(Thinning.get(chunk) - beforeBreach > 0.99F, "the Void's own feeding is not burned off");
            helper.assertTrue(Wards.shields(level, chunk.getPos()), "and the creep stops at its chunk");
            Thinning.set(level, chunk, before);
            wipe(helper);
        });
    }

    @HexTest(maxTicks = 100, padding = 9)
    public static void aDarkLamppostWardsNothing(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, ModBlocks.HEXTECH_LAMPPOST.get().defaultBlockState());
        helper.setBlock(at.above(), ModBlocks.HEXTECH_LAMPPOST.get().defaultBlockState().setValue(HextechLamppostBlock.HALF, DoubleBlockHalf.UPPER));
        HextechLamppostBlockEntity post = helper.getBlockEntity(at, HextechLamppostBlockEntity.class);
        helper.runAfterDelay(50, () -> {
            helper.assertFalse(post.lit(), "no light essence nearby: it stays dark");
            helper.assertTrue(Wards.lightAt(helper.getLevel(), helper.absolutePos(at.west(2))) == null, "and wards nothing");
            wipe(helper);
            helper.succeed();
        });
    }

    @HexTest
    public static void shimmerVeinsGiveWhileFedAndOnlyWithholdAfter(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        double health = player.getAttributeValue(Attributes.MAX_HEALTH);
        double size = player.getAttributeValue(Attributes.SCALE);
        ChemVialItem.of(Brew.REFINED_SHIMMER).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(ShimmerVeins.of(player).count() == 1, "a vial leaves a vein");
        helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == health + ShimmerVeins.HEALTH, "the fed vein gives a heart");
        for (int i = 0; i < ShimmerVeins.MAX + 2; i++) {
            ChemVialItem.of(Brew.REFINED_SHIMMER).finishUsingItem(helper.getLevel(), player);
        }
        ShimmerVeins.Veins veins = ShimmerVeins.of(player);
        helper.assertTrue(veins.count() == ShimmerVeins.MAX, "no more than " + ShimmerVeins.MAX + " veins");
        var shown = player.getEffect(com.pockyl.hextale.registry.ModEffects.SHIMMER_VEINS);
        helper.assertTrue(shown != null && shown.getAmplifier() == ShimmerVeins.MAX - 1, "the veins show as an effect, their number as its level");
        helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == health + ShimmerVeins.MAX * ShimmerVeins.HEALTH, "five hearts more");
        helper.assertTrue(player.getAttributeValue(Attributes.SCALE) > size, "a bigger body");
        helper.assertTrue(player.hasEffect(MobEffects.HASTE), "and hands that do not tire");
        long now = helper.getLevel().getGameTime();
        helper.assertTrue(veins.fedUntil() - now <= ShimmerVeins.FED_TICKS - ShimmerVeins.FED_LESS * (ShimmerVeins.MAX - 1),
                "more veins want feeding sooner");
        player.setHealth(player.getMaxHealth());
        player.setData(com.pockyl.hextale.registry.ModAttachments.SHIMMER_VEINS, new ShimmerVeins.Veins(veins.count(), now - 1));
        ShimmerVeins.apply(player);
        helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == health, "unfed, the body is what it was");
        helper.assertTrue(player.getAttributeValue(Attributes.SCALE) == size, "and its size too");
        helper.assertTrue(player.getHealth() <= player.getMaxHealth() && player.getHealth() >= health - 0.01F, "nothing is taken below it");
        helper.assertTrue(player.getActiveEffects().stream().noneMatch(effect -> effect.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL), "and nothing bad");
        helper.assertTrue(ShimmerVeins.of(player).count() == ShimmerVeins.MAX, "the veins stay");
        helper.assertTrue(!player.hasEffect(com.pockyl.hextale.registry.ModEffects.SHIMMER_VEINS)
                && player.hasEffect(com.pockyl.hextale.registry.ModEffects.SHIMMER_CRAVING), "unfed veins show as fading");
        helper.succeed();
    }

    @HexTest
    public static void lightEssenceWashesAVeinOut(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ChemVialItem.of(Brew.REFINED_SHIMMER).finishUsingItem(helper.getLevel(), player);
        ChemVialItem.of(Brew.REFINED_SHIMMER).finishUsingItem(helper.getLevel(), player);
        EssenceFlaskItem.filled(EssenceMix.pure(Essence.FIRE, 16)).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(ShimmerVeins.of(player).count() == 2, "fire does not wash a vein");
        EssenceFlaskItem.filled(EssenceMix.pure(Essence.LIGHT, 16)).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(ShimmerVeins.of(player).count() == 1, "a bottle of light washes one out");
        EssenceFlaskItem.filled(EssenceMix.pure(Essence.LIGHT, 16)).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(ShimmerVeins.of(player).count() == 0, "and the last one");
        helper.assertTrue(player.getAttributeValue(Attributes.MAX_HEALTH) == 20, "no gifts without veins");
        helper.assertTrue(!player.hasEffect(com.pockyl.hextale.registry.ModEffects.SHIMMER_VEINS)
                && !player.hasEffect(com.pockyl.hextale.registry.ModEffects.SHIMMER_CRAVING), "and no icon");
        helper.succeed();
    }

    @HexTest(padding = 9)
    public static void aCrystalGrownInShimmerPowersThePurifierAndCrumbles(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.HEXTECH_PURIFIER.get());
        HextechPurifierBlockEntity purifier = helper.getBlockEntity(at, HextechPurifierBlockEntity.class);
        jar(helper, new BlockPos(3, 2, 1), null, EssenceMix.pure(Essence.LIGHT, 200));
        LevelChunk chunk = level.getChunkAt(helper.absolutePos(at));
        Thinning.setAround(level, helper.absolutePos(at), 1, 0);
        Thinning.set(level, chunk, 1800);
        helper.assertTrue(purifier.insert(new ItemStack(ModItems.SYNTHETIC_CRYSTAL.get())), "a crystal grown in shimmer goes in");
        for (int i = 0; i < 1000 && !purifier.crystal().isEmpty(); i++) {
            purifier.work(level);
        }
        helper.assertTrue(purifier.crystal().isEmpty(), "spent, it crumbles instead of clouding");
        float drawn = 1800 - Thinning.get(chunk);
        float holds = HextechPurifierBlockEntity.HOLDS * DeviceCrystals.SYNTHETIC_SHARE;
        helper.assertTrue(Math.abs(drawn - holds) < 1, "it holds less than a cut crystal: " + drawn + " of " + holds);
        AABB around = new AABB(helper.absolutePos(at)).inflate(2);
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, around).stream().anyMatch(item -> item.getItem().is(ModItems.HEXTECH_DUST.get())),
                "it leaves a pinch of dust");
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, around).stream().noneMatch(item -> item.getItem().is(ModItems.CLOUDED_CRYSTAL.get())),
                "never a clouded crystal");
        Thinning.setAround(level, helper.absolutePos(at), 1, 0);
        level.getEntitiesOfClass(ItemEntity.class, around).forEach(ItemEntity::discard);
        wipe(helper);
        helper.succeed();
    }

    @HexTest(padding = 9)
    public static void aVoidAnchorHoldsWithACrystalGrownInShimmer(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.VOID_ANCHOR.get());
        VoidAnchorBlockEntity anchor = helper.getBlockEntity(at, VoidAnchorBlockEntity.class);
        helper.assertTrue(anchor.insert(new ItemStack(ModItems.SYNTHETIC_CRYSTAL.get())) && anchor.holds(), "it holds with it");
        helper.assertTrue(anchor.capacity() < VoidAnchorBlockEntity.HOLDS, "but not as long");
        anchor.strain(helper.getLevel(), anchor.capacity() + 1);
        helper.assertTrue(anchor.crystal().isEmpty(), "worn through, it crumbles instead of clouding");
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(at)).inflate(2)).forEach(ItemEntity::discard);
        wipe(helper);
        helper.succeed();
    }
}
