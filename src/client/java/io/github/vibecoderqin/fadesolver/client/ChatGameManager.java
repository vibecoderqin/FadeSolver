package io.github.vibecoderqin.fadesolver.client;

import io.github.vibecoderqin.fadesolver.AutoChatMod;
import io.github.vibecoderqin.fadesolver.ChatGameConfig;
import io.github.vibecoderqin.fadesolver.game.ChatGameParser;
import io.github.vibecoderqin.fadesolver.game.ChatGameSolver;
import io.github.vibecoderqin.fadesolver.game.LearnedAnswerDatabase;
import io.github.vibecoderqin.fadesolver.game.ServerAnswerParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

public final class ChatGameManager {
    private static final Pattern WINNER = Pattern.compile("\\b(?:typed|unscrambled|unreversed|unshuffled|solved)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final int ANSWER_LEARNING_TTL_TICKS = 2400;
    private static final Random RANDOM = new Random();
    private static final ChatGameParser PARSER = new ChatGameParser();
    private static final ServerAnswerParser SERVER_ANSWER_PARSER = new ServerAnswerParser();
    private static final ChatGameSolver SOLVER = new ChatGameSolver();

    private static PendingAnswer pendingAnswer;
    private static ChatGameParser.Task currentTask;
    private static long currentGameId;
    private static int ticksSinceHeader;
    private static boolean gameActive;
    private static boolean answerSent;
    private static boolean solverInitialized;

    private ChatGameManager() {
    }

    public static void initialize() {
        solverInitialized = false;
    }

    public static void handle(String message, String source) {
        String clean = ChatGameParser.cleanMessage(message);
        if (clean.isEmpty()) {
            return;
        }
        ChatGameParser.ParseResult result = PARSER.accept(clean);
        if (result.newGame()) {
            beginGame(source, clean);
            return;
        }

        if (currentTask != null && ticksSinceHeader <= ANSWER_LEARNING_TTL_TICKS) {
            handleServerAnswer(clean);
        }
        if (currentTask != null && gameActive && WINNER.matcher(clean).find()) {
            cancelPending("another player answered first");
            gameActive = false;
            DebugLog.info("Game resolved by another player: " + printable(clean));
        }
        if (!result.completed()) {
            return;
        }
        if (result.task() == null) {
            AutoChatMod.LOGGER.debug("Unrecognized Fade chat game: {}", result.unrecognizedPrompt());
            DebugLog.info("Unrecognized chat game prompt=" + printable(result.unrecognizedPrompt()));
            return;
        }
        currentTask = result.task();
        AutoChatMod.LOGGER.info("Recognized Fade chat game: type={}, prompt={}",
                currentTask.type().key(), currentTask.promptRaw());
        DebugLog.info("Recognized task type=" + currentTask.type().key()
                + " prompt=" + printable(currentTask.promptRaw()));
        Optional<ChatGameSolver.Solution> solution = SOLVER.solve(currentTask);
        if (solution.isEmpty()) {
            AutoChatMod.LOGGER.debug("No answer found for Fade chat game: type={}, prompt={}",
                    currentTask.type().key(), currentTask.promptRaw());
            DebugLog.info("No answer found type=" + currentTask.type().key()
                    + " prompt=" + printable(currentTask.promptRaw()));
            return;
        }
        ChatGameSolver.Solution found = solution.get();
        AutoChatMod.LOGGER.info("Solved Fade chat game: type={}, answer={}, source={}",
                currentTask.type().key(), found.answer(), found.source());
        DebugLog.info("Solved type=" + currentTask.type().key() + " answer=" + printable(found.answer())
                + " source=" + found.source());
        if (!ChatGameConfig.get().enabled) {
            DebugLog.info("Answer not scheduled: mod disabled");
            return;
        }
        if (gameActive && !answerSent && pendingAnswer == null) {
            scheduleAnswer(found.answer());
        }
    }

    public static void tick(MinecraftClient client) {
        if (!solverInitialized && client.getResourceManager() != null) {
            SOLVER.initialize();
            solverInitialized = true;
        }
        if (currentTask != null && ticksSinceHeader < ANSWER_LEARNING_TTL_TICKS + 1) {
            ticksSinceHeader++;
            if (ticksSinceHeader > ANSWER_LEARNING_TTL_TICKS) {
                SERVER_ANSWER_PARSER.reset();
                if (pendingAnswer != null) {
                    cancelPending("game answer window expired");
                }
                gameActive = false;
                currentTask = null;
            }
        }
        if (pendingAnswer == null) {
            return;
        }
        if (!ChatGameConfig.get().enabled) {
            cancelPending("mod disabled before send");
            return;
        }
        if (!gameActive || answerSent || pendingAnswer.gameId() != currentGameId) {
            cancelPending("game is no longer eligible");
            return;
        }
        if (pendingAnswer.tick()) {
            sendIfValid(client, pendingAnswer);
        }
    }

    private static void beginGame(String source, String header) {
        cancelPending("new chat-game event");
        currentGameId++;
        currentTask = null;
        ticksSinceHeader = 0;
        gameActive = true;
        answerSent = false;
        SERVER_ANSWER_PARSER.reset();
        DebugLog.info("CHAT GAME header source=" + source + " event=" + currentGameId
                + " message=" + printable(header));
    }

    private static void handleServerAnswer(String message) {
        boolean reveal = ServerAnswerParser.isAnswerReveal(message);
        if (reveal) {
            cancelPending("server revealed the answer");
        }
        ChatGameConfig config = ChatGameConfig.get();
        if (!config.learnFromServerAnswers) {
            if (reveal) {
                SERVER_ANSWER_PARSER.reset();
            }
            return;
        }
        Optional<String> answer = SERVER_ANSWER_PARSER.accept(message);
        if (answer.isEmpty()) {
            return;
        }
        LearnedAnswerDatabase.learn(currentTask, answer.get());
        String log = "Learned answer: type=" + currentTask.type().key() + ", prompt="
                + currentTask.promptRaw() + ", answer=" + answer.get();
        AutoChatMod.LOGGER.info("Learned answer: type={}, prompt={}, answer={}",
                currentTask.type().key(), currentTask.promptRaw(), answer.get());
        DebugLog.info(log);
        gameActive = false;
    }

    private static void scheduleAnswer(String answer) {
        ChatGameConfig config = ChatGameConfig.get();
        config.normalize();
        double delay = config.minDelaySeconds
                + RANDOM.nextDouble() * (config.maxDelaySeconds - config.minDelaySeconds);
        pendingAnswer = new PendingAnswer(answer, currentGameId, delay);
        AutoChatMod.LOGGER.info("Answer scheduled: type={}, delay={}s, answer={}",
                currentTask.type().key(), String.format(java.util.Locale.ROOT, "%.2f", delay), answer);
        DebugLog.info("Answer scheduled event=" + currentGameId + " answer=" + printable(answer)
                + " delaySeconds=" + String.format(java.util.Locale.ROOT, "%.2f", delay));
    }

    private static void sendIfValid(MinecraftClient client, PendingAnswer scheduled) {
        if (scheduled != pendingAnswer || scheduled.gameId() != currentGameId || !gameActive || answerSent) {
            cancelPending("answer became stale");
            return;
        }
        if (!ChatGameConfig.get().enabled || client.player == null || client.world == null) {
            cancelPending("client is not ready or mod is disabled");
            return;
        }
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null || !handler.isConnectionOpen()) {
            cancelPending("no active server connection");
            return;
        }
        handler.sendChatMessage(scheduled.answer());
        answerSent = true;
        gameActive = false;
        pendingAnswer = null;
        AutoChatMod.LOGGER.info("Sent Fade chat-game answer: type={}, answer={}",
                currentTask.type().key(), scheduled.answer());
        DebugLog.info("Answer sent event=" + currentGameId + " answer=" + printable(scheduled.answer()));
    }

    private static void cancelPending(String reason) {
        if (pendingAnswer == null) {
            return;
        }
        DebugLog.info("Answer canceled event=" + pendingAnswer.gameId() + " reason=" + reason);
        pendingAnswer = null;
    }

    private static String printable(String value) {
        return value == null ? "<null>" : value.replace("\\", "\\\\")
                .replace("\r", "\\r").replace("\n", "\\n");
    }
}