package dev.chimken.graylist.commands;

import com.mojang.brigadier.Command;
import dev.chimken.graylist.abstracts.GraylistCommandBundle;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;

import static io.papermc.paper.command.brigadier.Commands.*;

public class Reload_CMD extends GraylistCommandBundle<CommandSourceStack> {
    public Reload_CMD() {
        register(literal("reload")
                .requires(ctx -> ctx.getSender().hasPermission("graylist.command.reload"))
                .executes(ctx -> {
                    Bukkit.getServer().reloadWhitelist();
                    ctx.getSource().getSender().sendMessage("Reloaded the whitelist");

                    return Command.SINGLE_SUCCESS;
                })
        );
    }
}
