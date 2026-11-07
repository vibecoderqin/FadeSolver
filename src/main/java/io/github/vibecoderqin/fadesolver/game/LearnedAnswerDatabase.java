package io.github.vibecoderqin.fadesolver.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.vibecoderqin.fadesolver.AutoChatMod;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class LearnedAnswerDatabase {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ANSWER_LIST_TYPE = new TypeToken<List<AnswerRecord>>() { }.getType();
    private static final Path PATH = Path.of("config", "chatgamesolver_answers.json");
    private static final List<AnswerRecord> ANSWERS = new ArrayList<>();

    private LearnedAnswerDatabase() {
    }

    public static synchronized void load() {
        ANSWERS.clear();
        if (!Files.exists(PATH)) {
            return;
        }
        try (var reader = Files.newBufferedReader(PATH, StandardCharsets.UTF_8)) {
            List<AnswerRecord> loaded = GSON.fromJson(reader, ANSWER_LIST_TYPE);
            if (loaded != null) {
                for (AnswerRecord record : loaded) {
                    if (record != null && record.type != null && record.promptNormalized != null
                            && record.answer != null) {
                        if (record.aliases == null) {
                            record.aliases = new ArrayList<>();
                        }
                        ANSWERS.add(record);
                    }
                }
            }
        } catch (Exception exception) {
            AutoChatMod.LOGGER.warn("Could not read learned chat-game answers from {}", PATH, exception);
        }
    }

    public static synchronized Optional<String> find(ChatGameParser.Task task) {
        return ANSWERS.stream()
                .filter(record -> record.type.equals(task.type().key()))
                .filter(record -> record.promptNormalized.equals(task.promptNormalized()))
                .map(record -> record.answer)
                .findFirst();
    }

    public static synchronized void learn(ChatGameParser.Task task, String answer) {
        String normalizedPrompt = normalize(task.promptRaw());
        AnswerRecord record = ANSWERS.stream()
                .filter(candidate -> candidate.type.equals(task.type().key()))
                .filter(candidate -> candidate.promptNormalized.equals(normalizedPrompt))
                .findFirst()
                .orElse(null);
        String now = Instant.now().toString();
        if (record == null) {
            ANSWERS.add(new AnswerRecord(task.type().key(), task.promptRaw(), normalizedPrompt, answer,
                    "server_answer", 1, now, new ArrayList<>()));
            save();
            return;
        }
        record.hits++;
        record.lastSeen = now;
        record.promptRaw = task.promptRaw();
        record.source = "server_answer";
        if (!record.answer.equalsIgnoreCase(answer)) {
            AutoChatMod.LOGGER.warn("Conflicting learned answer: type={}, prompt={}, old={}, new={}",
                    task.type().key(), task.promptRaw(), record.answer, answer);
            String previousAnswer = record.answer;
            record.answer = answer;
            addAlias(record, previousAnswer);
        }
        save();
    }

    public static synchronized void clear() {
        ANSWERS.clear();
        save();
    }

    private static void addAlias(AnswerRecord record, String answer) {
        if (record.aliases == null) {
            record.aliases = new ArrayList<>();
        }
        if (!answer.equalsIgnoreCase(record.answer)
                && record.aliases.stream().noneMatch(alias -> alias.equalsIgnoreCase(answer))) {
            record.aliases.add(answer);
        }
    }

    private static String normalize(String value) {
        return ChatGameParser.collapseWhitespace(value).toLowerCase(Locale.ROOT);
    }

    private static void save() {
        Path temporary = PATH.resolveSibling(PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(temporary, GSON.toJson(ANSWERS), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(temporary, PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            AutoChatMod.LOGGER.warn("Could not save learned chat-game answers to {}", PATH, exception);
        }
    }

    private static final class AnswerRecord {
        private String type;
        private String promptRaw;
        private String promptNormalized;
        private String answer;
        private String source;
        private int hits;
        private String lastSeen;
        private List<String> aliases;

        private AnswerRecord(String type, String promptRaw, String promptNormalized, String answer,
                             String source, int hits, String lastSeen, List<String> aliases) {
            this.type = type;
            this.promptRaw = promptRaw;
            this.promptNormalized = promptNormalized;
            this.answer = answer;
            this.source = source;
            this.hits = hits;
            this.lastSeen = lastSeen;
            this.aliases = aliases;
        }
    }
}