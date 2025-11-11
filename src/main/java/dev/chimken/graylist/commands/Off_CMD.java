package dev.chimken.graylist.commands;

import com.mojang.brigadier.Command;
import dev.chimken.graylist.abstracts.GraylistCommandBundle;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import static io.papermc.paper.command.brigadier.Commands.literal;

public class Off_CMD extends GraylistCommandBundle<CommandSourceStack> {
    public Off_CMD() {
        register(literal("off")
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
}
