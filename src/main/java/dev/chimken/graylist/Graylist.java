package dev.chimken.graylist;

import dev.chimken.graylist.commands.*;
import dev.chimken.graylist.managers.CommandManager;
import dev.chimken.graylist.managers.ServiceManager;
import dev.chimken.graylist.managers.WhitelistManager;
import dev.chimken.graylist.services.Elyby;
import dev.chimken.graylist.services.Floodgate;
import dev.chimken.graylist.services.Mojang;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class Graylist extends JavaPlugin {
    private final FileConfiguration config = getConfig();
    private final Logger logger = getLogger();

    public final ServiceManager serviceManager = new ServiceManager(config, logger);
    public final WhitelistManager whitelistManager = new WhitelistManager(logger);
    private final CommandManager commandManager = new CommandManager("graylist");

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            serviceManager.register(new Mojang());
            serviceManager.register(new Floodgate());
            serviceManager.register(new Elyby());
        } catch (ServiceManager.ServiceAlreadyExists e) {
            throw new RuntimeException(e);
        }

        // TODO: Add modify command for editing whitelist entries directly
        // commandManager.register(new Modify());

        commandManager.register(new AddRemove_CMD());
        commandManager.register(new List_CMD());
        commandManager.register(new Off_CMD());
        commandManager.register(new On_CMD());
        commandManager.register(new Reload_CMD());
        commandManager.register(new Services_CMD());
        commandManager.register(new Status_CMD());

        this.getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS,
                commands ->
                        commands.registrar().register(commandManager.build())
        );
    }

    @Override
    public void onDisable() {
        serviceManager.clear();
    }
}
