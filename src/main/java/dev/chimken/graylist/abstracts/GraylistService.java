package dev.chimken.graylist.abstracts;

import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.UUID;

public abstract class GraylistService {
    private final String SERVICE_ID;
    private final String SERVICE_NAMESPACE;
    private final boolean DEFAULT_STATUS;

    public final String getID() {
        return SERVICE_ID;
    }
    public final @Nullable String getNamespace() {
        return SERVICE_NAMESPACE;
    }
    public final String getConfigPath() {
        return "services.configs." + (
                SERVICE_NAMESPACE != null
                        ? SERVICE_NAMESPACE + "." + SERVICE_ID
                        : SERVICE_ID
        );
    }
    public final boolean getDefaultStatus() {
        return DEFAULT_STATUS;
    }

    private ConfigurationSection config;

    public final void setConfig(ConfigurationSection config) {
        this.config = config;
    }

    public GraylistService(String id, boolean enabled) {
        this(id, null, enabled);
    }

    public GraylistService(String id, @Nullable String namespace, boolean enabled) {
        SERVICE_ID = id;
        SERVICE_NAMESPACE = namespace;
        DEFAULT_STATUS = enabled;
    }

    public abstract UUID findUUIDByName(String name) throws UserNotFound;

    public abstract String findNameByUUID(UUID uuid) throws UserNotFound;

    public class UserNotFound extends Exception {
        public UserNotFound (String search) {
            super(MessageFormat.format("Couldn't find '{0}' on service '{1}'", search, SERVICE_ID));
        }
    }

    public enum PlayerWhitelistStatus {
        ABSENT,
        PRESENT,
        ADDED,
        REMOVED,
    }
}
