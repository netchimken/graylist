package dev.chimken.graylist.abstracts;

import com.mojang.brigadier.builder.ArgumentBuilder;
import dev.chimken.graylist.Graylist;
import dev.chimken.graylist.managers.ServiceManager;
import dev.chimken.graylist.managers.WhitelistManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class GraylistCommandBundle<S> {
    private static final Graylist graylist = Graylist.getPlugin(Graylist.class);

    protected final FileConfiguration config = graylist.getConfig();
    protected final Logger logger = graylist.getLogger();
    protected final ServiceManager serviceManager = graylist.serviceManager;
    protected final WhitelistManager whitelistManager = graylist.whitelistManager;

    private final List<ArgumentBuilder<S, ?>> commands = new ArrayList<>();

    public final List<ArgumentBuilder<S, ?>> getCommands() {
        return commands;
    }

    public final void register(ArgumentBuilder<S, ?> command) {
        commands.add(command);
    }
}
