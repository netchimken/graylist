package dev.chimken.graylist;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Util {
    public static final File WHITELIST_FILE = new File(Bukkit.getServer().getWorldContainer(), "whitelist.json");

    public static String expandUUIDString(String uuid) {
        StringBuilder builder = new StringBuilder(uuid);
        builder.insert(8, "-");
        builder.insert(13, "-");
        builder.insert(18, "-");
        builder.insert(23, "-");
        return builder.toString();
    }

    public static JsonObject fetchJSON(String url) throws RuntimeException {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            return JsonParser.parseString(res.body()).getAsJsonObject();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static String removePrefix(String text, String prefix) {
        return StringUtils.replaceOnce(text, prefix, "");
    }

    public static UUID findKnownUUIDByName(String name) {
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

    public static String findKnownNameByUUID(UUID uuid) {
        Set<OfflinePlayer> playerlist = new HashSet<>(Bukkit.getOnlinePlayers());
        OfflinePlayer onlinePlayer = playerlist.stream()
                .filter(p -> Objects.equals(p.getUniqueId(), uuid))
                .findFirst()
                .orElse(null);

        if (onlinePlayer != null) return onlinePlayer.getName();

        Set<OfflinePlayer> whitelist = Bukkit.getWhitelistedPlayers();
        OfflinePlayer trustedPlayer = whitelist.stream()
                .filter(p -> Objects.equals(p.getUniqueId(), uuid))
                .findFirst()
                .orElse(null);

        if (trustedPlayer != null) return trustedPlayer.getName();

        return null;
    }
}
