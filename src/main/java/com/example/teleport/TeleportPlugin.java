package com.example.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TeleportPlugin extends JavaPlugin implements CommandExecutor {

    private File storageFile;
    private FileConfiguration storage;

    @Override
    public void onEnable() {
        // create data folder
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        storageFile = new File(getDataFolder(), "TeleportPlaces.yml");
        if (!storageFile.exists()) {
            try {
                storageFile.createNewFile();
                storage = YamlConfiguration.loadConfiguration(storageFile);
                storage.set("places", null);
                storage.save(storageFile);
            } catch (IOException e) {
                getLogger().severe("Could not create TeleportPlaces.yml");
                e.printStackTrace();
            }
        }

        storage = YamlConfiguration.loadConfiguration(storageFile);

        // register command
        getCommand("t").setExecutor(this);

        getLogger().info("TeleportPlugin enabled.");
    }

    @Override
    public void onDisable() {
        saveStorage();
        getLogger().info("TeleportPlugin disabled.");
    }

    private void saveStorage() {
        try {
            storage.save(storageFile);
        } catch (IOException e) {
            getLogger().severe("Failed to save TeleportPlaces.yml");
            e.printStackTrace();
        }
    }

    private Map<Integer, Map<String, Object>> loadAllPlaces() {
        Map<Integer, Map<String, Object>> result = new TreeMap<>();
        if (storage.isConfigurationSection("places")) {
            for (String key : storage.getConfigurationSection("places").getKeys(false)) {
                try {
                    int id = Integer.parseInt(key);
                    Map<String, Object> map = new HashMap<>();
                    String base = "places." + key + ".";
                    map.put("world", storage.getString(base + "world", "world"));
                    map.put("x", storage.getDouble(base + "x", 0));
                    map.put("y", storage.getDouble(base + "y", 64));
                    map.put("z", storage.getDouble(base + "z", 0));
                    map.put("yaw", (float) storage.getDouble(base + "yaw", 0));
                    map.put("pitch", (float) storage.getDouble(base + "pitch", 0));
                    map.put("name", storage.getString(base + "name", "place-" + key));
                    result.put(id, map);
                } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    private int nextAvailableId() {
        Map<Integer, Map<String, Object>> all = loadAllPlaces();
        int i = 1;
        while (all.containsKey(i)) i++;
        return i;
    }

    private void savePlace(int id, Location loc, String name) {
        String base = "places." + id + ".";
        storage.set(base + "world", loc.getWorld().getName());
        storage.set(base + "x", loc.getX());
        storage.set(base + "y", loc.getY());
        storage.set(base + "z", loc.getZ());
        storage.set(base + "yaw", loc.getYaw());
        storage.set(base + "pitch", loc.getPitch());
        storage.set(base + "name", name == null ? "place-" + id : name);
        saveStorage();
    }

    private void removePlace(int id) {
        storage.set("places." + id, null);
        saveStorage();
    }

    private Optional<Location> getLocationById(int id) {
        String base = "places." + id + ".";
        if (!storage.contains(base + "world")) return Optional.empty();
        String worldName = storage.getString(base + "world");
        World w = Bukkit.getWorld(worldName);
        if (w == null) return Optional.empty();
        double x = storage.getDouble(base + "x");
        double y = storage.getDouble(base + "y");
        double z = storage.getDouble(base + "z");
        float yaw = (float) storage.getDouble(base + "yaw", 0);
        float pitch = (float) storage.getDouble(base + "pitch", 0);
        return Optional.of(new Location(w, x, y, z, yaw, pitch));
    }

    private Optional<String> getNameById(int id) {
        String base = "places." + id + ".";
        if (!storage.contains(base + "name")) return Optional.empty();
        return Optional.ofNullable(storage.getString(base + "name"));
    }

    // Command handling
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only players can teleport / add their position
        if (args.length == 0) {
            sender.sendMessage("§e/t <番号> - テレポート  /t add /t set <番号> <名前> /t list /t remove <番号>");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("add")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
                return true;
            }
            Player p = (Player) sender;
            int id = nextAvailableId();
            String name = "place-" + id;
            savePlace(id, p.getLocation(), name);
            p.sendMessage("§a場所を追加しました: §f#" + id + " §a(" + name + ")");
            return true;
        } else if (sub.equals("list")) {
            Map<Integer, Map<String, Object>> all = loadAllPlaces();
            if (all.isEmpty()) {
                sender.sendMessage("§c保存されたテレポート先はありません。");
                return true;
            }
            sender.sendMessage("§e—— 保存済みテレポート一覧 ——");
            for (Map.Entry<Integer, Map<String, Object>> e : all.entrySet()) {
                int id = e.getKey();
                String name = (String) e.getValue().get("name");
                String world = (String) e.getValue().get("world");
                double x = (double) e.getValue().get("x");
                double y = (double) e.getValue().get("y");
                double z = (double) e.getValue().get("z");
                sender.sendMessage(" §a#" + id + " §f" + name + " §7(" + world + " " +
                        Math.round(x) + "," + Math.round(y) + "," + Math.round(z) + ")");
            }
            return true;
        } else if (sub.equals("set")) {
            if (args.length < 3) {
                sender.sendMessage("§c使い方: /t set <番号> <名前>");
                return true;
            }
            int id;
            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage("§c番号を指定してください。");
                return true;
            }
            String name = joinArgs(args, 2);
            if (!storage.contains("places." + id + ".world")) {
                sender.sendMessage("§cその番号は存在しません。");
                return true;
            }
            storage.set("places." + id + ".name", name);
            saveStorage();
            sender.sendMessage("§a#" + id + " の名前を " + name + " に変更しました。");
            return true;
        } else if (sub.equals("remove")) {
            if (args.length < 2) {
                sender.sendMessage("§c使い方: /t remove <番号>");
                return true;
            }
            int id;
            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                sender.sendMessage("§c番号を指定してください。");
                return true;
            }
            if (!storage.contains("places." + id + ".world")) {
                sender.sendMessage("§cその番号は存在しません。");
                return true;
            }
            removePlace(id);
            sender.sendMessage("§a#" + id + " を削除しました。");
            return true;
        } else {
            // assume teleport to number: /t <number>
            int id;
            try {
                id = Integer.parseInt(sub);
            } catch (NumberFormatException ex) {
                sender.sendMessage("§cコマンドが不正です。/t list で一覧を確認してください。");
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage("このコマンドはプレイヤーのみ使用できます。");
                return true;
            }
            Player p = (Player) sender;
            Optional<Location> locOpt = getLocationById(id);
            if (!locOpt.isPresent()) {
                p.sendMessage("§cその番号は存在しません。/t list で確認してください。");
                return true;
            }
            Location loc = locOpt.get();
            p.teleport(loc);
            Optional<String> nm = getNameById(id);
            p.sendMessage("§aテレポートしました: §f#" + id + " §a(" + nm.orElse("place-"+id) + ")");
            return true;
        }
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
