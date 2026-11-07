# FadeSolver

FadeSolver is a client-side Fabric mod for recognizing and solving supported chat mini-games on FadeCloud Prison. It listens to incoming Minecraft chat messages, parses game prompts, looks for an answer, and can submit that answer after a configurable delay.

## Features

- Detects supported chat-game prompts in normal chat and game/system messages.
- Supports the **type**, **unscramble**, **unreverse**, **unshuffle**, and **solve** prompt types.
- Solves multiplication expressions and reverse-text prompts directly.
- Uses English Minecraft item and block names as its local word dictionary for scramble-based prompts.
- Learns answers revealed by the server and stores them locally for future matching prompts.
- Adds a randomized answer delay with configurable minimum and maximum values.
- Provides an in-game configuration screen through the `/chatgames` command and, when installed, Mod Menu.

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | 1.21.11 |
| Java | 21 |
| Fabric Loader | 0.19.2 or newer |
| Fabric API | Required |
| Yet Another Config Lib (YACL) | 3.8.0 or newer |
| Mod Menu | Optional; 17.0.0 or newer for its configuration entry |

Use dependency versions compatible with Minecraft 1.21.11.

## Installation

1. Install Minecraft 1.21.11 with Fabric Loader.
2. Add Fabric API and Yet Another Config Lib to your `mods` folder.
3. Add the FadeSolver JAR to the same `mods` folder.
4. Optionally install Mod Menu to access the configuration screen from its interface.
5. Launch the game using your Fabric profile.

## Usage

After joining a supported server, FadeSolver watches incoming chat messages for recognized game announcements and prompts. When it finds a solution and the solver is enabled, it schedules the answer using the configured delay.

Open the configuration screen with:

```text
/chatgames
```

The configuration screen lets you:

- Enable or disable automatic solving.
- Enable or disable learning from answers announced by the server.
- Set the minimum and maximum answer delay.
- Clear the learned-answer database. Clearing requires enabling the confirmation option first.

The default settings enable the solver and answer learning, with a randomized delay between **6 and 9 seconds**. The delay can be adjusted from **0.5 to 15 seconds**.

## How solving works

- **Type:** uses the prompt text as the answer.
- **Unreverse:** reverses the prompt text.
- **Solve:** evaluates supported integer multiplication expressions.
- **Unscramble / Unshuffle:** compares the prompt against English names of Minecraft items and blocks available in the local game registry.
- **Learned answers:** checks locally stored answers before applying the built-in solving methods. When the server reveals an answer, it can be saved and reused for a matching game type and prompt.

Dictionary matching depends on the Minecraft registry names available in the installed game version. Prompts outside the supported formats may not produce an answer.

## Configuration and local data

FadeSolver stores its configuration and learned answers in the Minecraft game directory:

- `config/chatgames.json` — solver, learning, and delay settings.
- `config/chatgamesolver_answers.json` — answers learned from server announcements.
- `logs/chatgamesolver-fade-debug.log` — diagnostic event log.

These files are local to your Minecraft installation.

## Building from source

You need JDK 21 and a Gradle installation.

Clone the repository and run:

```bash
gradle build
```

The built mod JAR will be written to `build/libs/`.

## Notes

- FadeSolver runs on the Minecraft client and uses locally available game data to build its word dictionary.
- No external account or hosted solving service is required.
- Automatic chat responses may be restricted by individual servers. Check and follow the rules of the server where you use the mod.

