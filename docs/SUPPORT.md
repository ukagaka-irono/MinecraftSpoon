Getting Support for Spoon
=========================

English | [日本語](SUPPORT.ja.md)

Before reporting an issue on this repository's [issue tracker][Issues], please read this document. It shows how to find
out whether a problem comes from Spoon, a mod or Minecraft itself, where to report it, and what to put in the report.

### Contents

 1. [Identifying the Source of a Problem](#identifying-the-source-of-a-problem)
    1. [Analyzing a Stacktrace](#analyzing-a-stacktrace)
    2. [Comparing with Forge](#comparing-with-forge)
    3. [Isolating an Issue](#isolating-an-issue)
 2. [Choosing the Correct Support Channel](#choosing-the-correct-support-channel)
 3. [Writing a Helpful Bug Report](#writing-a-helpful-bug-report)
    1. [Preventing Duplicates](#preventing-duplicates)
    2. [Required Information](#required-information)
    3. [Markdown Issue Template](#markdown-issue-template)

Identifying the Source of a Problem
-----------------------------------

Start with the log files. They are in the `logs` folder of the game directory: `.minecraft/logs` for the official
launcher, and `minecraft/logs` (or `.minecraft/logs`) inside the instance folder for Prism Launcher.

* `fml-client-latest.log` (`fml-server-latest.log` on a server) is the full log of FML, Forge, Spoon and every mod, at
  every level. **This is the log to look at and to attach to a report.**
* `latest.log` only holds Minecraft's own messages.
* Crashes also write a crash report to the `crash-reports` folder, such as `crash-2026-01-01_12.00.00-client.txt`.

A crash report starts with a description and a stacktrace: lines like

```
at some.words.related.to.a.mod.MoreWords(MoreWords.java:123)
```

The top line is where the game stopped and the bottom line is where it started. Its `-- System Details --` section
ends with an `FML:` line that shows the Spoon version and every loaded mod.

As in newer Forge versions, the `-- Head --` section starts with the suspected mods: every mod whose code appears in the
stacktrace, including code that a mixin merged into another class, with the first line where it appears:

```
-- Head --
Suspected Mod: 
	Example Mod (examplemod), Version: 1.0
		Website: https://example.com/
		at com.example.ExampleBlock.onBlockActivated(ExampleBlock.java:42)
Stacktrace:
```

Start with these mods, but read the stacktrace too: a listed mod may only have passed the error on.

### Analyzing a Stacktrace

Follow these rules, in order:

 1. If you can find the name of a mod in the stacktrace, that mod most likely caused the problem. The suspected mods
    list them for you. If several mods appear, report to all of them or work out from the names which one is at fault.
 2. If you only find `net.minecraftforge` or `cpw.mods.fml` alongside `net.minecraft`, the problem may be in Spoon or in
    Forge itself. Spoon keeps Forge's package names, so its code shows up under the same names.
    [Compare with Forge](#comparing-with-forge) to tell them apart.
 3. If nothing points at a clear culprit, consider coremods first: they can change any code that runs in the game.
    `fml-client-latest.log` lists them as `Instantiating coremod class ...` lines.

### Comparing with Forge

Spoon changes only some parts of Forge 10.13.4.1614 (see [What Spoon Changes][Changes]). To find out whether a problem
comes from one of those changes, try the same mods and the same steps with Forge 10.13.4.1614. In Prism Launcher, you can
copy the instance and install Forge 10.13.4.1614 in place of Spoon.

* If the problem also happens with Forge, Spoon did not cause it. Report it to the mod.
* If it only happens with Spoon, report it to Spoon.

### Isolating an Issue

A report with clear steps and a small set of mods is much more likely to get fixed.

* **Retrace your last steps before the problem.** The names in the stacktrace often hint at what the game was doing,
  such as using an item or opening a screen.
* **Reduce the number of mods.** Instead of removing mods one by one, halve the list:
   1. Move half of the mods out of the instance, keeping any that are definitely involved.
   2. If the problem still happens, repeat step 1 with the mods that are left.
   3. If it no longer happens, the cause is among the mods you moved out. Put them back, move out the mods that were
      left instead, and repeat step 1 with the mods you put back.
   4. Stop when removing any more mods also makes the problem go away. Report with that set of mods.

Choosing the Correct Support Channel
------------------------------------

* **Spoon:** report problems that only happen with Spoon, and problems with Spoon's own features (IME support, signs,
  the mod list, startup), on [the issue tracker][Issues].
* **Mods:** report problems that also happen with Forge to the mod. Look for an issue tracker link on the mod's
  [CurseForge] or [Modrinth] page, or search for its repository on [GitHub].
* **Forge and Mojang:** neither fixes Minecraft 1.7.10 anymore. Do not report Spoon problems to them.

Writing a Helpful Bug Report
----------------------------

### Preventing Duplicates

Search [the issue tracker][Issues], including closed issues, for the same problem. If you find it, comment there with
anything new, such as other steps to reproduce it, instead of opening a new issue.
Also check [the latest release][Download] and its changelog: the problem may already be fixed.

### Required Information

 - **Spoon version:** as shown on the title screen, for example `Spoon 1.0.0-beta.1 (Forge 10.13.4.1614)`.
 - **Launcher and Java version:** for example Prism Launcher 9.4 with Java 8u462.
 - **OS:** and, for text input problems, the IME you use (for example Microsoft IME).
 - **Complete list of mods:** with their versions. If you [isolated the issue](#isolating-an-issue), the mods that are
   left are enough.
 - **Steps to reproduce:** as few as possible. If the problem needs a special setup, consider attaching your world.
 - **Full log:** `fml-client-latest.log`, and the crash report if there is one. Upload them to
   [GitHub Gist][Gist] and link them instead of pasting them into the report.

You can write the report in English or Japanese.

### Markdown Issue Template

The issue form already contains this template. Replace each `{...}` with your information.

```markdown
**Spoon Version:** {e.g. 1.0.0-beta.1}

**Launcher and Java:** {e.g. Prism Launcher, Java 8u462}

**OS and IME:** {e.g. Windows 11, Microsoft IME}

**Logs:** {Link(s) to GitHub Gist with full fml-client-latest.log and/or crash report}

**Mods in This Instance:**
| Name    | Version     |
| ------- | ----------- |
| {mod 1} | {version 1} |
| {mod 2} | {version 2} |

**Steps to Reproduce:**
 1. Do this
 2. Then do that
 3. ...

**Description of issue:**
```

[Issues]: https://github.com/ukagaka-irono/MinecraftSpoon/issues
[Download]: https://github.com/ukagaka-irono/MinecraftSpoon/releases
[Changes]: ./README.md#what-spoon-changes

[CurseForge]: https://www.curseforge.com/minecraft/mc-mods
[Modrinth]: https://modrinth.com/mods
[GitHub]: https://github.com/search
[Gist]: https://gist.github.com/
