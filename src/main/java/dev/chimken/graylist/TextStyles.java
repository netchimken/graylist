package dev.chimken.graylist;

import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

import java.util.UUID;

import static net.kyori.adventure.text.Component.text;

public class TextStyles {
    public static Style buildUsernameStyle(UUID uuid) {
        return Style.style()
                .color(TextColor.fromHexString("#7FFFFD"))
                .hoverEvent(HoverEvent.showText(
                        text(uuid.toString())
                ))
                .clickEvent(ClickEvent.copyToClipboard(uuid.toString()))
                .build();
    }
}
