package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;

import com.pockyl.hextale.block.AxolotlTankBlockEntity;
import com.pockyl.hextale.block.ChemBurnerBlock;
import com.pockyl.hextale.block.ChemBurnerBlockEntity;
import com.pockyl.hextale.block.ChemStillBlockEntity;
import com.pockyl.hextale.block.CrucibleBlockEntity;
import com.pockyl.hextale.block.PinkBrazierBlockEntity;
import com.pockyl.hextale.block.ShimmerbloomBlock;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.TankRune;
import com.pockyl.hextale.world.BloomRegions;

/**
 * Chemtech's shimmer: bloom caves cluster (some regions thick with blooms, most of the world without), an axolotl in a
 * tank sheds slime, slime turns dream and light into raw shimmer, the still refines it over a burner, the refined vial is
 * drunk for buffs only, the burner burns shimmer pink and the brazier makes crops grow.
 */
public final class ShimmerGameTests {
    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private ShimmerGameTests() {
    }

    @HexTest
    public static void bloomCavesClusterAndMostOfTheWorldHasNone(GameTestHelper helper) {
        for (long seed : new long[] {0L, 42L, -7_000_000_123L, 987_654_321L}) {
            int chunks = 0;
            int bloom = 0;
            int sameAsNeighbour = 0;
            for (int cx = -150; cx < 150; cx++) {
                for (int cz = -150; cz < 150; cz++) {
                    boolean here = BloomRegions.strength(seed, cx * 16 + 8, cz * 16 + 8) > 0;
                    boolean east = BloomRegions.strength(seed, cx * 16 + 24, cz * 16 + 8) > 0;
                    chunks++;
                    if (here) {
                        bloom++;
                    }
                    if (here == east) {
                        sameAsNeighbour++;
                    }
                }
            }
            float share = bloom / (float) chunks;
            helper.assertTrue(share > 0.1F && share < 0.35F, "seed " + seed + ": bloom regions cover " + share + " of the chunks");
            helper.assertTrue(sameAsNeighbour / (float) chunks > 0.9F, "seed " + seed + ": regions come in patches, not a sprinkle");
        }
        helper.succeed();
    }

