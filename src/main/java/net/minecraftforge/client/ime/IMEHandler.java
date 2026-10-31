package net.minecraftforge.client.ime;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraftforge.common.ForgeModContainer;

import org.apache.logging.log4j.Level;
import org.lwjgl.opengl.Display;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Keeps a CJK IME from swallowing gameplay keys on Windows.
 *
 * While a Windows IME is turned on, key presses reach the window as VK_PROCESSKEY, so LWJGL
 * never sees WASD and the player cannot move. This handler detaches the IME from the game
 * window while no text input is focused and attaches it again when one is.
 */
public class IMEHandler
{
    private static final int IACE_DEFAULT = 0x0010;

    private static final Map<Class<?>, Field[]> textFieldCache = Maps.newHashMap();

    private static Pointer hwnd;
    private static Boolean attached;

    private interface Imm32 extends Library
    {
        Imm32 INSTANCE = (Imm32) Native.loadLibrary("imm32", Imm32.class);

        boolean ImmAssociateContextEx(Pointer hWnd, Pointer hIMC, int dwFlags);
    }

    public static void register()
    {
        if (!System.getProperty("os.name", "").toLowerCase().startsWith("windows"))
        {
            return;
        }
        FMLCommonHandler.instance().bus().register(new IMEHandler());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }
        boolean wanted = !ForgeModContainer.imeControl || wantsText(Minecraft.getMinecraft().currentScreen);
        if (attached != null && attached == wanted)
        {
            return;
        }
        try
        {
            if (hwnd == null)
            {
                hwnd = findWindow();
            }
            Imm32.INSTANCE.ImmAssociateContextEx(hwnd, null, wanted ? IACE_DEFAULT : 0);
            attached = wanted;
        }
        catch (Throwable t)
        {
            FMLLog.log(Level.WARN, t, "Spoon could not control the IME, leaving it as it is");
            FMLCommonHandler.instance().bus().unregister(this);
        }
    }

    private static Pointer findWindow() throws Exception
    {
        Field implField = Display.class.getDeclaredField("display_impl");
        implField.setAccessible(true);
        Object impl = implField.get(null);
        Field hwndField = impl.getClass().getDeclaredField("hwnd");
        hwndField.setAccessible(true);
        long handle = hwndField.getLong(impl);
        if (handle == 0)
        {
            throw new IllegalStateException("The game window has no HWND yet");
        }
        return new Pointer(handle);
    }

    private static boolean wantsText(GuiScreen screen)
    {
        if (screen == null)
        {
            return false;
        }
        if (screen instanceof IIMETextInput)
        {
            return ((IIMETextInput) screen).wantsIME();
        }
        if (screen instanceof GuiChat || screen instanceof GuiEditSign || screen instanceof GuiScreenBook)
        {
            return true;
        }
        try
        {
            for (Field field : textFields(screen.getClass()))
            {
                Object value = field.get(screen);
                if (value instanceof GuiTextField)
                {
                    if (((GuiTextField) value).isFocused())
                    {
                        return true;
                    }
                }
                else if (value instanceof GuiTextField[])
                {
                    for (GuiTextField text : (GuiTextField[]) value)
                    {
                        if (text != null && text.isFocused())
                        {
                            return true;
                        }
                    }
                }
            }
        }
        catch (IllegalAccessException e)
        {
            return false;
        }
        return false;
    }

    private static Field[] textFields(Class<?> cls)
    {
        Field[] fields = textFieldCache.get(cls);
        if (fields == null)
        {
            List<Field> found = Lists.newArrayList();
            for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass())
            {
                for (Field field : c.getDeclaredFields())
                {
                    Class<?> type = field.getType();
                    if (GuiTextField.class.isAssignableFrom(type) || GuiTextField[].class.isAssignableFrom(type))
                    {
                        field.setAccessible(true);
                        found.add(field);
                    }
                }
            }
            fields = found.toArray(new Field[found.size()]);
            textFieldCache.put(cls, fields);
        }
        return fields;
    }
}
