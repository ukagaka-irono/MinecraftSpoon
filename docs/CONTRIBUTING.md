Contributing to Spoon
=====================

English | [日本語](CONTRIBUTING.ja.md)

1) Keep patches to Minecraft classes minimal and together. If you need a lot of things done, you may either add to relevant Spoon/Forge classes or make a new class. Try not to spread out your patches across multiple disjoint lines, as this makes maintenance of your patches difficult.

2) An example mod must be provided for all PRs adding events/hooks to Spoon. Whether it's a full mod using your proposed hooks, or just a pastebin containing a simple example, an example mod is required. Please also explain in detail what your new hook does.

3) Follow the code style of the class you're working in (braces on newlines and spaces instead of tabs, as in Forge and in the patches).

4) Keep existing Forge mods working: do not rename the `net.minecraftforge` / `cpw.mods.fml` packages or the `Forge` / `FML` mod ids.

For setting up a workspace and regenerating patches, refer to [the README][Workspace].

Open issues and pull requests at https://github.com/ukagaka-irono/MinecraftSpoon

[Workspace]: ./README.md#setting-up-a-workspace
