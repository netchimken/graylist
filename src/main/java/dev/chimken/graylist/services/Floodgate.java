package dev.chimken.graylist.services;

import dev.chimken.graylist.Util;

import java.util.UUID;

import static dev.chimken.graylist.Util.fetchJSON;

// TODO: Don't rely on mcprofile.io; implement Xbox API instead

public class Floodgate extends Service {
    public Floodgate() {
        super("floodgate", "Floodgate", false);
    }

    @Override
    public UUID findUUIDByName(String name) throws UserNotFound {
        UUID knownUUID = findKnownUUIDByName(name);
        if (knownUUID != null) return knownUUID;

        try {
            final String id = fetchJSON("https://mcprofile.io/api/v1/bedrock/gamertag/" + name)
                    .get("floodgateuid")
                    .getAsString();

            return UUID.fromString(id);
        } catch (RuntimeException e) {
            throw new UserNotFound(name);
        }
    }

    @Override
    public String findNameByUUID(UUID uuid) throws UserNotFound {
        try {
            return fetchJSON("https://mcprofile.io/api/v1/bedrock/fuid/" + uuid.toString())
                    .get("gamertag")
                    .getAsString();
        } catch (RuntimeException e) {
            throw new UserNotFound(uuid.toString());
        }
    }
}
