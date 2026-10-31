*** SPOON ***
Spoon is a fork of Minecraft Forge 10.13.4.1614 for Minecraft 1.7.10.
Repository: https://github.com/ukagaka-irono/MinecraftSpoon

Spoon keeps the Forge and FML packages (net.minecraftforge, cpw.mods.fml), the "Forge" and "FML" mod ids
and the Forge version number, so existing 1.7.10 Forge mods load without changes.

What Spoon changes:
  - Branding: the title screen and F3 show "Spoon <version> (Forge 10.13.4.1614)", and the client/server
    brand is "fml,forge,spoon".
  - Minecraft Realms is removed: the title screen shows a full-width "Mods" button instead of the Realms button.
  - CJK IME support on Windows: the IME is detached from the game window while no text field is focused,
    so WASD and other gameplay keys work with a Japanese/Chinese/Korean IME turned on.
    Set "imeControl" to false in config/forge.cfg to turn this off.
  - Faster startup: mod discovery skips method bodies, and each loading phase logs "Spoon startup: ..." timings.
  - The Forge version check is disabled; Spoon has no update server yet.


*** BUILDING ***
Requirements:
  - JDK 8. Minecraft 1.7.10 and ForgeGradle 1.2 do not build with newer JDKs.
  - Internet access for the first setup (Gradle, ForgeGradle, Minecraft and MCP files).

The build uses the GTNewHorizons fork of ForgeGradle 1.2 (com.github.GTNewHorizons:ForgeGradle:1.2.13) on
Gradle 4.10.3. The Gradle wrapper downloads Gradle for you. Do not upgrade Gradle: the fork's forgedev tasks
call Task.execute(), which Gradle 5 removed. The workarounds that keep the fork working live in
build.gradle, settings.gradle (registers eclipse/Clean and eclipse/Forge as subprojects) and
buildSrc (a fixed copy of the fork's ZipFileTree).

Point JAVA_HOME at JDK 8 before running the wrapper, for example:
  Windows (PowerShell): $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-8.0.502.7-hotspot"
  MacOS/Linux:          export JAVA_HOME=/path/to/jdk8

Set up the workspace (decompiles Minecraft and applies the FML and Forge patches):
  Windows:     .\gradlew.bat setupForge
  MacOS/Linux: ./gradlew setupForge

Build the universal jar and the installer (output in build/distributions):
  Windows:     .\gradlew.bat packageUniversal packageInstaller
  MacOS/Linux: ./gradlew packageUniversal packageInstaller

Run the client from the workspace (optionally with -PmcUsername=<name>):
  Windows:     .\gradlew.bat runClient
  MacOS/Linux: ./gradlew runClient

After changing Minecraft classes in eclipse/Forge, regenerate the patches with "gradlew genPatches".
Review the result before committing: the fork sometimes leaves MCP names (for example
WorldType.DEFAULT_1_1) in patches it did not need to touch. Keep only the patches you meant to change
and restore the others with git.
To use Eclipse, point your Eclipse workspace at the eclipse folder inside the repo.


*** LICENSE ***
Spoon is distributed under the same terms as Minecraft Forge: see MinecraftForge-License.txt (Forge Public
License) and MinecraftForge-Credits.txt. FML is under the LGPL: see fml/LICENSE-fml.txt.
The Paulscode license files apply to the bundled sound libraries.
