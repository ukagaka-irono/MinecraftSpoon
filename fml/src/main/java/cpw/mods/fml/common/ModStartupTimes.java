package cpw.mods.fml.common;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * How long each mod spent in its own loading events, from construction to load complete. Time that belongs to no
 * single mod, such as mod discovery and loading textures, is not counted. When loading ends, Spoon logs the slowest
 * mods, and the mod list shows each mod's time.
 */
public final class ModStartupTimes
{
    /** The loading phases in the order FML runs them. Other events, such as IMC and load complete, count as OTHER. */
    public static enum Phase
    {
        CONSTRUCTION("construction"),
        PREINITIALIZATION("preinit"),
        INITIALIZATION("init"),
        POSTINITIALIZATION("postinit"),
        OTHER("other");

        private final String logName;

        private Phase(String logName)
        {
            this.logName = logName;
        }
    }

    private static final int SLOWEST_LOGGED = 10;
    private static final long NANOS_PER_MILLI = 1000000L;

    // Nanoseconds per phase, by mod id. Mods load on one thread, and the mod list reads this only after loading ends.
    private static final Map<String, long[]> times = Maps.newHashMap();
    private static boolean finished;

    private ModStartupTimes()
    {
    }

    static void record(String modId, FMLEvent event, long nanos)
    {
        if (finished)
        {
            return;
        }
        long[] phases = times.get(modId);
        if (phases == null)
        {
            phases = new long[Phase.values().length];
            times.put(modId, phases);
        }
        phases[phaseOf(event).ordinal()] += nanos;
    }

    private static Phase phaseOf(FMLEvent event)
    {
        if (event instanceof FMLConstructionEvent)
        {
            return Phase.CONSTRUCTION;
        }
        if (event instanceof FMLPreInitializationEvent)
        {
            return Phase.PREINITIALIZATION;
        }
        if (event instanceof FMLInitializationEvent)
        {
            return Phase.INITIALIZATION;
        }
        if (event instanceof FMLPostInitializationEvent)
        {
            return Phase.POSTINITIALIZATION;
        }
        return Phase.OTHER;
    }

    /**
     * Stops counting, so that later events such as those of starting a server are left out, and logs the slowest mods.
     */
    static void finish(List<ModContainer> mods)
    {
        finished = true;
        List<ModContainer> timed = Lists.newArrayList();
        long totalMillis = 0;
        for (ModContainer mod : mods)
        {
            if (has(mod))
            {
                timed.add(mod);
                totalMillis += totalMillis(mod);
            }
        }
        Collections.sort(timed, SLOWEST_FIRST);
        StringBuilder slowest = new StringBuilder();
        for (ModContainer mod : timed.subList(0, Math.min(SLOWEST_LOGGED, timed.size())))
        {
            slowest.append(String.format("\n\t%6d ms  %s (%s)", totalMillis(mod), mod.getName(), mod.getModId()));
            String separator = ": ";
            for (Phase phase : Phase.values())
            {
                long millis = millis(mod, phase);
                if (millis > 0)
                {
                    slowest.append(separator).append(phase.logName).append(' ').append(millis).append(" ms");
                    separator = ", ";
                }
            }
        }
        FMLLog.info("Spoon startup: mods spent %d ms in their own loading events. The slowest:%s", totalMillis, slowest);
    }

    /** Returns whether the mod received any loading event. */
    public static boolean has(ModContainer mod)
    {
        return times.containsKey(mod.getModId());
    }

    public static long millis(ModContainer mod, Phase phase)
    {
        long[] phases = times.get(mod.getModId());
        return phases == null ? 0 : phases[phase.ordinal()] / NANOS_PER_MILLI;
    }

    public static long totalMillis(ModContainer mod)
    {
        long[] phases = times.get(mod.getModId());
        if (phases == null)
        {
            return 0;
        }
        long nanos = 0;
        for (long phase : phases)
        {
            nanos += phase;
        }
        return nanos / NANOS_PER_MILLI;
    }

    /** Orders mods from the slowest to start up, and those that took the same time by name. */
    public static final Comparator<ModContainer> SLOWEST_FIRST = new Comparator<ModContainer>()
    {
        @Override
        public int compare(ModContainer a, ModContainer b)
        {
            long timeA = totalMillis(a);
            long timeB = totalMillis(b);
            if (timeA != timeB)
            {
                return timeA > timeB ? -1 : 1;
            }
            return a.getName().compareToIgnoreCase(b.getName());
        }
    };
}
