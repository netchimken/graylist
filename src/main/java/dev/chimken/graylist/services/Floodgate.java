package dev.chimken.graylist.services;

import dev.chimken.graylist.abstracts.GraylistService;

import java.util.UUID;

import static dev.chimken.graylist.Util.fetchJSON;
import static dev.chimken.graylist.managers.ServiceManager.INTERNAL_NAMESPACE;

// TODO: Don't rely on mcprofile.io; implement Xbox API instead

public class Floodgate extends GraylistService {
    public Floodgate() {
        super("floodgate", INTERNAL_NAMESPACE, false);
    }

    @Override
    public UUID findUUIDByName(String name) throws UserNotFound {
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
