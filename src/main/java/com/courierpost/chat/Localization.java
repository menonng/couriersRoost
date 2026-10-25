package com.courierpost.chat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Localization {
    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final List<String> LANGUAGES = List.of("en_us", "ko_kr", "ja_jp");
    private static final Map<String, Map<String, String>> TABLES = new HashMap<>();

    static {
        for (String language : LANGUAGES) {
            TABLES.put(language, read(language));
        }
    }

    private Localization() {
    }

    private static Map<String, String> read(String language) {
        Map<String, String> entries = new HashMap<>();
        String path = "/assets/courierpost/lang/" + language + ".json";
        try (InputStream stream = Localization.class.getResourceAsStream(path)) {
            if (stream == null) {
                return entries;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                for (String key : json.keySet()) {
                    entries.put(key, json.get(key).getAsString());
                }
            }
        } catch (Exception exception) {
            return entries;
        }
        return entries;
    }

    public static String resolve(ServerPlayer player) {
        if (player == null) {
            return DEFAULT_LANGUAGE;
        }
        String reported = player.clientInformation().language();
        if (reported == null) {
            return DEFAULT_LANGUAGE;
        }
        String normalized = reported.toLowerCase();
        if (normalized.startsWith("ko")) {
            return "ko_kr";
        }
        if (normalized.startsWith("ja")) {
            return "ja_jp";
        }
        return DEFAULT_LANGUAGE;
    }

    public static MutableComponent text(ServerPlayer player, String key, Object... arguments) {
        return Component.literal(format(resolve(player), key, arguments));
    }

    public static MutableComponent text(CommandSourceStack source, String key, Object... arguments) {
        return Component.literal(format(resolve(source.getPlayer()), key, arguments));
    }

    private static String format(String language, String key, Object... arguments) {
        Map<String, String> table = TABLES.getOrDefault(language, Map.of());
        String pattern = table.get(key);
        if (pattern == null) {
            pattern = TABLES.getOrDefault(DEFAULT_LANGUAGE, Map.of()).get(key);
        }
        if (pattern == null) {
            return key;
        }
        if (arguments.length == 0) {
            return pattern;
        }
        try {
            return String.format(pattern, arguments);
        } catch (RuntimeException exception) {
            return pattern;
        }
    }
}
