package com.pocky.hextale.common.events;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.blocks.SuspiciousRemainsGravelEntity;
import com.pocky.hextale.common.register.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Random;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = HexTaleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RightClickBlockEvent {

    private final Set<BlockPos> cacheBlockPos = new HashSet<>();


    /**
     * Костыль, в будущем нужно убрать.
     * Выдаёт лут-таблицу блоку SUSPICIOUS_REMAINS_GRAVEL, при нажатии пкм
     * @param event
     */
    @SubscribeEvent
    public void onPlayerInteractRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;

        BlockState blockState = event.getLevel().getBlockState(event.getPos());

        if (event.getItemStack().is(Items.BRUSH) && blockState.is(ModBlocks.SUSPICIOUS_REMAINS_GRAVEL.get())) {
            if (!cacheBlockPos.contains(event.getPos())) {
                cacheBlockPos.add(event.getPos());
                SuspiciousRemainsGravelEntity brushableBlockEntity = (SuspiciousRemainsGravelEntity) event.getLevel().getBlockEntity(event.getPos());
                brushableBlockEntity.setLootTable(new ResourceLocation(HexTaleMod.MODID, "blocks/suspicious_remains_gravel"), Random.newSeed());
            }
        }
    }

}
