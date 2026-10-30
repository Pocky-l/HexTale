package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.infusion.Ritual;
import com.pockyl.hextale.registry.ModBlockEntities;

import java.util.List;

/**
 * The Rune Circle's centre: the altar's ritual, limited to runes - burning the steles' runes into a rune holder
 * ({@link Ritual.Kind#ENGRAVE}) and copying one onto a blank scroll ({@link Ritual.Kind#WRITE}). Pedestals take no part.
 */
public class RuneCircleBlockEntity extends HextechAltarBlockEntity {
    /** Height of the item over the circle's floor (blocks): it hovers low over the worn stone in the middle. */
    public static final double ITEM_Y = 0.9;

    public RuneCircleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUNE_CIRCLE.get(), pos, state);
    }

    @Override
    public boolean performs(Ritual.Kind kind) {
        return kind == Ritual.Kind.ENGRAVE || kind == Ritual.Kind.WRITE;
    }

    /** Just over the carved disc, which is 1 px thick round the middle stone. */
    @Override
    public float sigilY() {
        return 0.075F;
    }

    @Override
    protected Ritual.Circle scan(Level level) {
        Ritual.Circle circle = Ritual.Circle.scan(level, getBlockPos());
        return new Ritual.Circle(List.of(), circle.steles(), circle.tanks(), false);
    }

    @Override
    public String messageKey(String what) {
        return "block.hextale.rune_circle." + what;
    }

    @Override
    public double itemY() {
        return ITEM_Y;
    }
}
