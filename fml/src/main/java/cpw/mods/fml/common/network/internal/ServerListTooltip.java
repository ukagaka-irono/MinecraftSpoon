package cpw.mods.fml.common.network.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.Side;

/**
 * Writes the tooltip of the icon on a server in the multiplayer list. For a modded server it lists the mods that do
 * not match before the player tries to join, in the form of the message shown when the server is left, and the mods
 * only the server has, which the server may want here too.
 */
public final class ServerListTooltip
{
    // Mods every Forge server lists, which the player never installs
    private static final List<String> BUILT_IN = java.util.Arrays.asList("mcp", "FML", "Forge");
    static final int MAX_SERVER_ONLY = 5;

    private ServerListTooltip() {}

    /**
     * @param type FML, BUKKIT, VANILLA or anything else for an unknown server
     * @param serverMods the mod ids and versions of the server, for an FML server
     */
    public static String text(String type, boolean compatible, Map<String, String> serverMods)
    {
        if ("FML".equals(type))
        {
            List<ModContainer> rejects = compatible ? new ArrayList<ModContainer>() : FMLNetworkHandler.rejectedMods(serverMods, Side.SERVER);
            List<String> serverOnly = new ArrayList<String>();
            for (String modId : serverMods.keySet())
            {
                if (!BUILT_IN.contains(modId) && !Loader.isModLoaded(modId))
                {
                    serverOnly.add(modId);
                }
            }
            return fml(compatible, serverMods.size(), ModListRejection.lines(rejects, serverMods, Side.SERVER), serverOnly);
        }
        if ("BUKKIT".equals(type))
        {
            return ModListRejection.text("spoon.serverList.bukkit", "Bukkit modded server");
        }
        if ("VANILLA".equals(type))
        {
            return ModListRejection.text("spoon.serverList.vanilla", "Vanilla server");
        }
        return ModListRejection.text("spoon.serverList.unknown", "Unknown server data");
    }

    static String fml(boolean compatible, int modCount, List<String> mismatchLines, List<String> serverOnly)
    {
        StringBuilder text = new StringBuilder(compatible
                ? ModListRejection.text("spoon.serverList.compatible", "Compatible FML modded server")
                : ModListRejection.text("spoon.serverList.incompatible", "Incompatible FML modded server"));
        text.append('\n').append(ModListRejection.text("spoon.serverList.mods", "%s mods present", modCount));
        for (String line : mismatchLines)
        {
            text.append('\n').append(line);
        }
        if (!serverOnly.isEmpty())
        {
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < serverOnly.size() && i < MAX_SERVER_ONLY; i++)
            {
                names.append(i > 0 ? ", " : "").append(serverOnly.get(i));
            }
            if (serverOnly.size() > MAX_SERVER_ONLY)
            {
                names.append(", ").append(ModListRejection.more(serverOnly.size() - MAX_SERVER_ONLY));
            }
            text.append('\n').append(ModListRejection.text("spoon.serverList.serverOnly", "Only on the server, may be needed here too: %s", names));
        }
        return text.toString();
    }
}
