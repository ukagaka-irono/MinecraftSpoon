Spoon
=====
[![Latest Release](https://img.shields.io/github/v/release/ukagaka-irono/MinecraftSpoon?include_prereleases&label=Latest&color=blue&logo=github)][Download]

English | [日本語](README.ja.md)

Spoon is a fork of Minecraft Forge 10.13.4.1614 for Minecraft 1.7.10.
It keeps the Forge and FML packages (`net.minecraftforge`, `cpw.mods.fml`), the `Forge` and `FML` mod ids and the Forge
version number, so existing 1.7.10 Forge mods load without changes.

| Minecraft | Based on           | Status |
|-----------|--------------------|--------|
| 1.7.10    | Forge 10.13.4.1614 | Beta   |

* [Download]
* [Website] (Japanese)
* [Issues]
* [Support]

# Installing Spoon

Go to [the releases page][Download] and download the files of the latest release.
Spoon is in beta: back up your worlds before trying it.

## Prism Launcher

Spoon is tested with [Prism Launcher][Prism].

1. Download `spoon-<version>-prism.zip`.
2. In Prism, use **Add Instance > Import** and pick the zip. Prism downloads Minecraft 1.7.10 and the libraries itself.
3. Put mods in the instance's `mods` folder. The **Open Mods Folder** button in the in-game mod list opens it too.

If Prism does not pick Java 8, set it in the instance's settings.

## Official Launcher

1. Run Minecraft 1.7.10 once in the official launcher.
2. Run `spoon-<version>-installer.jar` with Java 8 and choose **Install client**.
3. The launcher gets a profile named `Spoon`.

The installer is the Forge 1.7.10 installer, rebranded for Spoon.
`spoon-<version>-universal.jar` is Spoon itself, for setting it up by hand.

For support and questions, read [the support guide][Support].

## Required Java Version

Spoon requires the version of Java that Minecraft 1.7.10 requires:

| Minecraft | Java            |
|-----------|-----------------|
| 1.7.10    | [8][Adoptium-8] |

# What Spoon Changes

* **Branding:** the title screen and F3 show `Spoon <version> (Forge 10.13.4.1614)`, and the client/server brand is
  `fml,forge,spoon`.
* **No Minecraft Realms:** the title screen shows a full-width **Mods** button instead of the Realms button.
* **CJK IME support on Windows:**
  * The IME is detached from the game window while no text field is focused, so WASD and other gameplay keys work with a
    Japanese, Chinese or Korean IME turned on.
  * Text committed by an IME reaches chat, signs and other text fields.
  * The IME composition and candidate windows open at the caret.
* **Signs:** a movable cursor (arrow keys, Home/End, Delete) so text can be inserted mid-line, and a line only accepts
  characters that fit on the board.
* **Full-width letters** such as `ｋ` (U+FF4B) are measured at the width they are drawn at, so centered text containing
  them no longer drifts left.
* **A [Mod Menu] style mod list:** search, sort, logo icons and badges on the left; details with **Website** and
  **Config** buttons, clickable links and scrolling text on the right; and an **Open Mods Folder** button.
  It is a separate screen that opens in place of Forge's `GuiModList`, which Spoon keeps unchanged so mods that extend
  it, such as EnderCore, keep working. The **Mod Options...** button of the pause menu opens it too, in place of
  Forge's `GuiIngameModOptions`, which Forge 1.7.10 never finished.
  For a mod without a config screen, the button becomes **Config File** or **Config Folder** and opens the config
  files the mod loaded through Forge's `Configuration`, in the program the system opens them with.
* **Faster startup and startup times:** mod discovery skips method bodies, and each loading phase logs
  `Spoon startup: ...` timings. Spoon also measures how long each mod spends in its own loading events (construction,
  pre-initialization, initialization, post-initialization and others), and logs the 10 slowest mods when loading ends.
  The mod list shows each mod's time in its details, and its sort button can sort the mods by startup time, slowest
  first. Time that belongs to no single mod, such as loading textures, is not counted.
* **Suspected mods in crash reports:** as in newer Forge versions, the `-- Head --` section of a crash report lists the
  mods whose code appears in the stacktrace, including code merged by mixins. See [the support guide][Support].
* **Crash window:** Minecraft 1.7.10 closes its window without a word when it crashes. Spoon opens a window instead,
  with the description of the crash, the suspected mods and the crash report, and buttons to open the report or its
  folder and to copy the report. The game exits when the window is closed.
* **World backups:** as in newer Minecraft versions, the world selection screen has a **Backup** button. It zips the
  selected world into the `backups` folder as `<date>_<world folder>.zip`, showing its progress, and the screen that
  follows has an **Open Backups Folder** button.
* **Screenshots without a freeze:** as in newer Minecraft versions, the screenshot key reads the screen at once and
  writes the PNG file on another thread, so the game no longer stops for a moment. The chat message comes once the file
  is written.
* **Japanese translation:** the config screens, Spoon's settings, the `/forge` command, the **Mods** and
  **Mod Options...** buttons and FML's error screens, such as the one for missing mods, are translated into Japanese.
  The error screens can open before the language files are loaded, and then show English.
* **Version check:** the title screen shows when a new Spoon version is out. At startup, Spoon reads
  [promotions_slim.json][Promotions] from its website, in the format Forge uses. While there is no stable release, the
  title screen also shows a beta warning. Other mods can draw over the title screen, so the mod list also shows an
  **Update** badge on Spoon, and the new version with a link to the releases page in its details.
  These notices use Spoon's own language keys (`spoon.update.*`), so resource packs that replace the text of Forge's
  update notice do not change them. The result is logged as `Spoon version check: ...`.

## Settings

| Setting                     | In `config/forge.cfg`              | Default | Description |
|-----------------------------|------------------------------------|---------|-------------|
| IME Control                 | `imeControl` (`general`)           | `true`  | Windows only. Set it to `false` to stop Spoon from detaching the IME. |
| Disable Spoon Version Check | `disableVersionCheck` (`general`)  | `false` | Set it to `true` to stop Spoon from checking for a new version at startup. |
| Crash Window                | `crashWindow` (`general`)          | `true`  | Client only. Set it to `false` to exit at once on a crash, without the crash window. |

To change a setting in game, open **Mods** on the title screen, select Spoon and choose
**Config > General Settings**. A change to IME Control or Crash Window made from the title screen applies at once; the
version check setting applies from the next start.

# Creating Mods

Spoon runs mods built for Forge 10.13.4.1614 and does not publish its own MDK.
Build your mod for Forge 1.7.10 as you would for Forge.

# Contribute to Spoon

If you wish to inspect Spoon, submit PRs or otherwise work with Spoon itself, you're in the right place!

## Setting up a Workspace

Requirements:

* JDK 8. Minecraft 1.7.10 and ForgeGradle 1.2 do not build with newer JDKs.
* Internet access for the first setup (Gradle, ForgeGradle, Minecraft and MCP files).

The build uses the GTNewHorizons fork of ForgeGradle 1.2 (`com.github.GTNewHorizons:ForgeGradle:1.2.13`) on
Gradle 4.10.3. The Gradle wrapper downloads Gradle for you. Do not upgrade Gradle: the fork's forgedev tasks call
`Task.execute()`, which Gradle 5 removed. The workarounds that keep the fork working live in `build.gradle`,
`settings.gradle` (registers `eclipse/Clean` and `eclipse/Forge` as subprojects) and `buildSrc` (a fixed copy of the
fork's `ZipFileTree`).

Point `JAVA_HOME` at JDK 8 before running the wrapper, for example:

```
Windows (PowerShell): $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.502.7-hotspot"
MacOS/Linux:          export JAVA_HOME=/path/to/jdk8
```

| Task | Windows | MacOS/Linux |
|------|---------|-------------|
| Set up the workspace (decompiles Minecraft and applies the FML and Forge patches) | `.\gradlew.bat setupForge` | `./gradlew setupForge` |
| Build the universal jar and the installer (in `build/distributions`) | `.\gradlew.bat packageUniversal packageInstaller` | `./gradlew packageUniversal packageInstaller` |
| Build a Prism Launcher instance (`build/distributions/spoon-...-prism.zip`) | `.\gradlew.bat packagePrism` | `./gradlew packagePrism` |
| Run the client from the workspace (optionally with `-PmcUsername=<name>`) | `.\gradlew.bat runClient` | `./gradlew runClient` |

After changing Minecraft classes in `eclipse/Forge`, regenerate the patches with `gradlew genPatches`. It only writes
`patches/minecraft`, diffing against the sources with the FML patches applied, which it rebuilds from `fml/patches` each
time. Review the result before committing: even on an unchanged workspace, the fork rewrites a few patches it did not
need to touch (`Minecraft`, `GuiChat`, `WorldGenDungeons` and `GenLayerBiome`) with MCP names such as
`WorldType.DEFAULT_1_1`, other decompiler labels or shifted line numbers. Keep only the patches you meant to change and
restore the others with git.

To use Eclipse, point your Eclipse workspace at the `eclipse` folder inside the repo.

### Pull requests

Please read [the contributing guidelines][Contributing] before making a pull request.

# License

Spoon is distributed under the same terms as Minecraft Forge: see [MinecraftForge-License.txt][License] (Forge Public
License) and [MinecraftForge-Credits.txt][Credits]. FML is under the LGPL: see [fml/LICENSE-fml.txt][FML-License].
The Paulscode license files apply to the bundled sound libraries.

[Contributing]: ./CONTRIBUTING.md
[Support]: ./SUPPORT.md

[Download]: https://github.com/ukagaka-irono/MinecraftSpoon/releases
[Website]: https://ukagaka-irono.github.io/minecraft-spoon/
[Issues]: https://github.com/ukagaka-irono/MinecraftSpoon/issues

[Prism]: https://prismlauncher.org/
[Mod Menu]: https://modrinth.com/mod/modmenu
[Adoptium-8]: https://adoptium.net/temurin/releases/?version=8
[Promotions]: https://ukagaka-irono.github.io/minecraft-spoon/promotions_slim.json

[License]: ../MinecraftForge-License.txt
[Credits]: ../MinecraftForge-Credits.txt
[FML-License]: ../fml/LICENSE-fml.txt
