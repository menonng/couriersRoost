package com.courierpost.bird;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BirdStorage {
    private static final Gson GSON = new Gson();
    private static final String FILE_NAME = "courierpost_birds.json";

    private BirdStorage() {
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILE_NAME);
    }

    public static void save(MinecraftServer server, List<CourierBird> birds) {
        JsonArray array = new JsonArray();
        for (CourierBird bird : birds) {
            JsonObject json = new JsonObject();
            json.addProperty("id", bird.id.toString());
            json.addProperty("sender", bird.senderId.toString());
            json.addProperty("sender_name", bird.senderName);
            json.addProperty("recipient", bird.recipientId.toString());
            json.addProperty("kind", bird.kind.name());
            json.addProperty("content", bird.content);
            json.addProperty("team", bird.teamName);
            json.addProperty("dimension", bird.dimension.identifier().toString());
            json.addProperty("x", bird.x);
            json.addProperty("y", bird.y);
            json.addProperty("z", bird.z);
            json.addProperty("yaw", bird.yaw);
            json.addProperty("speed_factor", bird.speedFactor);
            json.addProperty("delay", bird.delayTicks);
            json.addProperty("elapsed", bird.elapsedTicks);
            json.addProperty("death", bird.deathTick);
            json.addProperty("lost", bird.lostTick);
            json.addProperty("baby", bird.baby);
            json.addProperty("variant", bird.variantIndex);
            array.add(json);
        }
        Path path = file(server);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(array, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public static List<CourierBird> load(MinecraftServer server) {
        List<CourierBird> birds = new ArrayList<>();
        Path path = file(server);
        if (!Files.isRegularFile(path)) {
            return birds;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (int index = 0; index < array.size(); index++) {
                JsonObject json = array.get(index).getAsJsonObject();
                CourierBird bird = new CourierBird();
                bird.id = UUID.fromString(json.get("id").getAsString());
                bird.senderId = UUID.fromString(json.get("sender").getAsString());
                bird.senderName = json.get("sender_name").getAsString();
                bird.recipientId = UUID.fromString(json.get("recipient").getAsString());
                bird.kind = MessageKind.byName(json.get("kind").getAsString());
                bird.content = json.get("content").getAsString();
                bird.teamName = json.get("team").getAsString();
                Identifier dimension = Identifier.parse(json.get("dimension").getAsString());
                bird.dimension = ResourceKey.create(Registries.DIMENSION, dimension);
                bird.x = json.get("x").getAsDouble();
                bird.y = json.get("y").getAsDouble();
                bird.z = json.get("z").getAsDouble();
                bird.yaw = json.get("yaw").getAsDouble();
                bird.speedFactor = json.get("speed_factor").getAsDouble();
                bird.delayTicks = json.get("delay").getAsInt();
                bird.elapsedTicks = json.get("elapsed").getAsInt();
                bird.deathTick = json.get("death").getAsInt();
                bird.lostTick = json.get("lost").getAsInt();
                bird.baby = json.get("baby").getAsBoolean();
                bird.variantIndex = json.get("variant").getAsInt();
                if (bird.dimension.equals(Level.OVERWORLD) || server.getLevel(bird.dimension) != null) {
                    birds.add(bird);
                }
            }
        } catch (IOException | RuntimeException exception) {
            birds.clear();
        }
        return birds;
    }
}
