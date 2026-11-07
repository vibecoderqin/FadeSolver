package io.github.vibecoderqin.fadesolver.game;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.vibecoderqin.fadesolver.AutoChatMod;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatGameSolver {
    private static final Pattern MULTIPLICATION = Pattern.compile("^\\s*(-?\\d+)\\s*[xX×*]\\s*(-?\\d+)\\s*$");
    private static final Comparator<String> NAME_ORDER = Comparator.comparingInt(String::length)
            .thenComparing(String.CASE_INSENSITIVE_ORDER);
    private List<String> dictionary = List.of();

    public void initialize() {
        Map<String, String> translations = loadEnglishTranslations();
        Set<String> uniqueNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Item item : Registries.ITEM.stream().toList()) {
            addName(uniqueNames, translations.get(item.getTranslationKey()), item.getName().getString());
        }
        for (Block block : Registries.BLOCK.stream().toList()) {
            addName(uniqueNames, translations.get(block.getTranslationKey()), block.getName().getString());
        }
        dictionary = uniqueNames.stream().sorted(NAME_ORDER).toList();
        AutoChatMod.LOGGER.info("Loaded {} English item and block names for Fade chat games", dictionary.size());
    }

    public Optional<Solution> solve(ChatGameParser.Task task) {
        Optional<String> learned = LearnedAnswerDatabase.find(task);
        if (learned.isPresent()) {
            return Optional.of(new Solution(learned.get(), "learned"));
        }
        return switch (task.type()) {
            case TYPE -> Optional.of(new Solution(task.promptRaw(), "prompt"));
            case UNREVERSE -> Optional.of(new Solution(new StringBuilder(task.promptRaw()).reverse().toString(),
                    "reverse"));
            case SOLVE -> solveMultiplication(task.promptRaw());
            case UNSCRAMBLE -> findDictionaryAnswer(task, true);
            case UNSHUFFLE -> findDictionaryAnswer(task, false);
        };
    }

    private Optional<Solution> findDictionaryAnswer(ChatGameParser.Task task, boolean unscramble) {
        String target = unscramble ? orderedLetterSignature(task.promptRaw()) : unorderedWordSignature(task.promptRaw());
        List<String> matches = dictionary.stream()
                .filter(name -> (unscramble ? orderedLetterSignature(name) : unorderedWordSignature(name)).equals(target))
                .toList();
        if (matches.size() > 1) {
            AutoChatMod.LOGGER.warn("Ambiguous {} prompt '{}': candidates={}", task.type().key(),
                    task.promptRaw(), matches);
        }
        return matches.isEmpty() ? Optional.empty() : Optional.of(new Solution(matches.getFirst(), "dictionary"));
    }

    private Optional<Solution> solveMultiplication(String prompt) {
        Matcher matcher = MULTIPLICATION.matcher(prompt);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            BigInteger first = new BigInteger(matcher.group(1));
            BigInteger second = new BigInteger(matcher.group(2));
            return Optional.of(new Solution(first.multiply(second).toString(), "math"));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private Map<String, String> loadEnglishTranslations() {
        Map<String, String> translations = new HashMap<>();
        Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager()
                .getResource(Identifier.of("minecraft", "lang/en_us.json"));
        if (resource.isEmpty()) {
            AutoChatMod.LOGGER.warn("Could not find vanilla en_us.json; using registry display names");
            return translations;
        }
        try (Reader reader = resource.get().getReader()) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                    translations.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (Exception exception) {
            AutoChatMod.LOGGER.warn("Could not read vanilla en_us.json; using registry display names", exception);
        }
        return translations;
    }

    private static void addName(Set<String> names, String englishName, String fallbackName) {
        String name = englishName == null ? fallbackName : englishName;
        String normalized = ChatGameParser.collapseWhitespace(name);
        if (!normalized.isEmpty()) {
            names.add(normalized);
        }
    }

    private static String orderedLetterSignature(String value) {
        return String.join(" ", words(value).stream().map(ChatGameSolver::sortedLetters).toList());
    }

    private static String unorderedWordSignature(String value) {
        return words(value).stream().sorted().reduce((first, second) -> first + " " + second).orElse("");
    }

    private static List<String> words(String value) {
        return Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(word -> !word.isEmpty())
                .toList();
    }

    private static String sortedLetters(String word) {
        char[] letters = word.toCharArray();
        Arrays.sort(letters);
        return new String(letters);
    }

    public record Solution(String answer, String source) {
    }
}