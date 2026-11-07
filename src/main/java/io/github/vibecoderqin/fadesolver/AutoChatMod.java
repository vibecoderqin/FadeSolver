package io.github.vibecoderqin.fadesolver;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AutoChatMod implements ModInitializer {
    public static final String MOD_ID = "chatgames";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ChatGameConfig.load();
        LOGGER.info("FadeSolver configuration loaded");
    }
}