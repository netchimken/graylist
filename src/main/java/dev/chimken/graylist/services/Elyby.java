package dev.chimken.graylist.services;

import dev.chimken.graylist.Util;

import java.util.UUID;

import static dev.chimken.graylist.Util.fetchJSON;

public class Elyby extends Service {
    public Elyby() {
        super("elyby", "ely.by", false);
    }

    @Override
    public UUID findUUIDByName(String name) throws UserNotFound {
        UUID knownUUID = findKnownUUIDByName(name);
        if (knownUUID != null) return knownUUID;

        try {
            final String id = fetchJSON("http://skinsystem.ely.by/profile/" + name)
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
        return "<unknown>";
    }
}
