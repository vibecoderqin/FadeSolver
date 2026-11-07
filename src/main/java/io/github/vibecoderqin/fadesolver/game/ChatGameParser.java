package io.github.vibecoderqin.fadesolver.game;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatGameParser {
    private static final Pattern WINS = Pattern.compile("\\bwins!\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIVIDER = Pattern.compile("\\(x\\s*\\d+\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TASK = Pattern.compile(
            "(?is)^the\\s+first\\s+to\\s+(type|unscramble|unreverse|unshuffle|solve)\\s+'(.*?)'"
                    + "\\s*(?:back\\s+to\\s+the\\s+right\\s+order\\s*)?wins!$");
    private static final Pattern HEADER = Pattern.compile(".*›\\s*CHAT\\s+GAME\\s*‹.*", Pattern.CASE_INSENSITIVE);

    private final StringBuilder promptBuffer = new StringBuilder();
    private boolean collecting;
    private boolean promptStarted;

    public ParseResult accept(String message) {
        String clean = cleanMessage(message);
        if (HEADER.matcher(clean).matches()) {
            reset();
            collecting = true;
            return new ParseResult(true, false, null, null);
        }
        if (!collecting) {
            return ParseResult.empty();
        }
        if (clean.isEmpty()) {
            if (promptStarted) {
                reset();
            }
            return ParseResult.empty();
        }
        if (DIVIDER.matcher(clean).matches()) {
            return ParseResult.empty();
        }
        if (!promptStarted) {
            if (!clean.toLowerCase(Locale.ROOT).contains("the first to")) {
                return ParseResult.empty();
            }
            promptStarted = true;
        }
        if (!promptBuffer.isEmpty()) {
            promptBuffer.append(' ');
        }
        promptBuffer.append(collapseWhitespace(clean));
        String combined = collapseWhitespace(promptBuffer.toString());
        if (!WINS.matcher(combined).find()) {
            return ParseResult.empty();
        }

        collecting = false;
        promptStarted = false;
        promptBuffer.setLength(0);
        Matcher matcher = TASK.matcher(combined);
        if (!matcher.matches()) {
            return new ParseResult(false, true, null, combined);
        }
        GameType type = GameType.fromKey(matcher.group(1).toLowerCase(Locale.ROOT));
        String prompt = collapseWhitespace(matcher.group(2));
        if (prompt.isEmpty()) {
            return new ParseResult(false, true, null, combined);
        }
        return new ParseResult(false, true,
                new Task(type, prompt, normalizeKey(prompt)), null);
    }

    public void reset() {
        collecting = false;
        promptStarted = false;
        promptBuffer.setLength(0);
    }

    public static String cleanMessage(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("§.", "").trim();
    }

    public static String collapseWhitespace(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    public static String normalizeKey(String value) {
        return collapseWhitespace(value).toLowerCase(Locale.ROOT);
    }

    public enum GameType {
        TYPE("type"),
        UNSCRAMBLE("unscramble"),
        UNREVERSE("unreverse"),
        UNSHUFFLE("unshuffle"),
        SOLVE("solve");

        private final String key;

        GameType(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        private static GameType fromKey(String key) {
            for (GameType type : values()) {
                if (type.key.equals(key)) {
                    return type;
                }
            }
            throw new IllegalArgumentException(key);
        }
    }

    public record Task(GameType type, String promptRaw, String promptNormalized) {
    }

    public record ParseResult(boolean newGame, boolean completed, Task task, String unrecognizedPrompt) {
        private static ParseResult empty() {
            return new ParseResult(false, false, null, null);
        }

        public Optional<Task> recognizedTask() {
            return Optional.ofNullable(task);
        }
    }
}