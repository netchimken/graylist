package dev.chimken.graylist;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.chimken.graylist.managers.ServiceManager;
import dev.chimken.graylist.services.Elyby;
import dev.chimken.graylist.services.Floodgate;
import dev.chimken.graylist.services.Mojang;
import dev.chimken.graylist.services.Service;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Graylist extends JavaPlugin {
    private final FileConfiguration config = getConfig();
    private final Logger logger = getLogger();
    public final ServiceManager serviceManager = new ServiceManager(config, logger);

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            serviceManager.registerService(new Mojang());
            serviceManager.registerService(new Floodgate());
            serviceManager.registerService(new Elyby());
        } catch (ServiceManager.ServiceAlreadyExists e) {
            throw new RuntimeException(e);
        }

        this.getLifecycleManager().registerEventHandler(
                LifecycleEvents.COMMANDS,
                commands ->
                        commands.registrar().register(new MonoCommand("graylist").build())
        );
    }

    @Override
    public void onDisable() {
        serviceManager.clear();
    }

    private class MonoCommand extends LiteralArgumentBuilder<CommandSourceStack> {

        private RequiredArgumentBuilder<CommandSourceStack, String> whitelistCommand (boolean value) {
            return Commands.argument("user", StringArgumentType.string())
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
                    .executes(ctx -> whitelistExecutor(ctx, value))
                    .then(Commands.argument("service", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                serviceManager.getServices().forEach(
                                        (s, service) -> builder.suggest(s)
                                );

                                return builder.buildFuture();
                            })
                            .executes(ctx -> whitelistExecutor(ctx, value))
                    );
        }

        public MonoCommand (String label) {
            super(label);

            this.requires(ctx -> ctx.getSender().hasPermission("graylist"));

            this.then(Commands.literal("reload")
                    .requires(ctx -> ctx.getSender().hasPermission("graylist.command.reload"))
                    .executes(ctx -> {
                        Bukkit.getServer().reloadWhitelist();
                        ctx.getSource().getSender().sendMessage("Reloaded the whitelist");

                        return Command.SINGLE_SUCCESS;
                    })
            );

            this.then(Commands.literal("status")
                    .requires(ctx -> ctx.getSender().hasPermission("graylist.command.status"))
                    .executes(ctx -> {
                        ctx.getSource().getSender().sendMessage(
                                "Whitelist is currently " + (Bukkit.hasWhitelist() ? "on" : "off")
                        );

                        return Command.SINGLE_SUCCESS;
                    })
            );

            this.then(Commands.literal("add")
                    .requires(ctx -> ctx.getSender().hasPermission("graylist.command.add"))
                    .then(whitelistCommand(true))
            );

            this.then(Commands.literal("remove")
                    .requires(ctx -> ctx.getSender().hasPermission("graylist.command.remove"))
                    .then(whitelistCommand(false))
            );

            this.then(Commands.literal("list")
                    .requires(ctx -> ctx.getSender().hasPermission("graylist.command.list"))
                    .executes(ctx -> {
                        Set<OfflinePlayer> players = Bukkit.getWhitelistedPlayers();
                        String flatList = String.join(
                                ", ",
                                players.stream().map(OfflinePlayer::getName).toList()
                        );

                        ctx.getSource().getSender().sendRichMessage("There are <count> whitelisted player(s): <list>",
                                Placeholder.component("count", Component.text(players.toArray().length)),
                                Placeholder.component("list", Component.text(flatList))
                        );

                        return Command.SINGLE_SUCCESS;
                    })
            );

            this.then(Commands.literal("services")
                    .requires(ctx -> ctx.getSender().hasPermission("graylist.command.services"))
                    .executes(ctx -> {
                        String flatList = String.join(
                                ", ",
                                serviceManager.getServices().values().stream().map(Service::getID).toList()
                        );

                        ctx.getSource().getSender().sendRichMessage("There are <count> service(s) available: <list>",
                                Placeholder.component("count", Component.text(serviceManager.getServices().keySet().toArray().length)),
                                Placeholder.component("list", Component.text(flatList))
                        );

                        return Command.SINGLE_SUCCESS;
                    })
            );

            this.then(Commands.literal("on")
                    .requires(ctx -> ctx.getSender().hasPermission("graylist.command.on"))
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();

                        if (Bukkit.getServer().hasWhitelist())
                            sender.sendMessage("Whitelist is already turned on");
                        else {
                            Bukkit.getServer().setWhitelist(true);
                            sender.sendMessage("Whitelist is now turned on");
                        }

                        return Command.SINGLE_SUCCESS;
                    })
            );

            this.then(Commands.literal("off")
                    .requires(ctx -> ctx.getSender().hasPermission("graylist.command.off"))
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();

                        if (!Bukkit.getServer().hasWhitelist())
                            sender.sendMessage("Whitelist is already turned off");
                        else {
                            Bukkit.getServer().setWhitelist(false);
                            sender.sendMessage("Whitelist is now turned off");
                        }

                        return Command.SINGLE_SUCCESS;
                    })
            );
        }

        private String getServiceID (CommandContext<CommandSourceStack> ctx) {
            String service_id;

            try {
                service_id = ctx.getArgument("service", String.class).toLowerCase(Locale.ROOT);
            } catch (IllegalArgumentException e) {
                service_id = null;
            }

            return service_id;
        }

        private Component setWhitelisted (Service service, UUID uuid, String name, boolean value) {
            String prefix = getConfig().getString(MessageFormat.format(
                    "services.configs.{0}.{1}.prefix",
                    service.getNamespace(),
                    service.getID()
            ));

            return service.setWhitelisted(
                    uuid,
                    prefix == null || name.startsWith(prefix)
                        ? name
                        : prefix + name,
                    value
            );
        }

        private Component setWhitelisted (Service service, UUID uuid, boolean value) {
            return setWhitelisted(service, uuid, "<unknown>", value);
        }

        private Component setWhitelisted (Service service, String name, boolean value) throws Service.UserNotFound {
            String prefix = getConfig().getString(MessageFormat.format(
                    "services.configs.{0}.{1}.prefix",
                    service.getNamespace(),
                    service.getID()
            ));

            String unlabeledName = prefix == null
                    ? name
                    : StringUtils.replaceOnce(name, prefix, "");

            // Remove label for service
            final UUID uuid = service.findUUIDByName(unlabeledName);

            // Keep label for whitelist
            return setWhitelisted(service, uuid, name, value);
        }

        private int whitelistExecutor (CommandContext<CommandSourceStack> ctx, boolean value) {
            final String ref = ctx.getArgument("user", String.class);

            try {
                final UUID uuid = UUID.fromString(ref);

                final String service_id = getServiceID(ctx);
                Service service = service_id != null
                        ? serviceManager.getServices().get(service_id)
                        : serviceManager.getDefaultService();

                final CommandSender sender = ctx.getSource().getSender();

                sender.sendMessage(setWhitelisted(service, uuid, value));
            } catch (IllegalArgumentException __) {
                final String name = ref;

                String service_id = getServiceID(ctx);

                // If no service specified, fallback to prefix
                if (service_id == null) {
                    Map.Entry<String, String> prefix = serviceManager.getPrefixes().entrySet().stream()
                            .filter(entry -> name.startsWith(entry.getValue()))
                            .findFirst()
                            .orElse(null);

                    if (prefix != null) service_id = prefix.getKey();
                }

                // If no service or prefix specified, fallback to default
                Service service = service_id != null
                        ? serviceManager.getServices().get(service_id)
                        : serviceManager.getDefaultService();

                final CommandSender sender = ctx.getSource().getSender();

                sender.sendRichMessage("Finding <name> via <service>...",
                        Placeholder.component("name", Component.text(name)),
                        Placeholder.component("service", Component.text(service.getID()))
                );

                try {
                    sender.sendMessage(setWhitelisted(service, name, value));
                } catch (Service.UserNotFound e) {
                    sender.sendRichMessage("Couldn't find <name>",
                            Placeholder.component("name", Component.text(name))
                    );
                }
            }

            return Command.SINGLE_SUCCESS;
        }
    }
}
