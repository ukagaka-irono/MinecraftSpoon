package net.minecraftforge.logging;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

/**
 * Lists the mods whose code appears in a crash at the head of the crash report, like Forge's CrashReportAnalyser.
 *
 * Forge 1.7.10 has no module per mod, so each class in the stacktrace is mapped to the jar it was loaded from, and a
 * method that a mixin merged into a class is mapped to the jar of that mixin.
 */
public final class CrashReportAnalyser
{
    private static final String MIXIN_MERGED = "org.spongepowered.asm.mixin.transformer.meta.MixinMerged";
    // Minecraft, FML and Forge (Spoon itself) are in every stacktrace, so Forge leaves them out too.
    private static final List<String> IGNORED_MODS = Arrays.asList("mcp", "Minecraft", "FML", "Forge");

    private CrashReportAnalyser() {}

    /**
     * Returns the "Suspected Mods" lines for the head of a crash report, or nothing while the mods are not known yet.
     */
    public static String appendSuspectedMods(Throwable throwable, StackTraceElement[] uncategorizedStackTrace)
    {
        try
        {
            List<ModContainer> mods = Loader.instance().getModList();
            if (mods.isEmpty())
            {
                return "";
            }
            ListMultimap<File, ModContainer> modsBySource = cacheModList(mods);
            Map<ModContainer, String> suspected = new LinkedHashMap<ModContainer, String>();
            Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
            for (Throwable t = throwable; t != null && seen.add(t); t = t.getCause())
            {
                scanStacktrace(t.getStackTrace(), modsBySource, suspected);
            }
            if (uncategorizedStackTrace != null)
            {
                scanStacktrace(uncategorizedStackTrace, modsBySource, suspected);
            }
            return buildSuspectedModsSection(suspected);
        }
        catch (Throwable t)
        {
            FMLLog.log(Level.ERROR, t, "Failed to append suspected mod(s) to crash report!");
            return "";
        }
    }

    private static ListMultimap<File, ModContainer> cacheModList(List<ModContainer> mods)
    {
        File spoon = sourceOfClass(Loader.class.getName());
        ListMultimap<File, ModContainer> modsBySource = ArrayListMultimap.create();
        for (ModContainer mod : mods)
        {
            if (IGNORED_MODS.contains(mod.getModId()))
            {
                continue;
            }
            File source = mod.getSource() != null ? canonical(mod.getSource()) : sourceOfClass(mod.getClass().getName());
            // Coremod containers have no source, so their own class tells where they came from, unless it is Spoon's
            if (source != null && !source.equals(spoon))
            {
                modsBySource.put(source, mod);
            }
        }
        return modsBySource;
    }

    private static void scanStacktrace(StackTraceElement[] stackTrace, ListMultimap<File, ModContainer> modsBySource, Map<ModContainer, String> suspected)
    {
        for (StackTraceElement element : stackTrace)
        {
            blameIfPresent(element.getClassName(), modsBySource, suspected, "at " + element);
            String mixin = findMixin(element);
            if (mixin != null)
            {
                blameIfPresent(mixin, modsBySource, suspected, "Mixin class: " + mixin + "\n\t\tTarget: " + element.getClassName() + "\n\t\tat " + element);
            }
        }
    }

    private static void blameIfPresent(String className, ListMultimap<File, ModContainer> modsBySource, Map<ModContainer, String> suspected, String position)
    {
        File source = sourceOfClass(className);
        if (source == null)
        {
            return;
        }
        ModContainer mod = closestMod(className, modsBySource.get(source));
        if (mod != null && !suspected.containsKey(mod))
        {
            suspected.put(mod, position);
        }
    }

    /**
     * Picks the mod whose main class shares the longest package with the class, for jars that hold several mods.
     */
    private static ModContainer closestMod(String className, List<ModContainer> mods)
    {
        ModContainer closest = null;
        int closestLength = -1;
        for (ModContainer mod : mods)
        {
            Object instance = null;
            try
            {
                instance = mod.getMod();
            }
            catch (Throwable ignored) {}
            String modClass = (instance != null ? instance : mod).getClass().getName();
            int length = 0;
            while (length < className.length() && length < modClass.length() && className.charAt(length) == modClass.charAt(length))
            {
                length++;
            }
            if (length > closestLength)
            {
                closest = mod;
                closestLength = length;
            }
        }
        return closest;
    }

    private static String buildSuspectedModsSection(Map<ModContainer, String> suspected)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Suspected Mod");
        builder.append(suspected.size() == 1 ? ": " : "s: ");
        if (suspected.isEmpty())
        {
            return builder.append("NONE\n").toString();
        }
        for (Map.Entry<ModContainer, String> entry : suspected.entrySet())
        {
            ModContainer mod = entry.getKey();
            builder.append("\n\t").append(mod.getName()).append(" (").append(mod.getModId()).append("), Version: ").append(mod.getDisplayVersion());
            // mcmod.info of 1.7.10 has no issue tracker, so this is the closest place to report
            String url = mod.getMetadata() != null ? mod.getMetadata().url : null;
            if (!Strings.isNullOrEmpty(url))
            {
                builder.append("\n\t\tWebsite: ").append(url);
            }
            builder.append("\n\t\t").append(entry.getValue()).append("\n");
        }
        return builder.toString();
    }

    /**
     * Returns the mixin that merged the method of this stacktrace element into its class, if any.
     */
    private static String findMixin(StackTraceElement element)
    {
        try
        {
            Class<?> clazz = Class.forName(element.getClassName(), false, CrashReportAnalyser.class.getClassLoader());
            for (Method method : clazz.getDeclaredMethods())
            {
                if (!method.getName().equals(element.getMethodName()))
                {
                    continue;
                }
                for (Annotation annotation : method.getDeclaredAnnotations())
                {
                    if (annotation.annotationType().getName().equals(MIXIN_MERGED))
                    {
                        return (String)annotation.annotationType().getMethod("mixin").invoke(annotation);
                    }
                }
            }
        }
        catch (Throwable ignored) {}
        return null;
    }

    /**
     * Returns the jar or classpath folder a class is loaded from, without loading it.
     */
    private static File sourceOfClass(String className)
    {
        int lambda = className.indexOf("$$Lambda$");
        String name = lambda < 0 ? className : className.substring(0, lambda);
        if (name.indexOf('.') < 0)
        {
            // Obfuscated Minecraft classes are in the default package
            return null;
        }
        String resource = name.replace('.', '/') + ".class";
        URL url = CrashReportAnalyser.class.getClassLoader().getResource(resource);
        return url != null ? rootOf(url, resource) : null;
    }

    private static File rootOf(URL url, String resource)
    {
        try
        {
            if ("jar".equals(url.getProtocol()))
            {
                // jar:file:/path/to/mod.jar!/package/Class.class
                String path = url.getPath();
                int separator = path.indexOf("!/");
                return toFile(new URL(separator < 0 ? path : path.substring(0, separator)));
            }
            if ("file".equals(url.getProtocol()))
            {
                // A class in a classpath folder, or the jar itself as a code source
                File file = toFile(url);
                String path = file.getPath().replace(File.separatorChar, '/');
                return path.endsWith("/" + resource) ? canonical(new File(path.substring(0, path.length() - resource.length() - 1))) : file;
            }
        }
        catch (IOException e) {}
        return null;
    }

    private static File toFile(URL url) throws IOException
    {
        try
        {
            return canonical(new File(url.toURI()));
        }
        catch (Exception e)
        {
            // Some URLs keep spaces unescaped, which URI rejects
            return canonical(new File(URLDecoder.decode(url.getPath().replace("+", "%2B"), "UTF-8")));
        }
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
