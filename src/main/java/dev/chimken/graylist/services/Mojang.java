package dev.chimken.graylist.services;

import dev.chimken.graylist.Util;
import dev.chimken.graylist.abstracts.GraylistService;

import java.util.*;

import static dev.chimken.graylist.Util.fetchJSON;
import static dev.chimken.graylist.managers.ServiceManager.INTERNAL_NAMESPACE;

public class Mojang extends GraylistService {
    public Mojang () {
        super("mojang", INTERNAL_NAMESPACE, true);
    }

    @Override
    public UUID findUUIDByName(String name) throws UserNotFound {
        try {
            final String id = fetchJSON("https://api.mojang.com/users/profiles/minecraft/" + name)
                    .get("id")
                    .getAsString();

            // Use expandUUIDString because UUID is collapsed initially
            return UUID.fromString(Util.expandUUIDString(id));
        } catch (RuntimeException e) {
            throw new UserNotFound(name);
        }
    }

    @Override
    public String findNameByUUID(UUID uuid) throws UserNotFound {
        try {
            return fetchJSON("https://api.minecraftservices.com/minecraft/profile/lookup/" + uuid.toString())
                    .get("name")
                    .getAsString();
        } catch (RuntimeException e) {
            throw new UserNotFound(uuid.toString());
        }
    }
}
