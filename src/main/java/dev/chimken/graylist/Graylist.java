package dev.chimken.graylist;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.chimken.graylist.services.Elyby;
import dev.chimken.graylist.services.Floodgate;
import dev.chimken.graylist.services.Mojang;
import dev.chimken.graylist.services.Service;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
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

import java.util.*;
import java.util.logging.Level;

public final class Graylist extends JavaPlugin {
    private final HashMap<String, Service> services = new HashMap<>();
    private final HashMap<String, String> prefixes = new HashMap<>();
    private String default_service_id;

    public void registerService(Service service) throws ServiceAlreadyExists {
        final String id = service.SERVICE_ID;

        if (services.containsKey(id))
            throw new ServiceAlreadyExists(id);

        FileConfiguration config = getConfig();
        final String enabled_path = "services.configs." + id + ".enabled";

        final int isEnabled = config.isBoolean(enabled_path)
                ? config.getBoolean(enabled_path)
                    ? 1 // Is enabled
                    : 0 // Isn't enabled
                : 2; // Not set (use default)

        if (isEnabled == 0) {
            getLogger().log(Level.INFO, id + " tried to register itself but was disabled in the config.");
            return;
        }

        if (isEnabled == 2 && !service.DEFAULT_STATUS) {
            getLogger().log(Level.INFO, "Disabled service: " + id);
            return;
        }

        services.put(id, service);

        String prefix = config.getString("services.configs." + id + ".prefix");
        if (prefix != null) prefixes.put(id, prefix);

        getLogger().log(Level.INFO, "Registered service: " + id);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        default_service_id = getConfig().getString("services.default");

        try {
            registerService(new Mojang());
            registerService(new Floodgate());
            registerService(new Elyby());
        } catch (ServiceAlreadyExists e) {
            throw new RuntimeException(e);
        }

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(new MonoCommand("graylist").build());
        });
    }

    @Override
    public void onDisable() {
        services.clear();
        prefixes.clear();

        default_service_id = null;
    }

    public static class ServiceAlreadyExists extends Exception {
        ServiceAlreadyExists (String name) {
            super("Failed to register '" + name + "' because it already exists");
        }
    }

    private class MonoCommand extends LiteralArgumentBuilder<CommandSourceStack> {

        private LiteralArgumentBuilder<CommandSourceStack> nameCommand (boolean value) {
            return Commands.literal("name")
                    .then(Commands.argument("name", StringArgumentType.string())
                            .suggests((ctx, builder) -> {
                                Bukkit.getOnlinePlayers().stream()
                                        .map(Player::getName)
                                        .forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> setNameWhitelisted(ctx, value))
                            .then(Commands.argument("service", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        services.forEach(
                                                (s, service) -> builder.suggest(s)
                                        );

                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> setNameWhitelisted(ctx, value))
                            )
                    );
        }

        private LiteralArgumentBuilder<CommandSourceStack> uuidCommand (boolean value) {
            return Commands.literal("uuid")
                    .then(Commands.argument("uuid", ArgumentTypes.uuid())
                            .suggests((ctx, builder) -> {
                                Bukkit.getOnlinePlayers().stream()
                                        .map(Player::getUniqueId)
                                        .map(UUID::toString)
                                        .forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> setUUIDWhitelisted(ctx, value))
                            .then(Commands.argument("service", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        services.forEach(
                                                (s, service) -> builder.suggest(s)
                                        );

                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> setUUIDWhitelisted(ctx, value))
                            )
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
                    .then(nameCommand(true))
                    .then(uuidCommand(true))
            );

            this.then(Commands.literal("remove")
                    .requires(ctx -> ctx.getSender().hasPermission("graylist.command.remove"))
                    .then(nameCommand(false))
                    .then(uuidCommand(false))
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
            String prefix = getConfig().getString("services.configs." + service.SERVICE_ID + ".prefix");

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
            String prefix = getConfig().getString("services.configs." + service.SERVICE_ID + ".prefix");

            String unlabeledName = prefix == null
                    ? name
                    : StringUtils.replaceOnce(name, prefix, "");

            // Remove label for service
            final UUID uuid = service.findUUIDByName(unlabeledName);

            // Keep label for whitelist
            return setWhitelisted(service, uuid, name, value);
        }

        private int setNameWhitelisted (CommandContext<CommandSourceStack> ctx, boolean value) {
            String name = ctx.getArgument("name", String.class);

            String service_id = getServiceID(ctx);
            if (service_id == null) {
                Map.Entry<String, String> prefix = prefixes.entrySet().stream()
                        .filter(e -> name.startsWith(e.getValue()))
                        .findFirst()
                        .orElse(null);

                if (prefix == null) service_id = default_service_id;
                else service_id = prefix.getKey();
            }

            Service service = services.get(service_id);

            final CommandSender sender = ctx.getSource().getSender();

            sender.sendRichMessage("Finding <name> via <service>...",
                    Placeholder.component("name", Component.text(name)),
                    Placeholder.component("service", Component.text(service.SERVICE_NAME))
            );

            try {
                sender.sendMessage(setWhitelisted(service, name, value));
            } catch (Service.UserNotFound e) {
                sender.sendRichMessage("Couldn't find <name>",
                        Placeholder.component("name", Component.text(name))
                );
            }

            return Command.SINGLE_SUCCESS;
        }

        private int setUUIDWhitelisted (CommandContext<CommandSourceStack> ctx, boolean value) {
            UUID uuid = ctx.getArgument("uuid", UUID.class);

            Service service = services.get(
                    Optional.ofNullable(getServiceID(ctx)).orElse(default_service_id)
            );

            final CommandSender sender = ctx.getSource().getSender();

            sender.sendMessage(setWhitelisted(service, uuid, value));

            return Command.SINGLE_SUCCESS;
        }
    }
}
