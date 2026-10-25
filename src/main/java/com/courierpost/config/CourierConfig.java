package com.courierpost.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CourierConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final List<String> KEYS = List.of(
            "base_dimension_delay",
            "bird_speed",
            "nether_speed",
            "speed_variation",
            "rain_multiplier",
            "thunder_multiplier",
            "bird_death_chance",
            "lost_chance",
            "use_coordinate_conversion",
            "deliver_offline_messages",
            "save_birds",
            "bird_max_lifetime",
            "show_bird",
            "global_chat_cooldown"
    );

    public int baseDimensionDelay = 8;
    public double birdSpeed = 25.0D;
    public double netherSpeed = 16.0D;
    public double speedVariation = 0.2D;
    public double rainMultiplier = 0.75D;
    public double thunderMultiplier = 0.5D;
    public double birdDeathChance = 0.002D;
    public double lostChance = 0.0D;
    public boolean useCoordinateConversion = false;
    public boolean deliverOfflineMessages = false;
    public boolean saveBirds = true;
    public int birdMaxLifetime = 4800;
    public boolean showBird = true;
    public int globalChatCooldown = 10;

    public static CourierConfig load(Path path) {
        CourierConfig config = new CourierConfig();
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                config.readJson(json);
            } catch (IOException | RuntimeException exception) {
                config = new CourierConfig();
            }
        }
        config.save(path);
        return config;
    }

    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(writeJson(), writer);
            }
        } catch (IOException ignored) {
        }
    }

    private void readJson(JsonObject json) {
        for (String key : KEYS) {
            if (json.has(key)) {
                set(key, json.get(key).getAsString());
            }
        }
    }

    private JsonObject writeJson() {
        JsonObject json = new JsonObject();
        json.addProperty("base_dimension_delay", baseDimensionDelay);
        json.addProperty("bird_speed", birdSpeed);
        json.addProperty("nether_speed", netherSpeed);
        json.addProperty("speed_variation", speedVariation);
        json.addProperty("rain_multiplier", rainMultiplier);
        json.addProperty("thunder_multiplier", thunderMultiplier);
        json.addProperty("bird_death_chance", birdDeathChance);
        json.addProperty("lost_chance", lostChance);
        json.addProperty("use_coordinate_conversion", useCoordinateConversion);
        json.addProperty("deliver_offline_messages", deliverOfflineMessages);
        json.addProperty("save_birds", saveBirds);
        json.addProperty("bird_max_lifetime", birdMaxLifetime);
        json.addProperty("show_bird", showBird);
        json.addProperty("global_chat_cooldown", globalChatCooldown);
        return json;
    }

    public String get(String key) {
        return switch (key) {
            case "base_dimension_delay" -> Integer.toString(baseDimensionDelay);
            case "bird_speed" -> Double.toString(birdSpeed);
            case "nether_speed" -> Double.toString(netherSpeed);
            case "speed_variation" -> Double.toString(speedVariation);
            case "rain_multiplier" -> Double.toString(rainMultiplier);
            case "thunder_multiplier" -> Double.toString(thunderMultiplier);
            case "bird_death_chance" -> Double.toString(birdDeathChance);
            case "lost_chance" -> Double.toString(lostChance);
            case "use_coordinate_conversion" -> Boolean.toString(useCoordinateConversion);
            case "deliver_offline_messages" -> Boolean.toString(deliverOfflineMessages);
            case "save_birds" -> Boolean.toString(saveBirds);
            case "bird_max_lifetime" -> Integer.toString(birdMaxLifetime);
            case "show_bird" -> Boolean.toString(showBird);
            case "global_chat_cooldown" -> Integer.toString(globalChatCooldown);
            default -> null;
        };
    }

    public boolean set(String key, String value) {
        try {
            switch (key) {
                case "base_dimension_delay" -> baseDimensionDelay = Math.max(0, Integer.parseInt(value));
                case "bird_speed" -> birdSpeed = requirePositive(Double.parseDouble(value));
                case "nether_speed" -> netherSpeed = requirePositive(Double.parseDouble(value));
                case "speed_variation" -> speedVariation = clamp(Double.parseDouble(value), 0.0D, 0.95D);
                case "rain_multiplier" -> rainMultiplier = clamp(Double.parseDouble(value), 0.01D, 10.0D);
                case "thunder_multiplier" -> thunderMultiplier = clamp(Double.parseDouble(value), 0.01D, 10.0D);
                case "bird_death_chance" -> birdDeathChance = clamp(Double.parseDouble(value), 0.0D, 1.0D);
                case "lost_chance" -> lostChance = clamp(Double.parseDouble(value), 0.0D, 1.0D);
                case "use_coordinate_conversion" -> useCoordinateConversion = parseBoolean(value);
                case "deliver_offline_messages" -> deliverOfflineMessages = parseBoolean(value);
                case "save_birds" -> saveBirds = parseBoolean(value);
                case "bird_max_lifetime" -> birdMaxLifetime = Math.max(1, Integer.parseInt(value));
                case "show_bird" -> showBird = parseBoolean(value);
                case "global_chat_cooldown" -> globalChatCooldown = Math.max(0, Integer.parseInt(value));
                default -> {
                    return false;
                }
            }
        } catch (RuntimeException exception) {
            return false;
        }
        return true;
    }

    public void copyFrom(CourierConfig other) {
        for (String key : KEYS) {
            set(key, other.get(key));
        }
    }

    private static boolean parseBoolean(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        throw new IllegalArgumentException(value);
    }

    private static double requirePositive(double value) {
        if (value <= 0.0D) {
            throw new IllegalArgumentException(Double.toString(value));
        }
        return value;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
