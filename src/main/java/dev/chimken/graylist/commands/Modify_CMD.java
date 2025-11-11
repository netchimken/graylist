package dev.chimken.graylist.commands;

import com.mojang.brigadier.Command;
import dev.chimken.graylist.abstracts.GraylistCommandBundle;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import static io.papermc.paper.command.brigadier.Commands.literal;

public class Modify_CMD extends GraylistCommandBundle<CommandSourceStack> {
    public Modify_CMD() {
        register(literal("modify")
                .requires(ctx -> ctx.getSender().hasPermission("graylist.command.edit"))
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
    }
}
