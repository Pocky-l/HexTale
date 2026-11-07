package com.pockyl.hextale.gametest;

import com.mojang.serialization.JsonOps;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.chem.ShimmerMutations;
import com.pockyl.hextale.chem.ShimmerMutations.Kind;
import com.pockyl.hextale.chem.ShimmerMutations.Mutation;
import com.pockyl.hextale.chem.ShimmerRide;
import com.pockyl.hextale.item.ShimmerInjectorItem;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.thevoid.Thinning;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Shimmer mutations: who can be dosed, what each kind gets, how it fades, and riding as far as a server sees it. */
public final class ShimmerMutationGameTests {
    private static final BlockPos AT = new BlockPos(2, 2, 2);

    private ShimmerMutationGameTests() {
    }

    private static ServerPlayer survivor(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static ItemStack injector(ServerPlayer player, int vials) {
        ItemStack injector = new ItemStack(ModItems.SHIMMER_INJECTOR.get());
        if (vials > 0) {
            ShimmerInjectorItem.fill(injector, ChemVialItem.of(Brew.REFINED_SHIMMER, vials), player);
        }
        return injector;
    }

    private static <T extends TamableAnimal> T pet(GameTestHelper helper, EntityType<T> type, ServerPlayer owner) {
        T animal = helper.spawn(type, AT);
        animal.tame(owner);
        return animal;
    }

    private static Horse horse(GameTestHelper helper, ServerPlayer owner) {
        Horse horse = helper.spawn(EntityType.HORSE, AT);
        horse.setTamed(true);
        horse.setOwner(owner);
        return horse;
    }

    /** The full change at once, as if the animal had been ticking since the shot. */
    private static void grown(GameTestHelper helper, Mob animal) {
        ServerLevel level = helper.getLevel();
        Mutation mutation = ShimmerMutations.of(animal);
        animal.setData(ModAttachments.SHIMMER_MUTATION, new Mutation(mutation.kind(), mutation.until() - ShimmerMutations.GROW_TICKS, mutation.stamina()));
        ShimmerMutations.tick(level, animal, ShimmerMutations.of(animal));
    }

    private static void shoot(ServerPlayer player, ItemStack injector, Mob animal) {
        ShimmerInjectorItem.shoot(injector, player, animal);
    }

    private static void expire(GameTestHelper helper, Mob animal) {
        Mutation mutation = ShimmerMutations.of(animal);
        ServerLevel level = helper.getLevel();
        animal.setData(ModAttachments.SHIMMER_MUTATION, new Mutation(mutation.kind(), level.getGameTime() - 1, mutation.stamina()));
    }

    private static boolean click(ServerPlayer player, Mob animal) {
        return NeoForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, animal)).isCanceled();
    }

    /** Only refined shimmer goes in, one vial a dose, up to the top; the rest stays in the hand. */
    @HexTest
    public static void theInjectorDrawsRefinedShimmerOnly(GameTestHelper helper) {
        ServerPlayer player = survivor(helper);
        ItemStack injector = new ItemStack(ModItems.SHIMMER_INJECTOR.get());
        ItemStack raw = ChemVialItem.of(Brew.RAW_SHIMMER, 2);
        helper.assertFalse(ShimmerInjectorItem.fill(injector, raw, player), "raw shimmer stays out");
        ItemStack refined = ChemVialItem.of(Brew.REFINED_SHIMMER, 6);
        helper.assertTrue(ShimmerInjectorItem.fill(injector, refined, player), "refined shimmer goes in");
        helper.assertTrue(ShimmerInjectorItem.doses(injector) == ShimmerInjectorItem.MAX_DOSES, "up to the top");
        helper.assertTrue(refined.getCount() == 6 - ShimmerInjectorItem.MAX_DOSES, "the rest stays, left " + refined.getCount());
        helper.assertFalse(ShimmerInjectorItem.fill(injector, refined, player), "a full barrel takes nothing");
        helper.succeed();
    }

    /** A shot needs the owner, an adult, a kind in the list and a dose; it costs one dose and a little Thinning. */
    @HexTest
    public static void aDoseNeedsTheOwnerAndCostsThinning(GameTestHelper helper) {
        ServerPlayer owner = survivor(helper);
        ServerPlayer stranger = survivor(helper);
        ItemStack injector = injector(owner, 2);
        Wolf wolf = pet(helper, EntityType.WOLF, owner);

        shoot(stranger, injector, wolf);
        helper.assertTrue(ShimmerMutations.of(wolf) == null && ShimmerInjectorItem.doses(injector) == 2, "a stranger's shot does nothing");
        Wolf pup = pet(helper, EntityType.WOLF, owner);
        pup.setBaby(true);
        shoot(owner, injector, pup);
        helper.assertTrue(ShimmerMutations.of(pup) == null && ShimmerInjectorItem.doses(injector) == 2, "a pup is refused");
        Mob cow = helper.spawn(EntityType.COW, AT);
        helper.assertTrue(ShimmerInjectorItem.shoot(injector, owner, cow) == net.minecraft.world.InteractionResult.PASS, "a cow is not a companion");
        shoot(owner, new ItemStack(ModItems.SHIMMER_INJECTOR.get()), wolf);
        helper.assertTrue(ShimmerMutations.of(wolf) == null, "an empty injector does nothing");

        float thinning = Thinning.get(helper.getLevel(), wolf.blockPosition());
        shoot(owner, injector, wolf);
        helper.assertTrue(ShimmerMutations.is(wolf, Kind.HOUND), "the owner's shot takes");
        helper.assertTrue(ShimmerInjectorItem.doses(injector) == 1, "one dose per shot");
        helper.assertTrue(Thinning.get(helper.getLevel(), wolf.blockPosition()) >= thinning + Thinning.DOSED - 0.01F, "a dose wears the place thin");
        helper.succeed();
    }

    /** The gifts come, the size comes over time, and it all goes after five days with nothing lost below the animal's own. */
    @HexTest
    public static void aHoundGrowsAndFades(GameTestHelper helper) {
        ServerPlayer owner = survivor(helper);
        Wolf wolf = pet(helper, EntityType.WOLF, owner);
        double health = wolf.getMaxHealth();
        double knockback = wolf.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        ShimmerMutations.dose(owner, wolf, Kind.HOUND);
        helper.assertTrue(Math.abs(wolf.getMaxHealth() - health * 3) < 0.01, "three times the health, got " + wolf.getMaxHealth());
        helper.assertTrue(wolf.getHealth() == wolf.getMaxHealth(), "healed by the shot");
        helper.assertTrue(wolf.getAttributeValue(Attributes.ATTACK_KNOCKBACK) > knockback, "the bite knocks back");

        ShimmerMutations.tick(helper.getLevel(), wolf, ShimmerMutations.of(wolf));
        helper.assertTrue(wolf.getScale() > 1 && wolf.getScale() < 1.1F, "it starts to grow, at " + wolf.getScale());
        grown(helper, wolf);
        helper.assertTrue(Math.abs(wolf.getScale() - 1.4F) < 0.01F, "and ends at 1.4, at " + wolf.getScale());

        ShimmerMutations.dose(owner, wolf, Kind.HOUND);
        ShimmerMutations.tick(helper.getLevel(), wolf, ShimmerMutations.of(wolf));
        helper.assertTrue(Math.abs(wolf.getScale() - 1.4F) < 0.01F, "a renewal does not shrink it");
        long until = ShimmerMutations.of(wolf).until();
        helper.assertTrue(until - helper.getLevel().getGameTime() > ShimmerMutations.LASTS - 5, "and it lasts five days from now");

        wolf.setHealth(10);
        wolf.tickCount = ShimmerMutations.REGEN_TICKS;
        ShimmerMutations.tick(helper.getLevel(), wolf, ShimmerMutations.of(wolf));
        helper.assertTrue(wolf.getHealth() == 10 + ShimmerMutations.REGEN, "a Hound regenerates");

        expire(helper, wolf);
        ShimmerMutations.tick(helper.getLevel(), wolf, ShimmerMutations.of(wolf));
        helper.assertTrue(ShimmerMutations.of(wolf) == null, "the mutation ends");
        helper.assertTrue(Math.abs(wolf.getMaxHealth() - health) < 0.01 && Math.abs(wolf.getScale() - 1) < 0.01F, "the wolf is itself again");
        helper.assertTrue(wolf.getAttributeValue(Attributes.ATTACK_KNOCKBACK) == knockback, "bite included");
        helper.succeed();
    }

    /** The state survives a save, and a Hound's pounce is thrown at its target from three to eight blocks. */
    @HexTest
    public static void theStateSavesAndAHoundPounces(GameTestHelper helper) {
        Mutation mutation = new Mutation(Kind.RAPTOR, 1234, 77);
        Mutation back = Mutation.CODEC.parse(JsonOps.INSTANCE, Mutation.CODEC.encodeStart(JsonOps.INSTANCE, mutation).getOrThrow()).getOrThrow();
        helper.assertTrue(mutation.equals(back), "the record round-trips, got " + back);

        ServerPlayer owner = survivor(helper);
        Wolf wolf = pet(helper, EntityType.WOLF, owner);
        Mob target = helper.spawn(EntityType.PIG, AT.east(1));
        helper.assertFalse(ShimmerMutations.canPounce(wolf, target), "a plain wolf does not pounce");
        ShimmerMutations.dose(owner, wolf, Kind.HOUND);
        wolf.setOnGround(true);
        target.setPos(wolf.position().add(5, 0, 0));
        helper.assertTrue(ShimmerMutations.canPounce(wolf, target), "five blocks is in reach");
        target.setPos(wolf.position().add(1.5, 0, 0));
        helper.assertFalse(ShimmerMutations.canPounce(wolf, target), "too close: it bites");
        target.setPos(wolf.position().add(10, 0, 0));
        helper.assertFalse(ShimmerMutations.canPounce(wolf, target), "too far");

        Vec3 leap = ShimmerMutations.leapSpeed(new Vec3(8, 0, 0));
        double travelled = 0;
        double speed = leap.x;
        double height = 0;
        double up = leap.y;
        double top = 0;
        for (int tick = 0; tick < 40 && (height >= 0 || tick == 0); tick++) {
            travelled += speed;
            height += up;
            top = Math.max(top, height);
            speed *= 0.91;
            up = (up - 0.08) * 0.98;
        }
        helper.assertTrue(travelled > 6.5 && travelled < 10, "the throw lands near the target, at " + travelled);
        helper.assertTrue(top > 0.5, "over the ground, top " + top);
        helper.succeed();
    }

    /** A Steed runs, jumps, does not feel a fall and keeps its rider under water. */
    @HexTest
    public static void aSteedKeepsItsRider(GameTestHelper helper) {
        ServerPlayer owner = survivor(helper);
        AbstractHorse horse = horse(helper, owner);
        AbstractHorse plain = horse(helper, owner);
        helper.assertTrue(ShimmerMutations.owns(owner, horse), "a tamed horse counts as the owner's");
        helper.assertFalse(ShimmerMutations.owns(survivor(helper), horse), "and only as the owner's");
        double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double jump = horse.getAttributeValue(Attributes.JUMP_STRENGTH);
        helper.assertTrue(plain.dismountsUnderwater(), "a plain horse throws its rider in deep water");

        ShimmerMutations.dose(owner, horse, Kind.STEED);
        helper.assertTrue(horse.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed * 1.5, "big speed");
        helper.assertTrue(horse.getAttributeValue(Attributes.JUMP_STRENGTH) > jump * 1.4, "big jump");
        helper.assertFalse(horse.dismountsUnderwater(), "a Steed swims with its rider");
        var fall = CommonHooks.onLivingFall(horse, 30, 1);
        helper.assertTrue(fall == null || fall.isCanceled(), "no fall damage");
        helper.assertFalse(CommonHooks.onLivingFall(plain, 30, 1) == null, "a plain horse falls as ever");

        helper.assertTrue(ShimmerMutations.kindFor(EntityType.DONKEY) == Kind.STEED && ShimmerMutations.kindFor(EntityType.MULE) == Kind.STEED
                && ShimmerMutations.kindFor(EntityType.CAMEL) == Kind.STEED, "donkeys, mules and camels too");
        helper.assertTrue(ShimmerMutations.kindFor(EntityType.LLAMA) == null && ShimmerMutations.kindFor(EntityType.SKELETON_HORSE) == null
                && ShimmerMutations.kindFor(EntityType.CAMEL_HUSK) == null, "no llamas and no undead");
        helper.succeed();
    }

    /** A Panther takes a saddle, then its owner, climbs what it runs into, and gives the saddle back when it fades. */
    @HexTest
    public static void aPantherCarriesAndClimbs(GameTestHelper helper) {
        ServerPlayer owner = survivor(helper);
        Cat cat = pet(helper, EntityType.CAT, owner);
        cat.horizontalCollision = true;
        helper.assertFalse(cat.onClimbable(), "a plain cat climbs nothing");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        helper.assertFalse(click(owner, cat), "and takes no saddle");

        ShimmerMutations.dose(owner, cat, Kind.PANTHER);
        grown(helper, cat);
        helper.assertTrue(Math.abs(cat.getScale() - Kind.PANTHER.scale()) < 0.01F, "a big cat, at " + cat.getScale());
        helper.assertTrue(cat.onClimbable(), "a Panther goes up a wall it runs into");
        cat.horizontalCollision = false;
        helper.assertFalse(cat.onClimbable(), "and is on no ladder in the open");

        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertFalse(click(owner, cat), "no saddle, no seat");
        helper.assertFalse(owner.isPassenger(), "still on foot");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        helper.assertTrue(click(owner, cat) && ShimmerRide.saddled(cat), "the saddle goes on");
        helper.assertTrue(owner.getMainHandItem().isEmpty(), "out of the hand");
        cat.setOrderedToSit(true);
        helper.assertTrue(click(owner, cat), "an empty hand gets on");
        helper.assertTrue(owner.getVehicle() == cat && cat.getControllingPassenger() == owner, "the owner steers");
        helper.assertFalse(cat.isOrderedToSit(), "it stood up for it");

        expire(helper, cat);
        ShimmerMutations.tick(helper.getLevel(), cat, ShimmerMutations.of(cat));
        helper.assertTrue(ShimmerMutations.is(cat, Kind.PANTHER), "it never fades under a rider");
        owner.stopRiding();
        ShimmerMutations.tick(helper.getLevel(), cat, ShimmerMutations.of(cat));
        helper.assertTrue(ShimmerMutations.of(cat) == null && !ShimmerRide.saddled(cat), "it fades once the rider is off");
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, cat.getBoundingBox().inflate(4), item -> item.getItem().is(Items.SADDLE)).isEmpty(),
                "and the saddle is not lost");
        helper.succeed();
    }

    /** Where a rider ends up on this mount, turned to face +Z, in blocks from the animal's feet. */
    private static Vec3 seatOf(ServerPlayer rider, Mob mount) {
        mount.setYRot(90);
        mount.positionRider(rider);
        return rider.position().add(rider.getVehicleAttachmentPoint(mount)).subtract(mount.position()).yRot(90 * Mth.DEG_TO_RAD);
    }

    /** A rider sits on the back at the middle of the body, never over the head, and a lean takes the seat along with the body. */
    @HexTest
    public static void aRiderSitsOnTheMiddleOfTheBack(GameTestHelper helper) {
        ServerPlayer owner = survivor(helper);
        Cat cat = pet(helper, EntityType.CAT, owner);
        ShimmerMutations.dose(owner, cat, Kind.PANTHER);
        grown(helper, cat);
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SADDLE));
        click(owner, cat);
        click(owner, cat);
        helper.assertTrue(owner.getVehicle() == cat, "on the Panther");
        Vec3 seat = seatOf(owner, cat);
        helper.assertTrue(Math.abs(seat.y - 1.25) < 0.02 && Math.abs(seat.z + 0.12) < 0.02, "on its back, a little behind the centre: " + seat);
        Vec3 wall = ShimmerRide.seat(Kind.PANTHER, cat.getDimensions(cat.getPose()).height(), cat.getScale(), ShimmerRide.WALL_PITCH);
        helper.assertTrue(wall.z < -0.4, "on a wall the rider hangs behind the body, clear of the head: " + wall);
        owner.stopRiding();

        ServerPlayer other = survivor(helper);
        Parrot parrot = pet(helper, EntityType.PARROT, other);
        ShimmerMutations.dose(other, parrot, Kind.RAPTOR);
        grown(helper, parrot);
        other.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        click(other, parrot);
        helper.assertTrue(other.getVehicle() == parrot, "on the Raptor");
        seat = seatOf(other, parrot);
        helper.assertTrue(Math.abs(seat.y - 0.91) < 0.02 && Math.abs(seat.z) < 0.06, "on the rear of its upright body: " + seat);
        Vec3 glide = ShimmerRide.seat(Kind.RAPTOR, parrot.getDimensions(parrot.getPose()).height(), parrot.getScale(), ShimmerRide.GLIDE_PITCH);
        helper.assertTrue(glide.z < -0.15 && glide.y < 1.1, "flat in a glide the back lies behind the centre, and low: " + glide);
        helper.succeed();
    }

    /** A Raptor carries its owner, pays for flaps out of a wind that only the ground gives back, and glides without any. */
    @HexTest
    public static void aRaptorFlapsOnItsWind(GameTestHelper helper) {
        ServerPlayer owner = survivor(helper);
        Parrot parrot = pet(helper, EntityType.PARROT, owner);
        parrot.setOnGround(true);
        owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertFalse(click(owner, parrot), "a plain parrot carries nobody");

        ShimmerMutations.dose(owner, parrot, Kind.RAPTOR);
        helper.assertFalse(parrot.canSitOnShoulder(), "too big for a shoulder");
        helper.assertTrue(ShimmerMutations.of(parrot).stamina() == ShimmerRide.STAMINA, "rested at first");
        helper.assertTrue(click(owner, parrot), "an empty hand gets on");
        helper.assertTrue(parrot.getControllingPassenger() == owner, "the owner steers");

        parrot.setOnGround(false);
        helper.assertTrue(ShimmerRide.flap(parrot, owner, ShimmerMutations.of(parrot)), "a flap is paid");
        helper.assertTrue(ShimmerMutations.of(parrot).stamina() == ShimmerRide.STAMINA - 1, "out of the wind");
        parrot.tickCount = ShimmerRide.REST_TICKS;
        ShimmerRide.rest(parrot, ShimmerMutations.of(parrot));
        helper.assertTrue(ShimmerMutations.of(parrot).stamina() == ShimmerRide.STAMINA - 1, "no rest in the air");

        parrot.setData(ModAttachments.SHIMMER_MUTATION, ShimmerMutations.of(parrot).withStamina(0));
        helper.assertFalse(ShimmerRide.flap(parrot, owner, ShimmerMutations.of(parrot)), "no wind, no flap");
        parrot.setOnGround(true);
        ShimmerRide.rest(parrot, ShimmerMutations.of(parrot));
        helper.assertTrue(ShimmerMutations.of(parrot).stamina() == ShimmerRide.REST, "the ground gives it back");

        Vec3 up = ShimmerRide.motion(Vec3.ZERO, true, false);
        helper.assertTrue(up.y == ShimmerRide.LIFT, "a flap lifts");
        helper.assertTrue(ShimmerRide.motion(new Vec3(0, 0.44, 0), true, false).y == ShimmerRide.CLIMB, "up to the top speed");
        helper.assertTrue(ShimmerRide.motion(new Vec3(0, -1, 0), false, false).y == -ShimmerRide.GLIDE, "without flaps it glides");
        helper.assertTrue(ShimmerRide.GLIDE > 0.04, "fast enough down for the floating check to leave it alone");
        owner.stopRiding();
        helper.assertTrue(parrot.getItemBySlot(EquipmentSlot.SADDLE).isEmpty(), "a Raptor needs no saddle");
        helper.succeed();
    }
}
