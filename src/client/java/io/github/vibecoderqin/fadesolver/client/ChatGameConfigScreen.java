package io.github.vibecoderqin.fadesolver.client;

import io.github.vibecoderqin.fadesolver.AutoChatMod;
import io.github.vibecoderqin.fadesolver.ChatGameConfig;
import io.github.vibecoderqin.fadesolver.game.LearnedAnswerDatabase;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ChatGameConfigScreen {
    private ChatGameConfigScreen() {
    }

    public static Screen createScreen(Screen parent) {
        ChatGameConfig config = ChatGameConfig.get();
        config.confirmClearAnswers = false;
        return YetAnotherConfigLib.createBuilder()
                .title(Text.literal("FadeSolver"))
                .save(ChatGameConfig::save)
                .category(ConfigCategory.createBuilder()
                        .name(Text.literal("Fade"))
                        .option(booleanOption("Enable solver", "Recognize and answer FadeCloud Prison chat games.",
                                true, () -> config.enabled, value -> config.enabled = value))
                        .option(booleanOption("Learn from server answers",
                                "Save correct answers revealed by the server for future prompts.",
                                true, () -> config.learnFromServerAnswers,
                                value -> config.learnFromServerAnswers = value))
                        .option(booleanOption("Confirm answer database deletion",
                                "Enable this before pressing the clear button.", false,
                                () -> config.confirmClearAnswers, value -> config.confirmClearAnswers = value))
                        .option(ButtonOption.createBuilder()
                                .name(Text.literal("Clear learned answers"))
                                .description(OptionDescription.of(Text.literal(
                                        "Requires the confirmation option above to be enabled.")))
                                .action((screen, buttonOption) -> {
                                    if (!config.confirmClearAnswers) {
                                        AutoChatMod.LOGGER.info("Enable deletion confirmation before clearing learned answers");
                                        return;
                                    }
                                    LearnedAnswerDatabase.clear();
                                    config.confirmClearAnswers = false;
                                    ChatGameConfig.save();
                                    AutoChatMod.LOGGER.info("Cleared learned chat-game answers");
                                })
                                .build())
                        .option(delayOption("Minimum delay", 6.0F, () -> config.minDelaySeconds, value -> {
                            config.minDelaySeconds = value;
                            config.normalize();
                        }))
                        .option(delayOption("Maximum delay", 9.0F, () -> config.maxDelaySeconds, value -> {
                            config.maxDelaySeconds = value;
                            config.normalize();
                        }))
                        .build())
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> booleanOption(String name, String description, boolean defaultValue,
                                                  BooleanSupplier getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(Text.literal(name))
                .description(OptionDescription.of(Text.literal(description)))
                .binding(defaultValue, getter::getAsBoolean, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<Float> delayOption(String name, float defaultValue, Supplier<Float> getter,
                                              Consumer<Float> setter) {
        return Option.<Float>createBuilder()
                .name(Text.literal(name))
                .description(OptionDescription.of(Text.literal("Random delay before sending an answer.")))
                .binding(defaultValue, getter, setter)
                .controller(option -> FloatSliderControllerBuilder.create(option)
                        .range(0.5F, 15.0F).step(0.1F))
                .build();
    }
}