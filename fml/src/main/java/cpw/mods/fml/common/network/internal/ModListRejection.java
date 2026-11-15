package cpw.mods.fml.common.network.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.util.StatCollector;

import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.Side;

/**
 * Writes the message a player reads when the mods of the client and the server do not match, in place of FML's
 * "Mod rejections [FMLMod:examplemod{1.2.3}]": each mod with the version on each side, or that one side lacks it.
 *
 * The message is written by the side that rejects. A client writes it in its own language; a server writes it in its
 * own, usually English, and sends it to the client.
 */
public final class ModListRejection
{
    // Beyond this many mods, the disconnect screen would run off the bottom
    static final int MAX_LISTED = 10;

    private ModListRejection() {}

    /**
     * @param rejects the mods of this side that do not accept the other side
     * @param remoteVersions the mod ids and versions of the other side
     * @param remoteSide the other side: SERVER when this is a client checking a server
     */
    public static String message(List<ModContainer> rejects, Map<String, String> remoteVersions, Side remoteSide)
    {
        StringBuilder message = new StringBuilder(text("spoon.network.rejected", "Your mods do not match the server's:"));
        for (String line : lines(rejects, remoteVersions, remoteSide))
        {
            message.append('\n').append(line);
        }
        return message.toString();
    }

    /**
     * Returns a line for each mod, at most {@link #MAX_LISTED} and then how many more, for the server list too.
     */
    public static List<String> lines(List<ModContainer> rejects, Map<String, String> remoteVersions, Side remoteSide)
    {
        List<String> lines = new ArrayList<String>();
        for (int i = 0; i < rejects.size() && i < MAX_LISTED; i++)
        {
            ModContainer mod = rejects.get(i);
            String name = String.format("%s (%s)", mod.getName(), mod.getModId());
            String here = mod.getDisplayVersion();
            String remote = remoteVersions.get(mod.getModId());
            if (remoteSide == Side.SERVER)
            {
                lines.add(remote == null
                        ? text("spoon.network.rejected.serverMissing", "%s: you have %s, the server does not", name, here)
                        : text("spoon.network.rejected.serverVersion", "%s: you have %s, the server has %s", name, here, remote));
            }
            else
            {
                lines.add(remote == null
                        ? text("spoon.network.rejected.clientMissing", "%s: the server has %s, you do not", name, here)
                        : text("spoon.network.rejected.clientVersion", "%s: the server has %s, you have %s", name, here, remote));
            }
        }
        if (rejects.size() > MAX_LISTED)
        {
            lines.add(more(rejects.size() - MAX_LISTED));
        }
        return lines;
    }

    static String more(int count)
    {
        return text("spoon.network.rejected.more", "and %s more", count);
    }

    static String text(String key, String english, Object... args)
    {
        try
        {
            // A dedicated server may not have Spoon's language files in its table
            if (StatCollector.canTranslate(key))
            {
                return StatCollector.translateToLocalFormatted(key, args);
            }
        }
        catch (Throwable ignored) {}
        return String.format(english, args);
    }
}
