package dev.chimken.graylist.services;

import dev.chimken.graylist.Util;
import java.util.*;

import static dev.chimken.graylist.Util.fetchJSON;

public class Mojang extends Service {
    public Mojang () {
        super("mojang", "Mojang", true);
    }

    @Override
    public UUID findUUIDByName(String name) throws UserNotFound {
        UUID knownUUID = findKnownUUIDByName(name);
        if (knownUUID != null) return knownUUID;

        final String id = fetchJSON("https://api.mojang.com/users/profiles/minecraft/" + name)
                .get("id")
                .getAsString();

        // Use expandUUIDString because UUID is collapsed initially
        return UUID.fromString(Util.expandUUIDString(id));
    }

    @Override
    public String findNameByUUID(UUID uuid) throws UserNotFound {
        return fetchJSON("https://api.minecraftservices.com/minecraft/profile/lookup/" + uuid.toString())
                .get("name")
                .getAsString();
    }
}