    @HexTest
    public static void bonemealPlantsANewBloomNearby(GameTestHelper helper) {
        for (int x = 0; x < 7; x++) {
            for (int z = 0; z < 7; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        BlockPos bloom = new BlockPos(3, 2, 3);
        helper.setBlock(bloom, ModBlocks.SHIMMERBLOOM.get());
        BlockPos grown = ShimmerbloomBlock.spread(helper.getLevel(), helper.getLevel().getRandom(), helper.absolutePos(bloom));
        helper.assertTrue(grown != null, "a new bloom took root");
        helper.assertTrue(helper.getLevel().getBlockState(grown).is(ModBlocks.SHIMMERBLOOM.get()), "and it is a shimmerbloom");
        helper.succeed();
    }

    @HexTest(maxTicks = 800)
    public static void aFedAxolotlShedsSlime(GameTestHelper helper) {
        helper.setBlock(AT, ModBlocks.AXOLOTL_TANK.get());
        AxolotlTankBlockEntity tank = helper.getBlockEntity(AT, AxolotlTankBlockEntity.class);
        helper.assertTrue(tank.letIn(new ItemStack(Items.AXOLOTL_BUCKET)), "the axolotl goes in");
        helper.assertFalse(tank.letIn(new ItemStack(Items.AXOLOTL_BUCKET)), "only one per tank");
        tank.setRunes(PickaxeRunes.EMPTY.with(TankRune.HASTE.rune(), 3).with(TankRune.LENS.rune(), 1));
        helper.assertTrue(tank.feed(new ItemStack(Items.COD, 2)) == 2, "it takes fish");
        helper.succeedWhen(() -> {
            helper.assertTrue(tank.output().is(ModItems.REGENERATIVE_SLIME.get()), "it shed regenerative slime");
            ItemStack out = tank.takeOut();
            helper.assertTrue(out.is(Items.AXOLOTL_BUCKET), "the axolotl comes back out in its bucket");
        });
    }

    @HexTest(maxTicks = 900)
    public static void slimeTurnsTheWaterIntoRawShimmerAndTheStillRefinesIt(GameTestHelper helper) {
        helper.setBlock(AT.below(), ModBlocks.CHEM_BURNER.get());
        helper.setBlock(AT, ModBlocks.CRUCIBLE.get());
        ChemBurnerBlockEntity burner = helper.getBlockEntity(AT.below(), ChemBurnerBlockEntity.class);
        burner.feed(ChemVialItem.of(Brew.FUEL, 4));
        CrucibleBlockEntity crucible = helper.getBlockEntity(AT, CrucibleBlockEntity.class);
        crucible.fillWater(CrucibleBlockEntity.BUCKET, false);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Research.research(player, ResearchTopics.RAW_SHIMMER);

        BlockPos still = new BlockPos(5, 2, 5);
        helper.setBlock(still.below(), ModBlocks.CHEM_BURNER.get());
        helper.setBlock(still, ModBlocks.CHEM_STILL.get());
        ChemBurnerBlockEntity stillBurner = helper.getBlockEntity(still.below(), ChemBurnerBlockEntity.class);
        ChemStillBlockEntity distiller = helper.getBlockEntity(still, ChemStillBlockEntity.class);
        boolean[] brewed = {false};
        boolean[] sawPink = {false};
        helper.succeedWhen(() -> {
            if (!brewed[0]) {
                helper.assertTrue(crucible.boiling(), "the water boils");
                crucible.pour(EssenceMix.pure(Essence.RIFT, 6).plus(EssenceMix.pure(Essence.LIGHT, 4)));
                helper.assertTrue(crucible.take(new ItemStack(ModItems.REGENERATIVE_SLIME.get()), 1, true, player) == 1, "the slime goes in");
                helper.assertTrue(crucible.brew() == Brew.RAW_SHIMMER && crucible.doses() == 3, "the water is three doses of raw shimmer");
                ItemStack first = crucible.bottle();
                ItemStack second = crucible.bottle();
                helper.assertTrue(ChemVialItem.brew(first) == Brew.RAW_SHIMMER, "a bottle takes raw shimmer");
                helper.assertTrue(distiller.feed(first) == 1 && distiller.feed(second) == 1, "the still takes raw vials");
                // The still's burner burns a raw vial too: shimmer is fuel, and its flame is pink.
                helper.assertTrue(stillBurner.feed(ChemVialItem.of(Brew.RAW_SHIMMER)) == 1, "a burner takes raw shimmer");
                brewed[0] = true;
            }
            if (ChemBurnerBlock.isLit(helper.getBlockState(still.below())) && stillBurner.pink()) {
                sawPink[0] = true;
            }
            helper.assertTrue(ChemVialItem.brew(distiller.output()) == Brew.REFINED_SHIMMER, "two raw vials became refined shimmer");
            helper.assertTrue(distiller.input().isEmpty(), "both raw vials were used");
            helper.assertTrue(sawPink[0], "the still's burner burned shimmer's pink flame");
        });
    }

    @HexTest
    public static void refinedShimmerIsDrunkForBuffsOnly(GameTestHelper helper) {
        ItemStack vial = ChemVialItem.of(Brew.REFINED_SHIMMER);
        helper.assertTrue(vial.has(DataComponents.CONSUMABLE) && vial.has(DataComponents.USE_COOLDOWN), "the vial is drunk and rests after");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        vial.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_BOOST) && player.hasEffect(MobEffects.REGENERATION)
                && player.hasEffect(MobEffects.MOVEMENT_SPEED) && player.hasEffect(MobEffects.DAMAGE_RESISTANCE), "it gives its buffs");
        helper.assertTrue(player.getActiveEffects().stream().allMatch(effect -> effect.getEffect().value().isBeneficial()), "and nothing bad");
        helper.assertTrue(vial.has(DataComponents.USE_REMAINDER), "the glass bottle is left");
        helper.assertFalse(ChemVialItem.of(Brew.RAW_SHIMMER).has(DataComponents.CONSUMABLE), "raw shimmer is not drunk");
        helper.succeed();
    }

    @HexTest(maxTicks = 1200)
    public static void thePinkBrazierMakesCropsGrow(GameTestHelper helper) {
        BlockPos brazier = new BlockPos(4, 2, 4);
        int start = 0;
        for (int x = 1; x < 8; x++) {
            for (int z = 1; z < 8; z++) {
                BlockPos crop = new BlockPos(x, 2, z);
                if (crop.equals(brazier)) {
                    continue;
                }
                helper.setBlock(crop.below(), Blocks.FARMLAND);
                helper.setBlock(crop, Blocks.WHEAT);
            }
        }
        helper.setBlock(brazier.below(), Blocks.STONE);
        helper.setBlock(brazier, ModBlocks.PINK_BRAZIER.get());
        PinkBrazierBlockEntity fire = helper.getBlockEntity(brazier, PinkBrazierBlockEntity.class);
        helper.assertTrue(fire.feed(ChemVialItem.of(Brew.REFINED_SHIMMER)) == 1, "it takes refined shimmer");
        helper.assertTrue(fire.feed(ChemVialItem.of(Brew.FUEL)) == 0, "but not combustible mix");
        helper.succeedWhen(() -> {
            int ages = 0;
            for (int x = 1; x < 8; x++) {
                for (int z = 1; z < 8; z++) {
                    var state = helper.getBlockState(new BlockPos(x, 2, z));
                    if (state.is(Blocks.WHEAT)) {
                        ages += state.getValue(CropBlock.AGE);
                    }
                }
            }
            helper.assertTrue(fire.burn() > 0, "it burns");
            helper.assertTrue(ages > start + 3, "the wheat in its light grew (" + ages + ")");
        });
    }
}
