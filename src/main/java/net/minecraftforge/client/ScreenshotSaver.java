package net.minecraftforge.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

import org.apache.logging.log4j.Level;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.common.FMLLog;

/**
 * Takes the screenshots of the screenshot key without stopping the game: the screen is read at once, as only the
 * game's thread can, and the PNG file is encoded and written on a thread of its own, as newer Minecraft versions do.
 * The chat message comes once the file is written.
 *
 * ScreenShotHelper.saveScreenshot is left as it is for mods that call it, as they may read the file once it returns.
 */
public final class ScreenshotSaver
{
    // Each waiting screenshot holds its pixels, so beyond this many the key waits for the writer, as it always did
    private static final int MAX_WAITING = 3;

    private static final AtomicInteger waiting = new AtomicInteger();
    private static final ExecutorService writer = Executors.newSingleThreadExecutor(new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable runnable)
        {
            Thread thread = new Thread(runnable, "Spoon screenshot writer");
            thread.setDaemon(true);
            return thread;
        }
    });
    // Only used on the game's thread
    private static IntBuffer pixelBuffer;

    private ScreenshotSaver() {}

    /**
     * Called by Minecraft for the screenshot key, in place of ScreenShotHelper.saveScreenshot.
     */
    public static void takeScreenshot(final Minecraft mc)
    {
        final Date time = new Date();
        final Capture capture;
        try
        {
            capture = capture(mc);
        }
        catch (Exception e)
        {
            FMLLog.log(Level.WARN, e, "Couldn't save screenshot");
            mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentTranslation("screenshot.failure", e.getMessage()));
            return;
        }

        final File dir = new File(mc.mcDataDir, "screenshots");
        waiting.incrementAndGet();
        Future<?> task = writer.submit(new Runnable()
        {
            @Override
            public void run()
            {
                File file = null;
                Exception error = null;
                try
                {
                    file = write(capture, dir, time);
                }
                catch (Exception e)
                {
                    FMLLog.log(Level.WARN, e, "Couldn't save screenshot");
                    error = e;
                }
                finally
                {
                    waiting.decrementAndGet();
                }
                final IChatComponent message = message(file, error);
                // The chat belongs to the game's thread
                mc.func_152344_a(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        mc.ingameGUI.getChatGUI().printChatMessage(message);
                    }
                });
            }
        });
        if (waiting.get() > MAX_WAITING)
        {
            try
            {
                task.get();
            }
            catch (Exception ignored) {}
        }
    }

    /**
     * Reads the screen the way ScreenShotHelper.saveScreenshot does.
     */
    private static Capture capture(Minecraft mc)
    {
        Framebuffer framebuffer = mc.getFramebuffer();
        boolean useFramebuffer = OpenGlHelper.isFramebufferEnabled();
        int textureWidth = useFramebuffer ? framebuffer.framebufferTextureWidth : mc.displayWidth;
        int textureHeight = useFramebuffer ? framebuffer.framebufferTextureHeight : mc.displayHeight;
        int size = textureWidth * textureHeight;
        if (pixelBuffer == null || pixelBuffer.capacity() < size)
        {
            pixelBuffer = BufferUtils.createIntBuffer(size);
        }

        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        pixelBuffer.clear();
        if (useFramebuffer)
        {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, framebuffer.framebufferTexture);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
        }
        else
        {
            GL11.glReadPixels(0, 0, textureWidth, textureHeight, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
        }
        int[] pixels = new int[size];
        pixelBuffer.get(pixels);
        if (useFramebuffer)
        {
            return new Capture(pixels, textureWidth, framebuffer.framebufferWidth, framebuffer.framebufferHeight);
        }
        return new Capture(pixels, textureWidth, textureWidth, textureHeight);
    }

    /**
     * Writes a screenshot to the screenshots folder, named after the time it was taken as ScreenShotHelper names it.
     */
    static File write(Capture capture, File dir, Date time) throws IOException
    {
        BufferedImage image = new BufferedImage(capture.width, capture.height, BufferedImage.TYPE_INT_RGB);
        // OpenGL gives the rows from the bottom up, and a framebuffer texture may be taller than the picture
        for (int y = 0; y < capture.height; y++)
        {
            image.setRGB(0, y, capture.width, 1, capture.pixels, (capture.height - 1 - y) * capture.stride, capture.stride);
        }
        dir.mkdir();
        String name = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(time);
        File file = new File(dir, name + ".png");
        for (int i = 2; file.exists(); i++)
        {
            file = new File(dir, name + "_" + i + ".png");
        }
        ImageIO.write(image, "png", file);
        return file;
    }

    private static IChatComponent message(File file, Exception error)
    {
        if (file == null)
        {
            return new ChatComponentTranslation("screenshot.failure", error.getMessage());
        }
        ChatComponentText name = new ChatComponentText(file.getName());
        name.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));
        name.getChatStyle().setUnderlined(Boolean.TRUE);
        return new ChatComponentTranslation("screenshot.success", name);
    }

    static final class Capture
    {
        final int[] pixels;
        /** The width of a row in the pixels */
        final int stride;
        final int width;
        final int height;

        Capture(int[] pixels, int stride, int width, int height)
        {
            this.pixels = pixels;
            this.stride = stride;
            this.width = width;
            this.height = height;
        }
    }
}
