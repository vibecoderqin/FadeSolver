package io.github.vibecoderqin.fadesolver.game;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServerAnswerParser {
    private static final Pattern ANSWER_START = Pattern.compile("\\banswer\\s+was\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUOTED_ANSWER = Pattern.compile("'([^']+)'|“([^”]+)”");
    private boolean awaitingAnswer;

    public Optional<String> accept(String message) {
        String clean = ChatGameParser.cleanMessage(message);
        Matcher start = ANSWER_START.matcher(clean);
        if (start.find()) {
            awaitingAnswer = true;
            return extract(clean.substring(start.end()));
        }
        if (!awaitingAnswer) {
            return Optional.empty();
        }
        return extract(clean);
    }

    public boolean isAwaitingAnswer() {
        return awaitingAnswer;
    }

    public void reset() {
        awaitingAnswer = false;
    }

    public static boolean isAnswerReveal(String message) {
        return ANSWER_START.matcher(ChatGameParser.cleanMessage(message)).find();
    }

    private Optional<String> extract(String value) {
        Matcher matcher = QUOTED_ANSWER.matcher(value);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String answer = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
        answer = answer.replaceFirst("[\\s.!?,;:]+$", "").trim();
        awaitingAnswer = false;
        return answer.isEmpty() ? Optional.empty() : Optional.of(answer);
    }
}