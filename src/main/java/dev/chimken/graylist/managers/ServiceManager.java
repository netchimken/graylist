package dev.chimken.graylist.managers;

import dev.chimken.graylist.abstracts.GraylistService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceManager {
    private final FileConfiguration config;
    private final Logger logger;

    private final HashMap<String, GraylistService> services = new HashMap<>();
    private final HashMap<String, String> prefixes = new HashMap<>();
    private String default_service_id;

    public static final String INTERNAL_NAMESPACE = "internal";

    public String getDefaultServiceID() { return default_service_id; }
    public GraylistService getDefaultService() {
        return services.get(default_service_id);
    }

    public HashMap<String, GraylistService> getServices() {
        return services;
    }

    public HashMap<String, String> getPrefixes() {
        return prefixes;
    }

    public void clear() {
        services.clear();
        prefixes.clear();
        default_service_id = null;
    }

    public ServiceManager (FileConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;

        default_service_id = config.getString("services.default");
    }

    public void register(GraylistService service) throws ServiceAlreadyExists {
        final String id = service.getID();

        if (services.containsKey(id))
            throw new ServiceAlreadyExists(id);

        final String namespace = service.getNamespace();
        final String service_config_path = service.getConfigPath();

        ConfigurationSection service_config = config.getConfigurationSection(service_config_path);
        if (service_config == null) {
            service_config = config.createSection(service_config_path);
            service_config.set("enabled", true);
        }

        final int isEnabled = service_config.isBoolean("enabled")
                ? service_config.getBoolean("enabled")
                    ? 1 // Is enabled
                    : 0 // Isn't enabled
                : 2; // Not set (use default)

        // If disabled (in config or by default)
        if (isEnabled == 0 || (isEnabled == 2 && !service.getDefaultStatus())) {
            if (!Objects.equals(namespace, INTERNAL_NAMESPACE)) logger.log(
                    Level.INFO,
                    MessageFormat.format(
                            "Disabled service: {0} (external)",
                            service.getID()
                    )
            );

            return;
        }

        service.setConfig(service_config);
        services.put(id, service);

        String prefix = service_config.getString("prefix");
        if (prefix != null) prefixes.put(id, prefix);

        logger.log(
                Level.INFO,
                MessageFormat.format(
                        "Registered service: {0} ({1})",
                        service.getID(),
                        Objects.equals(namespace, INTERNAL_NAMESPACE) ? "internal" : "external"
                )
        );
    }

    public static class ServiceAlreadyExists extends Exception {
        ServiceAlreadyExists (String name) {
            super("Failed to register '" + name + "' because it already exists");
        }
    }
}
