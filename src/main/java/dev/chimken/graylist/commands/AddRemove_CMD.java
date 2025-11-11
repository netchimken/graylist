package dev.chimken.graylist.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.chimken.graylist.TextStyles;
import dev.chimken.graylist.Util;
import dev.chimken.graylist.abstracts.GraylistCommandBundle;
import dev.chimken.graylist.abstracts.GraylistService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

import static io.papermc.paper.command.brigadier.Commands.*;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;

public class AddRemove_CMD extends GraylistCommandBundle<CommandSourceStack> {
    public AddRemove_CMD() {
        register(literal("add")
                .requires(ctx -> ctx.getSender().hasPermission("graylist.command.add"))
                .then(build(true))
        );

        register(literal("remove")
                .requires(ctx -> ctx.getSender().hasPermission("graylist.command.remove"))
                .then(build(false))
        );
    }

    public ArgumentBuilder<CommandSourceStack, ?> build(boolean value) {
        return argument("user", StringArgumentType.string())
                .suggests((ctx, builder) -> {
                    final String operation = ctx.getInput();

                    if (operation.startsWith("/graylist add")) {
                        Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .forEach(builder::suggest);
                    } else {
                        Bukkit.getWhitelistedPlayers().stream()
                                .map(player -> {
                                    final String name = player.getName();

                                    if (Objects.equals(name, "<unknown>")) {
                                        return player.getUniqueId().toString();
                                    } else {
                                        return name;
                                    }
                                })
                                .forEach(builder::suggest);
                    }

                    return builder.buildFuture();
                })
                .executes(ctx -> executor(ctx, value))
                .then(argument("service", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            serviceManager.getServices().forEach(
                                    (s, service) -> builder.suggest(s)
                            );

                            return builder.buildFuture();
                        })
                        .executes(ctx -> executor(ctx, value))
                );
    }

    public int executor(CommandContext<CommandSourceStack> ctx, boolean value) {
        final String ref = ctx.getArgument("user", String.class);
        final CommandSender sender = ctx.getSource().getSender();

        GraylistService service = serviceManager.getServices().get(
                getServiceID(ctx)
        );

        UUID uuid = null;
        String name = null;
        String unprefixed_name = null;

        // Determine whether UUID or name was provided
        try {
            uuid = UUID.fromString(ref);
        } catch (IllegalArgumentException __) {
            name = ref;

            // reassigned to keep Java happy for use inside lambda
            String checkName = name;

            String service_id = serviceManager.getPrefixes().entrySet().stream()
                    .filter(entry -> checkName.startsWith(entry.getValue()))
                    .findFirst()
                    .map(Map.Entry::getKey)
                    .orElse(null);

            if (service_id != null)
                service = serviceManager.getServices().get(service_id);
        }

        // If service undetermined
        if (service == null) {
            sender.sendRichMessage("Couldn't determine service for <user>",
                    component("user", text(ref))
            );

            return Command.SINGLE_SUCCESS;
        }

        // Search for prefix if name was provided
        if (name != null) {
            String service_prefix = config.getString(service.getConfigPath() + ".prefix");

            // If prefix exists but name doesn't start with it, add it
            if (service_prefix != null) {
                if (!name.startsWith(service_prefix)) {
                    name = service_prefix + name;
                    unprefixed_name = name;
                } else {
                    unprefixed_name = Util.removePrefix(name, service_prefix);
                }
            }
        }

        // Isolate logging
        {
            String user = name != null ? name : uuid.toString();

            // Verbose (console)
            logger.log(Level.INFO, "Searching " + service.getID() + " for " + user);

            // Simple (in-game)
            sender.sendRichMessage("Finding <user>...",
                    component("user", text(user))
            );
        }

        // Depending on input ID, find opposite
        if (uuid != null) {
            try {
                name = Util.findKnownNameByUUID(uuid);

                // If unknown name, query service with UUID
                if (name == null) name = service.findNameByUUID(uuid);
            } catch (GraylistService.UserNotFound e) { /* ignore */ }
        } else {
            try {
                uuid = Util.findKnownUUIDByName(name);

                // If unknown UUID, query service with name (minus prefix)
                if (uuid == null) uuid = service.findUUIDByName(unprefixed_name);
            } catch (GraylistService.UserNotFound e) {
                sender.sendRichMessage("Couldn't find <name>",
                        component("name", text(name))
                );

                return Command.SINGLE_SUCCESS;
            }
        }

        if (name == null) name = "<unknown>";

        GraylistService.PlayerWhitelistStatus status = setWhitelisted(uuid, name, value);

        switch (status) {
            case ABSENT -> sender.sendRichMessage("<name> is not whitelisted",
                    component("name",
                            text(name).style(TextStyles.buildUsernameStyle(uuid))
                    )
            );

            case PRESENT -> sender.sendRichMessage("<name> is already whitelisted",
                    component("name",
                            text(name).style(TextStyles.buildUsernameStyle(uuid))
                    )
            );

            case ADDED -> sender.sendRichMessage("Added <name> to the whitelist",
                    component("name",
                            text(name).style(TextStyles.buildUsernameStyle(uuid))
                    )
            );

            case REMOVED -> sender.sendRichMessage("Removed <name> from the whitelist",
                    component("name",
                            text(name).style(TextStyles.buildUsernameStyle(uuid))
                    )
            );

        }

        return Command.SINGLE_SUCCESS;
    }

    public String getServiceID (CommandContext<CommandSourceStack> ctx) {
        String service_id;

        try {
            service_id = ctx.getArgument("service", String.class).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            service_id = serviceManager.getDefaultServiceID();
        }

        return service_id;
    }

    public GraylistService.PlayerWhitelistStatus setWhitelisted (UUID uuid, String name, boolean value) {
        return whitelistManager.setWhitelisted(
                uuid,
                name,
                value
        );
    }
}
