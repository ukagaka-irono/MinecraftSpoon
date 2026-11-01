package net.minecraftforge.client.ime;

import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraftforge.common.ForgeModContainer;

import org.apache.logging.log4j.Level;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * Keeps a CJK IME from swallowing gameplay keys on Windows, and shows its window at the text being edited.
 *
 * While a Windows IME is turned on, key presses reach the window as VK_PROCESSKEY, so LWJGL
 * never sees WASD and the player cannot move. This handler detaches the IME from the game
 * window while no text input is focused and attaches it again when one is. While text is
 * being typed, it moves the IME composition and candidate windows to the caret.
 */
public class IMEHandler
{
    private static final int IACE_DEFAULT = 0x0010;
    private static final int CFS_POINT = 0x0002;
    private static final int CFS_EXCLUDE = 0x0080;

    private static final Map<Class<?>, Field[]> textFieldCache = Maps.newHashMap();
    private static final Field lineScrollOffset = ReflectionHelper.findField(GuiTextField.class, "lineScrollOffset", "field_146225_q");

    private static final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private static final FloatBuffer projected = BufferUtils.createFloatBuffer(3);

    private static Pointer hwnd;
    private static Boolean attached;
    private static int[] placedCaret;

    // Caret reported while a screen renders its own text, in window pixels from the top left
    private static GuiScreen caretScreen;
    private static int[] reportedCaret;

    private interface Imm32 extends Library
    {
        Imm32 INSTANCE = (Imm32) Native.loadLibrary("imm32", Imm32.class);

        boolean ImmAssociateContextEx(Pointer hWnd, Pointer hIMC, int dwFlags);

        Pointer ImmGetContext(Pointer hWnd);

        boolean ImmReleaseContext(Pointer hWnd, Pointer hIMC);

        boolean ImmSetCompositionWindow(Pointer hIMC, CompositionForm form);

        boolean ImmSetCandidateWindow(Pointer hIMC, CandidateForm form);
    }

    /** COMPOSITIONFORM with its POINT and RECT flattened. */
    public static class CompositionForm extends Structure
    {
        public int dwStyle;
        public int x, y;
        public int left, top, right, bottom;

        @Override
        protected List<String> getFieldOrder()
        {
            return Arrays.asList("dwStyle", "x", "y", "left", "top", "right", "bottom");
        }
    }

    /** CANDIDATEFORM with its POINT and RECT flattened. */
    public static class CandidateForm extends Structure
    {
        public int dwIndex;
        public int dwStyle;
        public int x, y;
        public int left, top, right, bottom;

        @Override
        protected List<String> getFieldOrder()
        {
            return Arrays.asList("dwIndex", "dwStyle", "x", "y", "left", "top", "right", "bottom");
        }
    }

    public static void register()
    {
        if (!System.getProperty("os.name", "").toLowerCase().startsWith("windows"))
        {
            return;
        }
        FMLCommonHandler.instance().bus().register(new IMEHandler());
    }

    /**
     * Reports the caret of the text being drawn, in the current GL coordinates, so the IME
     * windows open next to it. Call it while the open screen renders text it edits itself.
     */
    public static void setCaretFromRender(float x, float top, float bottom)
    {
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        float[] topPoint = project(x, top);
        float[] bottomPoint = project(x, bottom);
        int height = Minecraft.getMinecraft().displayHeight;
        setCaretPosition(Math.round(topPoint[0]),
                height - Math.round(Math.max(topPoint[1], bottomPoint[1])),
                height - Math.round(Math.min(topPoint[1], bottomPoint[1])));
    }

    /**
     * Reports the caret of the text being edited by the open screen, in window pixels from the top left.
     */
    public static void setCaretPosition(int x, int top, int bottom)
    {
        caretScreen = Minecraft.getMinecraft().currentScreen;
        reportedCaret = new int[] { x, top, bottom };
    }

    private static float[] project(float x, float y)
    {
        modelView.rewind();
        projection.rewind();
        viewport.rewind();
        projected.clear();
        GLU.gluProject(x, y, 0.0F, modelView, projection, viewport, projected);
        return new float[] { projected.get(0), projected.get(1) };
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
        {
            return;
        }
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        boolean typing = wantsText(screen);
        boolean wanted = !ForgeModContainer.imeControl || typing;
        try
        {
            if (hwnd == null)
            {
                hwnd = findWindow();
            }
            if (attached == null || attached != wanted)
            {
                Imm32.INSTANCE.ImmAssociateContextEx(hwnd, null, wanted ? IACE_DEFAULT : 0);
                attached = wanted;
                placedCaret = null;
            }
            if (typing)
            {
                placeWindows(caretFor(screen));
            }
        }
        catch (Throwable t)
        {
            FMLLog.log(Level.WARN, t, "Spoon could not control the IME, leaving it as it is");
            FMLCommonHandler.instance().bus().unregister(this);
        }
    }

    private static void placeWindows(int[] caret)
    {
        if (caret == null || Arrays.equals(caret, placedCaret))
        {
            return;
        }
        Pointer himc = Imm32.INSTANCE.ImmGetContext(hwnd);
        if (himc == null)
        {
            return;
        }
        try
        {
            CompositionForm composition = new CompositionForm();
            composition.dwStyle = CFS_POINT;
            composition.x = caret[0];
            composition.y = caret[1];
            Imm32.INSTANCE.ImmSetCompositionWindow(himc, composition);

            CandidateForm candidate = new CandidateForm();
            candidate.dwIndex = 0;
            candidate.dwStyle = CFS_EXCLUDE;
            candidate.x = caret[0];
            candidate.y = caret[2];
            candidate.left = caret[0];
            candidate.top = caret[1];
            candidate.right = caret[0] + 1;
            candidate.bottom = caret[2];
            Imm32.INSTANCE.ImmSetCandidateWindow(himc, candidate);
        }
        finally
        {
            Imm32.INSTANCE.ImmReleaseContext(hwnd, himc);
        }
        placedCaret = caret;
    }

    private static int[] caretFor(GuiScreen screen) throws IllegalAccessException
    {
        if (screen == caretScreen && reportedCaret != null)
        {
            return reportedCaret;
        }
        GuiTextField field = focusedTextField(screen);
        if (field == null)
        {
            return null;
        }
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRenderer;
        String text = field.getText();
        int scroll = Math.min(lineScrollOffset.getInt(field), text.length());
        int cursor = Math.max(scroll, Math.min(field.getCursorPosition(), text.length()));
        boolean background = field.getEnableBackgroundDrawing();
        int x = (background ? field.xPosition + 4 : field.xPosition) + font.getStringWidth(text.substring(scroll, cursor));
        int top = background ? field.yPosition + (field.height - 8) / 2 : field.yPosition;
        int scale = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
        return new int[] { x * scale, top * scale, (top + font.FONT_HEIGHT) * scale };
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
            return focusedTextField(screen) != null;
        }
        catch (IllegalAccessException e)
        {
            return false;
        }
    }

    private static GuiTextField focusedTextField(GuiScreen screen) throws IllegalAccessException
    {
        if (screen == null)
        {
            return null;
        }
        for (Field field : textFields(screen.getClass()))
        {
            Object value = field.get(screen);
            if (value instanceof GuiTextField)
            {
                if (((GuiTextField) value).isFocused())
                {
                    return (GuiTextField) value;
                }
            }
            else if (value instanceof GuiTextField[])
            {
                for (GuiTextField text : (GuiTextField[]) value)
                {
                    if (text != null && text.isFocused())
                    {
                        return text;
                    }
                }
            }
        }
        return null;
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
