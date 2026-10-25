package com.courierpost.ui;

import com.courierpost.CourierPost;
import com.courierpost.chat.Localization;
import com.courierpost.config.CourierConfig;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.action.CommandTemplate;
import net.minecraft.server.dialog.action.ParsedTemplate;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.NumberRangeInput;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ConfigDialog {
    private static final int BUTTON_WIDTH = 150;
    private static final int NARROW_WIDTH = 100;
    private static final int INPUT_WIDTH = 220;
    private static final int BODY_WIDTH = 320;
    private static final String VALUE_KEY = "value";

    private enum Kind {
        BOOL,
        INT,
        DECIMAL
    }

    private record Entry(String key, Kind kind, float min, float max, float step) {
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry("base_dimension_delay", Kind.INT, 0.0F, 120.0F, 1.0F),
            new Entry("bird_speed", Kind.DECIMAL, 0.5F, 200.0F, 0.5F),
            new Entry("nether_speed", Kind.DECIMAL, 0.5F, 200.0F, 0.5F),
            new Entry("speed_variation", Kind.DECIMAL, 0.0F, 0.95F, 0.05F),
            new Entry("rain_multiplier", Kind.DECIMAL, 0.05F, 2.0F, 0.05F),
            new Entry("thunder_multiplier", Kind.DECIMAL, 0.05F, 2.0F, 0.05F),
            new Entry("bird_death_chance", Kind.DECIMAL, 0.0F, 1.0F, 0.001F),
            new Entry("lost_chance", Kind.DECIMAL, 0.0F, 1.0F, 0.001F),
            new Entry("use_coordinate_conversion", Kind.BOOL, 0.0F, 0.0F, 0.0F),
            new Entry("deliver_offline_messages", Kind.BOOL, 0.0F, 0.0F, 0.0F),
            new Entry("save_birds", Kind.BOOL, 0.0F, 0.0F, 0.0F),
            new Entry("bird_max_lifetime", Kind.INT, 60.0F, 9600.0F, 60.0F),
            new Entry("show_bird", Kind.BOOL, 0.0F, 0.0F, 0.0F),
            new Entry("global_chat_cooldown", Kind.INT, 0.0F, 120.0F, 1.0F)
    );

    private ConfigDialog() {
    }

    public static boolean isEditable(String key) {
        return find(key) != null;
    }

    public static void open(ServerPlayer player) {
        CourierConfig config = CourierPost.config();
        List<ActionButton> buttons = new ArrayList<>(ENTRIES.size());

        for (Entry entry : ENTRIES) {
            String value = config.get(entry.key());
            MutableComponent label = label(player, entry.key()).append(Component.literal(": "))
                    .append(valueText(player, entry, value));
            String command = entry.kind() == Kind.BOOL
                    ? "/birdconfig toggle " + entry.key()
                    : "/birdconfig edit " + entry.key();
            buttons.add(new ActionButton(
                    new CommonButtonData(label, Optional.of(tooltip(player, entry)), BUTTON_WIDTH),
                    Optional.of(runCommand(command))));
        }

        ActionButton close = new ActionButton(
                new CommonButtonData(Localization.text(player, "courierpost.ui.close"), NARROW_WIDTH),
                Optional.empty());

        send(player, new MultiActionDialog(
                common(Localization.text(player, "courierpost.ui.title"),
                        Localization.text(player, "courierpost.ui.body"),
                        List.of()),
                buttons,
                Optional.of(close),
                2));
    }

    public static void edit(ServerPlayer player, String key) {
        Entry entry = find(key);
        if (entry == null || entry.kind() == Kind.BOOL) {
            open(player);
            return;
        }

        float current = current(entry);
        NumberRangeInput control = new NumberRangeInput(
                INPUT_WIDTH,
                label(player, entry.key()),
                "options.generic_value",
                new NumberRangeInput.RangeInfo(entry.min(), entry.max(),
                        Optional.of(current), Optional.of(entry.step())));

        ActionButton confirm = new ActionButton(
                new CommonButtonData(Localization.text(player, "courierpost.ui.confirm"), NARROW_WIDTH),
                Optional.of(new CommandTemplate(template("/birdconfig apply " + entry.key() + " $(" + VALUE_KEY + ")"))));
        ActionButton cancel = new ActionButton(
                new CommonButtonData(Localization.text(player, "courierpost.ui.cancel"), NARROW_WIDTH),
                Optional.of(runCommand("/birdconfig")));

        send(player, new MultiActionDialog(
                common(label(player, entry.key()),
                        Localization.text(player, "courierpost.ui.edit_body", range(entry)),
                        List.of(new Input(VALUE_KEY, control))),
                List.of(confirm),
                Optional.of(cancel),
                1));
    }

    public static String normalize(String key, String raw) {
        Entry entry = find(key);
        if (entry == null) {
            return raw;
        }
        try {
            double parsed = Double.parseDouble(raw);
            if (entry.kind() == Kind.INT) {
                return Long.toString(Math.round(parsed));
            }
            return Double.toString(Math.round(parsed * 1000.0D) / 1000.0D);
        } catch (NumberFormatException exception) {
            return raw;
        }
    }

    public static boolean toggle(String key) {
        CourierConfig config = CourierPost.config();
        String value = config.get(key);
        if (!"true".equals(value) && !"false".equals(value)) {
            return false;
        }
        return config.set(key, "true".equals(value) ? "false" : "true");
    }

    private static CommonDialogData common(Component title, Component body, List<Input> inputs) {
        List<DialogBody> contents = List.of(new PlainMessage(body, BODY_WIDTH));
        return new CommonDialogData(title, Optional.empty(), true, false, DialogAction.CLOSE, contents, inputs);
    }

    private static void send(ServerPlayer player, Dialog dialog) {
        player.connection.send(new ClientboundShowDialogPacket(Holder.direct(dialog)));
    }

    private static Action runCommand(String command) {
        return new StaticAction(new ClickEvent.RunCommand(command));
    }

    private static ParsedTemplate template(String value) {
        return ParsedTemplate.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(value)).result().orElseThrow();
    }

    private static MutableComponent label(ServerPlayer player, String key) {
        return Localization.text(player, "courierpost.config." + key);
    }

    private static Component valueText(ServerPlayer player, Entry entry, String value) {
        if (entry.kind() == Kind.BOOL) {
            boolean enabled = "true".equals(value);
            return Localization.text(player, enabled ? "courierpost.ui.on" : "courierpost.ui.off")
                    .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
        }
        return Component.literal(value).withStyle(ChatFormatting.AQUA);
    }

    private static Component tooltip(ServerPlayer player, Entry entry) {
        if (entry.kind() == Kind.BOOL) {
            return Localization.text(player, "courierpost.ui.tooltip_toggle");
        }
        return Localization.text(player, "courierpost.ui.tooltip_range", range(entry));
    }

    private static String range(Entry entry) {
        if (entry.kind() == Kind.INT) {
            return Math.round(entry.min()) + " ~ " + Math.round(entry.max());
        }
        return entry.min() + " ~ " + entry.max();
    }

    private static float current(Entry entry) {
        try {
            float value = Float.parseFloat(CourierPost.config().get(entry.key()));
            return Math.max(entry.min(), Math.min(entry.max(), value));
        } catch (NumberFormatException exception) {
            return entry.min();
        }
    }

    private static Entry find(String key) {
        for (Entry entry : ENTRIES) {
            if (entry.key().equals(key)) {
                return entry;
            }
        }
        return null;
    }
}
