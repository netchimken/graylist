package dev.chimken.graylist.commands;

import com.mojang.brigadier.Command;
import dev.chimken.graylist.TextStyles;
import dev.chimken.graylist.abstracts.GraylistCommandBundle;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;

import static io.papermc.paper.command.brigadier.Commands.literal;
import static net.kyori.adventure.text.Component.text;

public class List_CMD extends GraylistCommandBundle<CommandSourceStack> {
    public List_CMD() {
        register(literal("list")

                .requires(ctx -> ctx.getSender().hasPermission("graylist.command.list"))
                .executes(ctx -> {
                    List<TextComponent> playerlist = Bukkit.getWhitelistedPlayers().stream()
                            .map(player -> {
                                final String name = player.getName();
                                final UUID uuid = player.getUniqueId();

                                return text(name != null ? name : "<unknown>")
                                        .style(TextStyles.buildUsernameStyle(uuid));
                            })
                            .toList();
                    int count = playerlist.size();

                    TextComponent msg = text("There are " + count + " whitelisted player(s): \n");

                    for (int i = 0; i <= (count - 1); i++) {
                        if (i > 0 || i == (count - 1))
                            msg = msg.append(text(", "));

                        msg = msg.append(playerlist.get(i));
                    }

                    ctx.getSource().getSender().sendMessage(msg);

                    return Command.SINGLE_SUCCESS;
                })
        );
    }
}
