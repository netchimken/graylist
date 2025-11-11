package dev.chimken.graylist.commands;

import com.mojang.brigadier.Command;
import dev.chimken.graylist.abstracts.GraylistCommandBundle;
import dev.chimken.graylist.abstracts.GraylistService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import static io.papermc.paper.command.brigadier.Commands.literal;

public class Services_CMD extends GraylistCommandBundle<CommandSourceStack> {
    public Services_CMD() {
        register(literal("services")
                .requires(ctx -> ctx.getSender().hasPermission("graylist.command.services"))
                .executes(ctx -> {
                    String flatList = String.join(
                            ", ",
                            serviceManager.getServices().values().stream().map(GraylistService::getID).toList()
                    );

                    ctx.getSource().getSender().sendRichMessage("There are <count> service(s) available: <list>",
                            Placeholder.component("count", Component.text(serviceManager.getServices().keySet().toArray().length)),
                            Placeholder.component("list", Component.text(flatList))
                    );

                    return Command.SINGLE_SUCCESS;
                })
        );
    }
}
