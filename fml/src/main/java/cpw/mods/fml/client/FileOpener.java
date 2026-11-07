package cpw.mods.fml.client;

import java.io.File;
import java.net.URI;

import net.minecraft.util.Util;

import org.apache.logging.log4j.Level;
import org.lwjgl.Sys;

import cpw.mods.fml.common.FMLLog;

/**
 * Opens files and folders from the game's own screens, such as the mods folder or the folder of world backups.
 */
public final class FileOpener
{
    private FileOpener() {}

    /**
     * Opens a folder in the file manager, or a file in the program the system opens it with.
     */
    public static void open(File file)
    {
        String path = file.getAbsolutePath();
        try
        {
            if (Util.getOSType() == Util.EnumOS.WINDOWS)
            {
                Runtime.getRuntime().exec(String.format("cmd.exe /C start \"Open file\" \"%s\"", path));
                return;
            }
            if (Util.getOSType() == Util.EnumOS.OSX)
            {
                // -t opens a file in the text editor, because config files usually have no program of their own
                Runtime.getRuntime().exec(file.isDirectory() ? new String[] {"/usr/bin/open", path} : new String[] {"/usr/bin/open", "-t", path});
                return;
            }
            Class<?> desktop = Class.forName("java.awt.Desktop");
            Object instance = desktop.getMethod("getDesktop").invoke(null);
            if (file.isDirectory())
            {
                desktop.getMethod("browse", URI.class).invoke(instance, file.toURI());
            }
            else
            {
                desktop.getMethod("open", File.class).invoke(instance, file);
            }
        }
        catch (Throwable t)
        {
            FMLLog.log(Level.WARN, t, "Couldn't open %s, trying the system browser", path);
            Sys.openURL("file://" + path);
        }
    }
}
