package dev.chimken.graylist.managers;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.chimken.graylist.abstracts.GraylistCommandBundle;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public class CommandManager extends LiteralArgumentBuilder<CommandSourceStack> {
    public CommandManager (String label) {
        super(label);

        this.requires(ctx -> ctx.getSender().hasPermission("graylist"));
    }

    public void register(GraylistCommandBundle<CommandSourceStack> bundle) {
        bundle.getCommands().forEach(this::then);
    }
}
