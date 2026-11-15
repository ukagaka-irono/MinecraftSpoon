package net.minecraftforge.client;

import java.util.Arrays;
import java.util.List;

import net.minecraft.util.Util;
import net.minecraftforge.client.ime.IMEHandler;
import net.minecraftforge.common.ForgeModContainer;

import org.apache.logging.log4j.Level;
import org.lwjgl.opengl.Display;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import cpw.mods.fml.common.FMLLog;

/**
 * Flashes the taskbar button of the game when loading is done while another window is in front, so a modpack that
 * takes minutes to start can be left alone meanwhile. Windows only.
 */
public final class StartupNotifier
{
    static final int FLASHW_STOP = 0;
    static final int FLASHW_TRAY = 2;
    static final int FLASHW_TIMERNOFG = 12;
    // Without it Windows flashes at the caret blink rate, too fast for the button to look like it flashes
    static final int FLASH_INTERVAL_MILLIS = 1000;

    private interface User32 extends Library
    {
        User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class);

        boolean FlashWindowEx(FlashWindowInfo info);
    }

    /** FLASHWINFO */
    public static class FlashWindowInfo extends Structure
    {
        public int cbSize;
        public Pointer hwnd;
        public int dwFlags;
        public int uCount;
        public int dwTimeout;

        @Override
        protected List<String> getFieldOrder()
        {
            return Arrays.asList("cbSize", "hwnd", "dwFlags", "uCount", "dwTimeout");
        }
    }

    private StartupNotifier() {}

    /**
     * Called by FMLClientHandler when loading is done, on the title screen or on a screen about a loading problem.
     */
    public static void loadingDone()
    {
        if (!ForgeModContainer.startupNotification || Util.getOSType() != Util.EnumOS.WINDOWS || Display.isActive())
        {
            return;
        }
        try
        {
            // Flashes the taskbar button until the game window comes to the front
            flash(IMEHandler.findWindow(), FLASHW_TRAY | FLASHW_TIMERNOFG);
            FMLLog.info("Spoon startup: loading is done, flashed the taskbar button");
        }
        catch (Throwable t)
        {
            FMLLog.log(Level.WARN, t, "Spoon startup: couldn't flash the taskbar button");
        }
    }

    static boolean flash(Pointer hwnd, int flags)
    {
        FlashWindowInfo info = new FlashWindowInfo();
        info.cbSize = info.size();
        info.hwnd = hwnd;
        info.dwFlags = flags;
        info.uCount = 0;
        info.dwTimeout = FLASH_INTERVAL_MILLIS;
        return User32.INSTANCE.FlashWindowEx(info);
    }
}
