package io.github.vibecoderqin.fadesolver.client;

import io.github.vibecoderqin.fadesolver.game.LearnedAnswerDatabase;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;

public final class ChatGameSolverClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LearnedAnswerDatabase.load();
        ChatGameManager.initialize();
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                ChatGameManager.handle(message.getString(), "GAME"));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
                ChatGameManager.handle(message.getString(), "CHAT"));
        ClientTickEvents.END_CLIENT_TICK.register(ChatGameManager::tick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("chatgames")
                        .executes(context -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            client.setScreen(ChatGameConfigScreen.createScreen(client.currentScreen));
                            return 1;
                        })));
    }
}