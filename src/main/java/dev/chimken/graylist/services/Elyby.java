package dev.chimken.graylist.services;

import dev.chimken.graylist.Util;
import dev.chimken.graylist.abstracts.GraylistService;

import java.util.UUID;

import static dev.chimken.graylist.Util.fetchJSON;
import static dev.chimken.graylist.managers.ServiceManager.INTERNAL_NAMESPACE;

public class Elyby extends GraylistService {
    public Elyby() {
        super("elyby", INTERNAL_NAMESPACE, false);
    }

    @Override
    public UUID findUUIDByName(String name) throws UserNotFound {
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
    public String findNameByUUID(UUID uuid) {
        return null;
    }
}
