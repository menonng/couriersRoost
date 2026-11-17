package com.courierpost.ui;

import com.courierpost.CourierPost;
import com.courierpost.chat.Localization;
import com.courierpost.config.CourierConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class ConfigDialog {
    private ConfigDialog() {
    }

    public static boolean isEditable(String key) {
        return CourierConfig.KEYS.contains(key);
    }

    public static void open(ServerPlayer player) {
        CourierConfig config = CourierPost.config();
        player.sendSystemMessage(Localization.text(player, "courierpost.ui.title").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Localization.text(player, "courierpost.ui.body").withStyle(ChatFormatting.GRAY));

        for (String key : CourierConfig.KEYS) {
            String value = config.get(key);
            boolean toggle = "true".equals(value) || "false".equals(value);
            String command = toggle
                    ? "/birdconfig toggle " + key
                    : "/birdconfig set " + key + " " + value;

            MutableComponent line = Localization.text(player, "courierpost.config." + key)
                    .append(Component.literal(": "))
                    .append(value(player, key, value, toggle))
                    .withStyle(style -> style
                            .withClickEvent(new ClickEvent(
                                    toggle ? ClickEvent.Action.RUN_COMMAND : ClickEvent.Action.SUGGEST_COMMAND,
                                    command))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Localization.text(player,
                                    toggle ? "courierpost.ui.tooltip_toggle" : "courierpost.ui.tooltip_edit"))));
            player.sendSystemMessage(line);
        }
    }

    public static void edit(ServerPlayer player, String key) {
        String value = CourierPost.config().get(key);
        if (value == null) {
            return;
        }
        player.sendSystemMessage(Localization.text(player, "courierpost.ui.tooltip_edit")
                .append(Component.literal(" "))
                .append(Component.literal("/birdconfig set " + key + " " + value)
                        .withStyle(style -> style
                                .withColor(ChatFormatting.AQUA)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/birdconfig set " + key + " " + value)))));
    }

    public static String normalize(String key, String raw) {
        return raw;
    }

    public static boolean toggle(String key) {
        CourierConfig config = CourierPost.config();
        String value = config.get(key);
        if (!"true".equals(value) && !"false".equals(value)) {
            return false;
        }
        return config.set(key, "true".equals(value) ? "false" : "true");
    }

    private static Component value(ServerPlayer player, String key, String raw, boolean toggle) {
        if (toggle) {
            boolean enabled = "true".equals(raw);
            return Localization.text(player, enabled ? "courierpost.ui.on" : "courierpost.ui.off")
                    .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
        }
        return Component.literal(raw).withStyle(ChatFormatting.AQUA);
    }
}
