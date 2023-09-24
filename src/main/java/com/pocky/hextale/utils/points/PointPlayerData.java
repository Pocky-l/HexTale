package com.pocky.hextale.utils.points;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.*;

public class PointPlayerData {

    public static Map<UUID, Player> PLAYERS = new HashMap<>();

    public PointPlayerData() {
    }

    public void save(UUID uuid, String path) {
        Player player = PLAYERS.get(uuid);
        if (player == null) {
            player = new Player(uuid, 0);
            PLAYERS.put(uuid, player);
        } else {
            player.setHexPoints(player.getHexPoints());
        }

        File directory = new File(path);

        if (!directory.exists()) directory.mkdirs();

        File file = new File(directory, uuid.toString() + ".json");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (Writer writer = new FileWriter(file)) {
            gson.toJson(player, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Player load(UUID uuid, String path) {
        File file = new File(path + File.separator + uuid.toString() + ".json");

        if (!file.exists()) {
            save(uuid, path);
            return PLAYERS.get(uuid);
        }

        Gson gson = new Gson();

        if (!file.exists()) file.mkdirs();

        try (Reader reader = new FileReader(file)) {
            PLAYERS.put(uuid, gson.fromJson(reader, Player.class));
            return PLAYERS.get(uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static class Player implements Serializable {
        private final UUID uuid;
        private int hexPoints;

        public Player(UUID uuid, int hexPoints) {
            this.uuid = uuid;
            this.hexPoints = hexPoints;
        }

        public UUID getUuid() {
            return uuid;
        }

        public int getHexPoints() {
            return hexPoints;
        }

        public void setHexPoints(int hexPoints) {
            this.hexPoints = hexPoints;
        }
    }
}
