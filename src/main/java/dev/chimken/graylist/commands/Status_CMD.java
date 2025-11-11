package dev.chimken.graylist.commands;

import com.mojang.brigadier.Command;
import dev.chimken.graylist.abstracts.GraylistCommandBundle;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;

import static io.papermc.paper.command.brigadier.Commands.*;

public class Status_CMD extends GraylistCommandBundle<CommandSourceStack> {
    public Status_CMD() {
        register(literal("status")
                .requires(ctx -> ctx.getSender().hasPermission("graylist.command.status"))
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(
                            "Whitelist is currently " + (Bukkit.hasWhitelist() ? "on" : "off")
                    );

                    return Command.SINGLE_SUCCESS;
                })
        );
    }
}
