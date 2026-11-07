package io.github.vibecoderqin.fadesolver.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DebugLog {
    private static final Path PATH = Path.of("logs", "chatgamesolver-fade-debug.log");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private DebugLog() {
    }

    public static synchronized void info(String message) {
        String line = "[" + LocalDateTime.now().format(TIME_FORMAT) + "] ["
                + Thread.currentThread().getName() + "] " + message + System.lineSeparator();
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }
}