package cpw.mods.fml.common;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Level;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;

/**
 * Remembers the config files each mod opens, so the mod list can open them for mods that have no config screen.
 *
 * Forge's Configuration reports every file it opens, and the file belongs to the mod that is being loaded at that
 * moment. Files opened outside of mod loading, such as by coremods, are not attributed to any mod.
 */
public class ModConfigFiles
{
    private static final SetMultimap<String, File> files = LinkedHashMultimap.create();

    public static synchronized void record(File file)
    {
        try
        {
            ModContainer mod = Loader.activeModContainerIfLoading();
            if (mod != null && file != null)
            {
                files.put(mod.getModId(), canonical(file));
            }
        }
        catch (Throwable t)
        {
            // Never let this stop a mod from loading its config
            FMLLog.log(Level.DEBUG, t, "Couldn't tell which mod opened the config file %s", file);
        }
    }

    /**
     * Returns the config files of the mod that exist now, in the order the mod opened them.
     */
    public static synchronized List<File> get(ModContainer mod)
    {
        List<File> existing = Lists.newArrayList();
        for (File file : files.get(mod.getModId()))
        {
            if (file.isFile())
            {
                existing.add(file);
            }
        }
        return existing;
    }

    private static File canonical(File file)
    {
        try
        {
            return file.getCanonicalFile();
        }
        catch (IOException e)
        {
            return file.getAbsoluteFile();
        }
    }
}
