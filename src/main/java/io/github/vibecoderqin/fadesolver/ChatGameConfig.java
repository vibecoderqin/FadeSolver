package io.github.vibecoderqin.fadesolver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public final class ChatGameConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Path.of("config", "chatgames.json");
    private static ChatGameConfig instance;

    public boolean enabled = true;
    public boolean learnFromServerAnswers = true;
    public float minDelaySeconds = 6.0F;
    public float maxDelaySeconds = 9.0F;
    public transient boolean confirmClearAnswers;

    private ChatGameConfig() {
    }

    public static synchronized ChatGameConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static synchronized void load() {
        ChatGameConfig loaded = null;
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
                JsonObject defaults = GSON.toJsonTree(new ChatGameConfig()).getAsJsonObject();
                JsonObject saved = JsonParser.parseReader(reader).getAsJsonObject();
                saved.entrySet().stream().filter(entry -> defaults.has(entry.getKey()))
                        .forEach(entry -> defaults.add(entry.getKey(), entry.getValue()));
                loaded = GSON.fromJson(defaults, ChatGameConfig.class);
            } catch (Exception exception) {
                AutoChatMod.LOGGER.warn("Could not read {}, using defaults", PATH, exception);
            }
        }
        instance = loaded == null ? new ChatGameConfig() : loaded;
        instance.normalize();
        save();
    }

    public static synchronized void save() {
        if (instance == null) {
            instance = new ChatGameConfig();
        }
        instance.normalize();
        Path temporary = PATH.resolveSibling(PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(temporary, GSON.toJson(instance), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(temporary, PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            AutoChatMod.LOGGER.warn("Could not save {}", PATH, exception);
        }
    }

    public void normalize() {
        float minimum = normalizeDelay(minDelaySeconds, 6.0F);
        float maximum = normalizeDelay(maxDelaySeconds, 9.0F);
        if (minimum > maximum) {
            float temporary = minimum;
            minimum = maximum;
            maximum = temporary;
        }
        minDelaySeconds = minimum;
        maxDelaySeconds = maximum;
    }

    private static float normalizeDelay(float value, float defaultValue) {
        if (!Float.isFinite(value)) {
            return defaultValue;
        }
        return Math.round(Math.max(0.5F, Math.min(15.0F, value)) * 10.0F) / 10.0F;
    }
}