package com.pockyl.hextale.garden;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

/**
 * Pouring doses by hand: a vial of growth tonic or raw shimmer gives {@value Dose#PER_VIAL} doses, a bottle of essence
 * one dose of sap for every {@value Dose#SAP_MB} mB of sap in it (the rest of the bottle stays). Onto farmland (it turns
 * into Steeped Soil), steeped soil, a Chem Planter or an Essence Sprinkler.
 */
public final class GardenPouring {
    private GardenPouring() {
    }

    /** The doses this item pours and how many, or null when it pours none. */
    public record Pour(Dose dose, int count) {
    }

    public static Pour of(ItemStack stack) {
        Dose vial = Dose.ofVial(stack);
        if (vial != null) {
            return new Pour(vial, Dose.PER_VIAL);
        }
        if (stack.is(ModItems.ESSENCE_FLASK.get())) {
            int sap = EssenceFlaskItem.contents(stack).get(Essence.SAP);
            if (sap >= Dose.SAP_MB) {
                return new Pour(Dose.SAP, sap / Dose.SAP_MB);
            }
        }
        return null;
    }

    /** Pours the held item into a tank of doses (planter, sprinkler); passes when the item pours nothing. */
    public static InteractionResult pourInto(DoseBed bed, ItemStack stack, Level level, Player player, InteractionHand hand) {
        Pour pour = of(stack);
        if (pour == null) {
            return InteractionResult.PASS;
        }
        if (bed.room(pour.dose()) <= 0) {
            if (!level.isClientSide() && bed.charges() > 0 && bed.dose() != pour.dose()) {
                player.displayClientMessage(Component.translatable("block.hextale.garden.other_dose",
                        Component.translatable(bed.dose().translationKey()).withColor(bed.dose().color())), true);
            }
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            int taken = bed.pour(pour.dose(), pour.count());
            spend(stack, pour, taken, player, hand);
            if (bed instanceof ChemPlanterBlockEntity planter) {
                planter.poured(server);
            } else if (bed instanceof EssenceSprinklerBlockEntity sprinkler) {
                sprinkler.poured(server);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Pours the held item on farmland or steeped soil: it soaks the 3x3 round the block. A vial gives every bed there
     * {@value Dose#PER_VIAL} doses; a bottle of sap shares its doses out a dose at a time. False when it pours nothing.
     */
    public static boolean pourOnSoil(ServerLevel level, BlockPos pos, ItemStack stack, Player player, InteractionHand hand) {
        Pour pour = of(stack);
        if (pour == null || !SteepedSoilBlock.takesDose(level.getBlockState(pos))) {
            return false;
        }
        java.util.List<BlockPos> beds = new java.util.ArrayList<>();
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-Dose.POUR_RADIUS, 0, -Dose.POUR_RADIUS), pos.offset(Dose.POUR_RADIUS, 0, Dose.POUR_RADIUS))) {
            if (SteepedSoilBlock.room(level.getBlockState(at), pour.dose()) > 0) {
                beds.add(at.immutable());
            }
        }
        int used = 0;
        boolean vial = !stack.is(ModItems.ESSENCE_FLASK.get());
        if (vial) {
            for (BlockPos bed : beds) {
                int room = SteepedSoilBlock.room(level.getBlockState(bed), pour.dose());
                if (SteepedSoilBlock.steep(level, bed, pour.dose(), Math.min(Dose.PER_VIAL, room))) {
                    used++;
                }
            }
        } else {
            int[] give = new int[beds.size()];
            boolean placed = true;
            while (used < pour.count() && placed) {
                placed = false;
                for (int i = 0; i < beds.size() && used < pour.count(); i++) {
                    if (give[i] < SteepedSoilBlock.room(level.getBlockState(beds.get(i)), pour.dose())) {
                        give[i]++;
                        used++;
                        placed = true;
                    }
                }
            }
            for (int i = 0; i < beds.size(); i++) {
                if (give[i] > 0) {
                    SteepedSoilBlock.steep(level, beds.get(i), pour.dose(), give[i]);
                }
            }
        }
        if (used <= 0) {
            return true;
        }
        spend(stack, pour, used, player, hand);
        if (player instanceof ServerPlayer server) {
            Quests.fire(server, Quests.STEEPED_SOIL);
        }
        return true;
    }

    /** Takes what was poured out of the held item: the vial for a glass bottle, the sap out of a bottle of essence. */
    private static void spend(ItemStack stack, Pour pour, int taken, Player player, InteractionHand hand) {
        if (player.getAbilities().instabuild || taken <= 0) {
            return;
        }
        ItemStack back;
        if (stack.is(ModItems.ESSENCE_FLASK.get())) {
            EssenceMix contents = EssenceFlaskItem.contents(stack);
            back = EssenceFlaskItem.filled(contents.minus(EssenceMix.pure(Essence.SAP, taken * Dose.SAP_MB)));
        } else {
            back = new ItemStack(Items.GLASS_BOTTLE);
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            player.setItemInHand(hand, back);
        } else if (!player.getInventory().add(back)) {
            player.drop(back, false);
        }
    }
}
