package net.minecraftforge.client;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.ForgeModContainer;

import org.apache.logging.log4j.Level;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;

/**
 * Warns on the title screen when the memory given to Java looks too small for the mods loaded. The official launcher
 * gives 1 or 2 GB, which a big modpack runs out of, and a game that runs out of memory stalls or crashes without
 * saying why.
 *
 * The guide is 1 GB plus 12 MB per mod: about 2 GB for 90 mods and 4.3 GB for 280, which the modpacks Spoon is tested
 * with use comfortably.
 */
public final class MemoryCheck
{
    static final long BASE_MEGABYTES = 1024;
    static final long MEGABYTES_PER_MOD = 12;
    private static final long MEGABYTE = 1024L * 1024L;

    private static boolean low;
    private static long allocatedMegabytes;
    private static long suggestedMegabytes;
    private static int mods;

    private MemoryCheck() {}

    /**
     * Called by FMLClientHandler when loading is done, once the mods are known.
     */
    public static void loadingDone()
    {
        try
        {
            mods = Loader.instance().getActiveModList().size();
            allocatedMegabytes = allocatedMegabytes(ManagementFactory.getRuntimeMXBean().getInputArguments(), Runtime.getRuntime().maxMemory());
            suggestedMegabytes = suggestedMegabytes(mods);
            if (allocatedMegabytes < 0)
            {
                FMLLog.info("Spoon memory check: no memory limit known, %d mods", mods);
                return;
            }
            low = allocatedMegabytes < suggestedMegabytes;
            String text = String.format("Spoon memory check: %d MB allocated, %d MB or more suggested for %d mods", allocatedMegabytes, suggestedMegabytes, mods);
            FMLLog.log(low ? Level.WARN : Level.INFO, low ? text + ", which may not be enough" : text);
        }
        catch (Throwable t)
        {
            FMLLog.log(Level.WARN, t, "Spoon memory check: couldn't check the memory");
        }
    }

    /**
     * Returns the lines to show on the title screen, or none when the memory looks enough.
     */
    public static String[] titleScreenLines()
    {
        if (!low || !ForgeModContainer.memoryWarning)
        {
            return new String[0];
        }
        return new String[] {
            EnumChatFormatting.YELLOW + I18n.format("spoon.memory.low.1"),
            I18n.format("spoon.memory.low.2", formatGigabytes(allocatedMegabytes), mods, formatGigabytes(roundUpToHalfGigabyte(suggestedMegabytes)))
        };
    }

    static long suggestedMegabytes(int mods)
    {
        return BASE_MEGABYTES + MEGABYTES_PER_MOD * mods;
    }

    /**
     * Returns the -Xmx the game was started with, in megabytes, or else what Java reports, which is a little less than
     * -Xmx, or -1 when there is no limit.
     */
    static long allocatedMegabytes(List<String> jvmArguments, long maxMemory)
    {
        long xmx = -1;
        for (String argument : jvmArguments)
        {
            if (argument.startsWith("-Xmx"))
            {
                // The last -Xmx is the one Java uses
                long bytes = parseSize(argument.substring(4));
                if (bytes > 0)
                {
                    xmx = bytes;
                }
            }
        }
        if (xmx > 0)
        {
            return xmx / MEGABYTE;
        }
        return maxMemory == Long.MAX_VALUE ? -1 : maxMemory / MEGABYTE;
    }

    static long parseSize(String size)
    {
        if (size.isEmpty())
        {
            return -1;
        }
        char unit = Character.toLowerCase(size.charAt(size.length() - 1));
        long multiplier = unit == 'k' ? 1024L : unit == 'm' ? MEGABYTE : unit == 'g' ? 1024L * MEGABYTE : unit == 't' ? 1024L * 1024L * MEGABYTE : 1;
        String number = multiplier == 1 && Character.isDigit(unit) ? size : size.substring(0, size.length() - 1);
        try
        {
            return Long.parseLong(number) * multiplier;
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    static long roundUpToHalfGigabyte(long megabytes)
    {
        return (megabytes + 511) / 512 * 512;
    }

    static String formatGigabytes(long megabytes)
    {
        return String.format(Locale.ROOT, "%.1f GB", megabytes / 1024.0);
    }
}
