package dev.chimken.graylist.managers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.chimken.graylist.Util;
import dev.chimken.graylist.abstracts.GraylistService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

public class WhitelistManager {
    private final Logger logger;

    public WhitelistManager (Logger logger) {
        this.logger = logger;
    }

    public GraylistService.PlayerWhitelistStatus setWhitelisted(UUID uuid, String name, boolean value) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        try {
            JsonArray whitelist = JsonParser.parseReader(new FileReader(Util.WHITELIST_FILE)).getAsJsonArray();

            if (value) {
                if (player.isWhitelisted())
                    return GraylistService.PlayerWhitelistStatus.PRESENT;
                else {
                    JsonObject entry = new JsonObject();
                    entry.addProperty("uuid", uuid.toString());
                    entry.addProperty("name", name);

                    whitelist.add(entry);

                    try (FileWriter writer = new FileWriter(Util.WHITELIST_FILE)) {
                        new Gson().toJson(whitelist, writer);
                    }

                    Bukkit.reloadWhitelist();

                    return GraylistService.PlayerWhitelistStatus.ADDED;
                }
            } else {
                if (!player.isWhitelisted())
                    return GraylistService.PlayerWhitelistStatus.ABSENT;
                else {
                    player.setWhitelisted(false);
                    return GraylistService.PlayerWhitelistStatus.REMOVED;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
