package com.ashkiano.reactiontest;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ReactionTest extends JavaPlugin implements Listener, TabExecutor {

    private final Map<UUID, Long> playerReactionTimes = new HashMap<>();
    private boolean isGreen = false;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("startReactionTest").setExecutor(this);
        Metrics metrics = new Metrics(this, 21946);
        this.getLogger().info("Thank you for using the ReactionTest plugin! If you enjoy using this plugin, please consider making a donation to support the development. You can donate at: https://donate.ashkiano.com");
        checkForUpdates();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            startReactionTest(player);
            return true;
        } else if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }
        return false;
    }

    private void startReactionTest(Player player) {
        Random random = new Random();
        int initialDelay = 60 + random.nextInt(100); // Random initial delay between 3 and 8 seconds
        int reactionDelay = 60 + random.nextInt(200); // Random delay between 3 and 10 seconds
        player.sendMessage(ChatColor.RED + "Get ready...");
        player.sendMessage(ChatColor.YELLOW + "When the text above the hotbar turns green, press SHIFT as quickly as possible!");

        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendTitle(ChatColor.RED + "Wait for it...", "", 10, 70, 20);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        isGreen = true;
                        playerReactionTimes.put(player.getUniqueId(), System.currentTimeMillis());
                        player.sendTitle(ChatColor.GREEN + "GO!", "", 10, 70, 20);
                    }
                }.runTaskLater(ReactionTest.this, reactionDelay);
            }
        }.runTaskLater(this, initialDelay); // Initial delay between 3 and 8 seconds
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (isGreen && playerReactionTimes.containsKey(playerId)) {
            long reactionTime = System.currentTimeMillis() - playerReactionTimes.get(playerId);
            playerReactionTimes.remove(playerId);
            player.sendMessage(ChatColor.GOLD + "Your reaction time: " + reactionTime + "ms");
            isGreen = false;
        }
    }

    private void checkForUpdates() {
        try {
            String pluginName = this.getDescription().getName();
            URL url = new URL("https://www.ashkiano.com/version_check.php?plugin=" + pluginName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String jsonResponse = response.toString();
                JSONObject jsonObject = new JSONObject(jsonResponse);
                if (jsonObject.has("error")) {
                    this.getLogger().warning("Error when checking for updates: " + jsonObject.getString("error"));
                } else {
                    String latestVersion = jsonObject.getString("latest_version");

                    String currentVersion = this.getDescription().getVersion();
                    if (currentVersion.equals(latestVersion)) {
                        this.getLogger().info("This plugin is up to date!");
                    } else {
                        this.getLogger().warning("There is a newer version (" + latestVersion + ") available! Please update!");
                    }
                }
            } else {
                this.getLogger().warning("Failed to check for updates. Response code: " + responseCode);
            }
        } catch (Exception e) {
            this.getLogger().warning("Failed to check for updates. Error: " + e.getMessage());
        }
    }
}