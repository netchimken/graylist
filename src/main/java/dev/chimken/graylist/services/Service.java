package dev.chimken.graylist.services;

import com.google.gson.*;
import dev.chimken.graylist.Util;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.*;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public abstract class Service {
    private final String SERVICE_ID;
    private final String SERVICE_NAME;
    private final boolean DEFAULT_STATUS;

    public String getID() {
        return SERVICE_ID;
    }

    public String getName() {
        return SERVICE_NAME;
    }

    public boolean getDefaultStatus() {
        return DEFAULT_STATUS;
    }

    public Service(String id, boolean enabled) {
        this(id, id, enabled);
    }

    public Service(String id, String name, boolean enabled) {
        SERVICE_ID = id;
        SERVICE_NAME = name;
        DEFAULT_STATUS = enabled;
    }

    public UUID findKnownUUIDByName(String name) {
        Set<OfflinePlayer> playerlist = new HashSet<>(Bukkit.getOnlinePlayers());
        OfflinePlayer onlinePlayer = playerlist.stream()
                .filter(p -> Objects.equals(p.getName(), name))
                .findFirst()
                .orElse(null);

        if (onlinePlayer != null) return onlinePlayer.getUniqueId();

        Set<OfflinePlayer> whitelist = Bukkit.getWhitelistedPlayers();
        OfflinePlayer trustedPlayer = whitelist.stream()
                .filter(p -> Objects.equals(p.getName(), name))
                .findFirst()
                .orElse(null);

        if (trustedPlayer != null) return trustedPlayer.getUniqueId();

        return null;
    }

    public abstract UUID findUUIDByName(String name) throws UserNotFound;

    public abstract String findNameByUUID(UUID uuid) throws UserNotFound;

    public Component setWhitelisted(UUID uuid, String name, boolean value) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        try {
            JsonArray whitelist = JsonParser.parseReader(new FileReader(Util.WHITELIST_FILE)).getAsJsonArray();

            if (value) {
                if (player.isWhitelisted()) {
                    return Component.text(MessageFormat.format("{0} ({1}) is already whitelisted",
                            name,
                            uuid.toString()
                    ));
                } else {
                    JsonObject entry = new JsonObject();
                    entry.addProperty("uuid", uuid.toString());
                    entry.addProperty("name", name);

                    whitelist.add(entry);

                    try (FileWriter writer = new FileWriter(Util.WHITELIST_FILE)) {
                        new Gson().toJson(whitelist, writer);
                    }

                    Bukkit.reloadWhitelist();

                    return Component.text(MessageFormat.format("Added {0} ({1}) to the whitelist",
                            name,
                            uuid.toString()
                    ));
                }
            } else {
                player.setWhitelisted(false);

                return Component.text(MessageFormat.format("Removed {0} ({1}) from the whitelist",
                        name,
                        uuid.toString()
                ));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setWhitelisted(UUID uuid, boolean value) {
        setWhitelisted(uuid, uuid.toString(), value);
    }

    public class UserNotFound extends Exception {
        UserNotFound (String search) {
            super(MessageFormat.format("Couldn't find '{0}' on service '{1}'", search, SERVICE_ID));
        }
    }
}
