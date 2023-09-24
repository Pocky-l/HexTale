package com.pocky.hextale.common.events.points;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.register.ModItems;
import com.pocky.hextale.utils.ModColors;
import com.pocky.hextale.utils.points.PointPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = HexTaleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PointProgressEvent {

    public static PointPlayerData pointPlayerData = new PointPlayerData();

    public HashMap<Item, Integer> hexPointPrice = new HashMap<>();

    @SubscribeEvent
    public void onPlayerItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Item item = event.getCrafting().getItem();

        if (hexPointPrice.containsKey(item)) {
            UUID uuid = event.getEntity().getUUID();
            PointPlayerData.Player player = PointPlayerData.PLAYERS.get(uuid);

            int points = player.getHexPoints() + hexPointPrice.get(item);

            event.getEntity().displayClientMessage(Component
                    .translatable("message.hextale.give_hex_points")
                    .append(Component.literal(String.valueOf(hexPointPrice.get(item))))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(ModColors.HEXTECH))), true);

            PointPlayerData.PLAYERS.get(uuid).setHexPoints(points);
        }
    }

    @SubscribeEvent
    public void onPlayerSaveToFile(PlayerEvent.SaveToFile event) {
        String path = event.getPlayerDirectory().getAbsolutePath() + "\\" + HexTaleMod.MODID;
        UUID uuid = UUID.fromString(event.getPlayerUUID());
        pointPlayerData.save(uuid, path);
    }

    @SubscribeEvent
    public void onPlayerLoadFromFile(PlayerEvent.LoadFromFile event) {
        String path = event.getPlayerDirectory().getAbsolutePath() + "\\" + HexTaleMod.MODID;
        pointPlayerData.load(UUID.fromString(event.getPlayerUUID()), path);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        hexPointPrice.put(ModItems.HEXTECH_PICKAXE.get(), 5);
        hexPointPrice.put(ModItems.HEXTECH_AXE.get(), 5);
        hexPointPrice.put(ModItems.HEXTECH_HELMET.get(), 8);
        hexPointPrice.put(ModItems.HEXTECH_CHESTPLATE.get(), 12);
        hexPointPrice.put(ModItems.HEXTECH_LEGGINGS.get(), 10);
        hexPointPrice.put(ModItems.HEXTECH_BOOTS.get(), 6);

        /*
        hexPointPrice.put(ModItems.CUBE_HEXTECH_CRYSTAL.get(), 2);
        hexPointPrice.put(ModItems.RECTANGLE_HEXTECH_CRYSTAL.get(), 2);
         */
    }
}
